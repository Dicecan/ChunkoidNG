package com.noches.chunkoidng.core.entity

import br.com.gamemods.nbtmanipulator.*
import org.junit.Assert.*
import org.junit.Test

class MinecraftWikiValidationTest {

    @Test
    fun testWikiHorseColorAndMarkingsMath() {

        val testCases = listOf(
            Triple(0, 0, 0),
            Triple(0, 1, 256),
            Triple(4, 2, 516),
            Triple(5, 3, 773),
            Triple(6, 4, 1030)
        )

        for ((coat, marking, javaInt) in testCases) {

            val javaHorse = NbtCompound().apply {
                this["id"] = NbtString("minecraft:horse")
                this["Variant"] = NbtInt(javaInt)
            }
            val bedrockHorse = BedrockActorBuilder.build(javaHorse, 1000L)
            assertNotNull(bedrockHorse)
            assertEquals("Coat mismatch for $javaInt", coat, bedrockHorse!!.getNullableInt("Variant"))
            assertEquals("Marking mismatch for $javaInt", marking, bedrockHorse.getNullableInt("MarkVariant"))

            val reconstructedJava = JavaEntityBuilder.build(bedrockHorse)
            assertNotNull(reconstructedJava)
            assertEquals("Java variant mismatch", javaInt, reconstructedJava!!.getNullableInt("Variant"))
        }
    }

    @Test
    fun testWikiCatBreedsMapping() {

        val wikiCatTable = mapOf(
            0 to "white",
            1 to "black",
            2 to "red",
            3 to "siamese",
            4 to "british_shorthair",
            5 to "calico",
            6 to "persian",
            7 to "ragdoll",
            8 to "tabby",
            9 to "all_black",
            10 to "jellie"
        )

        for ((index, breed) in wikiCatTable) {

            val bedrockCat = NbtCompound().apply {
                this["identifier"] = NbtString("minecraft:cat")
                this["Variant"] = NbtInt(index)
            }
            val javaCat = JavaEntityBuilder.build(bedrockCat)
            assertNotNull(javaCat)
            assertEquals("Cat breed mismatch for index $index", "minecraft:$breed", javaCat!!.getNullableString("variant"))

            val javaInput = NbtCompound().apply {
                this["id"] = NbtString("minecraft:cat")
                this["variant"] = NbtString("minecraft:$breed")
            }
            val convertedBedrock = BedrockActorBuilder.build(javaInput, 2000L + index)
            assertNotNull(convertedBedrock)
            assertEquals("Bedrock cat variant mismatch for breed $breed", index, convertedBedrock!!.getNullableInt("Variant"))
        }
    }

    @Test
    fun testWikiWolfVariantsAndCollar() {
        val wolfVariants = listOf(
            "pale", "ashen", "black", "chestnut", "rusty",
            "spotted", "striped", "woods", "snowy"
        )

        for (variant in wolfVariants) {

            val javaWolf = NbtCompound().apply {
                this["id"] = NbtString("minecraft:wolf")
                this["variant"] = NbtString("minecraft:$variant")
                this["CollarColor"] = NbtByte(14)
                this["Sitting"] = NbtByte(1)
            }
            val bedrockWolf = BedrockActorBuilder.build(javaWolf, 3000L)
            assertNotNull(bedrockWolf)
            val defs = bedrockWolf!!.getNullableStringList("definitions")?.map { it.value } ?: emptyList()
            assertTrue("Must contain +minecraft:wolf_$variant", defs.contains("+minecraft:wolf_$variant"))
            assertEquals(14.toByte(), bedrockWolf.getNullableByte("Color"))
            assertEquals(1.toByte(), bedrockWolf.getNullableByte("Sitting"))

            val reconstructed = JavaEntityBuilder.build(bedrockWolf)
            assertNotNull(reconstructed)
            assertEquals("minecraft:$variant", reconstructed!!.getNullableString("variant"))
            assertEquals(14.toByte(), reconstructed.getNullableByte("CollarColor"))
            assertEquals(1.toByte(), reconstructed.getNullableByte("Sitting"))
        }
    }

    @Test
    fun testWikiVillagerProfessionsAndTiers() {
        val professions = listOf("farmer", "librarian", "armorer", "cleric", "weaponsmith", "cartographer")
        val biomes = listOf("desert", "taiga", "savanna", "snow", "plains")

        for ((idx, prof) in professions.withIndex()) {
            val biome = biomes[idx % biomes.size]
            val level = (idx % 5) + 1
            val expectedTier = level - 1

            val javaVillager = NbtCompound().apply {
                this["id"] = NbtString("minecraft:villager")
                val vData = NbtCompound().apply {
                    this["profession"] = NbtString("minecraft:$prof")
                    this["type"] = NbtString("minecraft:$biome")
                    this["level"] = NbtInt(level)
                }
                this["VillagerData"] = vData
                this["Xp"] = NbtInt(level * 50)
            }

            val bedrockVillager = BedrockActorBuilder.build(javaVillager, 4000L + idx)
            assertNotNull(bedrockVillager)
            assertEquals("minecraft:villager_v2", bedrockVillager!!.getNullableString("identifier"))
            assertEquals(expectedTier, bedrockVillager.getNullableInt("TradeTier"))
            assertEquals(level * 50, bedrockVillager.getNullableInt("TradeExperience"))

            val defs = bedrockVillager.getNullableStringList("definitions")?.map { it.value } ?: emptyList()
            assertTrue(defs.contains("+minecraft:$prof"))
            if (biome != "plains") {
                assertTrue(defs.contains("+minecraft:${biome}_villager"))
            }

            val reconstructed = JavaEntityBuilder.build(bedrockVillager)
            assertNotNull(reconstructed)
            assertEquals("minecraft:villager", reconstructed!!.getNullableString("id"))
            val reconData = reconstructed.getNullableCompound("VillagerData")
            assertNotNull(reconData)
            assertEquals("minecraft:$prof", reconData!!.getNullableString("profession"))
            assertEquals("minecraft:$biome", reconData.getNullableString("type"))
            assertEquals(level, reconData.getNullableInt("level"))
            assertEquals(level * 50, reconstructed.getNullableInt("Xp"))
        }
    }

    @Test
    fun testVillagerTradeOffersRecipesBidirectional() {
        val javaVillager = NbtCompound().apply {
            this["id"] = NbtString("minecraft:villager")
            this["VillagerData"] = NbtCompound().apply {
                this["profession"] = NbtString("minecraft:librarian")
                this["type"] = NbtString("minecraft:plains")
                this["level"] = NbtInt(2)
            }
            val recipes = NbtList<NbtCompound>()
            val recipe1 = NbtCompound().apply {
                this["buy"] = NbtCompound().apply { this["id"] = NbtString("minecraft:emerald"); this["Count"] = NbtByte(24) }
                this["buyB"] = NbtCompound().apply { this["id"] = NbtString("minecraft:book"); this["Count"] = NbtByte(1) }
                this["sell"] = NbtCompound().apply { this["id"] = NbtString("minecraft:enchanted_book"); this["Count"] = NbtByte(1) }
                this["uses"] = NbtInt(3)
                this["maxUses"] = NbtInt(12)
                this["xp"] = NbtInt(10)
            }
            recipes.add(recipe1)
            this["Offers"] = NbtCompound().apply { this["Recipes"] = recipes }
        }

        val bedrockActor = BedrockActorBuilder.build(javaVillager, 9999L)
        assertNotNull(bedrockActor)
        val beOffers = bedrockActor!!.getNullableCompound("Offers")
        assertNotNull(beOffers)
        val beRecipes = beOffers!!.getNullableCompoundList("Recipes")
        assertNotNull(beRecipes)
        assertEquals(1, beRecipes!!.size)
        assertEquals("minecraft:emerald", beRecipes[0].getNullableCompound("buyA")?.getNullableString("Name"))
        assertEquals("minecraft:enchanted_book", beRecipes[0].getNullableCompound("sell")?.getNullableString("Name"))
        assertEquals(3, beRecipes[0].getNullableInt("uses"))
        assertEquals(12, beRecipes[0].getNullableInt("maxUses"))

        val recon = JavaEntityBuilder.build(bedrockActor)
        assertNotNull(recon)
        val reconOffers = recon!!.getNullableCompound("Offers")
        assertNotNull(reconOffers)
        val reconRecipes = reconOffers!!.getNullableCompoundList("Recipes")
        assertNotNull(reconRecipes)
        assertEquals(1, reconRecipes!!.size)
        assertEquals("minecraft:emerald", reconRecipes[0].getNullableCompound("buy")?.getNullableString("id"))
        assertEquals("minecraft:book", reconRecipes[0].getNullableCompound("buyB")?.getNullableString("id"))
        assertEquals("minecraft:enchanted_book", reconRecipes[0].getNullableCompound("sell")?.getNullableString("id"))
        assertEquals(3, reconRecipes[0].getNullableInt("uses"))
        assertEquals(12, reconRecipes[0].getNullableInt("maxUses"))
    }

    fun testWikiArmorAndHandEquipmentInversion() {

        val javaZombie = NbtCompound().apply {
            this["id"] = NbtString("minecraft:zombie")

            val handItems = NbtList<NbtCompound>().apply {
                add(NbtCompound().apply { this["id"] = NbtString("minecraft:diamond_sword"); this["Count"] = NbtByte(1) })
                add(NbtCompound().apply { this["id"] = NbtString("minecraft:shield"); this["Count"] = NbtByte(1) })
            }
            this["HandItems"] = handItems

            val armorItems = NbtList<NbtCompound>().apply {
                add(NbtCompound().apply { this["id"] = NbtString("minecraft:iron_boots"); this["Count"] = NbtByte(1) })
                add(NbtCompound().apply { this["id"] = NbtString("minecraft:iron_leggings"); this["Count"] = NbtByte(1) })
                add(NbtCompound().apply { this["id"] = NbtString("minecraft:iron_chestplate"); this["Count"] = NbtByte(1) })
                add(NbtCompound().apply { this["id"] = NbtString("minecraft:iron_helmet"); this["Count"] = NbtByte(1) })
            }
            this["ArmorItems"] = armorItems
        }

        val bedrockZombie = BedrockActorBuilder.build(javaZombie, 5000L)
        assertNotNull(bedrockZombie)

        val bArmor = bedrockZombie!!.getNullableCompoundList("Armor")
        assertNotNull(bArmor)
        assertEquals(5, bArmor!!.size)
        assertEquals("minecraft:iron_helmet", bArmor[0].getNullableString("Name"))
        assertEquals("minecraft:iron_chestplate", bArmor[1].getNullableString("Name"))
        assertEquals("minecraft:iron_leggings", bArmor[2].getNullableString("Name"))
        assertEquals("minecraft:iron_boots", bArmor[3].getNullableString("Name"))

        val reconJava = JavaEntityBuilder.build(bedrockZombie)
        assertNotNull(reconJava)

        val reconArmor = reconJava!!.getNullableCompoundList("ArmorItems")
        assertNotNull(reconArmor)
        assertEquals(4, reconArmor!!.size)
        assertEquals("minecraft:iron_boots", reconArmor[0].getNullableString("id"))
        assertEquals("minecraft:iron_leggings", reconArmor[1].getNullableString("id"))
        assertEquals("minecraft:iron_chestplate", reconArmor[2].getNullableString("id"))
        assertEquals("minecraft:iron_helmet", reconArmor[3].getNullableString("id"))

        val reconHands = reconJava.getNullableCompoundList("HandItems")
        assertNotNull(reconHands)
        assertEquals("minecraft:diamond_sword", reconHands!![0].getNullableString("id"))
        assertEquals("minecraft:shield", reconHands[1].getNullableString("id"))
    }

    @Test
    fun testWikiSpecialEntitiesCreeperSlimeShulker() {

        val chargedCreeper = NbtCompound().apply {
            this["id"] = NbtString("minecraft:creeper")
            this["powered"] = NbtByte(1)
        }
        val bCreeper = BedrockActorBuilder.build(chargedCreeper, 6001L)!!
        assertEquals(1.toByte(), bCreeper.getNullableByte("powered"))
        val creeperDefs = bCreeper.getNullableStringList("definitions")?.map { it.value } ?: emptyList()
        assertTrue(creeperDefs.contains("+minecraft:charged_creeper"))

        val jCreeper = JavaEntityBuilder.build(bCreeper)!!
        assertEquals(1.toByte(), jCreeper.getNullableByte("powered"))

        val largeSlime = NbtCompound().apply {
            this["id"] = NbtString("minecraft:slime")
            this["Size"] = NbtInt(4)
        }
        val bSlime = BedrockActorBuilder.build(largeSlime, 6002L)!!
        assertEquals(4, bSlime.getNullableInt("Size"))
        val slimeDefs = bSlime.getNullableStringList("definitions")?.map { it.value } ?: emptyList()
        assertTrue(slimeDefs.contains("+minecraft:slime_large"))

        val jSlime = JavaEntityBuilder.build(bSlime)!!
        assertEquals(4, jSlime.getNullableInt("Size"))

        val dyedShulker = NbtCompound().apply {
            this["id"] = NbtString("minecraft:shulker")
            this["Color"] = NbtByte(11)
        }
        val bShulker = BedrockActorBuilder.build(dyedShulker, 6003L)!!
        assertEquals(11.toByte(), bShulker.getNullableByte("Color"))

        val jShulker = JavaEntityBuilder.build(bShulker)!!
        assertEquals(11.toByte(), jShulker.getNullableByte("Color"))
    }
}
