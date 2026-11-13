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

        val profile = JavaTargetFormatHelper.resolveProfile(targetFormatId, outputDir = null, inputDir = inputDir)

        if ("overworld" in dimensions) {
            scanDimension(0, inputDir, "dimensions/minecraft/overworld", "")
        }
        if ("the_nether" in dimensions) {
            scanDimension(1, inputDir, "dimensions/minecraft/the_nether", "DIM-1")
        }
        if ("the_end" in dimensions) {
            scanDimension(2, inputDir, "dimensions/minecraft/the_end", "DIM1")
        }

        for (list in stagedChunks.values) {
            for (entity in list) {
                adaptEntityRecursive(entity, profile)
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

        val profile = JavaTargetFormatHelper.resolveProfile(targetFormatId, javaWorldDir, inputDir)
        val isModern = profile.usesEntitiesDir
        val targetDataVersion = profile.dataVersion

        var writtenChunks = 0
        var writtenEntities = 0
        val openRegionFiles = mutableMapOf<File, AnvilRegionFile>()

        try {
            for ((chunkKey, entityList) in stagedChunks) {
                if (entityList.isEmpty()) continue

                val targetDir = if (isModern) {
                    JavaTargetFormatHelper.getEntitiesDirectory(
                        javaWorldDir,
                        chunkKey.dimensionId,
                        profile.usesDimensionNamespaces
                    )
                } else {
                    JavaTargetFormatHelper.getRegionDirectory(
                        javaWorldDir,
                        chunkKey.dimensionId,
                        profile.usesDimensionNamespaces
                    )
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

    private fun adaptEntityRecursive(entity: NbtCompound, profile: JavaTargetProfile) {
        val id = entity.getNullableString("id")
        if (id == "minecraft:potion" && profile.usesModernPotionIds) {
            val thrownId = entity.getNullableCompound("Item")?.getNullableString("id")
            entity["id"] = NbtString(
                if (thrownId == "minecraft:lingering_potion") "minecraft:lingering_potion"
                else "minecraft:splash_potion"
            )
        } else if ((id == "minecraft:splash_potion" || id == "minecraft:lingering_potion") && !profile.usesModernPotionIds) {
            entity["id"] = NbtString("minecraft:potion")
            if (entity.getNullableCompound("Item") == null) {
                entity["Item"] = NbtCompound().apply {
                    this["id"] = NbtString(id)
                    if (profile.usesModernComponents) {
                        this["count"] = NbtInt(1)
                    } else {
                        this["Count"] = br.com.gamemods.nbtmanipulator.NbtByte(1)
                    }
                }
            }
        }

        if (id == "minecraft:tnt") {
            val fuseVal = entity.getNullableInt("fuse") ?: entity.getNullableInt("Fuse")
            if (fuseVal != null) {
                if (profile.usesShortFuse) {
                    entity.remove("Fuse")
                    entity["fuse"] = br.com.gamemods.nbtmanipulator.NbtShort(fuseVal.coerceAtLeast(0).toShort())
                } else {
                    entity.remove("fuse")
                    entity["Fuse"] = br.com.gamemods.nbtmanipulator.NbtByte(fuseVal.coerceIn(0, 127).toByte())
                }
            }
        }

        if (profile.usesModernEquipment) {
            adaptToModernEquipment(entity)
        } else {
            adaptToLegacyEquipment(entity)
        }

        adaptEntityItems(entity, profile)

        val passengers = entity.getNullableCompoundList("Passengers")
        if (!passengers.isNullOrEmpty()) {
            for (passenger in passengers) {
                adaptEntityRecursive(passenger, profile)
            }
        }
    }

    private fun adaptEntityItems(entity: NbtCompound, profile: JavaTargetProfile) {
        entity.getNullableCompound("Item")?.let { adaptSingleJavaItem(it, profile) }
        entity.getNullableCompound("ArmorItem")?.let { adaptSingleJavaItem(it, profile) }
        entity.getNullableCompound("SaddleItem")?.let { adaptSingleJavaItem(it, profile) }
        entity.getNullableCompoundList("Items")?.forEach { adaptSingleJavaItem(it, profile) }
        entity.getNullableCompoundList("HandItems")?.forEach { adaptSingleJavaItem(it, profile) }
        entity.getNullableCompoundList("ArmorItems")?.forEach { adaptSingleJavaItem(it, profile) }
        entity.getNullableCompound("equipment")?.let { eq ->
            for ((_, tag) in eq) {
                if (tag is NbtCompound) adaptSingleJavaItem(tag, profile)
            }
        }
        entity.getNullableCompound("Offers")?.getNullableCompoundList("Recipes")?.forEach { recipe ->
            recipe.getNullableCompound("buy")?.let { adaptSingleJavaItem(it, profile) }
            recipe.getNullableCompound("buyB")?.let { adaptSingleJavaItem(it, profile) }
            recipe.getNullableCompound("sell")?.let { adaptSingleJavaItem(it, profile) }
        }
    }

    private fun adaptSingleJavaItem(item: NbtCompound, profile: JavaTargetProfile) {
        EntityEquipment.adaptJavaItem(
            item = item,
            modernComponents = profile.usesModernComponents,
            flatComponents = profile.usesFlatComponents
        )
    }

    private fun adaptToModernEquipment(entity: NbtCompound) {
        val equip = entity.getNullableCompound("equipment") ?: NbtCompound()
        val hands = entity.getNullableCompoundList("HandItems")
        val armor = entity.getNullableCompoundList("ArmorItems")
        val body = entity.getNullableCompound("ArmorItem")
        val saddle = entity.getNullableCompound("SaddleItem")
        if (hands == null && armor == null && body == null && saddle == null) return

        if (hands != null) {
            if (hands.size > 0 && !isAir(hands[0]) && !equip.contains("mainhand")) equip["mainhand"] = hands[0]
            if (hands.size > 1 && !isAir(hands[1]) && !equip.contains("offhand")) equip["offhand"] = hands[1]
        }
        if (armor != null) {
            if (armor.size > 0 && !isAir(armor[0]) && !equip.contains("feet")) equip["feet"] = armor[0]
            if (armor.size > 1 && !isAir(armor[1]) && !equip.contains("legs")) equip["legs"] = armor[1]
            if (armor.size > 2 && !isAir(armor[2]) && !equip.contains("chest")) equip["chest"] = armor[2]
            if (armor.size > 3 && !isAir(armor[3]) && !equip.contains("head")) equip["head"] = armor[3]
        }
        if (body != null && !isAir(body) && !equip.contains("body")) {
            equip["body"] = body
        }
        if (saddle != null && !isAir(saddle) && !equip.contains("saddle")) {
            equip["saddle"] = saddle
        }
        if (equip.isNotEmpty()) {
            entity["equipment"] = equip
        }
        entity.remove("HandItems")
        entity.remove("ArmorItems")
        entity.remove("ArmorItem")
        entity.remove("SaddleItem")
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
        val saddle = equip.getNullableCompound("saddle")
        if (saddle != null && !isAir(saddle)) {
            entity["SaddleItem"] = saddle
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
