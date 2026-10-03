package com.noches.chunkoidng

import com.noches.chunkoidng.core.crash.CrashLogManager
import com.noches.chunkoidng.core.crash.CrashReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CrashReportTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testCrashReportFormatting() {
        val report = CrashReport(
            timestamp = 1700000000000L,
            appVersionName = "CANARY 0.4",
            appVersionCode = 4L,
            packageName = "com.noches.chunkoidng",
            threadName = "main",
            isMainThread = true,
            exceptionClass = "java.lang.IllegalStateException",
            exceptionMessage = "Test crash occurred",
            stackTrace = "java.lang.IllegalStateException: Test crash occurred\n\tat com.noches.test.Main(Main.kt:10)"
        )

        val text = report.formatFormattedText()
        assertTrue(text.contains("CHUNKOID CRASH REPORT"))
        assertTrue(text.contains("com.noches.chunkoidng"))
        assertTrue(text.contains("CANARY 0.4"))
        assertTrue(text.contains("java.lang.IllegalStateException"))
        assertTrue(text.contains("Test crash occurred"))
        assertTrue(text.contains("Main.kt:10"))

        val summary = report.formatSummary()
        assertEquals("java.lang.IllegalStateException: Test crash occurred", summary)
    }

    @Test
    fun testCrashReportSummaryWithNullMessage() {
        val report = CrashReport(
            timestamp = 1700000000000L,
            appVersionName = "CANARY 0.4",
            appVersionCode = 4L,
            packageName = "com.noches.chunkoidng",
            threadName = "pool-1-thread-1",
            isMainThread = false,
            exceptionClass = "java.lang.NullPointerException",
            exceptionMessage = null,
            stackTrace = "java.lang.NullPointerException\n\tat com.noches.test.Main(Main.kt:20)"
        )

        val summary = report.formatSummary()
        assertEquals("java.lang.NullPointerException: No message", summary)
    }

    @Test
    fun testCrashLogManagerOperationsOnDirectory() {
        val testDir = tempFolder.newFolder("crash_logs")

        val file1 = File(testDir, "crash_20260101_100000_000.log").apply {
            writeText("crash 1 content")
            setLastModified(1000L)
        }
        val file2 = File(testDir, "crash_20260101_100001_000.log").apply {
            writeText("crash 2 content")
            setLastModified(2000L)
        }
        File(testDir, "other_file.txt").apply {
            writeText("not a crash log")
        }

        val read1 = CrashLogManager.readCrashLog(file1)
        assertEquals("crash 1 content", read1)

        val deleted = CrashLogManager.deleteCrashLog(file1)
        assertTrue(deleted)
        assertFalse(file1.exists())
        assertTrue(file2.exists())
    }
}
