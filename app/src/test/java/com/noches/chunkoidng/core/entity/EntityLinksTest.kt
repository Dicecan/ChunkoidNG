package com.noches.chunkoidng.core.entity

import br.com.gamemods.nbtmanipulator.NbtCompound
import br.com.gamemods.nbtmanipulator.NbtIntArray
import br.com.gamemods.nbtmanipulator.NbtList
import br.com.gamemods.nbtmanipulator.NbtLong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class EntityLinksTest {

    @Test
    fun testUniqueIdFromUuidDeterminism() {

        val uuid1 = intArrayOf(123456, -789012, 345678, -901234)
        val id1 = EntityLinks.uniqueIdFromUuid(uuid1)
        val id2 = EntityLinks.uniqueIdFromUuid(uuid1)

        assertNotEquals(0L, id1)
        assertEquals(id1, id2)

        val uuid2 = intArrayOf(123456, -789012, 345678, -901235)
        val id3 = EntityLinks.uniqueIdFromUuid(uuid2)
        assertNotEquals(id1, id3)

        assertEquals(0L, EntityLinks.uniqueIdFromUuid(null))
        assertEquals(0L, EntityLinks.uniqueIdFromUuid(intArrayOf(1, 2, 3)))
    }

    @Test
    fun testStorageKeyLengthAndFormat() {
        val uniqueId = 0x123456789ABCDEF0L
        val key = EntityLinks.storageKey(uniqueId)
        assertEquals(8, key.size)

        val buffer = ByteBuffer.wrap(key).order(ByteOrder.BIG_ENDIAN)
        val decoded = buffer.long
        assertNotEquals(0L, decoded)
    }

    @Test
    fun testRidingAndCarriedLinks() {
        val rider = NbtCompound()
        val riderId = 5555L
        val vehicleId = 9999L
        EntityLinks.riding(rider, riderId = riderId, vehicleId = vehicleId)

        val links = rider.getNullableCompoundList("LinksTag")
        assertNotNull(links)
        assertEquals(1, links!!.size)
        assertEquals(riderId, links[0].getNullableLong("RideID"))
        assertEquals(vehicleId, links[0].getNullableLong("VehicleID"))

        val vehicle = NbtCompound()
        val riderIds = listOf(111L, 222L)
        EntityLinks.carried(vehicle, vehicleId = vehicleId, riderIds = riderIds)

        val vLinks = vehicle.getNullableCompoundList("LinksTag")
        assertNotNull(vLinks)
        assertEquals(2, vLinks!!.size)
        assertEquals(111L, vLinks[0].getNullableLong("RideID"))
        assertEquals(vehicleId, vLinks[0].getNullableLong("VehicleID"))
        assertEquals(222L, vLinks[1].getNullableLong("RideID"))
        assertEquals(vehicleId, vLinks[1].getNullableLong("VehicleID"))
    }

    @Test
    fun testLeashHolderExtraction() {
        val javaEntity = NbtCompound()
        val leash = NbtCompound()
        leash["UUID"] = NbtIntArray(intArrayOf(10, 20, 30, 40))
        javaEntity["leash"] = leash

        val expectedId = EntityLinks.uniqueIdFromUuid(intArrayOf(10, 20, 30, 40))
        val extractedId = EntityLinks.leashHolder(javaEntity)
        assertEquals(expectedId, extractedId)
    }
}
