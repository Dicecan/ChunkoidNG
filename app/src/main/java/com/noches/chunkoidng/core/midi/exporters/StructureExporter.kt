package com.noches.chunkoidng.core.midi.exporters

import br.com.gamemods.nbtmanipulator.*
import com.noches.chunkoidng.core.midi.NoteBlockInstrument
import com.noches.chunkoidng.core.midi.NoteBlockSong
import java.io.File
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

    fun buildContraption(song: NoteBlockSong): ContraptionLayout {
        val blocks = mutableListOf<BlockPlacement>()
        val notesByTick = song.notes.groupBy { it.tick }.toSortedMap()

        blocks.add(
            BlockPlacement(
                x = 0, y = 1, z = 0,
                bedrockName = "minecraft:stone",
                javaStateString = "minecraft:stone"
            )
        )
        blocks.add(
            BlockPlacement(
                x = 0, y = 2, z = 0,
                bedrockName = "minecraft:stone",
                javaStateString = "minecraft:stone"
            )
        )
        blocks.add(
            BlockPlacement(
                x = 0, y = 3, z = 0,
                bedrockName = "minecraft:stone_button",
                javaStateString = "minecraft:stone_button[face=floor,facing=north,powered=false]",
                bedrockStates = mapOf("facing_direction" to 1)
            )
        )

        var currentZ = 1
        var lastTick = 0

        for ((tick, noteList) in notesByTick) {
            val delta = (tick - lastTick).coerceIn(1, 8)
            var remainingDelay = delta

            while (remainingDelay > 0) {
                val rDelay = minOf(remainingDelay, 4)
                blocks.add(
                    BlockPlacement(
                        x = 0, y = 1, z = currentZ,
                        bedrockName = "minecraft:stone",
                        javaStateString = "minecraft:stone"
                    )
                )
                blocks.add(
                    BlockPlacement(
                        x = 0, y = 2, z = currentZ,
                        bedrockName = "minecraft:unpowered_repeater",
                        javaStateString = "minecraft:repeater[delay=$rDelay,facing=south,powered=false]",
                        bedrockStates = mapOf("repeater_delay" to (rDelay - 1), "direction" to 0)
                    )
                )
                currentZ++
                remainingDelay -= rDelay
            }

            val busZ = currentZ
            blocks.add(
                BlockPlacement(
                    x = 0, y = 1, z = busZ,
                    bedrockName = "minecraft:stone",
                    javaStateString = "minecraft:stone"
                )
            )
            blocks.add(
                BlockPlacement(
                    x = 0, y = 2, z = busZ,
                    bedrockName = "minecraft:redstone_wire",
                    javaStateString = "minecraft:redstone_wire[power=0]",
                    bedrockStates = mapOf("redstone_signal" to 0)
                )
            )

            noteList.forEachIndexed { index, note ->
                val xOffset = if (index % 2 == 0) (index / 2 + 1) else -(index / 2 + 1)

                val xStart = if (xOffset > 0) 1 else -1
                val xStep = if (xOffset > 0) 1 else -1
                var wireX = xStart
                while (if (xStep > 0) wireX <= xOffset else wireX >= xOffset) {
                    blocks.add(
                        BlockPlacement(
                            x = wireX, y = 1, z = busZ,
                            bedrockName = "minecraft:stone",
                            javaStateString = "minecraft:stone"
                        )
                    )
                    blocks.add(
                        BlockPlacement(
                            x = wireX, y = 2, z = busZ,
                            bedrockName = "minecraft:redstone_wire",
                            javaStateString = "minecraft:redstone_wire[power=0]",
                            bedrockStates = mapOf("redstone_signal" to 0)
                        )
                    )
                    wireX += xStep
                }

                blocks.add(
                    BlockPlacement(
                        x = xOffset, y = 1, z = busZ + 1,
                        bedrockName = "minecraft:stone",
                        javaStateString = "minecraft:stone"
                    )
                )
                blocks.add(
                    BlockPlacement(
                        x = xOffset, y = 2, z = busZ + 1,
                        bedrockName = "minecraft:unpowered_repeater",
                        javaStateString = "minecraft:repeater[delay=1,facing=south,powered=false]",
                        bedrockStates = mapOf("repeater_delay" to 0, "direction" to 0)
                    )
                )

                blocks.add(
                    BlockPlacement(
                        x = xOffset, y = 1, z = busZ + 2,
                        bedrockName = note.instrument.baseBlockId,
                        javaStateString = note.instrument.baseBlockId
                    )
                )
                blocks.add(
                    BlockPlacement(
                        x = xOffset, y = 2, z = busZ + 2,
                        bedrockName = "minecraft:noteblock",
                        javaStateString = "minecraft:note_block[instrument=${note.instrument.name.lowercase()},note=${note.key},powered=false]",
                        notePitch = note.key,
                        instrument = note.instrument
                    )
                )
            }

            currentZ = busZ + 3
            lastTick = tick
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

        val shiftedBlocks = blocks.map {
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
            val key = "$name|$states"
            paletteIndexMap[key]?.let { return it }

            val statesCompound = NbtCompound()
            states.forEach { (k, v) ->
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

        val totalBlocks = layout.sizeX * layout.sizeY * layout.sizeZ
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

        val totalBlocks = layout.sizeX * layout.sizeY * layout.sizeZ
        val blockData = ByteArray(totalBlocks)

        for (block in layout.blocks) {
            val paletteId = paletteMap.getOrPut(block.javaStateString) { nextPaletteId++ }
            val index = (block.y * layout.sizeZ + block.z) * layout.sizeX + block.x
            if (index in 0 until totalBlocks) {
                blockData[index] = paletteId.toByte()
            }
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
            this["BlockData"] = NbtByteArray(blockData)
            this["BlockEntities"] = NbtList<NbtCompound>()
        }

        outputFile.parentFile?.mkdirs()
        FileOutputStream(outputFile).use { fos ->
            NbtIO.writeNbtFile(fos, NbtFile("Schematic", root), compressed = true, littleEndian = false)
        }
    }
}
