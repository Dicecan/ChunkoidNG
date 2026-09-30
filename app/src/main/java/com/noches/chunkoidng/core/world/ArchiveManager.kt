package com.noches.chunkoidng.core.world

import android.content.ContentResolver
import android.content.Context
import com.noches.chunkoidng.R
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.coroutines.coroutineContext

class ArchiveManager(private val context: Context) {
    private companion object {
        const val MAX_ARCHIVE_ENTRIES = 100_000
        const val MAX_ENTRY_BYTES = 512L * 1024 * 1024
        const val MAX_TOTAL_BYTES = 2L * 1024 * 1024 * 1024
    }
    private val contentResolver: ContentResolver get() = context.contentResolver

    val workspaceDir: File get() = File(context.filesDir, "workspace").apply { mkdirs() }
    val inputDir: File get() = File(workspaceDir, "input")
    val outputDir: File get() = File(workspaceDir, "output")

    fun cleanWorkspace() {
        if (inputDir.exists()) inputDir.deleteRecursively()
        if (outputDir.exists()) outputDir.deleteRecursively()
    }

    fun getUriDisplayName(uri: Uri): String {
        try {
            if (uri.scheme == "content") {
                contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            val name = cursor.getString(nameIndex)
                            if (!name.isNullOrBlank()) return name
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { "Minecraft_World" } ?: "Minecraft_World"
    }

    fun isArchiveUri(uri: Uri): Boolean {
        val name = getUriDisplayName(uri).lowercase()
        return name.endsWith(".zip") || name.endsWith(".mcworld") ||
                uri.path?.lowercase()?.endsWith(".zip") == true ||
                uri.path?.lowercase()?.endsWith(".mcworld") == true
    }

    suspend fun extractArchive(
        archiveUri: Uri,
        onProgress: (progress: Int, status: String) -> Unit
    ): Result<WorldInfo> = withContext(Dispatchers.IO) {
        try {
            if (inputDir.exists()) inputDir.deleteRecursively()
            inputDir.mkdirs()

            onProgress(5, context.getString(R.string.archive_staging_extracting))

            val inputStream = contentResolver.openInputStream(archiveUri)
                ?: return@withContext Result.failure(Exception(context.getString(R.string.archive_error_open_input)))

            val buffer = ByteArray(64 * 1024)
            var extractedCount = 0
            var totalExtractedBytes = 0L
            val inputCanonicalPath = inputDir.canonicalPath + File.separator

            inputStream.use { rawIn ->
                ZipInputStream(rawIn).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        if (!coroutineContext.isActive) {
                            zis.closeEntry()
                            return@withContext Result.failure(Exception(context.getString(R.string.archive_error_cancelled)))
                        }

                        if (++extractedCount > MAX_ARCHIVE_ENTRIES) {
                            throw IllegalArgumentException(context.getString(R.string.archive_error_entry_limit))
                        }
                        val entryName = entry.name
                        val entryFile = File(inputDir, entryName)
                        val canonicalEntry = entryFile.canonicalFile
                        if (canonicalEntry.path != inputDir.canonicalPath &&
                            !canonicalEntry.path.startsWith(inputCanonicalPath)) {
                            throw SecurityException(context.getString(R.string.archive_error_invalid_path, entryName))
                        }

                        if (entry.isDirectory) {
                            entryFile.mkdirs()
                        } else {
                            entryFile.parentFile?.mkdirs()
                            var entryBytes = 0L
                            FileOutputStream(canonicalEntry).buffered(64 * 1024).use { out ->
                                var len: Int
                                while (zis.read(buffer).also { len = it } != -1) {
                                    entryBytes += len
                                    totalExtractedBytes += len
                                    if (entryBytes > MAX_ENTRY_BYTES || totalExtractedBytes > MAX_TOTAL_BYTES) {
                                        throw IllegalArgumentException(context.getString(R.string.archive_error_size_limit))
                                    }
                                    out.write(buffer, 0, len)
                                }
                            }
                        }
                        zis.closeEntry()
                        if (extractedCount % 15 == 0) {
                            val pct = (5 + (extractedCount / 5)).coerceIn(5, 75)
                            onProgress(pct, context.getString(R.string.archive_staging_extracting_progress, extractedCount))
                        }
                        entry = zis.nextEntry
                    }
                }
            }

            onProgress(80, context.getString(R.string.archive_staging_searching_structure))
            val levelDat = findFileRecursive(inputDir, "level.dat")
                ?: return@withContext Result.failure(Exception(context.getString(R.string.archive_error_no_level_dat)))

            val actualWorldDir = levelDat.parentFile ?: inputDir
            if (actualWorldDir.canonicalPath != inputDir.canonicalPath) {
                onProgress(85, context.getString(R.string.archive_staging_organizing))
                moveDirectoryContents(actualWorldDir, inputDir)
            }

            onProgress(95, context.getString(R.string.archive_staging_parsing_metadata))
            val rawName = getUriDisplayName(archiveUri).removeSuffix(".mcworld").removeSuffix(".zip")
            val worldInfo = WorldMetadataReader.inspectWorld(inputDir, fallbackName = rawName).copy(
                sourceUri = archiveUri,
                isArchive = true
            )

            onProgress(100, context.getString(R.string.archive_staging_ready))
            Result.success(worldInfo)
        } catch (e: Exception) {
            if (inputDir.exists()) inputDir.deleteRecursively()
            Result.failure(e)
        }
    }

    suspend fun stageTreeUri(
        treeUri: Uri,
        onProgress: (progress: Int, status: String) -> Unit
    ): Result<WorldInfo> = withContext(Dispatchers.IO) {
        try {
            if (inputDir.exists()) inputDir.deleteRecursively()
            inputDir.mkdirs()

            onProgress(5, context.getString(R.string.archive_staging_reading_dir))
            val treeDoc = DocumentFile.fromTreeUri(context, treeUri)
                ?: return@withContext Result.failure(Exception(context.getString(R.string.archive_error_access_dir)))

            if (!treeDoc.isDirectory) {
                return@withContext Result.failure(Exception(context.getString(R.string.archive_error_not_a_dir)))
            }

            var copiedCount = 0
            val copyResult = copyDocumentDirectory(treeDoc, inputDir) { count ->
                copiedCount = count
                val pct = (10 + count / 2).coerceIn(10, 85)
                onProgress(pct, context.getString(R.string.archive_staging_copying, copiedCount))
            }
            if (copyResult.failedFiles.isNotEmpty()) {
                return@withContext Result.failure(Exception(context.getString(R.string.archive_error_export_files, copyResult.failedFiles.take(3).joinToString())))
            }

            onProgress(90, context.getString(R.string.archive_staging_verifying))
            val levelDat = findFileRecursive(inputDir, "level.dat")
                ?: return@withContext Result.failure(Exception(context.getString(R.string.archive_error_no_level_dat_dir)))

            val actualWorldDir = levelDat.parentFile ?: inputDir
            if (actualWorldDir.canonicalPath != inputDir.canonicalPath) {
                onProgress(93, context.getString(R.string.archive_staging_organizing))
                moveDirectoryContents(actualWorldDir, inputDir)
            }

            onProgress(97, context.getString(R.string.archive_staging_parsing_metadata))
            val folderName = treeDoc.name ?: "Minecraft_World"
            val worldInfo = WorldMetadataReader.inspectWorld(inputDir, fallbackName = folderName).copy(
                sourceUri = treeUri,
                isArchive = false
            )

            onProgress(100, context.getString(R.string.archive_staging_ready))
            Result.success(worldInfo)
        } catch (e: Exception) {
            if (inputDir.exists()) inputDir.deleteRecursively()
            Result.failure(e)
        }
    }

    private data class CopyStats(val copiedFiles: Int, val failedFiles: List<String>)

    private suspend fun copyDocumentDirectory(
        source: DocumentFile,
        destDir: File,
        fileCounter: (Int) -> Unit
    ): CopyStats {
        var totalCopied = 0
        val failures = mutableListOf<String>()
        suspend fun recursiveCopy(currSource: DocumentFile, currDest: File) {
            currDest.mkdirs()
            for (child in currSource.listFiles()) {
                if (!coroutineContext.isActive) throw java.util.concurrent.CancellationException(context.getString(R.string.archive_error_cancelled))
                val childName = child.name ?: "unnamed"
                val destChild = File(currDest, childName)
                if (child.isDirectory) {
                    recursiveCopy(child, destChild)
                } else if (child.isFile) {
                    try {
                        val input = contentResolver.openInputStream(child.uri)
                            ?: throw java.io.IOException(context.getString(R.string.archive_error_open_stream))
                        input.use { inStream ->
                            FileOutputStream(destChild).buffered(64 * 1024).use { outStream ->
                                inStream.copyTo(outStream, 64 * 1024)
                            }
                        }
                        totalCopied++
                        if (totalCopied % 10 == 0) fileCounter(totalCopied)
                    } catch (e: Exception) {
                        destChild.delete()
                        failures += child.uri.toString()
                    }
                }
            }
        }
        recursiveCopy(source, destDir)
        fileCounter(totalCopied)
        return CopyStats(totalCopied, failures)
    }

    private fun findFileRecursive(dir: File, targetName: String): File? {
        val files = dir.listFiles() ?: return null
        for (file in files) {
            if (file.isDirectory) {
                val found = findFileRecursive(file, targetName)
                if (found != null) return found
            } else if (file.name.equals(targetName, ignoreCase = true)) {
                return file
            }
        }
        return null
    }

    private fun moveDirectoryContents(sourceDir: File, destDir: File) {
        val files = sourceDir.listFiles() ?: return
        for (file in files) {
            val target = File(destDir, file.name)
            if (target.exists()) target.deleteRecursively()
            file.renameTo(target)
        }
        deleteEmptyDirs(sourceDir)
    }

    private fun deleteEmptyDirs(dir: File) {
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                deleteEmptyDirs(file)
            }
        }
        if (dir.listFiles()?.isEmpty() == true) {
            dir.delete()
        }
    }

    suspend fun exportToUri(
        targetTreeUri: Uri,
        worldName: String,
        packAsArchive: Boolean = false,
        isBedrock: Boolean = false
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            if (!outputDir.exists() || outputDir.listFiles()?.isEmpty() == true) {
                return@withContext Result.failure(Exception(context.getString(R.string.archive_error_output_empty)))
            }

            val treeDoc = DocumentFile.fromTreeUri(context, targetTreeUri)
                ?: return@withContext Result.failure(Exception(context.getString(R.string.archive_error_access_dir)))

            val safeName = worldName.replace(Regex("[^\\p{L}\\p{N}_\\- ]"), "_").trim().ifBlank { "ConvertedWorld" }

            if (packAsArchive) {
                val extension = if (isBedrock) ".mcworld" else ".zip"
                val docFile = treeDoc.createFile("application/zip", ".${safeName}.${System.currentTimeMillis()}.tmp")
                    ?: return@withContext Result.failure(Exception(context.getString(R.string.archive_error_create_archive)))

                context.contentResolver.openOutputStream(docFile.uri)?.use { out ->
                    ZipOutputStream(BufferedOutputStream(out, 64 * 1024)).use { zos ->
                        zipDirectory(outputDir, outputDir, zos)
                    }
                } ?: return@withContext Result.failure(Exception(context.getString(R.string.archive_error_open_stream)))
                if (!docFile.renameTo("$safeName$extension")) {
                    docFile.delete()
                    return@withContext Result.failure(Exception(context.getString(R.string.archive_error_finish_export)))
                }
                Result.success(docFile.uri)
            } else {
                val destDirDoc = treeDoc.createDirectory(safeName)
                    ?: return@withContext Result.failure(Exception(context.getString(R.string.archive_error_create_folder)))

                val failures = copyDirToDocumentFile(outputDir, destDirDoc)
                if (failures.isNotEmpty()) {
                    destDirDoc.delete()
                    return@withContext Result.failure(Exception(context.getString(R.string.archive_error_export_files, failures.take(3).joinToString())))
                }
                Result.success(destDirDoc.uri)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun copyDirToDocumentFile(sourceDir: File, destDirDoc: DocumentFile): List<String> {
        val failures = mutableListOf<String>()
        val files = sourceDir.listFiles() ?: return listOf(sourceDir.absolutePath)
        files.forEach { file ->
            if (file.isDirectory) {
                val newDir = destDirDoc.createDirectory(file.name)
                if (newDir != null) {
                    failures += copyDirToDocumentFile(file, newDir)
                } else failures += file.absolutePath
            } else {
                val newFile = destDirDoc.createFile("application/octet-stream", file.name)
                try {
                    if (newFile == null) throw java.io.IOException(context.getString(R.string.archive_error_create_target))
                    val out = context.contentResolver.openOutputStream(newFile.uri)
                        ?: throw java.io.IOException(context.getString(R.string.archive_error_open_output))
                    out.buffered(64 * 1024).use { buffered ->
                        file.inputStream().use { input -> input.copyTo(buffered, 64 * 1024) }
                    }
                } catch (e: Exception) { failures += file.absolutePath }
            }
        }
        return failures
    }

    private fun zipDirectory(rootDir: File, currentDir: File, zos: ZipOutputStream) {
        val files = currentDir.listFiles() ?: return
        val buffer = ByteArray(64 * 1024)
        for (file in files) {
            if (file.isDirectory) {
                val relPath = file.relativeTo(rootDir).path.replace('\\', '/') + "/"
                zos.putNextEntry(ZipEntry(relPath))
                zos.closeEntry()
                zipDirectory(rootDir, file, zos)
            } else {
                val relPath = file.relativeTo(rootDir).path.replace('\\', '/')
                zos.putNextEntry(ZipEntry(relPath))
                file.inputStream().use { input ->
                    var len: Int
                    while (input.read(buffer).also { len = it } != -1) {
                        zos.write(buffer, 0, len)
                    }
                }
                zos.closeEntry()
            }
        }
    }
}
