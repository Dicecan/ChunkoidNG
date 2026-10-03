package br.com.gamemods.nbtmanipulator

import java.io.*
import java.lang.Double.doubleToLongBits
import java.lang.Float.floatToIntBits
import java.lang.Long.reverseBytes

public class LittleEndianDataOutputStream(out: OutputStream) : FilterOutputStream(DataOutputStream(out)), DataOutput {
    private val data = this.out as DataOutputStream
    override fun writeBoolean(v: Boolean) {
        data.writeBoolean(v)
    }

    override fun writeByte(v: Int) {
        data.writeByte(v)
    }

    override fun writeShort(v: Int) {
        out.write(v and 0xFF)
        out.write((v ushr 8) and 0xFF)
    }

    override fun writeChar(v: Int) {
        writeShort(v)
    }

    override fun writeInt(v: Int) {
        out.write(v and 0xFF)
        out.write((v ushr 8) and 0xFF)
        out.write((v ushr 16) and 0xFF)
        out.write((v ushr 24) and 0xFF)
    }

    override fun writeLong(v: Long) {
        data.writeLong(reverseBytes(v))
    }

    override fun writeFloat(v: Float) {
        writeInt(floatToIntBits(v))
    }

    override fun writeDouble(v: Double) {
        writeLong(doubleToLongBits(v))
    }

    override fun writeBytes(s: String) {
        data.writeBytes(s)
    }

    override fun writeChars(s: String) {
        s.forEach { char ->
            writeChar(char.code)
        }
    }

    override fun writeUTF(s: String) {
        val bytes = s.toByteArray(Charsets.UTF_8)
        val length = bytes.size
        if (length > 65535) {
            throw UTFDataFormatException("encoded string too long: $length bytes")
        }
        writeShort(length)
        write(bytes)
    }
}
