package com.noches.chunkoidng.core.leveldb

import org.iq80.leveldb.env.DbLock
import org.iq80.leveldb.env.Env
import org.iq80.leveldb.env.File
import org.iq80.leveldb.env.RandomInputFile
import org.iq80.leveldb.env.SequentialFile
import org.iq80.leveldb.env.WritableFile
import org.iq80.leveldb.fileenv.EnvImpl
import org.iq80.leveldb.Logger
import java.io.RandomAccessFile
import java.nio.ByteBuffer

class SafeEnv : Env {
    private val delegate = EnvImpl.createEnv()

    override fun nowMicros(): Long = delegate.nowMicros()
    override fun toFile(path: String): File = delegate.toFile(path)
    override fun createTempDir(prefix: String): File = delegate.createTempDir(prefix)
    override fun newSequentialFile(file: File): SequentialFile = delegate.newSequentialFile(file)
    override fun newWritableFile(file: File): WritableFile = delegate.newWritableFile(file)
    override fun newAppendableFile(file: File): WritableFile = delegate.newAppendableFile(file)
    override fun writeStringToFileSync(file: File, content: String) = delegate.writeStringToFileSync(file, content)
    override fun readFileToString(file: File): String = delegate.readFileToString(file)
    override fun newLogger(file: File): Logger = delegate.newLogger(file)
    override fun tryLock(file: File): DbLock = delegate.tryLock(file)

    override fun newRandomAccessFile(file: File): RandomInputFile {
        return SafeRandomInputFile(java.io.File(file.path))
    }
}

class SafeRandomInputFile(file: java.io.File) : RandomInputFile {
    private val raf = RandomAccessFile(file, "r")
    private val channel = raf.channel

    override fun size(): Long = raf.length()

    override fun read(offset: Long, length: Int): ByteBuffer {
        require(offset >= 0 && length >= 0) { "Invalid read range" }
        val available = (raf.length() - offset).coerceAtLeast(0L).coerceAtMost(length.toLong()).toInt()
        val buffer = ByteBuffer.allocate(available)
        var totalRead = 0
        while (totalRead < available) {
            val bytesRead = channel.read(buffer, offset + totalRead)
            if (bytesRead <= 0) {
                break
            }
            totalRead += bytesRead
        }
        buffer.flip()
        return buffer
    }

    override fun close() {
        raf.close()
    }
}
