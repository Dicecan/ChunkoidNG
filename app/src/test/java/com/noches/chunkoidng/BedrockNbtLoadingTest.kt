package com.noches.chunkoidng

import br.com.gamemods.nbtmanipulator.NbtCompound
import br.com.gamemods.nbtmanipulator.NbtFile
import br.com.gamemods.nbtmanipulator.NbtInt
import br.com.gamemods.nbtmanipulator.NbtIO
import com.noches.chunkoidng.core.leveldb.BedrockLevelDbHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayOutputStream

class BedrockNbtLoadingTest {

    @Test
    fun readsSingleLittleEndianRootWithoutChangingItsName() {
        val original = NbtFile("root", NbtCompound("answer" to NbtInt(42)))
        val encoded = BedrockLevelDbHelper.writeBedrockNbt(original)

        val loaded = BedrockLevelDbHelper.readBedrockNbt(encoded)

        assertEquals("root", loaded.name)
        assertEquals(42, (loaded.compound["answer"] as NbtInt).value)
    }

    @Test
    fun readsAllRootsFromMultiplePayloadAndIgnoresZeroPadding() {
        val original = NbtFile(
            "unused",
            NbtCompound("first" to NbtInt(1), "second" to NbtInt(2))
        )
        val output = ByteArrayOutputStream()
        NbtIO.writeNbtFile(output, NbtFile("first", original.compound["first"]!!), compressed = false, littleEndian = true)
        NbtIO.writeNbtFile(output, NbtFile("second", original.compound["second"]!!), compressed = false, littleEndian = true)
        val encoded = output.toByteArray() + ByteArray(5)

        val loaded = BedrockLevelDbHelper.readBedrockNbt(encoded, isMultiple = true)

        assertEquals(2, loaded.compound.size)
        assertEquals(1, (loaded.compound["first"] as NbtInt).value)
        assertEquals(2, (loaded.compound["second"] as NbtInt).value)
    }

    @Test
    fun rejectsNonZeroCorruptTailAfterMultipleRoot() {
        val original = NbtFile("unused", NbtCompound("value" to NbtInt(1)))
        val encoded = BedrockLevelDbHelper.writeBedrockNbt(original, wasMultiple = true) + byteArrayOf(0x7F)

        assertThrows(java.io.IOException::class.java) {
            BedrockLevelDbHelper.readBedrockNbt(encoded, isMultiple = true)
        }
    }

    @Test
    fun testLittleEndianUtf8WithNonAsciiCharacters() {
        val original = NbtFile(
            "",
            NbtCompound(
                "LevelName" to br.com.gamemods.nbtmanipulator.NbtString("钢琴, Addictive keys"),
                "StorageVersion" to NbtInt(10),
                "Generator" to NbtInt(2)
            )
        )
        val tempFile = java.io.File.createTempFile("test_nbt_utf8", ".dat").apply { deleteOnExit() }
        NbtIO.writeNbtFile(tempFile, original, compressed = false, littleEndian = true, writeHeaders = true)

        val loaded = tempFile.inputStream().use {
            NbtIO.readNbtFile(it, compressed = false, littleEndian = true, readHeaders = true)
        }

        assertEquals("钢琴, Addictive keys", (loaded.compound["LevelName"] as br.com.gamemods.nbtmanipulator.NbtString).value)
        assertEquals(10, (loaded.compound["StorageVersion"] as NbtInt).value)
        assertEquals(2, (loaded.compound["Generator"] as NbtInt).value)
    }
}
