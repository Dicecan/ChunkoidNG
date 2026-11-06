package com.noches.chunkoidng.maid

import com.noches.chunkoidng.maid.audit.SecurityAuditor
import com.noches.chunkoidng.maid.defense.ArchiveShield
import com.noches.chunkoidng.maid.defense.BinaryGuard
import com.noches.chunkoidng.maid.defense.PathSanitizer
import com.noches.chunkoidng.maid.model.PerformanceLimits
import com.noches.chunkoidng.maid.model.ViolationSeverity
import com.noches.chunkoidng.maid.model.ViolationType
import com.noches.chunkoidng.maid.tuning.PagingAdvisor
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MaidFrameworkTest {

    private lateinit var auditor: SecurityAuditor
    private lateinit var pathSanitizer: PathSanitizer
    private lateinit var archiveShield: ArchiveShield
    private lateinit var binaryGuard: BinaryGuard
    private lateinit var pagingAdvisor: PagingAdvisor

    @Before
    fun setUp() {
        auditor = SecurityAuditor(capacity = 100)
        pathSanitizer = PathSanitizer(auditor)
        archiveShield = ArchiveShield(pathSanitizer, auditor)
        binaryGuard = BinaryGuard(auditor)
        pagingAdvisor = PagingAdvisor(auditor)
    }

    @Test
    fun testPathSanitizerNormalResolution() {
        val tempDir = File.createTempFile("maid_test", "").apply {
            delete()
            mkdirs()
        }
        try {
            val resolved = pathSanitizer.resolveUnderBase(tempDir, "world/db/000001.ldb")
            assertTrue(resolved.path.startsWith(tempDir.path))
            assertEquals(0, auditor.getRecentViolations().size)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testPathSanitizerBlocksZipSlip() {
        val tempDir = File.createTempFile("maid_test", "").apply {
            delete()
            mkdirs()
        }
        try {
            assertThrows(SecurityException::class.java) {
                pathSanitizer.resolveUnderBase(tempDir, "../../evil.sh")
            }
            val violations = auditor.getRecentViolations()
            assertEquals(1, violations.size)
            assertEquals(ViolationType.ZIP_SLIP, violations.first().type)
            assertEquals(ViolationSeverity.BLOCKED, violations.first().severity)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testPathSanitizerBlocksNestedZipSlip() {
        val tempDir = File.createTempFile("maid_test", "").apply {
            delete()
            mkdirs()
        }
        try {
            assertThrows(SecurityException::class.java) {
                pathSanitizer.resolveUnderBase(tempDir, "folder/sub/../../../escape.dat")
            }
            assertEquals(1, auditor.getRecentViolations().size)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testPagingAdvisorGentleTruncationForAnimationFrames() {
        val normalFrames = pagingAdvisor.sanitizeAnimationFrames(
            requestedFrames = 500L,
            maxFrames = 1000,
            texturePath = "textures/fire.png"
        )
        assertEquals(500, normalFrames)
        assertEquals(0, auditor.getRecentViolations().size)

        val truncatedFrames = pagingAdvisor.sanitizeAnimationFrames(
            requestedFrames = 250_000L,
            maxFrames = 100_000,
            texturePath = "textures/huge_water.png"
        )
        assertEquals(100_000, truncatedFrames)

        val violations = auditor.getRecentViolations()
        assertEquals(1, violations.size)
        val event = violations.first()
        assertEquals(ViolationSeverity.GENTLE_TRUNCATED, event.severity)
        assertEquals(ViolationType.FRAME_COUNT_LIMIT, event.type)
        assertEquals(250_000L, event.requestedValue)
        assertEquals(100_000L, event.clampedValue)
    }

    @Test
    fun testPagingAdvisorGentleTruncationForMidiNotes() {
        val normalNotes = pagingAdvisor.sanitizeMidiNotes(
            requestedNotes = 8_000,
            maxNotes = 50_000,
            songTitle = "Mozart"
        )
        assertEquals(8_000, normalNotes)
        assertEquals(0, auditor.getRecentViolations().size)

        val truncatedNotes = pagingAdvisor.sanitizeMidiNotes(
            requestedNotes = 1_500_000,
            maxNotes = 600_000,
            songTitle = "BlackMidi"
        )
        assertEquals(600_000, truncatedNotes)

        val violations = auditor.getRecentViolations()
        assertEquals(1, violations.size)
        val event = violations.first()
        assertEquals(ViolationSeverity.GENTLE_TRUNCATED, event.severity)
        assertEquals(ViolationType.NOTE_COUNT_LIMIT, event.type)
    }

    @Test
    fun testArchiveShieldExtraction() {
        val zipBytes = createTestZip(
            mapOf(
                "level.dat" to "Minecraft World".toByteArray(),
                "db/CURRENT" to "MANIFEST-000001".toByteArray()
            )
        )
        val destDir = File.createTempFile("maid_extract", "").apply {
            delete()
            mkdirs()
        }
        try {
            val limits = PerformanceLimits(
                jvmMaxMemoryMb = 2048,
                parallelism = 2,
                useSerialGc = true,
                archiveMaxEntryBytes = 1024 * 1024,
                archiveMaxTotalBytes = 10 * 1024 * 1024,
                archiveMaxEntries = 100,
                nbtArrayPageSize = 100,
                packMaxFrames = 1000,
                midiMaxFileBytes = 10 * 1024 * 1024,
                midiMaxNotes = 1000
            )

            val summary = archiveShield.extractZipSecurely(
                inputStream = ByteArrayInputStream(zipBytes),
                destDir = destDir,
                limits = limits
            )
            assertEquals(2, summary.entryCount)
            assertTrue(File(destDir, "level.dat").exists())
            assertTrue(File(destDir, "db/CURRENT").exists())
            assertEquals(0, auditor.getRecentViolations().size)
        } finally {
            destDir.deleteRecursively()
        }
    }

    @Test
    fun testArchiveShieldBlocksEntryCountLimit() {
        val entries = (0 until 10).associate { "entry_$it.txt" to "data".toByteArray() }
        val zipBytes = createTestZip(entries)
        val destDir = File.createTempFile("maid_extract_entry_limit", "").apply {
            delete()
            mkdirs()
        }
        try {
            val limits = PerformanceLimits(
                jvmMaxMemoryMb = 2048,
                parallelism = 2,
                useSerialGc = true,
                archiveMaxEntryBytes = 1024 * 1024,
                archiveMaxTotalBytes = 10 * 1024 * 1024,
                archiveMaxEntries = 5,
                nbtArrayPageSize = 100,
                packMaxFrames = 1000,
                midiMaxFileBytes = 10 * 1024 * 1024,
                midiMaxNotes = 1000
            )

            assertThrows(IllegalArgumentException::class.java) {
                archiveShield.extractZipSecurely(
                    inputStream = ByteArrayInputStream(zipBytes),
                    destDir = destDir,
                    limits = limits
                )
            }
            val violations = auditor.getRecentViolations()
            assertEquals(1, violations.size)
            assertEquals(ViolationType.ZIP_ENTRY_COUNT_LIMIT, violations.first().type)
        } finally {
            destDir.deleteRecursively()
        }
    }

    @Test
    fun testArchiveShieldBlocksSingleEntrySizeBomb() {
        val bigData = ByteArray(100 * 1024) { 0x41 }
        val zipBytes = createTestZip(mapOf("big.bin" to bigData))
        val destDir = File.createTempFile("maid_extract_size_bomb", "").apply {
            delete()
            mkdirs()
        }
        try {
            val limits = PerformanceLimits(
                jvmMaxMemoryMb = 2048,
                parallelism = 2,
                useSerialGc = true,
                archiveMaxEntryBytes = 50 * 1024,
                archiveMaxTotalBytes = 10 * 1024 * 1024,
                archiveMaxEntries = 100,
                nbtArrayPageSize = 100,
                packMaxFrames = 1000,
                midiMaxFileBytes = 10 * 1024 * 1024,
                midiMaxNotes = 1000
            )

            assertThrows(IllegalArgumentException::class.java) {
                archiveShield.extractZipSecurely(
                    inputStream = ByteArrayInputStream(zipBytes),
                    destDir = destDir,
                    limits = limits
                )
            }
            val violations = auditor.getRecentViolations()
            assertEquals(1, violations.size)
            assertEquals(ViolationType.ZIP_BOMB_ENTRY_SIZE, violations.first().type)
        } finally {
            destDir.deleteRecursively()
        }
    }

    @Test
    fun testSecurityAuditorRingBufferFifo() {
        repeat(150) { i ->
            auditor.recordBlock(
                type = ViolationType.BUFFER_OVERFLOW_ATTEMPT,
                module = "Test",
                message = "Violation #$i"
            )
        }
        val violations = auditor.getRecentViolations()
        assertEquals(100, violations.size)
        assertEquals("Violation #149", violations.first().message)

        auditor.clear()
        assertEquals(0, auditor.getRecentViolations().size)
    }

    @Test
    fun testBinaryGuardRangeVerification() {
        binaryGuard.verifyRange(0, 100, 1000, "Test")

        assertThrows(IndexOutOfBoundsException::class.java) {
            binaryGuard.verifyRange(900, 200, 1000, "Test")
        }
        val violations = auditor.getRecentViolations()
        assertEquals(1, violations.size)
        assertEquals(ViolationType.BUFFER_OVERFLOW_ATTEMPT, violations.first().type)
    }

    private fun createTestZip(entries: Map<String, ByteArray>): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zos ->
            for ((name, data) in entries) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(data)
                zos.closeEntry()
            }
        }
        return bos.toByteArray()
    }
}
