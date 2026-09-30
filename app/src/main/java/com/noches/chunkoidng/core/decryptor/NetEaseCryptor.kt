package com.noches.chunkoidng.core.decryptor

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

object NetEaseCryptor {

    val MAGIC_HEADER = byteArrayOf(0x80.toByte(), 0x1D.toByte(), 0x30.toByte(), 0x01.toByte())
    val SECONDARY_MAGIC_HEADER = byteArrayOf(0x90.toByte(), 0x1D.toByte(), 0x30.toByte(), 0x01.toByte())
    const val HEADER_SIZE = 4

    val DEFAULT_KEY: ByteArray = "88329851".toByteArray(StandardCharsets.US_ASCII)

    val LEVELDB_MAGIC = byteArrayOf(
        0xDB.toByte(), 0x47.toByte(), 0x75.toByte(), 0x24.toByte(),
        0x8B.toByte(), 0x80.toByte(), 0xFB.toByte(), 0x57.toByte()
    )

    fun hasMagicHeader(data: ByteArray): Boolean {
        if (data.size < HEADER_SIZE) return false
        val isPrimary = data[0] == MAGIC_HEADER[0] &&
                data[1] == MAGIC_HEADER[1] &&
                data[2] == MAGIC_HEADER[2] &&
                data[3] == MAGIC_HEADER[3]
        if (isPrimary) return true

        return data[0] == SECONDARY_MAGIC_HEADER[0] &&
                data[1] == SECONDARY_MAGIC_HEADER[1] &&
                data[2] == SECONDARY_MAGIC_HEADER[2] &&
                data[3] == SECONDARY_MAGIC_HEADER[3]
    }

    fun isEncrypted(inputStream: InputStream): Boolean {
        if (!inputStream.markSupported()) return false
        inputStream.mark(HEADER_SIZE)
        val header = ByteArray(HEADER_SIZE)
        val read = inputStream.read(header)
        inputStream.reset()
        if (read < HEADER_SIZE) return false
        return hasMagicHeader(header)
    }

    fun deriveKey(currentBytes: ByteArray, manifestName: String): ByteArray {
        if (currentBytes.size < HEADER_SIZE || !hasMagicHeader(currentBytes)) {

            return DEFAULT_KEY
        }

        val encryptedBody = currentBytes.copyOfRange(HEADER_SIZE, currentBytes.size)
        val manifestBytes = manifestName.toByteArray(StandardCharsets.UTF_8)
        val sourceBytes = ByteArray(manifestBytes.size + 1)
        manifestBytes.copyInto(sourceBytes)
        sourceBytes[sourceBytes.size - 1] = 0x0A.toByte()

        val rawKey = ByteArray(encryptedBody.size)
        for (i in encryptedBody.indices) {
            rawKey[i] = (encryptedBody[i].toInt() xor sourceBytes[i % sourceBytes.size].toInt()).toByte()
        }

        val expected = (manifestName + "\n").toByteArray(StandardCharsets.UTF_8)
        for (length in listOf(8, DEFAULT_KEY.size, rawKey.size).distinct()) {
            if (length == 0 || rawKey.size < length) continue
            val candidate = rawKey.copyOf(length)
            val decoded = ByteArray(expected.size)
            for (i in expected.indices) {
                decoded[i] = (encryptedBody[i % encryptedBody.size].toInt() xor
                        candidate[i % candidate.size].toInt()).toByte()
            }
            if (decoded.contentEquals(expected)) return candidate
        }
        throw IllegalArgumentException("Cannot verify Netease world key from CURRENT")
    }

    fun decryptData(data: ByteArray, key: ByteArray = DEFAULT_KEY): ByteArray {
        if (!hasMagicHeader(data)) {
            return data
        }
        val effectiveKey = if (key.isNotEmpty()) key else DEFAULT_KEY
        val body = data.copyOfRange(HEADER_SIZE, data.size)
        val result = ByteArray(body.size)
        for (i in body.indices) {
            result[i] = (body[i].toInt() xor effectiveKey[i % effectiveKey.size].toInt()).toByte()
        }
        return result
    }

    fun encryptData(data: ByteArray, key: ByteArray = DEFAULT_KEY): ByteArray {
        if (hasMagicHeader(data)) {

            return data
        }
        val effectiveKey = if (key.isNotEmpty()) key else DEFAULT_KEY
        val result = ByteArray(HEADER_SIZE + data.size)
        System.arraycopy(MAGIC_HEADER, 0, result, 0, HEADER_SIZE)

        for (i in data.indices) {
            result[HEADER_SIZE + i] = (data[i].toInt() xor effectiveKey[i % effectiveKey.size].toInt()).toByte()
        }
        return result
    }

    fun verifyLdbFooter(data: ByteArray): Boolean {
        if (data.size < 8) return false
        val offset = data.size - 8
        for (i in 0 until 8) {
            if (data[offset + i] != LEVELDB_MAGIC[i]) {
                return false
            }
        }
        return true
    }

    fun keyToHexString(key: ByteArray): String {
        return key.joinToString("") { "%02X".format(it) }
    }

    fun processFile(input: InputStream, output: OutputStream, decrypt: Boolean, key: ByteArray): Boolean {
        require(key.isNotEmpty()) { "Key cannot be empty" }
        val source = BufferedInputStream(input, BUFFER_SIZE)
        val target = BufferedOutputStream(output, BUFFER_SIZE)
        val header = ByteArray(HEADER_SIZE)
        var read = 0
        while (read < HEADER_SIZE) {
            val count = source.read(header, read, HEADER_SIZE - read)
            if (count < 0) break
            read += count
        }
        val encrypted = read == HEADER_SIZE && hasMagicHeader(header)
        if (decrypt && encrypted) {
            xorCopy(source, target, key)
        } else if (!decrypt && !encrypted) {
            target.write(MAGIC_HEADER)
            xorWrite(header, read, target, key, 0)
            xorCopy(source, target, key, read)
        } else {
            if (read > 0) target.write(header, 0, read)
            source.copyTo(target, BUFFER_SIZE)
        }
        target.flush()
        return encrypted
    }

    private fun xorCopy(input: InputStream, output: OutputStream, key: ByteArray, offset: Int = 0) {
        val buffer = ByteArray(BUFFER_SIZE)
        var position = offset
        var count: Int
        while (input.read(buffer).also { count = it } != -1) {
            xorWrite(buffer, count, output, key, position)
            position += count
        }
    }

    private fun xorWrite(data: ByteArray, count: Int, output: OutputStream, key: ByteArray, offset: Int) {
        for (i in 0 until count) data[i] = (data[i].toInt() xor key[(offset + i) % key.size].toInt()).toByte()
        output.write(data, 0, count)
    }

    private const val BUFFER_SIZE = 64 * 1024
}
