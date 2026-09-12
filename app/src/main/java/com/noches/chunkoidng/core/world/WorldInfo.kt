package com.noches.chunkoidng.core.world

import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.util.Locale

/**
 * Data model describing an inspected Minecraft world.
 */
data class WorldInfo(
    val name: String,
    val platform: Platform,
    val versionName: String? = null,
    val versionId: Int? = null,
    val sizeBytes: Long = 0L,
    val iconBitmap: Bitmap? = null,
    val worldDir: File,
    val sourceUri: Uri? = null,
    val isArchive: Boolean = false,
    val gameType: String? = null
) {
    val formattedSize: String
        get() {
            if (sizeBytes <= 0) return "未知大小"
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(Locale.getDefault(), "%.2f GB", gb)
                mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
                else -> String.format(Locale.getDefault(), "%.0f KB", kb)
            }
        }

    val displayVersion: String
        get() = when {
            !versionName.isNullOrBlank() -> versionName
            versionId != null -> "Build $versionId"
            else -> "未知版本"
        }
}
