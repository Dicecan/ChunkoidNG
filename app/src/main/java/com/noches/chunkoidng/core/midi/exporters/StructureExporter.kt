package com.noches.chunkoidng.core.midi.exporters

import br.com.gamemods.nbtmanipulator.*
import com.noches.chunkoidng.core.midi.NoteBlockInstrument
import com.noches.chunkoidng.core.midi.NoteBlockSong
import java.io.File
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.util.zip.GZIPOutputStream

data class BlockPlacement(
    val x: Int,
    val y: Int,
    val z: Int,
    val bedrockName: String,
    val javaStateString: String,
    val bedrockStates: Map<String, Any> = emptyMap(),
    val notePitch: Int? = null,
    val instrument: NoteBlockInstrument? = null
)

data class ContraptionLayout(
    val blocks: List<BlockPlacement>,
    val sizeX: Int,
    val sizeY: Int,
    val sizeZ: Int,
    val buttonPos: Triple<Int, Int, Int>
)

object StructureExporter {

    private const val MAX_BLOCK_PLACEMENTS = 2_000_000
    private const val MAX_STRUCTURE_VOLUME = 8_000_000L
    private const val MAX_COORDINATE = 1_000_000

    fun buildContraption(song: NoteBlockSong): ContraptionLayout {
        val blocks = mutableListOf<BlockPlacement>()
        val notesByTick = song.notes.groupBy { it.tick }.toSortedMap()

        val laneLength = 100
        val laneSpacing = 10

        fun addBlock(
            x: Int, y: Int, z: Int,
            bedrockName: String,
            javaStateString: String,
            bedrockStates: Map<String, Any> = emptyMap(),
            notePitch: Int? = null,
            instrument: NoteBlockInstrument? = null
        ) {
            require(blocks.size < MAX_BLOCK_PLACEMENTS) {
                "Music structure is too large to export"
            }
            require(x in -MAX_COORDINATE..MAX_COORDINATE && z in -MAX_COORDINATE..MAX_COORDINATE) {
                "Music structure coordinate is out of range"
            }
            val effectiveBedrockStates = if (bedrockName == "minecraft:dirt" && bedrockStates.isEmpty()) {
                mapOf("dirt_type" to "normal")
            } else {
                bedrockStates
            }
            blocks.add(
                BlockPlacement(
                    x = x, y = y, z = z,
                    bedrockName = bedrockName,
                    javaStateString = javaStateString,
                    bedrockStates = effectiveBedrockStates,
                    notePitch = notePitch,
                    instrument = instrument
                )
            )
        }

        fun addWire(x: Int, z: Int) {
            addBlock(
                x = x, y = 1, z = z,
                bedrockName = "minecraft:stone",
                javaStateString = "minecraft:stone"
            )
            addBlock(
                x = x, y = 2, z = z,
                bedrockName = "minecraft:redstone_wire",
                javaStateString = "minecraft:redstone_wire[power=0]",
                bedrockStates = mapOf("redstone_signal" to 0)
            )
        }

        fun addRepeater(x: Int, z: Int, dirZ: Int, delay: Int, isEast: Boolean = false) {
            val d = delay.coerceIn(1, 4)
            val dir = if (isEast) 1 else if (dirZ > 0) 2 else 0
            val card = if (isEast) "west" else if (dirZ > 0) "north" else "south"
            val javaFacing = if (isEast) "east" else if (dirZ > 0) "south" else "north"
            addBlock(
                x = x, y = 1, z = z,
                bedrockName = "minecraft:stone",
                javaStateString = "minecraft:stone"
            )
            addBlock(
                x = x, y = 2, z = z,
                bedrockName = "minecraft:unpowered_repeater",
                javaStateString = "minecraft:repeater[delay=$d,facing=$javaFacing,powered=false]",
                bedrockStates = mapOf(
                    "repeater_delay" to (d - 1),
                    "direction" to dir,
                    "minecraft:cardinal_direction" to card
                )
            )
        }

        addBlock(
            x = 0, y = 1, z = 0,
            bedrockName = "minecraft:stone",
            javaStateString = "minecraft:stone"
        )
        addBlock(
            x = 0, y = 2, z = 0,
            bedrockName = "minecraft:stone",
            javaStateString = "minecraft:stone"
        )
        addBlock(
            x = 0, y = 3, z = 0,
            bedrockName = "minecraft:stone_button",
            javaStateString = "minecraft:stone_button[face=floor,facing=north,powered=false]",
            bedrockStates = mapOf("facing_direction" to 1, "button_pressed_bit" to false)
        )
        addWire(0, 1)

        var currentX = 0
        var currentZ = 1
        var dirZ = 1

        fun doUTurn() {
            val midX = currentX + (laneSpacing / 2)
            if (dirZ > 0) {
                val turnZ = laneLength + 2
                addWire(currentX, currentZ + 1)
                addWire(currentX, turnZ)
                for (x in (currentX + 1)..(currentX + laneSpacing - 1)) {
                    if (x == midX) {
                        addRepeater(x, turnZ, 0, 1, isEast = true)
                    } else {
                        addWire(x, turnZ)
                    }
                }
                addWire(currentX + laneSpacing, turnZ)
                addWire(currentX + laneSpacing, turnZ - 1)
                addRepeater(currentX + laneSpacing, laneLength, -1, 1)
                currentX += laneSpacing
                currentZ = laneLength
                dirZ = -1
            } else {
                val turnZ = 0
                addWire(currentX, 1)
                addWire(currentX, 0)
                for (x in (currentX + 1)..(currentX + laneSpacing - 1)) {
                    if (x == midX) {
                        addRepeater(x, 0, 0, 1, isEast = true)
                    } else {
                        addWire(x, 0)
                    }
                }
                addWire(currentX + laneSpacing, 0)
                addWire(currentX + laneSpacing, 1)
                addRepeater(currentX + laneSpacing, 2, 1, 1)
                currentX += laneSpacing
                currentZ = 2
                dirZ = 1
            }
        }

        fun stepTrack(isRepeater: Boolean, delay: Int) {
            if (dirZ > 0) {
                if (currentZ >= laneLength) {
                    doUTurn()
                    currentZ += dirZ
                } else {
                    currentZ += dirZ
                }
            } else {
                if (currentZ <= 2) {
                    doUTurn()
                    currentZ += dirZ
                } else {
                    currentZ += dirZ
                }
            }

            if (isRepeater) {
                addRepeater(currentX, currentZ, dirZ, delay)
            } else {
                addWire(currentX, currentZ)
            }
        }

        var lastTick = 0L

        for ((tick, noteList) in notesByTick) {
            val rawDelta = (tick.toLong() - lastTick).coerceAtLeast(0L)
            require(rawDelta <= 1_000_000L) { "Music timeline gap is too large to export" }
            val delta = rawDelta * 2L

            if (dirZ > 0 && currentZ >= laneLength - 2) {
                while (currentZ < laneLength) {
                    stepTrack(false, 0)
                }
                doUTurn()
            } else if (dirZ < 0 && currentZ <= 4) {
                while (currentZ > 2) {
                    stepTrack(false, 0)
                }
                doUTurn()
            }

            if (delta in 1L..4L) {
                stepTrack(true, delta.toInt())
                stepTrack(false, 0)
            } else if (delta in 5L..8L) {
                stepTrack(true, 4)
                stepTrack(true, (delta - 4L).toInt())
            } else if (delta > 8) {
                var rem = delta
                while (rem > 0) {
                    val rDelay = minOf(rem, 4L)
                    stepTrack(true, rDelay.toInt())
                    rem -= rDelay
                }
            }

            stepTrack(false, 0)
            val busZ = currentZ

            noteList.forEachIndexed { index, note ->
                val xOffset = if (index % 2 == 0) (index / 2 + 2) else -(index / 2 + 2)
                val targetX = currentX + xOffset
                val stepX = if (xOffset > 0) 1 else -1

                var wireX = currentX + stepX
                while (if (stepX > 0) wireX <= targetX else wireX >= targetX) {
                    addWire(wireX, busZ)
                    wireX += stepX
                }

                val repDir = if (dirZ > 0) 2 else 0
                val repCard = if (dirZ > 0) "north" else "south"
                val repJava = if (dirZ > 0) "south" else "north"

                addBlock(
                    x = targetX, y = 1, z = busZ + dirZ,
                    bedrockName = "minecraft:stone",
                    javaStateString = "minecraft:stone"
                )
                addBlock(
                    x = targetX, y = 2, z = busZ + dirZ,
                    bedrockName = "minecraft:unpowered_repeater",
                    javaStateString = "minecraft:repeater[delay=1,facing=$repJava,powered=false]",
                    bedrockStates = mapOf(
                        "repeater_delay" to 0,
                        "direction" to repDir,
                        "minecraft:cardinal_direction" to repCard
                    )
                )

                addBlock(
                    x = targetX, y = 1, z = busZ + 2 * dirZ,
                    bedrockName = note.instrument.baseBlockId,
                    javaStateString = note.instrument.baseBlockId
                )
                addBlock(
                    x = targetX, y = 2, z = busZ + 2 * dirZ,
                    bedrockName = "minecraft:noteblock",
                    javaStateString = "minecraft:note_block[instrument=${note.instrument.name.lowercase()},note=${note.key},powered=false]",
                    notePitch = note.key,
                    instrument = note.instrument
                )
            }

            lastTick = tick.toLong()
        }

        val minX = blocks.minOfOrNull { it.x } ?: 0
        val maxX = blocks.maxOfOrNull { it.x } ?: 0
        val minY = blocks.minOfOrNull { it.y } ?: 0
        val maxY = blocks.maxOfOrNull { it.y } ?: 3
        val minZ = blocks.minOfOrNull { it.z } ?: 0
        val maxZ = blocks.maxOfOrNull { it.z } ?: currentZ

        val shiftX = -minX
        val shiftY = -minY
        val shiftZ = -minZ

        val shiftedBlocks = blocks.distinctBy { Triple(it.x, it.y, it.z) }.map {
            it.copy(x = it.x + shiftX, y = it.y + shiftY, z = it.z + shiftZ)
        }

        val sizeX = maxX - minX + 1
        val sizeY = maxY - minY + 2
        val sizeZ = maxZ - minZ + 1

        val buttonPos = Triple(0 + shiftX, 3 + shiftY, 0 + shiftZ)

        return ContraptionLayout(
            blocks = shiftedBlocks,
            sizeX = sizeX,
            sizeY = sizeY,
            sizeZ = sizeZ,
            buttonPos = buttonPos
        )
    }

    fun exportBedrockMcStructure(song: NoteBlockSong, outputFile: File) {
        val layout = buildContraption(song)
        val paletteList = mutableListOf<NbtCompound>()
        val paletteIndexMap = mutableMapOf<String, Int>()

        fun getOrAddPalette(name: String, states: Map<String, Any>): Int {
            val effectiveStates = if (name == "minecraft:dirt" && states.isEmpty()) {
                mapOf("dirt_type" to "normal")
            } else {
                states
            }
            val key = "$name|$effectiveStates"
            paletteIndexMap[key]?.let { return it }

            val statesCompound = NbtCompound()
            effectiveStates.forEach { (k, v) ->
                when (v) {
                    is Int -> statesCompound[k] = NbtInt(v)
                    is Byte -> statesCompound[k] = NbtByte(v)
                    is String -> statesCompound[k] = NbtString(v)
                    is Boolean -> statesCompound[k] = NbtByte(if (v) 1 else 0)
                }
            }

            val entry = NbtCompound().apply {
                this["name"] = NbtString(name)
                this["states"] = statesCompound
                this["version"] = NbtInt(18090528)
            }
            val idx = paletteList.size
            paletteList.add(entry)
            paletteIndexMap[key] = idx
            return idx
        }

        val totalBlocksLong = layout.sizeX.toLong() * layout.sizeY.toLong() * layout.sizeZ.toLong()
        require(totalBlocksLong in 1..MAX_STRUCTURE_VOLUME) {
            "Music structure volume is too large to export"
        }
        val totalBlocks = totalBlocksLong.toInt()
        val layer0 = IntArray(totalBlocks) { -1 }
        val layer1 = IntArray(totalBlocks) { -1 }
        val blockPositionData = NbtCompound()

        for (block in layout.blocks) {
            val flatIndex = (block.x * layout.sizeY + block.y) * layout.sizeZ + block.z
            if (flatIndex in 0 until totalBlocks) {
                layer0[flatIndex] = getOrAddPalette(block.bedrockName, block.bedrockStates)

                if (block.notePitch != null) {
                    val entityData = NbtCompound().apply {
                        this["id"] = NbtString("Music")
                        this["note"] = NbtByte(block.notePitch.toByte())
                        this["x"] = NbtInt(block.x)
                        this["y"] = NbtInt(block.y)
                        this["z"] = NbtInt(block.z)
                    }
                    val blockEntry = NbtCompound().apply {
                        this["block_entity_data"] = entityData
                    }
                    blockPositionData[flatIndex.toString()] = blockEntry
                }
            }
        }

        val negOne = NbtInt(-1)
        val list0 = ArrayList<NbtInt>(totalBlocks)
        val list1 = ArrayList<NbtInt>(totalBlocks)
        for (i in 0 until totalBlocks) {
            val v = layer0[i]
            list0.add(if (v == -1) negOne else NbtInt(v))
            list1.add(negOne)
        }

        val structureCompound = NbtCompound().apply {
            this["block_indices"] = NbtList(
                NbtList(list0),
                NbtList(list1)
            )
            this["entities"] = NbtList<NbtCompound>()
            this["palette"] = NbtCompound().apply {
                this["default"] = NbtCompound().apply {
                    this["block_palette"] = NbtList(paletteList)
                    this["block_position_data"] = blockPositionData
                }
            }
        }

        val root = NbtCompound().apply {
            this["format_version"] = NbtInt(1)
            this["size"] = NbtList(NbtInt(layout.sizeX), NbtInt(layout.sizeY), NbtInt(layout.sizeZ))
            this["structure_world_origin"] = NbtList(NbtInt(0), NbtInt(0), NbtInt(0))
            this["structure"] = structureCompound
        }

        outputFile.parentFile?.mkdirs()
        FileOutputStream(outputFile).use { fos ->
            NbtIO.writeNbtFile(fos, NbtFile("", root), compressed = false, littleEndian = true)
        }
    }

    fun exportJavaSchematic(song: NoteBlockSong, outputFile: File) {
        val layout = buildContraption(song)
        val paletteMap = mutableMapOf<String, Int>()
        paletteMap["minecraft:air"] = 0
        var nextPaletteId = 1

        val totalBlocksLong = layout.sizeX.toLong() * layout.sizeY.toLong() * layout.sizeZ.toLong()
        require(totalBlocksLong in 1..MAX_STRUCTURE_VOLUME) {
            "Music schematic volume is too large to export"
        }
        val totalBlocks = totalBlocksLong.toInt()
        val blockPaletteIds = IntArray(totalBlocks)

        for (block in layout.blocks) {
            val paletteId = paletteMap.getOrPut(block.javaStateString) { nextPaletteId++ }
            val index = (block.y * layout.sizeZ + block.z) * layout.sizeX + block.x
            if (index in 0 until totalBlocks) {
                blockPaletteIds[index] = paletteId
            }
        }

        val blockDataStream = ByteArrayOutputStream()
        for (paletteId in blockPaletteIds) {
            writeVarInt(blockDataStream, paletteId)
        }

        val paletteCompound = NbtCompound()
        paletteMap.forEach { (state, id) ->
            paletteCompound[state] = NbtInt(id)
        }

        val root = NbtCompound().apply {
            this["Version"] = NbtInt(2)
            this["DataVersion"] = NbtInt(3465)
            this["Width"] = NbtShort(layout.sizeX.toShort())
            this["Height"] = NbtShort(layout.sizeY.toShort())
            this["Length"] = NbtShort(layout.sizeZ.toShort())
            this["Offset"] = NbtIntArray(intArrayOf(0, 0, 0))
            this["Palette"] = paletteCompound
            this["BlockData"] = NbtByteArray(blockDataStream.toByteArray())
            this["BlockEntities"] = NbtList<NbtCompound>()
        }

        outputFile.parentFile?.mkdirs()
        FileOutputStream(outputFile).use { fos ->
            NbtIO.writeNbtFile(fos, NbtFile("Schematic", root), compressed = true, littleEndian = false)
        }
    }

    private fun writeVarInt(output: ByteArrayOutputStream, value: Int) {
        var remaining = value
        while ((remaining and -128) != 0) {
            output.write((remaining and 0x7F) or 0x80)
            remaining = remaining ushr 7
        }
        output.write(remaining)
    }
}
