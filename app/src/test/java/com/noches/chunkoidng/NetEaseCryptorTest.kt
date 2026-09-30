package com.noches.chunkoidng

import com.noches.chunkoidng.core.decryptor.NetEaseCryptor
import org.junit.Assert.*
import org.junit.Test
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

class NetEaseCryptorTest {

    @Test
    fun testEncryptionAndDecryptionRoundtrip() {
        val testString = "Hello Minecraft Bedrock LevelDB Data! Test 1234567890."
        val originalBytes = testString.toByteArray(StandardCharsets.UTF_8)

        assertFalse(NetEaseCryptor.hasMagicHeader(originalBytes))

        val encrypted = NetEaseCryptor.encryptData(originalBytes)
        assertTrue(NetEaseCryptor.hasMagicHeader(encrypted))
        assertEquals(originalBytes.size + NetEaseCryptor.HEADER_SIZE, encrypted.size)

        val bis = BufferedInputStream(ByteArrayInputStream(encrypted))
        assertTrue(NetEaseCryptor.isEncrypted(bis))

        val decrypted = NetEaseCryptor.decryptData(encrypted)
        assertFalse(NetEaseCryptor.hasMagicHeader(decrypted))
        assertEquals(originalBytes.size, decrypted.size)

        assertArrayEquals(originalBytes, decrypted)
    }

    @Test
    fun testDeriveKeyFromCurrentAndManifest() {

        val manifestName = "MANIFEST-000001"
        val plainCurrent = (manifestName + "\n").toByteArray(StandardCharsets.UTF_8)
        val encryptedCurrent = NetEaseCryptor.encryptData(plainCurrent, NetEaseCryptor.DEFAULT_KEY)

        val derivedKey = NetEaseCryptor.deriveKey(encryptedCurrent, manifestName)
        assertArrayEquals(NetEaseCryptor.DEFAULT_KEY, derivedKey)
    }

    @Test
    fun testLevelDbSSTableFooterVerification() {

        val dummySSTable = ByteArray(100)
        val sstableMagic = byteArrayOf(
            0xDB.toByte(), 0x47.toByte(), 0x75.toByte(), 0x24.toByte(),
            0x8B.toByte(), 0x80.toByte(), 0xFB.toByte(), 0x57.toByte()
        )
        System.arraycopy(sstableMagic, 0, dummySSTable, dummySSTable.size - 8, 8)

        assertTrue(NetEaseCryptor.verifyLdbFooter(dummySSTable))

        dummySSTable[dummySSTable.size - 1] = 0x00
        assertFalse(NetEaseCryptor.verifyLdbFooter(dummySSTable))
    }

    @Test
    fun testStreamingRoundtripAndAlreadyEncryptedInput() {
        val original = ByteArray(256 * 1024) { (it * 31).toByte() }
        val encryptedOut = ByteArrayOutputStream()
        NetEaseCryptor.processFile(ByteArrayInputStream(original), encryptedOut, decrypt = false, key = NetEaseCryptor.DEFAULT_KEY)
        val encrypted = encryptedOut.toByteArray()
        assertTrue(NetEaseCryptor.hasMagicHeader(encrypted))

        val decryptedOut = ByteArrayOutputStream()
        assertTrue(NetEaseCryptor.processFile(ByteArrayInputStream(encrypted), decryptedOut, decrypt = true, key = NetEaseCryptor.DEFAULT_KEY))
        assertArrayEquals(original, decryptedOut.toByteArray())

        val unchangedOut = ByteArrayOutputStream()
        NetEaseCryptor.processFile(ByteArrayInputStream(encrypted), unchangedOut, decrypt = false, key = NetEaseCryptor.DEFAULT_KEY)
        assertArrayEquals(encrypted, unchangedOut.toByteArray())
    }

    @Test(expected = IllegalArgumentException::class)
    fun testDeriveKeyRejectsInvalidCurrent() {
        NetEaseCryptor.deriveKey(
            NetEaseCryptor.encryptData("wrong".toByteArray(), NetEaseCryptor.DEFAULT_KEY),
            "MANIFEST-000001"
        )
    }
}
