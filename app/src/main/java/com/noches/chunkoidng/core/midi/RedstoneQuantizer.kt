package com.noches.chunkoidng.core.midi

import kotlin.math.roundToInt

data class QuantizationConfig(
    val ticksPerSecond: Double = 10.0,
    val autoTransposition: Boolean = true,
    val manualSemitoneShift: Int = 0,
    val trackInstrumentOverrides: Map<Int, NoteBlockInstrument> = emptyMap()
)

object RedstoneQuantizer {

    private const val MAX_OUTPUT_TICKS = 2_000_000

    fun quantize(
        parsedSong: ParsedMidiSong,
        config: QuantizationConfig = QuantizationConfig()
    ): NoteBlockSong {
        require(config.ticksPerSecond.isFinite() && config.ticksPerSecond in 0.1..100.0) {
            "ticksPerSecond must be between 0.1 and 100"
        }
        require(config.manualSemitoneShift in -120..120) {
            "manualSemitoneShift must be between -120 and 120"
        }
        val msPerTick = 1000.0 / config.ticksPerSecond

        val bestShift = if (config.autoTransposition) {
            findOptimalShift(parsedSong.notes)
        } else {
            config.manualSemitoneShift
        }

        val quantizedNotes = mutableListOf<NoteBlockNote>()
        var maxTick = 0

        for (raw in parsedSong.notes) {
            val calculatedTick = (raw.startMs / msPerTick).roundToInt()
            require(calculatedTick in 0..MAX_OUTPUT_TICKS) { "MIDI timeline is too long to export" }
            val tick = calculatedTick
            if (tick > maxTick) maxTick = tick

            val isPercussion = raw.channel == 9
            val instrument: NoteBlockInstrument = if (isPercussion) {
                NoteBlockInstrument.mapPercussionNote(raw.pitch)
            } else {
                config.trackInstrumentOverrides[raw.channel]
                    ?: NoteBlockInstrument.mapGeneralMidiProgram(raw.program)
            }

            val key = if (isPercussion) {
                12
            } else {
                var p = raw.pitch + bestShift
                while (p < 54) p += 12
                while (p > 78) p -= 12
                val finalKey = p - 54
                finalKey.coerceIn(0, 24)
            }

            quantizedNotes.add(
                NoteBlockNote(
                    tick = tick,
                    instrument = instrument,
                    key = key,
                    velocity = raw.velocity
                )
            )
        }

        quantizedNotes.sortWith(compareBy({ it.tick }, { it.instrument.id }, { it.key }))

        return NoteBlockSong(
            title = parsedSong.title,
            speedTicksPerSec = config.ticksPerSecond,
            notes = quantizedNotes,
            lengthTicks = maxTick
        )
    }

    fun findOptimalShift(notes: List<RawMidiNote>): Int {
        val melodicNotes = notes.filter { it.channel != 9 }
        if (melodicNotes.isEmpty()) return 0

        var bestShift = 0
        var maxInRange = -1

        val candidates = mutableListOf<Int>()
        candidates.add(0)
        for (dist in 1..12) {
            candidates.add(dist)
            candidates.add(-dist)
        }

        for (shift in candidates) {
            var inRangeCount = 0
            for (note in melodicNotes) {
                val p = note.pitch + shift
                if (p in 54..78) {
                    inRangeCount++
                }
            }
            if (inRangeCount > maxInRange) {
                maxInRange = inRangeCount
                bestShift = shift
            }
        }

        return bestShift
    }
}
