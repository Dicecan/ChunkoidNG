package com.noches.chunkoidng.core.decryptor

import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

/**
 * NetEase Minecraft Bedrock LevelDB XOR encryption/decryption algorithm helper.
 * References:
 * - Chunkoid NetEaseDecryptor
 * - HTMonkeyG/XOR-MC-Archive-Decrypt (XOREncryptHelper)
 */
object NetEaseCryptor {

    /** Standard NetEase LevelDB magic headers */
    val MAGIC_HEADER = byteArrayOf(0x80.toByte(), 0x1D.toByte(), 0x30.toByte(), 0x01.toByte())
    val SECONDARY_MAGIC_HEADER = byteArrayOf(0x90.toByte(), 0x1D.toByte(), 0x30.toByte(), 0x01.toByte())
    const val HEADER_SIZE = 4

    /** Default passive key used by NetEase Minecraft Bedrock ("88329851") */
    val DEFAULT_KEY: ByteArray = "88329851".toByteArray(StandardCharsets.US_ASCII)

    /** LevelDB SSTable footer magic number (0x57FB808B247547DB in little-endian) */
    val LEVELDB_MAGIC = byteArrayOf(
        0xDB.toByte(), 0x47.toByte(), 0x75.toByte(), 0x24.toByte(),
        0x8B.toByte(), 0x80.toByte(), 0xFB.toByte(), 0x57.toByte()
    )

    /**
     * Checks if data starts with NetEase magic header.
     */
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

    /**
     * Derives XOR key from encrypted CURRENT file and MANIFEST filename.
     * NetEase XORs CURRENT content with (manifestFileName + "\n").
     */
    fun deriveKey(currentBytes: ByteArray, manifestName: String): ByteArray {
        if (currentBytes.size < HEADER_SIZE || !hasMagicHeader(currentBytes)) {
            // Not encrypted or too short; fallback to default key
            return DEFAULT_KEY
        }

        val encryptedBody = currentBytes.copyOfRange(HEADER_SIZE, currentBytes.size)
        val manifestBytes = manifestName.toByteArray(StandardCharsets.UTF_8)
        val sourceBytes = ByteArray(manifestBytes.size + 1)
        manifestBytes.copyInto(sourceBytes)
        sourceBytes[sourceBytes.size - 1] = 0x0A.toByte() // Newline

        val rawKey = ByteArray(encryptedBody.size)
        for (i in encryptedBody.indices) {
            rawKey[i] = (encryptedBody[i].toInt() xor sourceBytes[i % sourceBytes.size].toInt()).toByte()
        }

        // Check if rawKey has repeating 8-byte pattern
        if (rawKey.size >= 8) {
            var isRepeating = true
            for (i in 8 until rawKey.size) {
                if (rawKey[i] != rawKey[i % 8]) {
                    isRepeating = false
                    break
                }
            }
            if (isRepeating) {
                return rawKey.copyOfRange(0, 8)
            }
        }

        return if (rawKey.isNotEmpty()) rawKey else DEFAULT_KEY
    }

    /**
     * Decrypts a file: if magic header is present, strips header and XORs with key.
     */
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

    /**
     * Encrypts a file: adds NetEase magic header (0x801D3001) and XORs data with key.
     */
    fun encryptData(data: ByteArray, key: ByteArray = DEFAULT_KEY): ByteArray {
        if (hasMagicHeader(data)) {
            // Already encrypted
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

    /**
     * Verifies whether an .ldb file has the standard LevelDB footer magic number.
     * Useful to confirm decryption correctness.
     */
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
}
