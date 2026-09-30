package com.noches.chunkoidng

import com.noches.chunkoidng.core.decryptor.NetEaseCryptor
import org.junit.Assert.*
import org.junit.Test
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class NetEaseCryptorTest {

    @Test
    fun testEncryptionAndDecryptionRoundtrip() {
        val testString = "Hello Minecraft Bedrock LevelDB Data! Test 1234567890."
        val originalBytes = testString.toByteArray(StandardCharsets.UTF_8)

        // 1. Initially raw data does not have magic header
        assertFalse(NetEaseCryptor.hasMagicHeader(originalBytes))

        // 2. Perform passive encryption
        val encrypted = NetEaseCryptor.encryptData(originalBytes)
        assertTrue(NetEaseCryptor.hasMagicHeader(encrypted))
        assertEquals(originalBytes.size + NetEaseCryptor.HEADER_SIZE, encrypted.size)

        // Test stream isEncrypted
        val bis = BufferedInputStream(ByteArrayInputStream(encrypted))
        assertTrue(NetEaseCryptor.isEncrypted(bis))

        // 3. Perform decryption
        val decrypted = NetEaseCryptor.decryptData(encrypted)
        assertFalse(NetEaseCryptor.hasMagicHeader(decrypted))
        assertEquals(originalBytes.size, decrypted.size)

        // 4. Verify roundtrip content match
        assertArrayEquals(originalBytes, decrypted)
    }

    @Test
    fun testDeriveKeyFromCurrentAndManifest() {
        // Mock CURRENT file: "MANIFEST-000001\n" encrypted with key "88329851"
        val manifestName = "MANIFEST-000001"
        val plainCurrent = (manifestName + "\n").toByteArray(StandardCharsets.UTF_8)
        val encryptedCurrent = NetEaseCryptor.encryptData(plainCurrent, NetEaseCryptor.DEFAULT_KEY)

        val derivedKey = NetEaseCryptor.deriveKey(encryptedCurrent, manifestName)
        assertArrayEquals(NetEaseCryptor.DEFAULT_KEY, derivedKey)
    }

    @Test
    fun testLevelDbSSTableFooterVerification() {
        // LevelDB SSTable footer ends with 0x57FB808B247547DB (8 bytes little-endian: DB 47 75 24 8B 80 FB 57)
        val dummySSTable = ByteArray(100)
        val sstableMagic = byteArrayOf(
            0xDB.toByte(), 0x47.toByte(), 0x75.toByte(), 0x24.toByte(),
            0x8B.toByte(), 0x80.toByte(), 0xFB.toByte(), 0x57.toByte()
        )
        System.arraycopy(sstableMagic, 0, dummySSTable, dummySSTable.size - 8, 8)

        assertTrue(NetEaseCryptor.verifyLdbFooter(dummySSTable))

        // Corrupt magic
        dummySSTable[dummySSTable.size - 1] = 0x00
        assertFalse(NetEaseCryptor.verifyLdbFooter(dummySSTable))
    }
}
