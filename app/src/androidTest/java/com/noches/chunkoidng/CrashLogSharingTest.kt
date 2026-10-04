package com.noches.chunkoidng

import android.content.Intent
import android.net.Uri
import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.noches.chunkoidng.core.crash.CrashLogActions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class CrashLogSharingTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val fullReport = "large crash report\n".repeat(70_000) + "END_OF_REPORT"

    @Test
    fun largeLogUsesASmallIntentAndAnUnabridgedAttachment() {
        val directory = File(context.filesDir, "crash_logs").apply { mkdirs() }
        val file = File.createTempFile("crash_sharing_test_", ".log", directory)
        try {
            file.writeText(fullReport)
            assertCompleteAttachment(CrashLogActions.createShareIntent(context, file, fullReport))
        } finally {
            file.delete()
        }
    }

    @Test
    fun missingLogFileExportsFullTextToAShareableAttachment() {
        val intent = CrashLogActions.createShareIntent(context, null, fullReport)
        try {
            assertCompleteAttachment(intent)
        } finally {
            context.contentResolver.delete(attachmentUri(intent), null, null)
        }
    }

    private fun assertCompleteAttachment(intent: Intent) {
        val parcel = Parcel.obtain()
        try {
            intent.writeToParcel(parcel, 0)
            assertTrue("A large report must not fill the Binder transaction", parcel.dataSize() < 32_768)
        } finally {
            parcel.recycle()
        }
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        val uri = attachmentUri(intent)
        assertEquals(uri, intent.clipData!!.getItemAt(0).uri)
        val actualText = context.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
        assertEquals(fullReport, actualText)
    }

    @Suppress("DEPRECATION")
    private fun attachmentUri(intent: Intent): Uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)!!
}
