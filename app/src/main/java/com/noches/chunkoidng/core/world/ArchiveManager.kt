package com.noches.chunkoidng.core.world

import android.content.ContentResolver
import android.content.Context
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

/**
 * Manages world archive extraction, folder staging from SAF, and converted world export.
 */
class ArchiveManager(private val context: Context) {
    private val contentResolver: ContentResolver get() = context.contentResolver

    val workspaceDir: File get() = File(context.filesDir, "workspace").apply { mkdirs() }
    val inputDir: File get() = File(workspaceDir, "input")
    val outputDir: File get() = File(workspaceDir, "output")

    fun cleanWorkspace() {
        if (inputDir.exists()) inputDir.deleteRecursively()
        if (outputDir.exists()) outputDir.deleteRecursively()
    }

    /**
     * Resolves the display name of a Uri.
     */
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

    /**
     * Extracts an archive URI (.zip / .mcworld) into the workspace input directory.
     * Flattens nested folders if level.dat is not at root.
     */
    suspend fun extractArchive(
        archiveUri: Uri,
        onProgress: (progress: Int, status: String) -> Unit
    ): Result<WorldInfo> = withContext(Dispatchers.IO) {
        try {
            if (inputDir.exists()) inputDir.deleteRecursively()
            inputDir.mkdirs()

            onProgress(5, "正在解压世界归档...")

            val inputStream = contentResolver.openInputStream(archiveUri)
                ?: return@withContext Result.failure(Exception("无法打开输入归档文件"))

            val buffer = ByteArray(64 * 1024)
            var extractedCount = 0
            val inputCanonicalPath = inputDir.canonicalPath + File.separator

            inputStream.use { rawIn ->
                ZipInputStream(rawIn).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        if (!coroutineContext.isActive) {
                            zis.closeEntry()
                            return@withContext Result.failure(Exception("解压已取消"))
                        }

                        val entryFile = File(inputDir, entry.name)
                        // Zip Slip vulnerability guard
                        if (!entryFile.canonicalPath.startsWith(inputCanonicalPath)) {
                            entry = zis.nextEntry
                            continue
                        }

                        if (entry.isDirectory) {
                            entryFile.mkdirs()
                        } else {
                            entryFile.parentFile?.mkdirs()
                        FileOutputStream(entryFile).buffered(64 * 1024).use { out ->
                                var len: Int
                                while (zis.read(buffer).also { len = it } != -1) {
                                    out.write(buffer, 0, len)
                                }
                            }
                        }
                        zis.closeEntry()
                        extractedCount++
                        if (extractedCount % 15 == 0) {
                            val pct = (5 + (extractedCount / 5)).coerceIn(5, 75)
                            onProgress(pct, "正在解压归档 ($extractedCount 个文件)...")
                        }
                        entry = zis.nextEntry
                    }
                }
            }

            onProgress(80, "正在检索世界结构...")
            val levelDat = findFileRecursive(inputDir, "level.dat")
                ?: return@withContext Result.failure(Exception("未在归档中找到有效的 level.dat 数据"))

            val actualWorldDir = levelDat.parentFile ?: inputDir
            if (actualWorldDir.canonicalPath != inputDir.canonicalPath) {
                onProgress(85, "正在整理世界目录结构...")
                moveDirectoryContents(actualWorldDir, inputDir)
            }

            onProgress(95, "正在解析世界元数据...")
            val rawName = getUriDisplayName(archiveUri).removeSuffix(".mcworld").removeSuffix(".zip")
            val worldInfo = WorldMetadataReader.inspectWorld(inputDir, fallbackName = rawName).copy(
                sourceUri = archiveUri,
                isArchive = true
            )

            onProgress(100, "世界加载就绪")
            Result.success(worldInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Staging an existing directory selected via OpenDocumentTree into the workspace.
     */
    suspend fun stageTreeUri(
        treeUri: Uri,
        onProgress: (progress: Int, status: String) -> Unit
    ): Result<WorldInfo> = withContext(Dispatchers.IO) {
        try {
            if (inputDir.exists()) inputDir.deleteRecursively()
            inputDir.mkdirs()

            onProgress(5, "正在读取目录结构...")
            val treeDoc = DocumentFile.fromTreeUri(context, treeUri)
                ?: return@withContext Result.failure(Exception("无法读取选中的目录"))

            if (!treeDoc.isDirectory) {
                return@withContext Result.failure(Exception("选中的路径不是有效目录"))
            }

            var copiedCount = 0
            copyDocumentDirectory(treeDoc, inputDir) { count ->
                copiedCount = count
                val pct = (10 + count / 2).coerceIn(10, 85)
                onProgress(pct, "正在复制世界数据 ($copiedCount 个文件)...")
            }

            onProgress(90, "正在验证世界数据...")
            val levelDat = findFileRecursive(inputDir, "level.dat")
                ?: return@withContext Result.failure(Exception("未在目录中找到 level.dat 存档文件"))

            val actualWorldDir = levelDat.parentFile ?: inputDir
            if (actualWorldDir.canonicalPath != inputDir.canonicalPath) {
                onProgress(93, "正在整理世界目录结构...")
                moveDirectoryContents(actualWorldDir, inputDir)
            }

            onProgress(97, "正在解析世界元数据...")
            val folderName = treeDoc.name ?: "Minecraft_World"
            val worldInfo = WorldMetadataReader.inspectWorld(inputDir, fallbackName = folderName).copy(
                sourceUri = treeUri,
                isArchive = false
            )

            onProgress(100, "世界加载就绪")
            Result.success(worldInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun copyDocumentDirectory(
        source: DocumentFile,
        destDir: File,
        fileCounter: (Int) -> Unit
    ) {
        var totalCopied = 0
        fun recursiveCopy(currSource: DocumentFile, currDest: File) {
            currDest.mkdirs()
            currSource.listFiles().forEach { child ->
                val childName = child.name ?: "unnamed"
                val destChild = File(currDest, childName)
                if (child.isDirectory) {
                    recursiveCopy(child, destChild)
                } else if (child.isFile) {
                    try {
                        contentResolver.openInputStream(child.uri)?.use { inStream ->
                            FileOutputStream(destChild).buffered(64 * 1024).use { outStream ->
                                inStream.copyTo(outStream, 64 * 1024)
                            }
                        }
                        totalCopied++
                        if (totalCopied % 10 == 0) fileCounter(totalCopied)
                    } catch (_: Exception) {}
                }
            }
        }
        recursiveCopy(source, destDir)
        fileCounter(totalCopied)
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

    /**
     * Exports the converted world directory to a user-selected SAF tree Uri.
     */
    suspend fun exportToUri(
        targetTreeUri: Uri,
        worldName: String,
        packAsArchive: Boolean = false,
        isBedrock: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!outputDir.exists() || outputDir.listFiles()?.isEmpty() == true) {
                return@withContext Result.failure(Exception("转换输出目录为空"))
            }

            val treeDoc = DocumentFile.fromTreeUri(context, targetTreeUri)
                ?: return@withContext Result.failure(Exception("无法访问选中的目录"))

            val safeName = worldName.replace(Regex("[^\\p{L}\\p{N}_\\- ]"), "_").trim().ifBlank { "ConvertedWorld" }

            if (packAsArchive) {
                val extension = if (isBedrock) ".mcworld" else ".zip"
                val docFile = treeDoc.createFile("application/zip", "$safeName$extension")
                    ?: return@withContext Result.failure(Exception("无法在目标位置创建归档文件"))

                context.contentResolver.openOutputStream(docFile.uri)?.use { out ->
                    ZipOutputStream(BufferedOutputStream(out, 64 * 1024)).use { zos ->
                        zipDirectory(outputDir, outputDir, zos)
                    }
                } ?: return@withContext Result.failure(Exception("无法打开归档输出流"))
                Result.success(Unit)
            } else {
                val destDirDoc = treeDoc.createDirectory(safeName)
                    ?: return@withContext Result.failure(Exception("无法在目标位置创建文件夹"))

                copyDirToDocumentFile(outputDir, destDirDoc)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun copyDirToDocumentFile(sourceDir: File, destDirDoc: DocumentFile) {
        sourceDir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                val newDir = destDirDoc.createDirectory(file.name)
                if (newDir != null) {
                    copyDirToDocumentFile(file, newDir)
                }
            } else {
                val newFile = destDirDoc.createFile("application/octet-stream", file.name)
                if (newFile != null) {
                        context.contentResolver.openOutputStream(newFile.uri)?.buffered(64 * 1024)?.use { out ->
                            file.inputStream().use { input ->
                                input.copyTo(out, 64 * 1024)
                            }
                    }
                }
            }
        }
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
