package com.noches.chunkoidng.maid.defense

import com.noches.chunkoidng.maid.audit.SecurityAuditor
import com.noches.chunkoidng.maid.model.ViolationType

class BinaryGuard(private val securityAuditor: SecurityAuditor) {

    val NETEASE_PRIMARY_MAGIC = byteArrayOf(0x80.toByte(), 0x1D.toByte(), 0x30.toByte(), 0x01.toByte())
    val NETEASE_SECONDARY_MAGIC = byteArrayOf(0x90.toByte(), 0x1D.toByte(), 0x30.toByte(), 0x01.toByte())
    val MIDI_HEADER_MAGIC = byteArrayOf(0x4D.toByte(), 0x54.toByte(), 0x68.toByte(), 0x64.toByte())

    fun matchesMagic(header: ByteArray, expected: ByteArray): Boolean {
        if (header.size < expected.size) return false
        for (i in expected.indices) {
            if (header[i] != expected[i]) return false
        }
        return true
    }

    fun verifyRange(
        offset: Long,
        length: Int,
        totalCapacity: Long,
        module: String = "BinaryGuard",
        target: String? = null
    ) {
        if (offset < 0 || length < 0 || (offset + length) > totalCapacity) {
            securityAuditor.recordBlock(
                type = ViolationType.BUFFER_OVERFLOW_ATTEMPT,
                module = module,
                message = "二进制读取越界拦截: offset=$offset, length=$length, capacity=$totalCapacity",
                pathOrTarget = target
            )
            throw IndexOutOfBoundsException("Invalid binary read range: offset=$offset, length=$length, capacity=$totalCapacity")
        }
    }

    fun checkFileSizeWithinLimit(
        size: Long,
        maxBytes: Long,
        type: ViolationType,
        module: String = "BinaryGuard",
        target: String? = null
    ) {
        if (size > maxBytes) {
            securityAuditor.recordBlock(
                type = type,
                module = module,
                message = "文件大小 ($size bytes) 超出安全上限 ($maxBytes bytes)",
                pathOrTarget = target,
                requestedValue = size
            )
            throw IllegalArgumentException("File size exceeds limit: $size > $maxBytes bytes")
        }
    }
}
