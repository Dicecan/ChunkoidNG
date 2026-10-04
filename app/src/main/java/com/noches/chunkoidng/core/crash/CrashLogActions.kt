package com.noches.chunkoidng.core.crash

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.noches.chunkoidng.R
import java.io.File

object CrashLogActions {
    fun copyLog(context: Context, text: String) {
        val message = try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Chunkoid Crash Log", text))
            R.string.crash_log_copied
        } catch (_: Exception) {
            R.string.crash_log_copy_failed
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun shareLog(context: Context, file: File?, text: String) {
        try {
            val shareIntent = createShareIntent(context, file, text)
            val chooser = Intent.createChooser(shareIntent, context.getString(R.string.crash_action_share))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (_: Exception) {
            Toast.makeText(context, R.string.crash_log_share_failed, Toast.LENGTH_SHORT).show()
        }
    }

    internal fun createShareIntent(context: Context, file: File?, text: String): Intent {
        val uri = file?.takeIf { it.isFile }?.let {
            try {
                getUri(context, it)
            } catch (_: Exception) {
                null
            }
        } ?: run {
            val directory = File(context.cacheDir, "crash_share")
            check(directory.isDirectory || directory.mkdirs()) { "Cannot create crash export directory" }
            val export = File.createTempFile("crash_", ".log", directory)
            export.writeText(text)
            getUri(context, export)
        }

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, file?.name ?: "ChunkoidNG Crash Log")
            putExtra(Intent.EXTRA_TEXT, CrashReport.abbreviateForIntent(text))
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("Crash Log", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun getUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
