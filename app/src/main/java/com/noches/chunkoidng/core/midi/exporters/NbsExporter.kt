package com.noches.chunkoidng.core.midi.exporters

import com.noches.chunkoidng.core.midi.NoteBlockSong
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

object NbsExporter {

    fun export(song: NoteBlockSong, outputFile: File) {
        val bytes = toByteArray(song)
        outputFile.parentFile?.mkdirs()
        outputFile.writeBytes(bytes)
    }

    fun toByteArray(song: NoteBlockSong): ByteArray {
        val stream = ByteArrayOutputStream()

        writeShort(stream, 0)
        stream.write(4)
        stream.write(16)
        writeShort(stream, song.lengthTicks.toShort())

        val notesByTick = song.notes.groupBy { it.tick }.toSortedMap()
        val maxLayer = notesByTick.values.maxOfOrNull { it.size } ?: 1
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
            val tickDelta = (tick - lastTick).toShort()
            lastTick = tick
            writeShort(stream, tickDelta)

            var lastLayer = -1
            for (idx in tickNotes.indices) {
                val layer = idx
                val layerDelta = (layer - lastLayer).toShort()
                lastLayer = layer
                writeShort(stream, layerDelta)

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
