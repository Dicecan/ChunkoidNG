package com.noches.chunkoidng.maid.defense

import com.noches.chunkoidng.maid.audit.SecurityAuditor
import com.noches.chunkoidng.maid.model.PerformanceLimits
import com.noches.chunkoidng.maid.model.ViolationType
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ArchiveShield(
    private val pathSanitizer: PathSanitizer,
    private val securityAuditor: SecurityAuditor
) {

    data class ExtractSummary(
        val entryCount: Int,
        val totalBytes: Long,
        val destinationDir: File
    )

    fun extractZipSecurely(
        inputStream: InputStream,
        destDir: File,
        limits: PerformanceLimits,
        module: String = "ArchiveShield",
        bufferSize: Int = 64 * 1024,
        shouldContinue: () -> Boolean = { true },
        onProgress: ((entryCount: Int, totalBytes: Long) -> Unit)? = null
    ): ExtractSummary {
        destDir.mkdirs()
        val buffer = ByteArray(bufferSize)
        var entryCount = 0
        var totalBytes = 0L

        ZipInputStream(inputStream).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (!shouldContinue()) {
                    zis.closeEntry()
                    throw java.util.concurrent.CancellationException("Archive extraction cancelled by user")
                }

                entryCount++
                if (entryCount > limits.archiveMaxEntries) {
                    securityAuditor.recordBlock(
                        type = ViolationType.ZIP_ENTRY_COUNT_LIMIT,
                        module = module,
                        message = "Archive entry count exceeded safety limit (${limits.archiveMaxEntries}); potential zip bomb",
                        requestedValue = entryCount.toLong()
                    )
                    throw IllegalArgumentException("Archive entry limit exceeded: max allowed is ${limits.archiveMaxEntries}")
                }

                val targetFile = pathSanitizer.resolveUnderBase(destDir, entry.name, module)

                if (entry.isDirectory) {
                    targetFile.mkdirs()
                } else {
                    if (targetFile.canonicalFile == destDir.canonicalFile) {
                        securityAuditor.recordBlock(
                            type = ViolationType.ZIP_SLIP,
                            module = module,
                            message = "Blocked invalid archive entry targeting base directory",
                            pathOrTarget = entry.name
                        )
                        throw SecurityException("Invalid zip file entry targeting base directory: ${entry.name}")
                    }
                    targetFile.parentFile?.mkdirs()
                    if (java.nio.file.Files.isSymbolicLink(targetFile.toPath())) {
                        securityAuditor.recordBlock(
                            type = ViolationType.ZIP_SLIP,
                            module = module,
                            message = "Blocked archive extraction onto symbolic link target",
                            pathOrTarget = entry.name
                        )
                        throw SecurityException("Security violation: symlink target refused: ${entry.name}")
                    }
                    var entryBytes = 0L
                    var entryCompleted = false
                    try {
                        FileOutputStream(targetFile).buffered(bufferSize).use { fos ->
                            var len: Int
                            while (zis.read(buffer).also { len = it } != -1) {
                                if (!shouldContinue()) {
                                    throw java.util.concurrent.CancellationException("Archive extraction cancelled by user")
                                }

                                entryBytes += len
                                totalBytes += len

                                if (entryBytes > limits.archiveMaxEntryBytes) {
                                    securityAuditor.recordBlock(
                                        type = ViolationType.ZIP_BOMB_ENTRY_SIZE,
                                        module = module,
                                        message = "Single entry extracted size exceeded quota (${limits.archiveMaxEntryBytes} bytes)",
                                        pathOrTarget = entry.name,
                                        requestedValue = entryBytes
                                    )
                                    throw IllegalArgumentException("Single entry exceeded maximum allowed size: ${entry.name}")
                                }

                                if (totalBytes > limits.archiveMaxTotalBytes) {
                                    securityAuditor.recordBlock(
                                        type = ViolationType.ZIP_BOMB_TOTAL_SIZE,
                                        module = module,
                                        message = "Total archive extracted size exceeded quota (${limits.archiveMaxTotalBytes} bytes); potential zip bomb",
                                        requestedValue = totalBytes
                                    )
                                    throw IllegalArgumentException("Total extracted size exceeded quota: $totalBytes bytes")
                                }

                                fos.write(buffer, 0, len)
                            }
                        }
                        entryCompleted = true
                    } finally {
                        if (!entryCompleted && targetFile.exists() && targetFile.isFile) {
                            targetFile.delete()
                        }
                    }
                }

                zis.closeEntry()
                onProgress?.invoke(entryCount, totalBytes)
                entry = zis.nextEntry
            }
        }

        return ExtractSummary(entryCount, totalBytes, destDir)
    }
}
