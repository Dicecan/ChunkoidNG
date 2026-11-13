package com.noches.chunkoidng.core.io

import android.content.ContentResolver
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

/**
 * Shared atomic file replacement and truncating Android SAF output stream helpers
 * to avoid truncated files or trailing garbage bytes on export overwrites.
 */
object SafeFileIO {

    fun openTruncatingOutputStream(contentResolver: ContentResolver, uri: Uri): OutputStream? {
        for (mode in listOf("wt", "rwt", "w")) {
            try {
                return contentResolver.openOutputStream(uri, mode)
            } catch (_: IllegalArgumentException) {
                // Provider does not support this mode string; try next fallback.
            } catch (_: UnsupportedOperationException) {
                // Provider does not support this mode string; try next fallback.
            }
        }
        return null
    }

    fun writeBytesAtomically(targetFile: File, data: ByteArray) {
        writeAtomically(targetFile) { out ->
            out.write(data)
        }
    }

    fun writeAtomically(
        targetFile: File,
        bufferSize: Int = 64 * 1024,
        writer: (OutputStream) -> Unit
    ) {
        val parent = targetFile.absoluteFile.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }
        val tempFile = File(parent, ".tmp_${targetFile.name}_${UUID.randomUUID()}")
        var committed = false
        try {
            FileOutputStream(tempFile).buffered(bufferSize).use { bufferedOut ->
                writer(bufferedOut)
                bufferedOut.flush()
            }
            replaceFile(tempFile, targetFile)
            committed = true
        } finally {
            if (!committed && tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    fun replaceFile(sourceFile: File, targetFile: File) {
        val parent = targetFile.absoluteFile.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }
        try {
            Files.move(
                sourceFile.toPath(),
                targetFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
            return
        } catch (_: IOException) {
            // Fallback for Android filesystems where Files.move REPLACE_EXISTING fails.
        }

        if (!targetFile.exists()) {
            if (sourceFile.renameTo(targetFile)) return
            throw IOException("Failed to move temp file ${sourceFile.name} to ${targetFile.absolutePath}")
        }

        val backupFile = File(parent, ".bak_${targetFile.name}_${UUID.randomUUID()}")
        if (!targetFile.renameTo(backupFile)) {
            if (!targetFile.delete() || !sourceFile.renameTo(targetFile)) {
                throw IOException("Failed to replace existing file ${targetFile.absolutePath}")
            }
            return
        }
        try {
            if (!sourceFile.renameTo(targetFile)) {
                backupFile.renameTo(targetFile)
                throw IOException("Failed to commit temp file ${sourceFile.name} to ${targetFile.absolutePath}")
            }
            backupFile.delete()
        } catch (e: IOException) {
            if (!targetFile.exists() && backupFile.exists()) {
                backupFile.renameTo(targetFile)
            }
            throw e
        }
    }
}
