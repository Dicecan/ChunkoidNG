package com.noches.chunkoidng

import br.com.gamemods.nbtmanipulator.*
import com.noches.chunkoidng.core.conversion.ChunkerFormat
import com.noches.chunkoidng.core.conversion.ConversionConfig
import com.noches.chunkoidng.core.decryptor.NetEaseCryptor
import com.noches.chunkoidng.core.io.SafeFileIO
import com.noches.chunkoidng.core.world.ArchiveManager
import com.noches.chunkoidng.maid.audit.SecurityAuditor
import com.noches.chunkoidng.maid.defense.ArchiveShield
import com.noches.chunkoidng.maid.defense.PathSanitizer
import com.noches.chunkoidng.maid.model.PerformanceLimits
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.*
import java.nio.file.Files
import java.util.Random
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SecurityAndExportIntegrityTest {

    private fun testLimits(
        maxEntryBytes: Long = 1024 * 1024,
        maxTotalBytes: Long = 10 * 1024 * 1024
    ) = PerformanceLimits(
        jvmMaxMemoryMb = 2048,
        parallelism = 2,
        useSerialGc = true,
        archiveMaxEntryBytes = maxEntryBytes,
        archiveMaxTotalBytes = maxTotalBytes,
        archiveMaxEntries = 100,
        nbtArrayPageSize = 100,
        packMaxFrames = 1000,
        midiMaxFileBytes = 10 * 1024 * 1024,
        midiMaxNotes = 1000
    )

    @Test
    fun archiveManagerFlattenPreservesIdenticallyNamedNestedDirectory() {
        val tempRoot = Files.createTempDirectory("chunkoid_flatten_test").toFile()
        try {
            val destDir = File(tempRoot, "input").apply { mkdirs() }
            val actualWorldDir = File(destDir, "world").apply { mkdirs() }
            val nestedWorldDir = File(actualWorldDir, "world").apply { mkdirs() }
            File(actualWorldDir, "level.dat").writeText("level-root")
            File(nestedWorldDir, "marker.txt").writeText("nested-world-data")
            val dbDir = File(actualWorldDir, "db").apply { mkdirs() }
            File(dbDir, "CURRENT").writeText("MANIFEST-000001\n")

            ArchiveManager.moveDirectoryContentsSafely(actualWorldDir, destDir)

            assertTrue("level.dat should be at destDir root", File(destDir, "level.dat").isFile)
            assertEquals("level-root", File(destDir, "level.dat").readText())
            assertTrue("Nested identically-named dir must not self-delete", File(destDir, "world/marker.txt").isFile)
            assertEquals("nested-world-data", File(destDir, "world/marker.txt").readText())
            assertEquals("MANIFEST-000001\n", File(destDir, "db/CURRENT").readText())
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    @Test
    fun safeFileIoWriteAtomicallyPreservesOriginalOnFailure() {
        val tempDir = Files.createTempDirectory("chunkoid_safeio_test").toFile()
        try {
            val target = File(tempDir, "export.zip")
            SafeFileIO.writeBytesAtomically(target, "ORIGINAL_GOOD_ARCHIVE".toByteArray())
            assertEquals("ORIGINAL_GOOD_ARCHIVE", target.readText())

            var threw = false
            try {
                SafeFileIO.writeAtomically(target) { out ->
                    out.write("PARTIAL_CORRUPT".toByteArray())
                    throw IOException("Simulated write failure mid-export")
                }
            } catch (_: IOException) {
                threw = true
            }

            assertTrue(threw)
            assertEquals("Original file must remain untouched on failed write", "ORIGINAL_GOOD_ARCHIVE", target.readText())
            val leftovers = tempDir.listFiles()?.filter { it.name.startsWith(".tmp_") || it.name.startsWith(".bak_") }.orEmpty()
            assertTrue("No temporary files should remain: $leftovers", leftovers.isEmpty())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun pathSanitizerAndArchiveShieldRejectTraversalDotAndCleansUpBombs() {
        val auditor = SecurityAuditor(capacity = 50)
        val sanitizer = PathSanitizer(auditor)
        val shield = ArchiveShield(sanitizer, auditor)

        assertEquals("unnamed", sanitizer.sanitizeFileName(".."))
        assertEquals("unnamed", sanitizer.sanitizeFileName("."))

        val tempDir = Files.createTempDirectory("chunkoid_shield_test").toFile()
        try {
            assertThrows(SecurityException::class.java) {
                sanitizer.resolveUnderBase(tempDir, "world\u0000/level.dat")
            }

            val dotZip = ByteArrayOutputStream().use { baos ->
                ZipOutputStream(baos).use { zos ->
                    zos.putNextEntry(ZipEntry("."))
                    zos.write("not-a-dir".toByteArray())
                    zos.closeEntry()
                }
                baos.toByteArray()
            }
            val dotDest = File(tempDir, "dot_dest")
            assertThrows(SecurityException::class.java) {
                shield.extractZipSecurely(ByteArrayInputStream(dotZip), dotDest, testLimits())
            }

            val bigPayload = ByteArray(4096) { 0x41 }
            val bombZip = ByteArrayOutputStream().use { baos ->
                ZipOutputStream(baos).use { zos ->
                    zos.putNextEntry(ZipEntry("huge.bin"))
                    zos.write(bigPayload)
                    zos.closeEntry()
                }
                baos.toByteArray()
            }
            val bombDest = File(tempDir, "bomb_dest")
            assertThrows(IllegalArgumentException::class.java) {
                shield.extractZipSecurely(
                    ByteArrayInputStream(bombZip),
                    bombDest,
                    testLimits(maxEntryBytes = 1024, maxTotalBytes = 1024 * 1024)
                )
            }
            assertFalse("Oversized partial file must be deleted", File(bombDest, "huge.bin").exists())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun anvilRegionFileRejectsOversizedChunkInsteadOfHeaderTruncation() {
        val tempDir = Files.createTempDirectory("chunkoid_anvil_limit").toFile()
        try {
            val mcaFile = File(tempDir, "r.0.0.mca")
            val hugeBytes = ByteArray(1_150_000)
            Random(42L).nextBytes(hugeBytes)
            val root = NbtCompound()
            root["Garbage"] = NbtByteArray(hugeBytes)
            val nbtFile = NbtFile("", root)

            AnvilRegionFile(mcaFile).use { region ->
                val ex = assertThrows(IOException::class.java) {
                    region.writeChunkNbt(0, 0, nbtFile)
                }
                assertTrue(ex.message?.contains("255-sector") == true)
            }
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun anvilRegionFileGuardsCorruptedSectorLengthsSafely() {
        val tempDir = Files.createTempDirectory("chunkoid_anvil_corrupt").toFile()
        try {
            val mcaFile = File(tempDir, "r.0.0.mca")
            RandomAccessFile(mcaFile, "rw").use { raf ->
                raf.write(ByteArray(8192 + 4096))
                raf.seek(0)
                raf.write(0)
                raf.write(0)
                raf.write(2)
                raf.write(1)
                raf.seek(8192)
                raf.writeInt(50_000_000)
                raf.write(2)
            }

            AnvilRegionFile(mcaFile, readOnly = true).use { region ->
                assertNull("Corrupt length exceeding sector allocation must return null safely", region.readChunkNbt(0, 0))
            }
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun nbtIoRejectsMaliciousArraySizesDepthAndUnknownTags() {
        val hugeArrayBytes = ByteArrayOutputStream().apply {
            DataOutputStream(this).apply {
                writeByte(10)
                writeUTF("")
                writeByte(7)
                writeUTF("bad")
                writeInt(Int.MAX_VALUE)
                writeByte(0)
            }
        }.toByteArray()
        assertThrows(IOException::class.java) {
            NbtIO.readNbtFileDirectly(DataInputStream(ByteArrayInputStream(hugeArrayBytes)))
        }

        val badTypeBytes = ByteArrayOutputStream().apply {
            DataOutputStream(this).apply {
                writeByte(99)
                writeUTF("")
            }
        }.toByteArray()
        assertThrows(IOException::class.java) {
            NbtIO.readNbtFileDirectly(DataInputStream(ByteArrayInputStream(badTypeBytes)))
        }

        val deepBytes = ByteArrayOutputStream().apply {
            DataOutputStream(this).apply {
                for (i in 0..520) {
                    writeByte(10)
                    writeUTF("n")
                }
                for (i in 0..520) {
                    writeByte(0)
                }
            }
        }.toByteArray()
        assertThrows(IOException::class.java) {
            NbtIO.readNbtFileDirectly(DataInputStream(ByteArrayInputStream(deepBytes)))
        }

        val tempFile = File.createTempFile("bad_nbt", ".dat")
        try {
            tempFile.writeBytes(byteArrayOf(0x01, 0x02, 0x03))
            val ex = assertThrows(IOException::class.java) {
                NbtIO.readNbtFileDetectingSettings(tempFile)
            }
            assertNotNull("Primary suppressed cause must not be lost", ex.cause)
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun conversionConfigEscapesWorldNameJsonSafely() {
        val tempDir = File(".")
        val cfg = ConversionConfig(
            inputDir = tempDir,
            outputDir = tempDir,
            targetFormat = ChunkerFormat.BEDROCK_FORMATS.first(),
            overrideWorldName = "My \"Quoted\" \\ World\nLine2"
        )
        val jsonStr = cfg.buildWorldSettingsJson()
        assertNotNull(jsonStr)
        val parsed = JSONObject(jsonStr!!)
        assertEquals("My \"Quoted\" \\ World\nLine2", parsed.getString("world_name"))
    }

    @Test
    fun netEaseCryptorVerifyLdbFooterStreamsTailBytes() {
        val tempFile = File.createTempFile("test_table", ".ldb")
        try {
            val magic = byteArrayOf(0x57, 0xFB.toByte(), 0x80.toByte(), 0x8B.toByte(), 0x24, 0x75, 0x47, 0xDB.toByte())
            tempFile.writeBytes(ByteArray(128) { 0x11 } + magic)
            assertTrue(NetEaseCryptor.verifyLdbFooter(tempFile))

            tempFile.writeBytes(ByteArray(128) { 0x11 })
            assertFalse(NetEaseCryptor.verifyLdbFooter(tempFile))

            tempFile.writeBytes(byteArrayOf(1, 2, 3))
            assertFalse(NetEaseCryptor.verifyLdbFooter(tempFile))
        } finally {
            tempFile.delete()
        }
    }
}
