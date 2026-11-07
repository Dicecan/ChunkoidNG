package com.noches.chunkoidng.core.entity

import br.com.gamemods.nbtmanipulator.NbtByte
import br.com.gamemods.nbtmanipulator.NbtCompound
import br.com.gamemods.nbtmanipulator.NbtFloat
import br.com.gamemods.nbtmanipulator.NbtInt
import br.com.gamemods.nbtmanipulator.NbtList
import br.com.gamemods.nbtmanipulator.NbtShort
import br.com.gamemods.nbtmanipulator.NbtString

object EntityStates {

    private const val NAMESPACE = "minecraft:"
    private const val SADDLE = "minecraft:saddle"

    private val MOUNTS = setOf(
        "minecraft:pig", "minecraft:horse", "minecraft:donkey", "minecraft:mule",
        "minecraft:camel", "minecraft:strider", "minecraft:skeleton_horse", "minecraft:zombie_horse"
    )

    fun apply(
        actor: NbtCompound,
        identifier: String,
        javaEntity: NbtCompound,
        definitions: MutableList<String>
    ) {
        saddle(actor, identifier, javaEntity, definitions)
        collar(actor, identifier, javaEntity)
        sitting(actor, identifier, javaEntity, definitions)
        chested(actor, identifier, javaEntity)
        villager(actor, identifier, javaEntity, definitions)
    }

    private fun saddle(
        actor: NbtCompound,
        identifier: String,
        entity: NbtCompound,
        definitions: MutableList<String>
    ) {
        if (identifier !in MOUNTS) return

        val bodyItem = bodySlot(entity)
        val itemId = bodyItem?.getNullableString("id") ?: return
        if (itemId == SADDLE || itemId.endsWith("saddle")) {
            actor["Saddled"] = NbtByte(1)
            val bare = identifier.removePrefix(NAMESPACE)
            EntityVariants.addGroup(definitions, "${bare}_saddled")
        }
    }

    fun bodySlot(entity: NbtCompound): NbtCompound? {
        val equipment = entity.getNullableCompound("equipment")
        if (equipment != null) {
            val body = equipment.getNullableCompound("body")
            if (body != null) return body
        }
        val saddle = entity.getNullableCompound("SaddleItem")
        if (saddle != null) return saddle
        return entity.getNullableCompound("ArmorItem")
    }

    private fun collar(actor: NbtCompound, identifier: String, entity: NbtCompound) {
        if (identifier != "minecraft:cat" && identifier != "minecraft:wolf") return
        val collar = entity.getNullableByte("CollarColor")
            ?: entity.getNullableInt("CollarColor")?.toByte()
            ?: return
        val c = collar.coerceIn(0, 15)
        actor["Color"] = NbtByte(c)
        actor["CollarColor"] = NbtByte(c)
    }

    private fun sitting(
        actor: NbtCompound,
        identifier: String,
        entity: NbtCompound,
        definitions: MutableList<String>
    ) {
        val isSitting = entity.getNullableByte("Sitting")?.toInt() == 1 ||
                entity.getNullableInt("Sitting") == 1
        if (isSitting) {
            actor["Sitting"] = NbtByte(1)
            if (identifier == "minecraft:camel") {
                EntityVariants.addGroup(definitions, "camel_sitting")
                definitions.remove("+minecraft:camel_standing")
            }
        }
    }

    private fun chested(actor: NbtCompound, identifier: String, entity: NbtCompound) {
        if (identifier !in setOf("minecraft:donkey", "minecraft:mule", "minecraft:llama", "minecraft:trader_llama")) return
        val chested = isTruthy(entity["ChestedHorse"]) || isTruthy(entity["Chested"])
        if (chested) {
            actor["Chested"] = NbtByte(1)
        }
    }

    private fun villager(
        actor: NbtCompound,
        identifier: String,
        entity: NbtCompound,
        definitions: MutableList<String>
    ) {
        val data = entity.getNullableCompound("VillagerData") ?: return
        val profession = data.getNullableString("profession")?.removePrefix(NAMESPACE)
        if (!profession.isNullOrEmpty() && profession != "none") {
            EntityVariants.addGroup(definitions, profession)
        }

        val type = data.getNullableString("type")?.removePrefix(NAMESPACE)
        if (!type.isNullOrEmpty() && type != "plains") {
            EntityVariants.addGroup(definitions, "${type}_villager")
        }

        val level = getInt(data, "level", 1)
        actor["TradeTier"] = NbtInt((level - 1).coerceIn(0, 4))

        val xp = getInt(entity, "Xp", 0)
        if (xp > 0) {
            actor["TradeExperience"] = NbtInt(xp)
        }

        val offers = mapOffersToBedrock(entity.getNullableCompound("Offers"))
        if (offers != null) {
            actor["Offers"] = offers
        }
    }

    fun mapOffersToBedrock(javaOffers: NbtCompound?): NbtCompound? {
        if (javaOffers == null) return null
        val javaRecipes = javaOffers.getNullableCompoundList("Recipes") ?: return null
        val bedrockRecipes = NbtList<NbtCompound>()
        for (r in javaRecipes) {
            val recipe = NbtCompound()
            val buy = EntityEquipment.convertItem(r.getNullableCompound("buy")) ?: continue
            recipe["buyA"] = buy
            val buyB = EntityEquipment.convertItem(r.getNullableCompound("buyB"))
            recipe["buyB"] = buyB ?: EntityEquipment.emptyItem()
            val sell = EntityEquipment.convertItem(r.getNullableCompound("sell")) ?: continue
            recipe["sell"] = sell
            recipe["uses"] = NbtInt(r.getNullableInt("uses") ?: 0)
            recipe["maxUses"] = NbtInt(r.getNullableInt("maxUses") ?: 16)
            recipe["traderExp"] = NbtInt(r.getNullableInt("xp") ?: 1)
            recipe["priceMultiplierA"] = br.com.gamemods.nbtmanipulator.NbtFloat(r.getNullableFloat("priceMultiplier") ?: 0.05f)
            recipe["demand"] = NbtInt(r.getNullableInt("demand") ?: 0)
            val tier = r.getNullableInt("tier")
            if (tier != null) recipe["tier"] = NbtInt(tier)
            bedrockRecipes.add(recipe)
        }
        if (bedrockRecipes.isEmpty()) return null
        val offers = NbtCompound()
        offers["Recipes"] = bedrockRecipes
        return offers
    }

    fun mapOffersToJava(bedrockOffers: NbtCompound?): NbtCompound? {
        if (bedrockOffers == null) return null
        val bedrockRecipes = bedrockOffers.getNullableCompoundList("Recipes") ?: return null
        val javaRecipes = NbtList<NbtCompound>()
        for (r in bedrockRecipes) {
            val recipe = NbtCompound()
            val buyA = convertBedrockItemToJavaItem(r.getNullableCompound("buyA")) ?: continue
            recipe["buy"] = buyA
            val buyB = convertBedrockItemToJavaItem(r.getNullableCompound("buyB"))
            if (buyB != null && buyB.getNullableString("id") != "minecraft:air") {
                recipe["buyB"] = buyB
            }
            val sell = convertBedrockItemToJavaItem(r.getNullableCompound("sell")) ?: continue
            recipe["sell"] = sell
            recipe["uses"] = NbtInt(r.getNullableInt("uses") ?: 0)
            recipe["maxUses"] = NbtInt(r.getNullableInt("maxUses") ?: 16)
            recipe["xp"] = NbtInt(r.getNullableInt("traderExp") ?: 1)
            recipe["priceMultiplier"] = br.com.gamemods.nbtmanipulator.NbtFloat(r.getNullableFloat("priceMultiplierA") ?: 0.05f)
            recipe["demand"] = NbtInt(r.getNullableInt("demand") ?: 0)
            val tier = r.getNullableInt("tier")
            if (tier != null) recipe["tier"] = NbtInt(tier)
            javaRecipes.add(recipe)
        }
        if (javaRecipes.isEmpty()) return null
        val offers = NbtCompound()
        offers["Recipes"] = javaRecipes
        return offers
    }

    fun convertBedrockItemToJavaItem(bedrockItem: NbtCompound?): NbtCompound? {
        if (bedrockItem == null) return null
        val name = bedrockItem.getNullableString("Name") ?: return null
        if (name.isEmpty() || name == "minecraft:air") return null
        val count = bedrockItem.getNullableByte("Count") ?: 1
        val item = NbtCompound()
        item["id"] = NbtString(name)
        item["Count"] = NbtByte(count)
        return item
    }

    private fun isTruthy(tag: br.com.gamemods.nbtmanipulator.NbtTag?): Boolean {
        return when (tag) {
            is NbtByte -> tag.signed != 0.toByte()
            is NbtInt -> tag.value != 0
            is NbtShort -> tag.value != 0.toShort()
            else -> false
        }
    }

    private fun getInt(compound: NbtCompound, key: String, default: Int): Int {
        return when (val tag = compound[key]) {
            is NbtInt -> tag.value
            is NbtByte -> tag.signed.toInt()
            is NbtShort -> tag.value.toInt()
            else -> default
        }
    }
}
