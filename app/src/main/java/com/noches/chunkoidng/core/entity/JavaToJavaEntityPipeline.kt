package com.noches.chunkoidng.core.entity

import android.util.Log
import br.com.gamemods.nbtmanipulator.AnvilRegionFile
import br.com.gamemods.nbtmanipulator.NbtCompound
import br.com.gamemods.nbtmanipulator.NbtFile
import br.com.gamemods.nbtmanipulator.NbtInt
import br.com.gamemods.nbtmanipulator.NbtIntArray
import br.com.gamemods.nbtmanipulator.NbtList
import br.com.gamemods.nbtmanipulator.NbtString
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern

class JavaToJavaEntityPipeline(
    private val onLog: ((String) -> Unit)? = null
) {
    companion object {
        private const val TAG = "JavaToJavaEntityPipeline"
        private val REGION_FILE_PATTERN = Pattern.compile("^r\\.(-?\\d+)\\.(-?\\d+)\\.mca$")
    }

    private data class ChunkKey(val dimensionId: Int, val chunkX: Int, val chunkZ: Int)

    private val stagedChunks = ConcurrentHashMap<ChunkKey, MutableList<NbtCompound>>()
    private val totalExtracted = AtomicInteger(0)
    private val skippedModdedCount = AtomicInteger(0)

    fun scan(inputDir: File, dimensions: List<String>, targetFormatId: String) {
        stagedChunks.clear()
        totalExtracted.set(0)
        skippedModdedCount.set(0)

        val toModern = isTargetModernEquipment(targetFormatId)
        val toLegacy = isTargetLegacyEquipment(targetFormatId)

        if ("overworld" in dimensions) {
            scanDimension(0, inputDir, "dimensions/minecraft/overworld", "")
        }
        if ("the_nether" in dimensions) {
            scanDimension(1, inputDir, "dimensions/minecraft/the_nether", "DIM-1")
        }
        if ("the_end" in dimensions) {
            scanDimension(2, inputDir, "dimensions/minecraft/the_end", "DIM1")
        }

        if (toModern) {
            for (list in stagedChunks.values) {
                for (entity in list) {
                    adaptToModernEquipment(entity)
                }
            }
        } else if (toLegacy) {
            for (list in stagedChunks.values) {
                for (entity in list) {
                    adaptToLegacyEquipment(entity)
                }
            }
        }
    }

    private fun scanDimension(dimId: Int, baseDir: File, vararg relativePaths: String) {
        for (rel in relativePaths) {
            val folder = if (rel.isEmpty()) baseDir else File(baseDir, rel)
            val entitiesDir = File(folder, "entities")
            if (entitiesDir.isDirectory) {
                val files = entitiesDir.listFiles { _, name -> REGION_FILE_PATTERN.matcher(name).matches() }
                if (files != null && files.isNotEmpty()) {
                    for (file in files) {
                        processEntitiesMca(dimId, file)
                    }
                    return
                }
            }

            val regionDir = File(folder, "region")
            if (regionDir.isDirectory) {
                val files = regionDir.listFiles { _, name -> REGION_FILE_PATTERN.matcher(name).matches() }
                if (files != null && files.isNotEmpty()) {
                    for (file in files) {
                        processRegionMca(dimId, file)
                    }
                    return
                }
            }
        }
    }

    private fun processEntitiesMca(dimId: Int, file: File) {
        val matcher = REGION_FILE_PATTERN.matcher(file.name)
        if (!matcher.matches()) return

        val regionX = matcher.group(1)?.toIntOrNull() ?: return
        val regionZ = matcher.group(2)?.toIntOrNull() ?: return

        try {
            AnvilRegionFile(file).use { regionFile ->
                val chunks = regionFile.getPopulatedChunks()
                for (info in chunks) {
                    val nbt = try {
                        regionFile.readChunkNbt(info.localX, info.localZ)
                    } catch (_: Exception) {
                        null
                    } ?: continue

                    val root = nbt.compound
                    val chunkX = root.getNullableIntArray("Position")?.getOrNull(0) ?: (regionX * 32 + info.localX)
                    val chunkZ = root.getNullableIntArray("Position")?.getOrNull(1) ?: (regionZ * 32 + info.localZ)
                    val entities = root.getNullableCompoundList("Entities") ?: continue

                    stageEntities(dimId, chunkX, chunkZ, entities)
                }
            }
        } catch (e: Exception) {
            log("读取实体文件失败 [${file.name}]: ${e.message}")
        }
    }

    private fun processRegionMca(dimId: Int, file: File) {
        val matcher = REGION_FILE_PATTERN.matcher(file.name)
        if (!matcher.matches()) return

        val regionX = matcher.group(1)?.toIntOrNull() ?: return
        val regionZ = matcher.group(2)?.toIntOrNull() ?: return

        try {
            AnvilRegionFile(file).use { regionFile ->
                val chunks = regionFile.getPopulatedChunks()
                for (info in chunks) {
                    val nbt = try {
                        regionFile.readChunkNbt(info.localX, info.localZ)
                    } catch (_: Exception) {
                        null
                    } ?: continue

                    val level = nbt.compound.getNullableCompound("Level") ?: continue
                    val chunkX = level.getNullableInt("xPos") ?: (regionX * 32 + info.localX)
                    val chunkZ = level.getNullableInt("zPos") ?: (regionZ * 32 + info.localZ)
                    val entities = level.getNullableCompoundList("Entities") ?: continue

                    stageEntities(dimId, chunkX, chunkZ, entities)
                }
            }
        } catch (e: Exception) {
            log("读取地形实体失败 [${file.name}]: ${e.message}")
        }
    }

    private fun stageEntities(dimId: Int, chunkX: Int, chunkZ: Int, entities: List<NbtCompound>) {
        if (entities.isEmpty()) return
        val key = ChunkKey(dimId, chunkX, chunkZ)
        val validList = mutableListOf<NbtCompound>()

        for (entity in entities) {
            val id = entity.getNullableString("id") ?: continue
            if (EntityIdMapper.isHandledByChunkerNative(id)) continue
            if (!EntityIdMapper.isVanilla(id)) {
                skippedModdedCount.incrementAndGet()
                continue
            }
            validList.add(entity)
            totalExtracted.incrementAndGet()
        }

        if (validList.isNotEmpty()) {
            stagedChunks.computeIfAbsent(key) { mutableListOf() }.addAll(validList)
        }
    }

    fun flushToJava(javaWorldDir: File, targetFormatId: String, inputDir: File): PipelineStats {
        val total = totalExtracted.get()
        if (total == 0 || stagedChunks.isEmpty()) {
            return PipelineStats(0, skippedModdedCount.get(), 0)
        }

        val isModern = isTargetModernJava(targetFormatId, inputDir)
        val targetDataVersion = getTargetDataVersion(targetFormatId, isModern)

        var writtenChunks = 0
        var writtenEntities = 0
        val openRegionFiles = mutableMapOf<File, AnvilRegionFile>()

        try {
            for ((chunkKey, entityList) in stagedChunks) {
                if (entityList.isEmpty()) continue

                val targetDir = if (isModern) {
                    getEntitiesDirectory(javaWorldDir, chunkKey.dimensionId)
                } else {
                    getRegionDirectory(javaWorldDir, chunkKey.dimensionId)
                }
                if (!targetDir.exists()) targetDir.mkdirs()

                val regionX = Math.floorDiv(chunkKey.chunkX, 32)
                val regionZ = Math.floorDiv(chunkKey.chunkZ, 32)
                val localX = Math.floorMod(chunkKey.chunkX, 32)
                val localZ = Math.floorMod(chunkKey.chunkZ, 32)

                val mcaFile = File(targetDir, "r.$regionX.$regionZ.mca")
                val regionFile = openRegionFiles.computeIfAbsent(mcaFile) { AnvilRegionFile(it) }

                try {
                    if (isModern) {
                        val existingFile = regionFile.readChunkNbt(localX, localZ)
                        val chunkRoot = existingFile?.compound ?: NbtCompound().apply {
                            this["Position"] = NbtIntArray(intArrayOf(chunkKey.chunkX, chunkKey.chunkZ))
                            this["DataVersion"] = NbtInt(targetDataVersion)
                        }

                        val existingList = chunkRoot.getNullableCompoundList("Entities")?.toMutableList() ?: mutableListOf()
                        for (e in entityList) {
                            existingList.add(e)
                            writtenEntities++
                        }

                        val mergedList = NbtList<NbtCompound>()
                        for (e in existingList) mergedList.add(e)
                        chunkRoot["Entities"] = mergedList
                        chunkRoot["DataVersion"] = NbtInt(targetDataVersion)

                        regionFile.writeChunkNbt(localX, localZ, NbtFile("", chunkRoot))
                        writtenChunks++
                    } else {
                        val existingFile = regionFile.readChunkNbt(localX, localZ) ?: continue
                        val root = existingFile.compound
                        val level = root.getNullableCompound("Level") ?: continue

                        val existingList = level.getNullableCompoundList("Entities")?.toMutableList() ?: mutableListOf()
                        for (e in entityList) {
                            existingList.add(e)
                            writtenEntities++
                        }

                        val mergedList = NbtList<NbtCompound>()
                        for (e in existingList) mergedList.add(e)
                        level["Entities"] = mergedList

                        regionFile.writeChunkNbt(localX, localZ, NbtFile("", root))
                        writtenChunks++
                    }
                } catch (e: Exception) {
                    log("写入实体至区块失败 [dim=${chunkKey.dimensionId}, chunk=${chunkKey.chunkX},${chunkKey.chunkZ}]: ${e.message}")
                }
            }
        } finally {
            for (rf in openRegionFiles.values) {
                try {
                    rf.close()
                } catch (_: Exception) {}
            }
        }

        return PipelineStats(writtenEntities, skippedModdedCount.get(), writtenChunks)
    }

    private fun getEntitiesDirectory(worldDir: File, dimensionId: Int): File {
        return when (dimensionId) {
            1 -> File(worldDir, "DIM-1/entities")
            2 -> File(worldDir, "DIM1/entities")
            else -> File(worldDir, "entities")
        }
    }

    private fun getRegionDirectory(worldDir: File, dimensionId: Int): File {
        return when (dimensionId) {
            1 -> File(worldDir, "DIM-1/region")
            2 -> File(worldDir, "DIM1/region")
            else -> File(worldDir, "region")
        }
    }

    private fun isTargetModernJava(targetFormatId: String, inputDir: File): Boolean {
        if (targetFormatId == "INPUT") {
            return File(inputDir, "entities").isDirectory
        }
        val parts = targetFormatId.removePrefix("JAVA_").split("_")
        val major = parts.getOrNull(0)?.toIntOrNull() ?: 1
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: if (major >= 26) 99 else 21
        return major > 1 || minor >= 17
    }

    private fun isTargetModernEquipment(targetFormatId: String): Boolean {
        if (targetFormatId == "INPUT") return false
        val parts = targetFormatId.removePrefix("JAVA_").split("_")
        val major = parts.getOrNull(0)?.toIntOrNull() ?: 1
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: if (major >= 26) 99 else 21
        val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
        return major > 1 || minor > 20 || (minor == 20 && patch >= 5)
    }

    private fun isTargetLegacyEquipment(targetFormatId: String): Boolean {
        if (targetFormatId == "INPUT") return false
        val parts = targetFormatId.removePrefix("JAVA_").split("_")
        val major = parts.getOrNull(0)?.toIntOrNull() ?: 1
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: if (major >= 26) 99 else 21
        val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
        return major == 1 && (minor < 20 || (minor == 20 && patch < 5))
    }

    private fun getTargetDataVersion(targetFormatId: String, isModern: Boolean): Int {
        if (targetFormatId.startsWith("JAVA_1_21")) return 3953
        if (targetFormatId.startsWith("JAVA_1_20")) return 3700
        if (targetFormatId.startsWith("JAVA_1_19")) return 3337
        if (targetFormatId.startsWith("JAVA_1_18")) return 2975
        if (targetFormatId.startsWith("JAVA_1_17")) return 2730
        if (targetFormatId.startsWith("JAVA_1_16")) return 2586
        if (targetFormatId.startsWith("JAVA_1_15")) return 2230
        if (targetFormatId.startsWith("JAVA_1_14")) return 1976
        if (targetFormatId.startsWith("JAVA_1_13")) return 1631
        if (targetFormatId.startsWith("JAVA_1_12")) return 1343
        return if (isModern) 3953 else 2586
    }

    private fun adaptToModernEquipment(entity: NbtCompound) {
        if (entity.getNullableCompound("equipment") != null) return
        val hands = entity.getNullableCompoundList("HandItems")
        val armor = entity.getNullableCompoundList("ArmorItems")
        val body = entity.getNullableCompound("ArmorItem")
        if (hands == null && armor == null && body == null) return

        val equip = NbtCompound()
        if (hands != null) {
            if (hands.size > 0 && !isAir(hands[0])) equip["mainhand"] = hands[0]
            if (hands.size > 1 && !isAir(hands[1])) equip["offhand"] = hands[1]
        }
        if (armor != null) {
            if (armor.size > 0 && !isAir(armor[0])) equip["feet"] = armor[0]
            if (armor.size > 1 && !isAir(armor[1])) equip["legs"] = armor[1]
            if (armor.size > 2 && !isAir(armor[2])) equip["chest"] = armor[2]
            if (armor.size > 3 && !isAir(armor[3])) equip["head"] = armor[3]
        }
        if (body != null && !isAir(body)) {
            equip["body"] = body
        }
        if (equip.isNotEmpty()) {
            entity["equipment"] = equip
        }
    }

    private fun adaptToLegacyEquipment(entity: NbtCompound) {
        val equip = entity.getNullableCompound("equipment") ?: return
        val main = equip.getNullableCompound("mainhand") ?: airItem()
        val off = equip.getNullableCompound("offhand") ?: airItem()
        entity["HandItems"] = NbtList(listOf(main, off))

        val feet = equip.getNullableCompound("feet") ?: airItem()
        val legs = equip.getNullableCompound("legs") ?: airItem()
        val chest = equip.getNullableCompound("chest") ?: airItem()
        val head = equip.getNullableCompound("head") ?: airItem()
        entity["ArmorItems"] = NbtList(listOf(feet, legs, chest, head))

        val body = equip.getNullableCompound("body")
        if (body != null && !isAir(body)) {
            entity["ArmorItem"] = body
        }
        entity.remove("equipment")
    }

    private fun isAir(item: NbtCompound?): Boolean {
        if (item == null) return true
        val id = item.getNullableString("id") ?: return true
        return id == "minecraft:air" || id.isEmpty()
    }

    private fun airItem(): NbtCompound {
        val item = NbtCompound()
        item["id"] = NbtString("minecraft:air")
        item["Count"] = br.com.gamemods.nbtmanipulator.NbtByte(0)
        return item
    }

    private fun log(message: String) {
        onLog?.invoke(message)
        Log.i(TAG, message)
    }
}
