package com.noches.chunkoidng.core.midi

import com.noches.chunkoidng.maid.Maid
import com.noches.chunkoidng.maid.model.ViolationType
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class RawMidiNote(
    val channel: Int,
    val pitch: Int,
    val velocity: Int,
    val tick: Long,
    val startMs: Long,
    val program: Int
)

data class MidiTrackInfo(
    val trackIndex: Int,
    val trackName: String,
    val channel: Int,
    val program: Int,
    val noteCount: Int
)

data class ParsedMidiSong(
    val title: String,
    val format: Int,
    val division: Int,
    val durationMs: Long,
    val initialBpm: Double,
    val tracks: List<MidiTrackInfo>,
    val notes: List<RawMidiNote>
)

object MidiParser {

    private const val MAX_TRACK_BYTES = 64 * 1024 * 1024
    private const val MAX_TRACKS = 1024

    private fun getMaxMidiBytes(): Long {
        return try {
            Maid.tuning.limits.midiMaxFileBytes
        } catch (_: Exception) {
            128L * 1024 * 1024
        }
    }

    private fun getMaxNotes(): Int {
        return try {
            Maid.tuning.limits.midiMaxNotes
        } catch (_: Exception) {
            1_000_000
        }
    }

    private data class TempoChange(
        val tick: Long,
        val usPerQuarter: Long
    )

    fun parse(file: File): ParsedMidiSong {
        val maxBytes = getMaxMidiBytes()
        Maid.defense.binaryGuard.checkFileSizeWithinLimit(
            size = file.length(),
            maxBytes = maxBytes,
            type = ViolationType.MIDI_FILE_SIZE_LIMIT,
            module = "MidiParser",
            target = file.name
        )
        return parse(file.readBytes(), file.nameWithoutExtension)
    }

    fun parse(bytes: ByteArray, defaultTitle: String = "Untitled"): ParsedMidiSong {
        val maxBytes = getMaxMidiBytes()
        Maid.defense.binaryGuard.checkFileSizeWithinLimit(
            size = bytes.size.toLong(),
            maxBytes = maxBytes,
            type = ViolationType.MIDI_FILE_SIZE_LIMIT,
            module = "MidiParser",
            target = defaultTitle
        )
        val stream = ByteArrayInputStream(bytes)

        val headerId = readString(stream, 4)
        if (headerId != "MThd") {
            Maid.securityAuditor.recordBlock(
                type = ViolationType.MALFORMED_HEADER,
                module = "MidiParser",
                message = "非法MIDI文件头标识: $headerId",
                pathOrTarget = defaultTitle
            )
            throw IllegalArgumentException("Invalid MIDI file header: $headerId")
        }

        val headerLength = readInt32(stream)
        require(headerLength >= 6) { "Invalid MIDI header length: $headerLength" }
        require(headerLength <= 1024) { "MIDI header is too large" }
        val format = readInt16(stream)
        val numTracks = readInt16(stream)
        val division = readInt16(stream)
        require(format in 0..2) { "Unsupported MIDI format: $format" }
        require(numTracks in 1..MAX_TRACKS) { "Invalid MIDI track count: $numTracks" }
        require(division > 0 && (division and 0x8000) == 0) {
            "SMPTE MIDI division is not supported"
        }

        if (headerLength > 6) {
            skipFully(stream, (headerLength - 6).toLong())
        }

        val tempoChanges = mutableListOf<TempoChange>()
        tempoChanges.add(TempoChange(0L, 500000L))

        val trackNotes = mutableListOf<MutableList<RawMidiNote>>()
        val trackInfos = mutableListOf<MidiTrackInfo>()
        var songTitle = defaultTitle

        for (trackIdx in 0 until numTracks) {
            val trackChunkId = readString(stream, 4)
            if (trackChunkId != "MTrk") {
                throw IllegalArgumentException("Invalid MIDI track header at index $trackIdx")
            }
            val trackChunkLength = readInt32(stream)
            require(trackChunkLength >= 0 && trackChunkLength <= MAX_TRACK_BYTES) {
                "Invalid MIDI track length: $trackChunkLength"
            }
            require(trackChunkLength <= stream.available()) { "Truncated MIDI track" }
            val trackData = ByteArray(trackChunkLength)
            var bytesRead = 0
            while (bytesRead < trackChunkLength) {
                val count = stream.read(trackData, bytesRead, trackChunkLength - bytesRead)
                if (count < 0) throw IllegalArgumentException("Truncated MIDI track")
                bytesRead += count
            }

            var offset = 0
            var currentTick = 0L
            var runningStatus = 0
            var trackName = "Track ${trackIdx + 1}"
            val channelPrograms = IntArray(16) { 0 }
            val currentTrackNotes = mutableListOf<RawMidiNote>()

            fun readVarInt(): Long {
                var value = 0L
                var b: Int
                do {
                    if (offset >= trackData.size) throw IllegalArgumentException("Truncated MIDI varint")
                    b = trackData[offset++].toInt() and 0xFF
                    value = (value shl 7) or ((b and 0x7F).toLong())
                } while ((b and 0x80) != 0)
                return value
            }

            while (offset < trackData.size) {
                val deltaTick = readVarInt()
                currentTick += deltaTick

                if (offset >= trackData.size) break

                var statusByte = trackData[offset].toInt() and 0xFF
                if ((statusByte and 0x80) != 0) {
                    offset++
                    runningStatus = statusByte
                } else {
                    statusByte = runningStatus
                }

                val eventType = statusByte and 0xF0
                val channel = statusByte and 0x0F

                when (eventType) {
                    0x80 -> {
                        if (offset + 1 < trackData.size) {
                            offset += 2
                        }
                    }
                    0x90 -> {
                        if (offset + 1 < trackData.size) {
                            val pitch = trackData[offset++].toInt() and 0xFF
                            val velocity = trackData[offset++].toInt() and 0xFF
                            if (velocity > 0) {
                                val prog = channelPrograms[channel]
                                currentTrackNotes.add(
                                    RawMidiNote(
                                        channel = channel,
                                        pitch = pitch,
                                        velocity = velocity,
                                        tick = currentTick,
                                        startMs = 0L,
                                        program = prog
                                    )
                                )
                            }
                        }
                    }
                    0xA0, 0xB0, 0xE0 -> {
                        if (offset + 1 < trackData.size) {
                            offset += 2
                        }
                    }
                    0xC0 -> {
                        if (offset < trackData.size) {
                            val prog = trackData[offset++].toInt() and 0xFF
                            channelPrograms[channel] = prog
                        }
                    }
                    0xD0 -> {
                        if (offset < trackData.size) {
                            offset++
                        }
                    }
                    0xF0 -> {
                        if (statusByte == 0xFF) {
                            if (offset < trackData.size) {
                                val metaType = trackData[offset++].toInt() and 0xFF
                                val metaLengthLong = readVarInt()
                                require(metaLengthLong <= Int.MAX_VALUE) { "MIDI meta event is too large" }
                                val metaLength = metaLengthLong.toInt()
                                require(metaLength <= trackData.size - offset) { "Truncated MIDI meta event" }
                                val metaBytes = trackData.copyOfRange(offset, offset + metaLength)
                                offset += metaLength

                                when (metaType) {
                                    0x51 -> {
                                        if (metaBytes.size >= 3) {
                                            val usPerQuarter = ((metaBytes[0].toInt() and 0xFF) shl 16) or
                                                    ((metaBytes[1].toInt() and 0xFF) shl 8) or
                                                    (metaBytes[2].toInt() and 0xFF)
                                            tempoChanges.add(TempoChange(currentTick, usPerQuarter.toLong()))
                                        }
                                    }
                                    0x03 -> {
                                        val name = String(metaBytes, Charsets.UTF_8).trim { it <= ' ' || it == '\u0000' }
                                        if (name.isNotEmpty()) {
                                            trackName = name
                                            if (trackIdx == 0 && songTitle == defaultTitle) {
                                                songTitle = name
                                            }
                                        }
                                    }
                                }
                                runningStatus = 0
                            }
                        } else {
                            if (statusByte != 0xF0 && statusByte != 0xF7) {
                                val dataBytes = when (statusByte) {
                                    0xF1, 0xF3 -> 1
                                    0xF2 -> 2
                                    else -> 0
                                }
                                require(offset + dataBytes <= trackData.size) { "Truncated MIDI system event" }
                                offset += dataBytes
                            } else {
                                val sysexLengthLong = readVarInt()
                                require(sysexLengthLong <= Int.MAX_VALUE) { "MIDI sysex event is too large" }
                                val sysexLength = sysexLengthLong.toInt()
                                require(sysexLength <= trackData.size - offset) { "Truncated MIDI sysex event" }
                                offset += sysexLength
                            }
                            runningStatus = 0
                        }
                    }
                }
            }

            trackNotes.add(currentTrackNotes)
            val primaryChannel = if (currentTrackNotes.isNotEmpty()) currentTrackNotes.first().channel else 0
            val primaryProgram = channelPrograms[primaryChannel]
            trackInfos.add(
                MidiTrackInfo(
                    trackIndex = trackIdx,
                    trackName = trackName,
                    channel = primaryChannel,
                    program = primaryProgram,
                    noteCount = currentTrackNotes.size
                )
            )
        }

        tempoChanges.sortBy { it.tick }
        val sortedTempos = mutableListOf<TempoChange>()
        var lastTick = -1L
        for (tc in tempoChanges) {
            if (tc.tick == lastTick && sortedTempos.isNotEmpty()) {
                sortedTempos[sortedTempos.size - 1] = tc
            } else {
                sortedTempos.add(tc)
                lastTick = tc.tick
            }
        }

        val allNotes = mutableListOf<RawMidiNote>()
        var maxTimeMs = 0L

        val div = if (division > 0) division else 480

        for (trackList in trackNotes) {
            for (raw in trackList) {
                val ms = calculateTimeMs(raw.tick, sortedTempos, div)
                if (ms > maxTimeMs) maxTimeMs = ms
                allNotes.add(raw.copy(startMs = ms))
            }
        }

        allNotes.sortBy { it.startMs }

        val maxNotes = getMaxNotes()
        val safeNotesCount = Maid.pagingAdvisor.sanitizeMidiNotes(allNotes.size, maxNotes, songTitle)
        val finalNotes = if (allNotes.size > safeNotesCount) allNotes.take(safeNotesCount) else allNotes

        val firstTempo = sortedTempos.firstOrNull()?.usPerQuarter ?: 500000L
        val bpm = 60000000.0 / firstTempo.toDouble()

        return ParsedMidiSong(
            title = songTitle,
            format = format,
            division = div,
            durationMs = maxTimeMs,
            initialBpm = bpm,
            tracks = trackInfos,
            notes = finalNotes
        )
    }

    private fun calculateTimeMs(tick: Long, tempos: List<TempoChange>, division: Int): Long {
        var totalMs = 0.0
        var currentTick = 0L
        var currentUsPerQuarter = tempos[0].usPerQuarter

        for (i in 1 until tempos.size) {
            val nextChange = tempos[i]
            if (tick <= nextChange.tick) {
                val deltaTicks = tick - currentTick
                val ms = (deltaTicks * currentUsPerQuarter).toDouble() / (division * 1000.0)
                totalMs += ms
                return totalMs.toLong()
            } else {
                val deltaTicks = nextChange.tick - currentTick
                val ms = (deltaTicks * currentUsPerQuarter).toDouble() / (division * 1000.0)
                totalMs += ms
                currentTick = nextChange.tick
                currentUsPerQuarter = nextChange.usPerQuarter
            }
        }

        val deltaTicks = tick - currentTick
        val ms = (deltaTicks * currentUsPerQuarter).toDouble() / (division * 1000.0)
        totalMs += ms
        return totalMs.toLong()
    }

    private fun readVariableLength(stream: InputStream): Long {
        var value = 0L
        var b: Int
        do {
            b = stream.read()
            if (b < 0) break
            value = (value shl 7) or ((b and 0x7F).toLong())
        } while ((b and 0x80) != 0)
        return value
    }

    private fun readString(stream: InputStream, length: Int): String {
        val bytes = ByteArray(length)
        readFully(stream, bytes)
        return String(bytes, Charsets.US_ASCII)
    }

    private fun readInt32(stream: InputStream): Int {
        val b0 = readByte(stream)
        val b1 = readByte(stream)
        val b2 = readByte(stream)
        val b3 = readByte(stream)
        return (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
    }

    private fun readInt16(stream: InputStream): Int {
        val b0 = readByte(stream)
        val b1 = readByte(stream)
        return (b0 shl 8) or b1
    }

    private fun readByte(stream: InputStream): Int =
        stream.read().takeIf { it >= 0 } ?: throw IllegalArgumentException("Unexpected end of MIDI file")

    private fun readFully(stream: InputStream, target: ByteArray) {
        var offset = 0
        while (offset < target.size) {
            val count = stream.read(target, offset, target.size - offset)
            if (count < 0) throw IllegalArgumentException("Unexpected end of MIDI file")
            offset += count
        }
    }

    private fun skipFully(stream: InputStream, bytes: Long) {
        var remaining = bytes
        while (remaining > 0) {
            val skipped = stream.skip(remaining)
            if (skipped > 0) {
                remaining -= skipped
            } else {
                readByte(stream)
                remaining--
            }
        }
    }
}
