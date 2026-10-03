package com.noches.chunkoidng.core.midi.exporters

import com.noches.chunkoidng.core.midi.NoteBlockSong
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

object NbsExporter {

    private const val MAX_NBS_VALUE = 65_535

    fun export(song: NoteBlockSong, outputFile: File) {
        val bytes = toByteArray(song)
        outputFile.parentFile?.mkdirs()
        outputFile.writeBytes(bytes)
    }

    fun toByteArray(song: NoteBlockSong): ByteArray {
        require(song.lengthTicks in 0..MAX_NBS_VALUE) { "NBS song length is out of range" }
        require(song.notes.size <= 1_000_000) { "NBS song contains too many notes" }
        val stream = ByteArrayOutputStream()

        writeShort(stream, 0)
        stream.write(4)
        stream.write(16)
        writeShort(stream, song.lengthTicks.toShort())

        val notesByTick = song.notes.groupBy { it.tick }.toSortedMap()
        val maxLayer = notesByTick.values.maxOfOrNull { it.size } ?: 1
        require(maxLayer in 1..MAX_NBS_VALUE) { "NBS layer count is out of range" }
        writeShort(stream, maxLayer.toShort())

        writeString(stream, song.title)
        writeString(stream, song.author)
        writeString(stream, song.author)
        writeString(stream, song.description)

        val tempoHundredths = (song.speedTicksPerSec * 100.0).toInt().toShort()
        writeShort(stream, tempoHundredths)

        stream.write(0)
        stream.write(10)
        stream.write(4)
        writeInt(stream, 0)
        writeInt(stream, 0)
        writeInt(stream, 0)
        writeInt(stream, 0)
        writeInt(stream, 0)
        writeString(stream, "")
        stream.write(0)
        stream.write(0)
        writeShort(stream, 0)

        var lastTick = -1
        for ((tick, tickNotes) in notesByTick) {
            require(tick >= 0 && tick >= lastTick) { "NBS tick order is invalid" }
            val tickDelta = tick - lastTick
            require(tickDelta in 0..MAX_NBS_VALUE) { "NBS tick delta is out of range" }
            lastTick = tick
            writeShort(stream, tickDelta.toShort())

            var lastLayer = -1
            for (idx in tickNotes.indices) {
                val layer = idx
                val layerDelta = layer - lastLayer
                require(layerDelta in 0..MAX_NBS_VALUE) { "NBS layer delta is out of range" }
                lastLayer = layer
                writeShort(stream, layerDelta.toShort())

                val note = tickNotes[idx]
                stream.write(note.instrument.id)
                val nbsKey = (note.key + 33).coerceIn(0, 87)
                stream.write(nbsKey)
                stream.write(note.velocity.coerceIn(0, 100))
                stream.write(100)
                writeShort(stream, 0)
            }
            writeShort(stream, 0)
        }
        writeShort(stream, 0)

        for (l in 0 until maxLayer) {
            writeString(stream, "Layer $l")
            stream.write(0)
            stream.write(100)
            stream.write(100)
        }

        stream.write(0)

        return stream.toByteArray()
    }

    private fun writeShort(stream: ByteArrayOutputStream, value: Short) {
        stream.write(value.toInt() and 0xFF)
        stream.write((value.toInt() shr 8) and 0xFF)
    }

    private fun writeInt(stream: ByteArrayOutputStream, value: Int) {
        stream.write(value and 0xFF)
        stream.write((value shr 8) and 0xFF)
        stream.write((value shr 16) and 0xFF)
        stream.write((value shr 24) and 0xFF)
    }

    private fun writeString(stream: ByteArrayOutputStream, text: String) {
        val bytes = text.toByteArray(Charsets.UTF_8)
        writeInt(stream, bytes.size)
        stream.write(bytes)
    }
}
