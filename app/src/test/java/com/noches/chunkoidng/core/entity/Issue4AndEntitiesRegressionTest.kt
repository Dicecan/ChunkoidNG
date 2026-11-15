package com.noches.chunkoidng.core.entity

import br.com.gamemods.nbtmanipulator.AnvilRegionFile
import br.com.gamemods.nbtmanipulator.NbtByte
import br.com.gamemods.nbtmanipulator.NbtCompound
import br.com.gamemods.nbtmanipulator.NbtDouble
import br.com.gamemods.nbtmanipulator.NbtFile
import br.com.gamemods.nbtmanipulator.NbtFloat
import br.com.gamemods.nbtmanipulator.NbtIO
import br.com.gamemods.nbtmanipulator.NbtInt
import br.com.gamemods.nbtmanipulator.NbtIntArray
import br.com.gamemods.nbtmanipulator.NbtList
import br.com.gamemods.nbtmanipulator.NbtLong
import br.com.gamemods.nbtmanipulator.NbtShort
import br.com.gamemods.nbtmanipulator.NbtString
import br.com.gamemods.nbtmanipulator.NbtTag
import org.iq80.leveldb.CompressionType
import org.iq80.leveldb.Options
import org.iq80.leveldb.impl.Iq80DBFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Issue4AndEntitiesRegressionTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun nbtFloatValue(tag: NbtTag?): Float? = when (tag) {
        is NbtFloat -> tag.value
        is NbtDouble -> tag.value.toFloat()
        is NbtShort -> tag.value.toFloat()
        is NbtInt -> tag.value.toFloat()
        else -> null
    }

    private fun baseJavaEntity(id: String, x: Double = 10.5, y: Double = 64.0, z: Double = 20.5): NbtCompound {
        return NbtCompound().apply {
            this["id"] = id
            this["Pos"] = NbtList(NbtDouble(x), NbtDouble(y), NbtDouble(z))
            this["Motion"] = NbtList(NbtDouble(0.0), NbtDouble(0.0), NbtDouble(0.0))
            this["Rotation"] = NbtList(NbtFloat(0f), NbtFloat(0f))
        }
    }

    private fun serializeBedrockActor(actor: NbtCompound): ByteArray {
        val baos = ByteArrayOutputStream()
        NbtIO.writeNbtFile(baos, NbtFile("", actor), compressed = false, littleEndian = true)
        return baos.toByteArray()
    }

    private fun putActorInDb(
        db: org.iq80.leveldb.DB,
        uniqueId: Long,
        actor: NbtCompound,
        chunkX: Int = 0,
        chunkZ: Int = 0,
        dimId: Int = 0,
        useBigEndianDigp: Boolean = true
    ) {
        val storageKey = EntityLinks.storageKey(uniqueId)
        val actorKey = "actorprefix".toByteArray(Charsets.US_ASCII) + storageKey
        db.put(actorKey, serializeBedrockActor(actor))

        val digpKey = EntityConversionPipeline().buildDigpKey(dimId, chunkX, chunkZ)
        val existingDigp = db.get(digpKey) ?: ByteArray(0)
        val entryBytes = if (useBigEndianDigp) {
            storageKey
        } else {
            ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(uniqueId).array()
        }
        db.put(digpKey, existingDigp + entryBytes)
    }

    @Test
    fun testEntityIdMapperNewEntitiesRaftsProjectilesAndPotions() {
        assertEquals("minecraft:boat", EntityIdMapper.toBedrock("minecraft:bamboo_raft"))
        assertEquals("minecraft:chest_boat", EntityIdMapper.toBedrock("minecraft:bamboo_chest_raft"))
        assertEquals("minecraft:boat", EntityIdMapper.toBedrock("minecraft:poplar_boat"))
        assertEquals("minecraft:chest_boat", EntityIdMapper.toBedrock("minecraft:poplar_chest_boat"))
        assertEquals("minecraft:evocation_fang", EntityIdMapper.toBedrock("minecraft:evoker_fangs"))
        assertEquals("minecraft:breeze_wind_charge_projectile", EntityIdMapper.toBedrock("minecraft:breeze_wind_charge"))
        assertEquals("minecraft:wind_charge_projectile", EntityIdMapper.toBedrock("minecraft:wind_charge"))
        assertEquals("minecraft:splash_potion", EntityIdMapper.toBedrock("minecraft:splash_potion"))
        assertEquals("minecraft:lingering_potion", EntityIdMapper.toBedrock("minecraft:lingering_potion"))

        val modernMobs = listOf(
            "camel_husk",
            "copper_golem",
            "creaking",
            "happy_ghast",
            "nautilus",
            "parched",
            "zombie_nautilus",
            "sulfur_cube",
            "mannequin",
            "area_effect_cloud"
        )
        for (mob in modernMobs) {
            assertTrue("Expected $mob in vanilla whitelist", EntityIdMapper.isVanilla("minecraft:$mob"))
            assertEquals("minecraft:$mob", EntityIdMapper.toBedrock("minecraft:$mob"))
            assertEquals("minecraft:$mob", EntityIdMapper.toJavaId("minecraft:$mob"))
        }

        assertEquals("minecraft:wither_skull", EntityIdMapper.toJavaId("minecraft:wither_skull_dangerous"))
        assertEquals("minecraft:evoker_fangs", EntityIdMapper.toJavaId("minecraft:evocation_fang"))
        assertEquals("minecraft:breeze_wind_charge", EntityIdMapper.toJavaId("minecraft:breeze_wind_charge_projectile"))
        assertEquals("minecraft:wind_charge", EntityIdMapper.toJavaId("minecraft:wind_charge_projectile"))
        assertEquals("minecraft:bamboo_raft", EntityIdMapper.toJavaId("minecraft:boat", variant = 7))
        assertEquals("minecraft:bamboo_chest_raft", EntityIdMapper.toJavaId("minecraft:chest_boat", variant = 7))
        assertEquals("minecraft:cherry_boat", EntityIdMapper.toJavaId("minecraft:boat", variant = 8))
        assertEquals("minecraft:cherry_chest_boat", EntityIdMapper.toJavaId("minecraft:chest_boat", variant = 8))
        assertEquals("minecraft:pale_oak_boat", EntityIdMapper.toJavaId("minecraft:boat", variant = 9))
        assertEquals("minecraft:pale_oak_chest_boat", EntityIdMapper.toJavaId("minecraft:chest_boat", variant = 9))
        assertEquals("minecraft:poplar_boat", EntityIdMapper.toJavaId("minecraft:boat", variant = 10))
        assertEquals("minecraft:poplar_chest_boat", EntityIdMapper.toJavaId("minecraft:chest_boat", variant = 10))
        assertEquals("minecraft:lingering_potion", EntityIdMapper.toJavaId("minecraft:lingering_potion", modernPotionIds = true))
        assertEquals("minecraft:potion", EntityIdMapper.toJavaId("minecraft:lingering_potion", modernPotionIds = false))
    }

    @Test
    fun testHealthCapsAndNonLivingEntitiesHaveNoHealthOrAttributes() {
        val withHealth = listOf(
            "armor_stand", "camel_husk", "copper_golem", "creaking",
            "happy_ghast", "nautilus", "parched", "zombie_nautilus", "sulfur_cube"
        )
        withHealth.forEach {
            assertTrue("Expected hasHealth($it)", EntityGroupTable.hasHealth("minecraft:$it"))
        }
        val nonLiving = listOf(
            "arrow", "tnt", "boat", "chest_boat", "minecart",
            "chest_minecart", "evocation_fang", "xp_orb", "splash_potion", "lingering_potion"
        )
        nonLiving.forEach {
            assertFalse("Expected !hasHealth($it)", EntityGroupTable.hasHealth("minecraft:$it"))
        }

        val jeArmorStand = baseJavaEntity("minecraft:armor_stand").apply {
            this["Health"] = 20.0f
        }
        val beArmorStand = BedrockActorBuilder.build(jeArmorStand, 1001L)!!
        assertEquals(6f, nbtFloatValue(beArmorStand["Health"])!!, 0.01f)

        val jeBoat = baseJavaEntity("minecraft:bamboo_raft")
        val beBoat = BedrockActorBuilder.build(jeBoat, 1002L)!!
        assertEquals("minecraft:boat", beBoat.getNullableString("identifier"))
        assertEquals(7, beBoat.getNullableInt("Variant"))
        assertNull(beBoat["Health"])
        assertNull(beBoat["Attributes"])

        val jeFang = baseJavaEntity("minecraft:evoker_fangs").apply {
            this["Warmup"] = 12
        }
        val beFang = BedrockActorBuilder.build(jeFang, 1003L)!!
        assertEquals("minecraft:evocation_fang", beFang.getNullableString("identifier"))
        assertEquals(12, beFang.getNullableInt("Warmup"))
        assertNull(beFang["Health"])
        assertNull(beFang["Attributes"])

        val jeFangRoundTrip = JavaEntityBuilder.build(beFang)!!
        assertEquals("minecraft:evoker_fangs", jeFangRoundTrip.getNullableString("id"))
        assertEquals(12, jeFangRoundTrip.getNullableInt("Warmup"))
        assertNull(jeFangRoundTrip["Health"])
        assertNull(jeFangRoundTrip["Attributes"])
        assertNull(jeFangRoundTrip["attributes"])
    }

    @Test
    fun testPosMotionClampingAndMinecartYOffsetRoundTrip() {
        val extremeEntity = baseJavaEntity("minecraft:pig", x = 45_000_000.0, y = 100.0, z = -45_000_000.0).apply {
            this["Motion"] = NbtList(NbtDouble(50.0), NbtDouble(-50.0), NbtDouble(15.0))
        }
        val bePig = BedrockActorBuilder.build(extremeEntity, 2001L)!!
        val pos = bePig.getNullableList("Pos")!!
        assertEquals(30_000_000f, nbtFloatValue(pos[0])!!, 1f)
        assertEquals(-30_000_000f, nbtFloatValue(pos[2])!!, 1f)
        val motion = bePig.getNullableList("Motion")!!
        assertEquals(10f, nbtFloatValue(motion[0])!!, 0.01f)
        assertEquals(-10f, nbtFloatValue(motion[1])!!, 0.01f)
        assertEquals(10f, nbtFloatValue(motion[2])!!, 0.01f)

        val jeMinecart = baseJavaEntity("minecraft:minecart", x = 5.5, y = 64.0, z = 5.5)
        val beMinecart = BedrockActorBuilder.build(jeMinecart, 2002L)!!
        val beY = nbtFloatValue(beMinecart.getNullableList("Pos")!![1])!!
        assertEquals(64.35f, beY, 0.01f)

        val jeMinecartRestored = JavaEntityBuilder.build(beMinecart)!!
        val jeY = (jeMinecartRestored.getNullableList("Pos")!![1] as NbtDouble).value
        assertEquals(64.0, jeY, 0.01)
    }

    @Test
    fun testCompoundCustomNameAndPersistenceRules() {
        val jeNamedPig = baseJavaEntity("minecraft:pig").apply {
            this["CustomName"] = NbtCompound().apply {
                this["text"] = "Sir Oink"
            }
            this["CustomNameVisible"] = true
        }
        val beNamedPig = BedrockActorBuilder.build(jeNamedPig, 3001L)!!
        assertEquals("Sir Oink", beNamedPig.getNullableString("CustomName"))
        assertEquals(1.toByte(), beNamedPig.getNullableByte("CustomNameVisible"))
        assertEquals(1.toByte(), beNamedPig.getNullableByte("Persistent"))

        val jeRestoredModern = JavaEntityBuilder.build(
            beNamedPig,
            JavaEntityBuilder.JavaTargetOptions(modernComponents = true, flatComponents = true)
        )!!
        val customNameComp = jeRestoredModern.getNullableCompound("CustomName")
        assertNotNull(customNameComp)
        assertEquals("Sir Oink", customNameComp!!.getNullableString("text"))
        assertEquals(1.toByte(), jeRestoredModern.getNullableByte("PersistenceRequired"))

        val wildPassenger = baseJavaEntity("minecraft:zombie")
        val bePassenger = BedrockActorBuilder.build(wildPassenger, 3002L, isPassenger = true)!!
        assertEquals(1.toByte(), bePassenger.getNullableByte("Persistent"))

        val vehiclePig = baseJavaEntity("minecraft:pig")
        val beVehicle = BedrockActorBuilder.build(vehiclePig, 3003L, hasPassengers = true)!!
        assertEquals(1.toByte(), beVehicle.getNullableByte("Persistent"))
    }

    @Test
    fun testXpOrbTntCommandBlockAndPotionRoundTrips() {
        val jeOrb = baseJavaEntity("minecraft:experience_orb").apply {
            this["Value"] = 42.toShort()
        }
        val beOrb = BedrockActorBuilder.build(jeOrb, 4001L)!!
        assertEquals("minecraft:xp_orb", beOrb.getNullableString("identifier"))
        assertEquals(42, beOrb.getNullableInt("experience value"))
        val jeOrbBack = JavaEntityBuilder.build(beOrb)!!
        assertEquals("minecraft:experience_orb", jeOrbBack.getNullableString("id"))
        assertEquals(42.toShort(), jeOrbBack.getNullableShort("Value"))

        val jeTnt = baseJavaEntity("minecraft:tnt").apply {
            this["fuse"] = 60.toShort()
        }
        val beTnt = BedrockActorBuilder.build(jeTnt, 4002L)!!
        assertEquals("minecraft:tnt", beTnt.getNullableString("identifier"))
        assertEquals(60.toShort(), beTnt.getNullableShort("Fuse"))
        val jeTntShort = JavaEntityBuilder.build(beTnt, JavaEntityBuilder.JavaTargetOptions(shortFuse = true))!!
        assertEquals(60.toShort(), jeTntShort.getNullableShort("fuse"))
        val jeTntByte = JavaEntityBuilder.build(beTnt, JavaEntityBuilder.JavaTargetOptions(shortFuse = false))!!
        assertEquals(60.toByte(), jeTntByte.getNullableByte("Fuse"))

        val jeCmdCart = baseJavaEntity("minecraft:command_block_minecart").apply {
            this["Command"] = "say chunkoid"
        }
        val beCmdCart = BedrockActorBuilder.build(jeCmdCart, 4003L)!!
        assertEquals("say chunkoid", beCmdCart.getNullableString("Command"))
        val jeCmdCartBack = JavaEntityBuilder.build(beCmdCart)!!
        assertEquals("say chunkoid", jeCmdCartBack.getNullableString("Command"))

        val jeLegacyLingering = baseJavaEntity("minecraft:potion").apply {
            this["Item"] = NbtCompound().apply {
                this["id"] = "minecraft:lingering_potion"
                this["Count"] = 1.toByte()
            }
        }
        val beLingering = BedrockActorBuilder.build(jeLegacyLingering, 4004L)!!
        assertEquals("minecraft:lingering_potion", beLingering.getNullableString("identifier"))
        val jeModernPotion = JavaEntityBuilder.build(
            beLingering,
            JavaEntityBuilder.JavaTargetOptions(modernPotionIds = true)
        )!!
        assertEquals("minecraft:lingering_potion", jeModernPotion.getNullableString("id"))
        val jeOldPotion = JavaEntityBuilder.build(
            beLingering,
            JavaEntityBuilder.JavaTargetOptions(modernPotionIds = false)
        )!!
        assertEquals("minecraft:potion", jeOldPotion.getNullableString("id"))
    }

    @Test
    fun testEntityContainersAndEnchantedItemsRoundTrip() {
        val sword = NbtCompound().apply {
            this["Slot"] = 0.toByte()
            this["id"] = "minecraft:diamond_sword"
            this["count"] = 1
            this["components"] = NbtCompound().apply {
                this["minecraft:damage"] = 12
                this["minecraft:custom_name"] = NbtCompound().apply {
                    this["text"] = "Excalibur"
                }
                this["minecraft:enchantments"] = NbtCompound().apply {
                    this["minecraft:sharpness"] = 5
                }
            }
        }

        val jeChestCart = baseJavaEntity("minecraft:chest_minecart").apply {
            this["Items"] = NbtList(sword)
        }
        val beChestCart = BedrockActorBuilder.build(jeChestCart, 5001L)!!
        val chestItems = beChestCart.getNullableCompoundList("ChestItems")
        assertNotNull(chestItems)
        assertEquals(1, chestItems!!.size)
        assertEquals("minecraft:diamond_sword", chestItems[0].getNullableString("Name"))
        assertEquals(12.toShort(), chestItems[0].getNullableShort("Damage"))
        val enchList = chestItems[0].getNullableCompound("tag")?.getNullableCompoundList("ench")
        assertNotNull(enchList)
        assertEquals(9.toShort(), enchList!![0].getNullableShort("id"))
        assertEquals(5.toShort(), enchList[0].getNullableShort("lvl"))

        val jeChestCartBack = JavaEntityBuilder.build(
            beChestCart,
            JavaEntityBuilder.JavaTargetOptions(modernComponents = true, flatComponents = true, modernEquipment = true)
        )!!
        val restoredItems = jeChestCartBack.getNullableCompoundList("Items")
        assertNotNull(restoredItems)
        assertEquals(1, restoredItems!!.size)
        val restoredSword = restoredItems[0]
        assertEquals("minecraft:diamond_sword", restoredSword.getNullableString("id"))
        assertEquals(1, restoredSword.getNullableInt("count"))
        val comps = restoredSword.getNullableCompound("components")!!
        assertEquals(12, comps.getNullableInt("minecraft:damage"))
        assertEquals("Excalibur", comps.getNullableCompound("minecraft:custom_name")?.getNullableString("text"))
        assertEquals(5, comps.getNullableCompound("minecraft:enchantments")?.getNullableInt("minecraft:sharpness"))

        val jeDonkey = baseJavaEntity("minecraft:donkey").apply {
            this["ChestedHorse"] = true
            this["Items"] = NbtList(
                NbtCompound().apply {
                    this["Slot"] = 2.toByte()
                    this["id"] = "minecraft:apple"
                    this["count"] = 3
                }
            )
        }
        val beDonkey = BedrockActorBuilder.build(jeDonkey, 5002L)!!
        assertEquals(1.toByte(), beDonkey.getNullableByte("Chested"))
        val beDonkeyItems = beDonkey.getNullableCompoundList("Items")
        assertNotNull(beDonkeyItems)
        assertEquals(1, beDonkeyItems!!.size)

        val jeDonkeyBack = JavaEntityBuilder.build(beDonkey)!!
        assertEquals(1.toByte(), jeDonkeyBack.getNullableByte("ChestedHorse"))
        assertEquals("minecraft:apple", jeDonkeyBack.getNullableCompoundList("Items")!![0].getNullableString("id"))
    }

    @Test
    fun testBedrockToJavaPipelineLinksNoCyclesAndJava26Layout() {
        val beWorld = tempFolder.newFolder("be_pipeline_world")
        val dbDir = File(beWorld, "db")
        val jeOut = tempFolder.newFolder("je_26_3_out")

        val options = Options().apply {
            compressionType(CompressionType.ZLIB_RAW)
            createIfMissing(true)
        }
        val db = Iq80DBFactory.factory.open(dbDir, options)
        try {
            val boatActor = NbtCompound().apply {
                this["identifier"] = "minecraft:boat"
                this["UniqueID"] = 100L
                this["Variant"] = 7
                this["Pos"] = NbtList(NbtFloat(8.5f), NbtFloat(64f), NbtFloat(8.5f))
                this["LinksTag"] = NbtList(EntityLinks.createVehicleLinkEntry(101L, 0))
            }
            val pigActor = NbtCompound().apply {
                this["identifier"] = "minecraft:pig"
                this["UniqueID"] = 101L
                this["Pos"] = NbtList(NbtFloat(8.5f), NbtFloat(64.5f), NbtFloat(8.5f))
                this["LinksTag"] = NbtList(EntityLinks.createPassengerLinkEntry(100L))
            }
            putActorInDb(db, 100L, boatActor, chunkX = 0, chunkZ = 0)
            putActorInDb(db, 101L, pigActor, chunkX = 0, chunkZ = 0)

            val cycleA = NbtCompound().apply {
                this["identifier"] = "minecraft:zombie"
                this["UniqueID"] = 200L
                this["Pos"] = NbtList(NbtFloat(4.5f), NbtFloat(64f), NbtFloat(4.5f))
                this["LinksTag"] = NbtList(
                    NbtCompound().apply {
                        this["entityID"] = 201L
                        this["linkID"] = 0
                        this["RideID"] = 200L
                        this["VehicleID"] = 201L
                    }
                )
            }
            val cycleB = NbtCompound().apply {
                this["identifier"] = "minecraft:skeleton"
                this["UniqueID"] = 201L
                this["Pos"] = NbtList(NbtFloat(4.5f), NbtFloat(65f), NbtFloat(4.5f))
                this["LinksTag"] = NbtList(
                    NbtCompound().apply {
                        this["entityID"] = 200L
                        this["linkID"] = 0
                        this["RideID"] = 201L
                        this["VehicleID"] = 200L
                    }
                )
            }
            putActorInDb(db, 200L, cycleA, chunkX = 0, chunkZ = 0)
            putActorInDb(db, 201L, cycleB, chunkX = 0, chunkZ = 0)
        } finally {
            db.close()
        }

        val pipeline = BedrockToJavaEntityPipeline()
        pipeline.scan(beWorld, listOf("overworld"), "JAVA_26_3")
        val stats = pipeline.flushToJava(jeOut, "JAVA_26_3", beWorld)
        assertTrue(stats.totalActors > 0)

        val mcaFile = File(jeOut, "dimensions/minecraft/overworld/entities/r.0.0.mca")
        assertTrue("Expected JE 26.3 dimension entities mca at $mcaFile", mcaFile.exists())

        AnvilRegionFile(mcaFile).use { region ->
            val chunk = region.readChunkNbt(0, 0)
            assertNotNull(chunk)
            val root = chunk!!.compound
            assertEquals(5001, root.getNullableInt("DataVersion"))

            val entities = root.getNullableCompoundList("Entities")!!
            val bambooRaft = entities.find { it.getNullableString("id") == "minecraft:bamboo_raft" }
            assertNotNull("Expected bamboo_raft", bambooRaft)

            val passengers = bambooRaft!!.getNullableCompoundList("Passengers")
            assertNotNull("Expected pig passenger attached to boat", passengers)
            assertEquals(1, passengers!!.size)
            assertEquals("minecraft:pig", passengers[0].getNullableString("id"))

            assertNull(entities.find { it.getNullableString("id") == "minecraft:pig" })
            assertTrue(entities.any { it.getNullableString("id") == "minecraft:zombie" || it.getNullableString("id") == "minecraft:skeleton" })
        }
    }

    @Test
    fun testJavaToJavaPipelineAdaptsNestedPassengersEquipmentTntAndPotions() {
        val legacyWorld = tempFolder.newFolder("je_1201_world")
        val modernWorld = tempFolder.newFolder("je_26_3_world")
        val backToLegacyWorld = tempFolder.newFolder("je_back_1201_world")

        val legacyEntitiesDir = File(legacyWorld, "entities").apply { mkdirs() }
        val legacyEntitiesMca = File(legacyEntitiesDir, "r.0.0.mca")

        val zombiePassenger = baseJavaEntity("minecraft:zombie").apply {
            this["HandItems"] = NbtList(
                NbtCompound().apply {
                    this["id"] = "minecraft:iron_sword"
                    this["Count"] = 1.toByte()
                    this["tag"] = NbtCompound().apply {
                        this["Damage"] = 5
                        this["display"] = NbtCompound().apply {
                            this["Name"] = "{\"text\":\"OldBlade\"}"
                        }
                        this["Enchantments"] = NbtList(
                            NbtCompound().apply {
                                this["id"] = "minecraft:sharpness"
                                this["lvl"] = 3.toShort()
                            }
                        )
                    }
                },
                NbtCompound()
            )
            this["ArmorItems"] = NbtList(
                NbtCompound(),
                NbtCompound(),
                NbtCompound().apply {
                    this["id"] = "minecraft:iron_chestplate"
                    this["Count"] = 1.toByte()
                },
                NbtCompound()
            )
        }
        val legacyBoat = baseJavaEntity("minecraft:oak_boat").apply {
            this["Passengers"] = NbtList(zombiePassenger)
        }
        val legacyTnt = baseJavaEntity("minecraft:tnt").apply {
            this["Fuse"] = 40.toByte()
        }
        val legacyLingeringPotion = baseJavaEntity("minecraft:potion").apply {
            this["Item"] = NbtCompound().apply {
                this["id"] = "minecraft:lingering_potion"
                this["Count"] = 1.toByte()
            }
        }

        AnvilRegionFile(legacyEntitiesMca).use { region ->
            val chunkRoot = NbtCompound().apply {
                this["DataVersion"] = 3465
                this["Position"] = NbtIntArray(intArrayOf(0, 0))
                this["Entities"] = NbtList(legacyBoat, legacyTnt, legacyLingeringPotion)
            }
            region.writeChunkNbt(0, 0, NbtFile("", chunkRoot))
        }

        val j2jUp = JavaToJavaEntityPipeline()
        j2jUp.scan(legacyWorld, listOf("overworld"), "JAVA_26_3")
        val upStats = j2jUp.flushToJava(modernWorld, "JAVA_26_3", legacyWorld)
        assertEquals(3, upStats.totalActors)

        val modernMca = File(modernWorld, "dimensions/minecraft/overworld/entities/r.0.0.mca")
        assertTrue(modernMca.exists())
        AnvilRegionFile(modernMca).use { region ->
            val root = region.readChunkNbt(0, 0)!!.compound
            assertEquals(5001, root.getNullableInt("DataVersion"))
            val entities = root.getNullableCompoundList("Entities")!!

            val boat = entities.find { it.getNullableString("id") == "minecraft:oak_boat" }!!
            val rider = boat.getNullableCompoundList("Passengers")!![0]
            val equipment = rider.getNullableCompound("equipment")!!
            val mainhand = equipment.getNullableCompound("mainhand")!!
            assertEquals("minecraft:iron_sword", mainhand.getNullableString("id"))
            assertEquals(1, mainhand.getNullableInt("count"))
            val comps = mainhand.getNullableCompound("components")!!
            assertEquals(5, comps.getNullableInt("minecraft:damage"))
            assertEquals(3, comps.getNullableCompound("minecraft:enchantments")?.getNullableInt("minecraft:sharpness"))

            val tnt = entities.find { it.getNullableString("id") == "minecraft:tnt" }!!
            assertEquals(40.toShort(), tnt.getNullableShort("fuse"))

            val potion = entities.find { it.getNullableString("id") == "minecraft:lingering_potion" }
            assertNotNull("Expected lingering_potion split in >=1.21.5", potion)
        }

        val j2jDown = JavaToJavaEntityPipeline()
        j2jDown.scan(modernWorld, listOf("overworld"), "JAVA_1_20_1")
        val downStats = j2jDown.flushToJava(backToLegacyWorld, "JAVA_1_20_1", modernWorld)
        assertEquals(3, downStats.totalActors)

        val backMca = File(backToLegacyWorld, "entities/r.0.0.mca")
        assertTrue(backMca.exists())
        AnvilRegionFile(backMca).use { region ->
            val root = region.readChunkNbt(0, 0)!!.compound
            assertEquals(3465, root.getNullableInt("DataVersion"))
            val entities = root.getNullableCompoundList("Entities")!!

            val boat = entities.find { it.getNullableString("id") == "minecraft:oak_boat" }!!
            val rider = boat.getNullableCompoundList("Passengers")!![0]
            val handItems = rider.getNullableCompoundList("HandItems")!!
            assertEquals("minecraft:iron_sword", handItems[0].getNullableString("id"))
            assertEquals(1.toByte(), handItems[0].getNullableByte("Count"))
            val legacyTag = handItems[0].getNullableCompound("tag")!!
            assertEquals(5, legacyTag.getNullableInt("Damage"))
            val legacyEnch = legacyTag.getNullableCompoundList("Enchantments")!!
            assertEquals("minecraft:sharpness", legacyEnch[0].getNullableString("id"))
            assertEquals(3.toShort(), legacyEnch[0].getNullableShort("lvl"))

            val tnt = entities.find { it.getNullableString("id") == "minecraft:tnt" }!!
            assertEquals(40.toByte(), tnt.getNullableByte("Fuse"))

            val potion = entities.find { it.getNullableString("id") == "minecraft:potion" }
            assertNotNull("Expected potion unified in <=1.21.4", potion)
        }
    }

    @Test
    fun testDigpPreservesRegularActorsAndLeashKnotsInSameChunk() {
        val jeDir = tempFolder.newFolder("je_digp_knot")
        val beWorldDir = tempFolder.newFolder("be_digp_knot")
        val beDbDir = File(beWorldDir, "db").apply { mkdirs() }
        Iq80DBFactory.factory.open(
            beDbDir,
            Options().createIfMissing(true).compressionType(CompressionType.ZLIB_RAW)
        ).close()
        val entitiesDir = File(jeDir, "entities").apply { mkdirs() }
        val mcaFile = File(entitiesDir, "r.0.0.mca")

        val boatWithPassenger = baseJavaEntity("minecraft:oak_boat", x = 3.5, y = 64.0, z = 3.5).apply {
            this["UUID"] = NbtIntArray(intArrayOf(11, 22, 33, 44))
            this["Passengers"] = NbtList(
                baseJavaEntity("minecraft:zombie", x = 3.5, y = 64.5, z = 3.5).apply {
                    this["UUID"] = NbtIntArray(intArrayOf(55, 66, 77, 88))
                }
            )
        }
        val leashedPig = baseJavaEntity("minecraft:pig", x = 7.5, y = 64.0, z = 7.5).apply {
            this["UUID"] = NbtIntArray(intArrayOf(99, 100, 101, 102))
            this["leash"] = NbtIntArray(intArrayOf(5, 64, 5))
        }
        val javaKnot = baseJavaEntity("minecraft:leash_knot", x = 5.5, y = 64.0, z = 5.5).apply {
            this["UUID"] = NbtIntArray(intArrayOf(103, 104, 105, 106))
        }

        AnvilRegionFile(mcaFile).use { region ->
            val chunkRoot = NbtCompound().apply {
                this["DataVersion"] = 5023
                this["Position"] = NbtIntArray(intArrayOf(0, 0))
                this["Entities"] = NbtList(boatWithPassenger, leashedPig, javaKnot)
            }
            region.writeChunkNbt(0, 0, NbtFile("", chunkRoot))
        }

        val pipeline = EntityConversionPipeline()
        pipeline.preExtract(jeDir, listOf("overworld"))
        val stats = pipeline.flushToLevelDb(beWorldDir)
        assertEquals(4, stats.totalActors)

        Iq80DBFactory.factory.open(beDbDir, Options().createIfMissing(false)).use { db ->
            val digpKey = pipeline.buildDigpKey(0, 0, 0)
            val digpBytes = db.get(digpKey)
            assertNotNull("Expected digp entry for chunk (0,0)", digpBytes)
            assertEquals("digp must retain all 4 actor refs (boat + passenger + pig + leash_knot)", 32, digpBytes!!.size)

            val identifiers = mutableListOf<String>()
            val buf = ByteBuffer.wrap(digpBytes).order(ByteOrder.BIG_ENDIAN)
            while (buf.remaining() >= 8) {
                val storageKey = ByteArray(8)
                buf.get(storageKey)
                val actorKey = "actorprefix".toByteArray(Charsets.US_ASCII) + storageKey
                val actorBytes = db.get(actorKey)
                assertNotNull("Missing actorprefix record referenced by digp", actorBytes)
                val actor = NbtIO.readNbtFile(actorBytes!!.inputStream(), compressed = false, littleEndian = true).compound
                identifiers.add(actor.getNullableString("identifier")!!)
            }
            assertEquals(
                listOf("minecraft:boat", "minecraft:zombie", "minecraft:pig", "minecraft:leash_knot").sorted(),
                identifiers.sorted()
            )
        }
    }

    @Test
    fun testCushionHandledByNativeJe2BeButPreservedInBe2JeAndJe2Je() {
        assertTrue(EntityIdMapper.isVanilla("minecraft:cushion"))
        assertTrue(EntityIdMapper.isVanilla("minecraft:block_display"))
        assertEquals("minecraft:cushion", EntityIdMapper.toJavaId("minecraft:cushion"))
        assertFalse(EntityIdMapper.isHandledByChunkerNative("minecraft:cushion"))
        assertTrue(EntityIdMapper.isHandledByChunkerNative("minecraft:cushion", includeBedrockExclusiveNative = true))

        val beWorld = tempFolder.newFolder("be_cushion")
        val beDbDir = File(beWorld, "db")
        val jeOut = tempFolder.newFolder("je_cushion_out")
        Iq80DBFactory.factory.open(beDbDir, Options().createIfMissing(true).compressionType(CompressionType.ZLIB)).use { db ->
            val cushionActor = NbtCompound().apply {
                this["identifier"] = "minecraft:cushion"
                this["UniqueID"] = -4294967296L
                this["Pos"] = NbtList(NbtFloat(5.5f), NbtFloat(64.0f), NbtFloat(5.5f))
                this["Motion"] = NbtList(NbtFloat(0f), NbtFloat(0f), NbtFloat(0f))
                this["Rotation"] = NbtList(NbtFloat(0f), NbtFloat(0f))
                this["Variant"] = 15
            }
            putActorInDb(db, -4294967296L, cushionActor, chunkX = 0, chunkZ = 0)
        }
        val b2j = BedrockToJavaEntityPipeline()
        b2j.scan(beWorld, listOf("overworld", "the_nether", "the_end"), "JAVA_26_3")
        val b2jStats = b2j.flushToJava(jeOut, "JAVA_26_3", beWorld)
        assertEquals(0, b2jStats.skippedModded)
        assertEquals(1, b2jStats.totalActors)

        val j2jOut = tempFolder.newFolder("j2j_cushion_out")
        val j2j = JavaToJavaEntityPipeline()
        j2j.scan(jeOut, listOf("overworld"), "JAVA_26_3")
        val j2jStats = j2j.flushToJava(j2jOut, "JAVA_26_3", jeOut)
        assertEquals(0, j2jStats.skippedModded)
        assertEquals(1, j2jStats.totalActors)
    }

    @Test
    fun testBoatVariantOrderAndCanRideDefinitions() {
        val expectedVariants = listOf(
            "oak" to 0,
            "spruce" to 1,
            "birch" to 2,
            "jungle" to 3,
            "acacia" to 4,
            "dark_oak" to 5,
            "mangrove" to 6,
            "bamboo" to 7,
            "cherry" to 8,
            "pale_oak" to 9,
            "poplar" to 10
        )

        for ((wood, expectedVariant) in expectedVariants) {
            val jeBoatId = if (wood == "bamboo") "minecraft:bamboo_raft" else "minecraft:${wood}_boat"
            val jeChestBoatId = if (wood == "bamboo") "minecraft:bamboo_chest_raft" else "minecraft:${wood}_chest_boat"

            val beBoat = BedrockActorBuilder.build(baseJavaEntity(jeBoatId), 2000L + expectedVariant)!!
            val beChestBoat = BedrockActorBuilder.build(baseJavaEntity(jeChestBoatId), 2100L + expectedVariant)!!

            assertEquals("JE->BE Variant mismatch for $jeBoatId", expectedVariant, beBoat.getNullableInt("Variant"))
            assertEquals("JE->BE Variant mismatch for $jeChestBoatId", expectedVariant, beChestBoat.getNullableInt("Variant"))

            val boatDefs = beBoat.getNullableList("definitions")?.mapNotNull { (it as? NbtString)?.value } ?: emptyList()
            val chestDefs = beChestBoat.getNullableList("definitions")?.mapNotNull { (it as? NbtString)?.value } ?: emptyList()
            val expectedRideDef = if (expectedVariant == 7) "+minecraft:can_ride_bamboo" else "+minecraft:can_ride_default"
            val unexpectedRideDef = if (expectedVariant == 7) "+minecraft:can_ride_default" else "+minecraft:can_ride_bamboo"

            assertTrue("Missing +minecraft:boat in $boatDefs", "+minecraft:boat" in boatDefs)
            assertTrue("Missing $expectedRideDef in $boatDefs", expectedRideDef in boatDefs)
            assertFalse("Unexpected $unexpectedRideDef in $boatDefs", unexpectedRideDef in boatDefs)

            assertTrue("Missing +minecraft:chest_boat in $chestDefs", "+minecraft:chest_boat" in chestDefs)
            assertTrue("Missing $expectedRideDef in $chestDefs", expectedRideDef in chestDefs)
            assertFalse("Unexpected $unexpectedRideDef in $chestDefs", unexpectedRideDef in chestDefs)

            assertEquals(jeBoatId, EntityIdMapper.toJavaId("minecraft:boat", variant = expectedVariant))
            assertEquals(jeChestBoatId, EntityIdMapper.toJavaId("minecraft:chest_boat", variant = expectedVariant))
        }
    }

    @Test
    fun testOcelotDefinitionsAndEntityPropertiesPresent() {
        val beOcelot = BedrockActorBuilder.build(baseJavaEntity("minecraft:ocelot"), 3001L)!!
        val ocelotDefs = beOcelot.getNullableStringList("definitions")?.map { it.value } ?: emptyList()
        assertTrue("Expected +minecraft:ocelot in $ocelotDefs", "+minecraft:ocelot" in ocelotDefs)
        assertEquals(10.toShort(), beOcelot.getNullableShort("Health"))
        assertNotNull(beOcelot.getNullableCompoundList("Attributes"))

        val beCow = BedrockActorBuilder.build(baseJavaEntity("minecraft:cow"), 3002L)!!
        val cowProps = beCow.getNullableCompound("properties")
        assertNotNull("Expected cow properties compound", cowProps)
        assertEquals("temperate", cowProps!!.getNullableString("minecraft:climate_variant"))
        assertEquals("default", cowProps.getNullableString("minecraft:sound_variant"))

        val beCat = BedrockActorBuilder.build(baseJavaEntity("minecraft:cat"), 3003L)!!
        assertNotNull(beCat.getNullableCompound("properties"))
    }

    @Test
    fun testMooshroomFrogFoxSheepValidGroupNames() {
        val jeBrownMooshroom = baseJavaEntity("minecraft:mooshroom").apply {
            this["Type"] = "brown"
        }
        val beMooshroom = BedrockActorBuilder.build(jeBrownMooshroom, 3101L)!!
        val mooshroomDefs = beMooshroom.getNullableStringList("definitions")?.map { it.value } ?: emptyList()
        assertEquals(1, beMooshroom.getNullableInt("Variant"))
        assertTrue("+minecraft:mooshroom_brown" in mooshroomDefs)
        assertFalse("+minecraft:brown_mooshroom" in mooshroomDefs)
        val jeMooshroomRestored = JavaEntityBuilder.build(beMooshroom)!!
        assertEquals("brown", jeMooshroomRestored.getNullableString("Type"))

        val jeFrog = baseJavaEntity("minecraft:frog").apply {
            this["variant"] = "minecraft:cold"
        }
        val beFrog = BedrockActorBuilder.build(jeFrog, 3102L)!!
        val frogDefs = beFrog.getNullableStringList("definitions")?.map { it.value } ?: emptyList()
        assertEquals(1, beFrog.getNullableInt("Variant"))
        assertTrue("+minecraft:cold_frog" in frogDefs)
        assertFalse("+minecraft:frog_cold" in frogDefs)

        val jeSnowFox = baseJavaEntity("minecraft:fox").apply {
            this["Type"] = "snow"
        }
        val beFox = BedrockActorBuilder.build(jeSnowFox, 3103L)!!
        val foxDefs = beFox.getNullableStringList("definitions")?.map { it.value } ?: emptyList()
        assertEquals(1, beFox.getNullableInt("Variant"))
        assertTrue("+minecraft:fox_arctic" in foxDefs)
        assertFalse("+minecraft:fox_snow" in foxDefs)

        val jeMagentaSheep = baseJavaEntity("minecraft:sheep").apply {
            this["Color"] = 2.toByte()
        }
        val beSheep = BedrockActorBuilder.build(jeMagentaSheep, 3104L)!!
        val sheepDefs = beSheep.getNullableStringList("definitions")?.map { it.value } ?: emptyList()
        assertEquals(2.toByte(), beSheep.getNullableByte("Color"))
        assertFalse("+minecraft:sheep_magenta" in sheepDefs)
    }

    @Test
    fun testFallingBlockTagsAndJsonExtraCustomName() {
        val jeSandString = baseJavaEntity("minecraft:falling_block").apply {
            this["BlockState"] = "minecraft:sand"
            this["Tags"] = NbtList(NbtString("lc_entity"), NbtString("exhibit"))
            this["CustomName"] = "{\"text\":\"Hello \",\"color\":\"gold\",\"extra\":[{\"text\":\"World\",\"bold\":true}]}"
            this["CustomNameVisible"] = 1.toByte()
        }
        val beSand = BedrockActorBuilder.build(jeSandString, 3201L)!!
        val fbCompound = beSand.getNullableCompound("FallingBlock")
        assertNotNull("Expected FallingBlock compound", fbCompound)
        assertEquals("minecraft:sand", fbCompound!!.getNullableString("name"))
        assertNotNull(fbCompound.getNullableCompound("states"))
        assertEquals(18163713, fbCompound.getNullableInt("version"))

        val beTags = beSand.getNullableStringList("Tags")?.map { it.value }
        assertEquals(listOf("lc_entity", "exhibit"), beTags)
        assertEquals("\u00A76Hello \u00A7lWorld", beSand.getNullableString("CustomName"))

        val jeSandRestored = JavaEntityBuilder.build(beSand)!!
        assertEquals("minecraft:sand", jeSandRestored.getNullableCompound("BlockState")?.getNullableString("Name"))
        assertEquals(listOf("lc_entity", "exhibit"), jeSandRestored.getNullableStringList("Tags")?.map { it.value })

        val jeAnvilCompound = baseJavaEntity("minecraft:falling_block").apply {
            this["BlockState"] = NbtCompound().apply {
                this["Name"] = "minecraft:anvil"
                this["Properties"] = NbtCompound().apply { this["facing"] = "north" }
            }
        }
        val beAnvil = BedrockActorBuilder.build(jeAnvilCompound, 3202L)!!
        val anvilFb = beAnvil.getNullableCompound("FallingBlock")!!
        assertEquals("minecraft:anvil", anvilFb.getNullableString("name"))
        assertEquals("north", anvilFb.getNullableCompound("states")?.getNullableString("facing"))
    }

    @Test
    fun `empty vehicles get Persistent 1b without living Attributes`() {
        for (jeId in listOf("minecraft:minecart", "minecraft:boat", "minecraft:hopper_minecart")) {
            val jeEntity = baseJavaEntity(jeId)
            val beEntity = BedrockActorBuilder.build(jeEntity, 4001L)!!
            assertEquals("Expected Persistent=1b for $jeId", 1.toByte(), beEntity.getNullableByte("Persistent"))
            assertNull("Expected no Attributes for non-living $jeId", beEntity["Attributes"])
        }
    }

    @Test
    fun `hopper minecart maps Enabled flag and preserves ChestItems`() {
        val jeEnabled = baseJavaEntity("minecraft:hopper_minecart").apply {
            this["Enabled"] = 1.toByte()
            this["Items"] = NbtList<NbtCompound>().apply {
                add(NbtCompound().apply {
                    this["id"] = "minecraft:diamond"
                    this["Count"] = 3.toByte()
                    this["Slot"] = 0.toByte()
                })
            }
        }
        val beEnabled = BedrockActorBuilder.build(jeEnabled, 4002L)!!
        val enabledDefs = beEnabled.getNullableStringList("definitions")?.map { it.value }.orEmpty()
        assertTrue("Expected +minecraft:hopper_active in $enabledDefs", "+minecraft:hopper_active" in enabledDefs)
        val chestItems = beEnabled.getNullableCompoundList("ChestItems")?.toList().orEmpty()
        assertEquals(1, chestItems.size)
        assertEquals("minecraft:diamond", chestItems[0].getNullableString("Name"))
        assertEquals(3.toByte(), chestItems[0].getNullableByte("Count"))

        val jeDisabled = baseJavaEntity("minecraft:hopper_minecart").apply {
            this["Enabled"] = 0.toByte()
        }
        val beDisabled = BedrockActorBuilder.build(jeDisabled, 4003L)!!
        val disabledDefs = beDisabled.getNullableStringList("definitions")?.map { it.value }.orEmpty()
        assertTrue("Expected +minecraft:hopper_inactive in $disabledDefs", "+minecraft:hopper_inactive" in disabledDefs)
    }

    @Test
    fun `horse emits movement and jump_strength Attributes`() {
        val jeHorse = baseJavaEntity("minecraft:horse").apply {
            this["Health"] = 24f
            this["Attributes"] = NbtList<NbtCompound>().apply {
                add(NbtCompound().apply {
                    this["Name"] = "minecraft:generic.movement_speed"
                    this["Base"] = 0.225
                })
                add(NbtCompound().apply {
                    this["Name"] = "minecraft:horse.jump_strength"
                    this["Base"] = 0.7
                })
            }
        }
        val beHorse = BedrockActorBuilder.build(jeHorse, 4004L)!!
        val attrs = beHorse.getNullableCompoundList("Attributes")?.toList().orEmpty()
        val byName = attrs.associateBy { it.getNullableString("Name") }

        val movement = byName["minecraft:movement"]!!
        assertEquals(0.225f, nbtFloatValue(movement["Base"])!!, 1e-4f)
        assertEquals(0f, nbtFloatValue(movement["DefaultMin"])!!, 1e-6f)
        assertEquals(Float.MAX_VALUE, nbtFloatValue(movement["DefaultMax"])!!)

        val jump = byName["minecraft:horse.jump_strength"]!!
        assertEquals(0.7f, nbtFloatValue(jump["Base"])!!, 1e-4f)
        assertEquals(0f, nbtFloatValue(jump["DefaultMin"])!!, 1e-6f)
        assertEquals(Float.MAX_VALUE, nbtFloatValue(jump["DefaultMax"])!!)
    }

    @Test
    fun `cat variant fallback legacy CatType and roundTrip`() {
        val jeUnknown = baseJavaEntity("minecraft:cat").apply {
            this["variant"] = "minecraft:custom_unknown"
        }
        val beUnknown = BedrockActorBuilder.build(jeUnknown, 4005L)!!
        assertEquals(8, beUnknown.getNullableInt("Variant"))
        val unknownDefs = beUnknown.getNullableStringList("definitions")?.map { it.value }.orEmpty()
        assertTrue("+minecraft:cat_tabby" in unknownDefs)

        val jeLegacyJellie = baseJavaEntity("minecraft:cat").apply {
            this["CatType"] = 9
            this["CollarColor"] = 11.toByte()
        }
        val beLegacyJellie = BedrockActorBuilder.build(jeLegacyJellie, 4006L)!!
        assertEquals(10, beLegacyJellie.getNullableInt("Variant"))
        assertEquals(11.toByte(), beLegacyJellie.getNullableByte("CollarColor"))
        assertNull(beLegacyJellie.getNullableByte("Color"))
        val jellieDefs = beLegacyJellie.getNullableStringList("definitions")?.map { it.value }.orEmpty()
        assertTrue("+minecraft:cat_jellie" in jellieDefs)

        val jeRoundTrip = JavaEntityBuilder.build(beLegacyJellie)!!
        assertEquals("minecraft:jellie", jeRoundTrip.getNullableString("variant"))
        assertEquals(11.toByte(), jeRoundTrip.getNullableByte("CollarColor"))

        val legacyBedrockCat = NbtCompound().apply {
            this["identifier"] = "minecraft:cat"
            this["Variant"] = 9
            this["Color"] = 14.toByte()
        }
        assertEquals(14.toByte(), JavaEntityBuilder.build(legacyBedrockCat)!!.getNullableByte("CollarColor"))
    }

    @Test
    fun `shulker color maps Bedrock Variant definitions Color and roundTrips`() {
        val jeSilver = baseJavaEntity("minecraft:shulker").apply {
            this["Color"] = 8.toByte()
        }
        val beSilver = BedrockActorBuilder.build(jeSilver, 4007L)!!
        assertEquals(7, beSilver.getNullableInt("Variant"))
        assertEquals(8.toByte(), beSilver.getNullableByte("Color"))
        val silverDefs = beSilver.getNullableStringList("definitions")?.map { it.value }.orEmpty()
        assertTrue("+minecraft:shulker_silver" in silverDefs)
        assertEquals(8.toByte(), JavaEntityBuilder.build(beSilver)!!.getNullableByte("Color"))

        val jeRed = baseJavaEntity("minecraft:shulker").apply {
            this["Color"] = 14.toByte()
        }
        val beRed = BedrockActorBuilder.build(jeRed, 4008L)!!
        assertEquals(1, beRed.getNullableInt("Variant"))
        assertEquals(14.toByte(), beRed.getNullableByte("Color"))
        val redDefs = beRed.getNullableStringList("definitions")?.map { it.value }.orEmpty()
        assertTrue("+minecraft:shulker_red" in redDefs)
        assertEquals(14.toByte(), JavaEntityBuilder.build(beRed)!!.getNullableByte("Color"))

        val jeUndyed = baseJavaEntity("minecraft:shulker").apply {
            this["Color"] = 16.toByte()
        }
        val beUndyed = BedrockActorBuilder.build(jeUndyed, 4009L)!!
        assertEquals(16, beUndyed.getNullableInt("Variant"))
        assertEquals(16.toByte(), beUndyed.getNullableByte("Color"))
        val undyedDefs = beUndyed.getNullableStringList("definitions")?.map { it.value }.orEmpty()
        assertTrue("+minecraft:shulker_undyed" in undyedDefs)
        assertEquals(16.toByte(), JavaEntityBuilder.build(beUndyed)!!.getNullableByte("Color"))
    }
}
