package com.noches.chunkoidng.core.entity

import br.com.gamemods.nbtmanipulator.NbtCompound
import br.com.gamemods.nbtmanipulator.NbtList
import br.com.gamemods.nbtmanipulator.NbtLong
import java.nio.ByteBuffer
import java.nio.ByteOrder

object EntityLinks {

    private val LEASH_FIELDS = listOf("leash", "Leash")

    fun uniqueIdFromUuid(uuid: IntArray?): Long {
        if (uuid == null || uuid.size < 4) return 0L
        val high = (uuid[0].toLong() shl 32) or (uuid[1].toLong() and 0xFFFFFFFFL)
        val low = (uuid[2].toLong() shl 32) or (uuid[3].toLong() and 0xFFFFFFFFL)
        val mixed = mix(high xor mix(low))
        return if (mixed == 0L) 1L else mixed
    }

    fun uuidFromUniqueId(uniqueId: Long): IntArray {
        val high = mix(uniqueId xor -0x61c8864680b583ebL)
        val highV4 = (high and 0x0FFFFFFFFFFFFFFFL) or 0x4000000000000000L
        val lowV4 = (uniqueId and 0x3FFFFFFFFFFFFFFFL) or Long.MIN_VALUE

        return intArrayOf(
            (highV4 ushr 32).toInt(),
            highV4.toInt(),
            (lowV4 ushr 32).toInt(),
            lowV4.toInt()
        )
    }

    private fun mix(value: Long): Long {
        var z = value + -0x61c8864680b583ebL
        z = (z xor (z ushr 30)) * -0x40aa7b92e31b1a47L
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
        return z xor (z ushr 31)
    }

    fun storageKey(uniqueId: Long): ByteArray {
        val storageKey = 0x100000000L + (uniqueId xor -0x100000000L)
        val buffer = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)
        buffer.putLong(storageKey)
        return buffer.array()
    }

    fun riding(rider: NbtCompound, riderId: Long, vehicleId: Long) {
        val existing = rider.getNullableCompoundList("LinksTag")
        val links = NbtList<NbtCompound>()
        existing?.forEach { links.add(it) }
        links.add(createLinkEntry(rideId = riderId, vehicleId = vehicleId))
        rider["LinksTag"] = links
    }

    fun carried(vehicle: NbtCompound, vehicleId: Long, riderIds: List<Long>) {
        if (riderIds.isEmpty()) return
        val existing = vehicle.getNullableCompoundList("LinksTag")
        val links = NbtList<NbtCompound>()
        existing?.forEach { links.add(it) }
        for (riderId in riderIds) {
            links.add(createLinkEntry(rideId = riderId, vehicleId = vehicleId))
        }
        vehicle["LinksTag"] = links
    }

    fun leashed(actor: NbtCompound, holderId: Long) {
        actor["LeasherID"] = NbtLong(holderId)
    }

    fun leashHolder(entity: NbtCompound): Long {
        for (field in LEASH_FIELDS) {
            val leash = entity.getNullableCompound(field) ?: continue
            val uuid = leash.getNullableIntArray("UUID")
            if (uuid != null) {
                val id = uniqueIdFromUuid(uuid)
                if (id != 0L) return id
            }
            val x = leash.getNullableInt("X")
            val y = leash.getNullableInt("Y")
            val z = leash.getNullableInt("Z")
            if (x != null && y != null && z != null) {
                return deterministicKnotId(x, y, z)
            }
        }
        return 0L
    }

    fun deterministicKnotId(x: Int, y: Int, z: Int): Long {
        return mix((x.toLong() shl 40) xor (z.toLong() shl 16) xor y.toLong())
    }

    private fun createLinkEntry(rideId: Long, vehicleId: Long): NbtCompound {
        val entry = NbtCompound()
        entry["RideID"] = NbtLong(rideId)
        entry["VehicleID"] = NbtLong(vehicleId)
        return entry
    }
}
