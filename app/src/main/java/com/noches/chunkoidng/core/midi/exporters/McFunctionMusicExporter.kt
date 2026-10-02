package com.noches.chunkoidng.core.midi.exporters

import com.noches.chunkoidng.core.midi.NoteBlockSong
import com.noches.chunkoidng.core.pack.PackPlatform
import java.io.File
import java.util.Locale

object McFunctionMusicExporter {

    fun exportSingleFunction(
        song: NoteBlockSong,
        platform: PackPlatform,
        outputFile: File
    ) {
        val content = generateSingleFunctionContent(song, platform)
        outputFile.parentFile?.mkdirs()
        outputFile.writeText(content)
    }

    fun generateSingleFunctionContent(
        song: NoteBlockSong,
        platform: PackPlatform
    ): String {
        val builder = StringBuilder()
        val isBedrock = platform == PackPlatform.BEDROCK

        val notesByTick = song.notes.groupBy { it.tick }.toSortedMap()

        for ((tick, notes) in notesByTick) {
            for (note in notes) {
                val sound = if (isBedrock) note.instrument.bedrockSoundId else note.instrument.soundId
                val pitch = String.format(Locale.US, "%.3f", note.calculatePlaysoundPitch())
                val volume = String.format(Locale.US, "%.2f", (note.velocity.toDouble() / 100.0).coerceIn(0.1, 1.0))

                val cmd = if (isBedrock) {
                    "playsound $sound @a ~ ~ ~ $volume $pitch"
                } else {
                    "playsound $sound record @a ~ ~ ~ $volume $pitch"
                }
                builder.append(cmd).append("\n")
            }
        }

        return builder.toString()
    }

    fun exportScheduledDatapack(
        song: NoteBlockSong,
        namespace: String,
        outputDir: File
    ) {
        val functionDir = File(outputDir, "data/$namespace/function")
        functionDir.mkdirs()

        val notesByTick = song.notes.groupBy { it.tick }.toSortedMap()
        val mainFunction = File(functionDir, "play.mcfunction")
        val mainBuilder = StringBuilder()

        for ((tick, notes) in notesByTick) {
            val tickFunction = File(functionDir, "tick_$tick.mcfunction")
            val tickBuilder = StringBuilder()

            for (note in notes) {
                val sound = note.instrument.soundId
                val pitch = String.format(Locale.US, "%.3f", note.calculatePlaysoundPitch())
                val volume = String.format(Locale.US, "%.2f", (note.velocity.toDouble() / 100.0).coerceIn(0.1, 1.0))
                tickBuilder.append("playsound $sound record @a ~ ~ ~ $volume $pitch\n")
            }
            tickFunction.writeText(tickBuilder.toString())

            val gameTicks = tick * 2
            mainBuilder.append("schedule function $namespace:tick_$tick ${gameTicks}t\n")
        }

        mainFunction.writeText(mainBuilder.toString())

        val mcmeta = File(outputDir, "pack.mcmeta")
        mcmeta.writeText(
            """
            {
              "pack": {
                "pack_format": 48,
                "description": "Music Datapack: ${song.title}"
              }
            }
            """.trimIndent()
        )
    }
}
