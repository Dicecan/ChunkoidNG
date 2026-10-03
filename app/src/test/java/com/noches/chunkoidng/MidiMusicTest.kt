package com.noches.chunkoidng

import br.com.gamemods.nbtmanipulator.*
import com.noches.chunkoidng.core.midi.*
import com.noches.chunkoidng.core.midi.exporters.FlatWorldMusicGenerator
import com.noches.chunkoidng.core.midi.exporters.NbsExporter
import com.noches.chunkoidng.core.midi.exporters.StructureExporter
import com.noches.chunkoidng.core.world.Platform
import com.noches.chunkoidng.core.world.WorldMetadataReader
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipInputStream

class MidiMusicTest {

    private fun createSyntheticMidiBytes(): ByteArray {
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)

        dos.writeBytes("MThd")
        dos.writeInt(6)
        dos.writeShort(0)
        dos.writeShort(1)
        dos.writeShort(480)

        val trackBaos = ByteArrayOutputStream()
        val trackDos = DataOutputStream(trackBaos)

        trackDos.writeByte(0x00)
        trackDos.writeByte(0xFF)
        trackDos.writeByte(0x51)
        trackDos.writeByte(0x03)
        trackDos.writeByte(0x07)
        trackDos.writeByte(0xA1)
        trackDos.writeByte(0x20)

        trackDos.writeByte(0x00)
        trackDos.writeByte(0x90)
        trackDos.writeByte(60)
        trackDos.writeByte(100)

        trackDos.writeByte(0x83)
        trackDos.writeByte(0x60)
        trackDos.writeByte(0x80)
        trackDos.writeByte(60)
        trackDos.writeByte(0)

        trackDos.writeByte(0x00)
        trackDos.writeByte(0xFF)
        trackDos.writeByte(0x2F)
        trackDos.writeByte(0x00)

        val trackBytes = trackBaos.toByteArray()
        dos.writeBytes("MTrk")
        dos.writeInt(trackBytes.size)
        dos.write(trackBytes)

        return baos.toByteArray()
    }

    @Test
    fun testMidiParserAndQuantization() {
        val midiBytes = createSyntheticMidiBytes()
        val parsed = MidiParser.parse(midiBytes, "Test Song")

        assertEquals(1, parsed.tracks.size)
        assertTrue(parsed.notes.isNotEmpty())

        val note = parsed.notes.first()
        assertEquals(60, note.pitch)
        assertEquals(0, note.channel)

        val song = RedstoneQuantizer.quantize(parsed, QuantizationConfig(autoTransposition = true))

        assertEquals("Test Song", song.title)
        assertTrue(song.notes.isNotEmpty())
        assertEquals(6, song.notes.first().key)
    }

    @Test
    fun testNbsExporter() {
        val song = NoteBlockSong(
            title = "Test NBS",
            notes = listOf(
                NoteBlockNote(tick = 0, instrument = NoteBlockInstrument.HARP, key = 12),
                NoteBlockNote(tick = 2, instrument = NoteBlockInstrument.BELL, key = 16)
            ),
            lengthTicks = 5
        )

        val testFile = File.createTempFile("test_export", ".nbs").apply { deleteOnExit() }
        NbsExporter.export(song, testFile)

        assertTrue(testFile.exists())
        assertTrue(testFile.length() > 20)
    }

    @Test
    fun testStructureExporter() {
        val song = NoteBlockSong(
            title = "Structure Test",
            notes = listOf(
                NoteBlockNote(tick = 0, instrument = NoteBlockInstrument.HARP, key = 12),
                NoteBlockNote(tick = 3, instrument = NoteBlockInstrument.BASEDRUM, key = 6)
            ),
            lengthTicks = 4
        )

        val mcstructureFile = File.createTempFile("test_struct", ".mcstructure").apply { deleteOnExit() }
        StructureExporter.exportBedrockMcStructure(song, mcstructureFile)

        assertTrue(mcstructureFile.exists())
        val nbt = NbtIO.readNbtFile(mcstructureFile.inputStream(), compressed = false, littleEndian = true)
        val tag = nbt.tag as NbtCompound
        assertEquals(1, (tag["format_version"] as NbtInt).value)
        assertTrue(tag.containsKey("structure"))

        val schemFile = File.createTempFile("test_schem", ".schem").apply { deleteOnExit() }
        StructureExporter.exportJavaSchematic(song, schemFile)

        assertTrue(schemFile.exists())
        val schemNbt = NbtIO.readNbtFile(schemFile.inputStream(), compressed = true, littleEndian = false)
        val schemTag = schemNbt.tag as NbtCompound
        assertEquals(2, (schemTag["Version"] as NbtInt).value)
        assertTrue(schemTag.containsKey("BlockData"))
    }

    @Test
    fun testFlatWorldMusicGenerator() {
        val song = NoteBlockSong(
            title = "Superflat Test",
            notes = listOf(
                NoteBlockNote(tick = 0, instrument = NoteBlockInstrument.HARP, key = 12)
            ),
            lengthTicks = 2
        )

        val mcworldFile = File.createTempFile("test_world", ".mcworld").apply { deleteOnExit() }
        FlatWorldMusicGenerator.generateFlatWorld(song, mcworldFile)

        assertTrue(mcworldFile.exists())
        assertTrue(mcworldFile.length() > 0)

        val entryNames = mutableListOf<String>()
        val extractedDir = File.createTempFile("extracted_mcworld", "").apply {
            delete()
            mkdirs()
        }

        try {
            ZipInputStream(FileInputStream(mcworldFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    entryNames.add(entry.name)
                    val destFile = File(extractedDir, entry.name)
                    destFile.parentFile?.mkdirs()
                    destFile.outputStream().use { fos ->
                        zis.copyTo(fos)
                    }
                    entry = zis.nextEntry
                }
            }

            assertTrue(entryNames.contains("levelname.txt"))
            assertTrue(entryNames.contains("level.dat"))
            assertTrue(entryNames.contains("level.dat_old"))
            assertTrue(entryNames.contains("world_icon.jpeg"))
            assertTrue(entryNames.contains("structures/music.mcstructure"))
            assertTrue(entryNames.any { it.startsWith("db/") })

            val levelDat = File(extractedDir, "level.dat")
            assertTrue(levelDat.exists())
            val metadata = WorldMetadataReader.readLevelDat(levelDat)
            assertNotNull(metadata)
            assertEquals(Platform.BEDROCK, metadata?.platform)
            assertEquals("Superflat Test", metadata?.worldName)
            assertEquals("Creative", metadata?.gameType)

            val dbDir = File(extractedDir, "db")
            assertTrue(dbDir.exists())
            assertTrue((dbDir.listFiles()?.size ?: 0) > 0)
        } finally {
            extractedDir.deleteRecursively()
        }
    }

    @Test
    fun testBlockLayeringAndSupport() {
        val song = NoteBlockSong(
            title = "Layer Test",
            notes = listOf(
                NoteBlockNote(tick = 0, instrument = NoteBlockInstrument.HARP, key = 12),
                NoteBlockNote(tick = 0, instrument = NoteBlockInstrument.BASS, key = 8),
                NoteBlockNote(tick = 4, instrument = NoteBlockInstrument.BASEDRUM, key = 6),
                NoteBlockNote(tick = 8, instrument = NoteBlockInstrument.SNARE, key = 10)
            ),
            lengthTicks = 10
        )

        val layout = StructureExporter.buildContraption(song)
        val blockMap = layout.blocks.associateBy { Triple(it.x, it.y, it.z) }

        for (block in layout.blocks) {
            if (block.bedrockName == "minecraft:redstone_wire" ||
                block.bedrockName == "minecraft:unpowered_repeater" ||
                block.bedrockName == "minecraft:noteblock"
            ) {
                assertEquals(1, block.y)
                val base = blockMap[Triple(block.x, 0, block.z)]
                assertNotNull("Missing support block under ${block.bedrockName} at (${block.x}, ${block.y}, ${block.z})", base)
                assertTrue(base!!.bedrockName != "minecraft:air")
            }
        }

        val buttonBlock = layout.blocks.find { it.bedrockName == "minecraft:stone_button" }
        assertNotNull(buttonBlock)
        assertEquals(2, buttonBlock!!.y)
        val buttonPedestal = blockMap[Triple(buttonBlock.x, 1, buttonBlock.z)]
        assertNotNull(buttonPedestal)
        assertEquals("minecraft:stone", buttonPedestal!!.bedrockName)
    }
}
