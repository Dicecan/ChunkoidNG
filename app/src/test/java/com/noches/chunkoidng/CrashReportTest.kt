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
            appVersionName = "CANARY 0.5",
            appVersionCode = 5L,
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
        assertTrue(text.contains("CANARY 0.5"))
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
            appVersionName = "CANARY 0.5",
            appVersionCode = 5L,
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

    @Test
    fun largeSummaryStaysSmallWhileFullReportRetainsTheException() {
        val message = "x".repeat(600_000) + "END_OF_MESSAGE"
        val trace = "at first.frame(First.kt:1)\n" + "at frame(Frame.kt:2)\n".repeat(40_000) + "END_OF_TRACE"
        val report = CrashReport(
            appVersionName = "test",
            appVersionCode = 1L,
            packageName = "com.noches.chunkoidng",
            threadName = "main",
            isMainThread = true,
            exceptionClass = "java.lang.IllegalStateException",
            exceptionMessage = message,
            stackTrace = trace
        )

        assertTrue(report.formatSummary().length <= CrashReport.MAX_INTENT_TEXT_LENGTH)
        assertTrue(report.formatSummary().endsWith("…"))
        val fullReport = report.formatFormattedText()
        assertTrue(fullReport.contains(message))
        assertTrue(fullReport.contains(trace))
    }

    @Test
    fun shortenedIntentTextDoesNotSplitAnEmoji() {
        val prefix = "x".repeat(CrashReport.MAX_INTENT_TEXT_LENGTH - 2)
        val result = CrashReport.abbreviateForIntent(prefix + "😀tail")
        assertEquals(prefix + "…", result)
    }

    @Test
    fun deletingNewestLogUpdatesLatestCopyAndDeletingLastLogRemovesIt() {
        val dir = tempFolder.newFolder("latest_crash_logs")
        val older = File(dir, "crash_older.log").apply {
            writeText("older crash")
            setLastModified(1000L)
        }
        val newer = File(dir, "crash_newer.log").apply {
            writeText("newer crash")
            setLastModified(2000L)
        }
        val latest = File(dir, "latest_crash.log").apply { writeText("newer crash") }
        val other = File(dir, "other.txt").apply { writeText("keep this") }

        assertTrue(CrashLogManager.deleteCrashLog(newer))
        assertFalse(newer.exists())
        assertEquals("older crash", latest.readText())
        assertTrue(CrashLogManager.deleteCrashLog(older))
        assertFalse(older.exists())
        assertFalse(latest.exists())
        assertTrue(other.exists())
    }

    @Test
    fun deletingOlderLogKeepsTheNewestReportInLatestCopy() {
        val dir = tempFolder.newFolder("delete_older")
        val older = File(dir, "crash_older.log").apply {
            writeText("older crash")
            setLastModified(1000L)
        }
        File(dir, "crash_newer.log").apply {
            writeText("newer crash")
            setLastModified(2000L)
        }
        val latest = File(dir, "latest_crash.log").apply { writeText("newer crash") }

        assertTrue(CrashLogManager.deleteCrashLog(older))
        assertEquals("newer crash", latest.readText())
    }

    @Test
    fun orphanedLatestCopyCanBeDeleted() {
        val dir = tempFolder.newFolder("orphaned_latest")
        val latest = File(dir, "latest_crash.log").apply { writeText("orphaned crash") }

        assertTrue(CrashLogManager.deleteCrashLog(latest))
        assertFalse(latest.exists())
    }
}
