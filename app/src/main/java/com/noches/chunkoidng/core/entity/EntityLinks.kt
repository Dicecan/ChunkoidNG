package com.noches.chunkoidng.core.entity

import br.com.gamemods.nbtmanipulator.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

object EntityLinks {

    private val LEASH_FIELDS = listOf("leash", "Leash")
    private const val LEASH_KNOT = "minecraft:leash_knot"

    sealed class LeashTarget {
        data class Block(val x: Int, val y: Int, val z: Int) : LeashTarget()
        data class Holder(val uniqueId: Long) : LeashTarget()
    }

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

    fun mix(value: Long): Long {
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

    fun isLeashKnot(entity: NbtCompound): Boolean {
        return LEASH_KNOT == entity.getNullableString("id")
    }

    fun position(knot: NbtCompound): IntArray? {
        val tx = knot.getNullableInt("TileX")
        val ty = knot.getNullableInt("TileY")
        val tz = knot.getNullableInt("TileZ")
        if (tx != null && ty != null && tz != null) {
            return intArrayOf(tx, ty, tz)
        }
        val pos = knot.getNullableDoubleList("Pos")
        if (pos != null && pos.size >= 3) {
            return intArrayOf(
                Math.floor(pos[0].value).toInt(),
                Math.floor(pos[1].value).toInt(),
                Math.floor(pos[2].value).toInt()
            )
        }
        return null
    }

    fun leash(entity: NbtCompound, uniqueIdProvider: (IntArray?) -> Long = { uniqueIdFromUuid(it) }): LeashTarget? {
        for (field in LEASH_FIELDS) {
            val tag = entity[field] ?: continue
            if (tag is NbtIntArray) {
                val arr = tag.value
                if (arr.size >= 3) {
                    return LeashTarget.Block(arr[0], arr[1], arr[2])
                }
            } else if (tag is NbtCompound) {
                val uuid = tag.getNullableIntArray("UUID")
                if (uuid != null) {
                    val id = uniqueIdProvider(uuid)
                    if (id != 0L) return LeashTarget.Holder(id)
                }
                val x = tag.getNullableInt("X")
                val y = tag.getNullableInt("Y")
                val z = tag.getNullableInt("Z")
                if (x != null && y != null && z != null) {
                    return LeashTarget.Block(x, y, z)
                }
            }
        }
        return null
    }

    fun knotId(x: Long, y: Long, z: Long): Long {
        val id = mix(mix(x) xor (mix(z) * -0x61c8864680b583ebL) xor (y * -0x3d4d51c2d82b14b1L))
        return if (id == 0L) 1L else id
    }

    fun buildKnotActor(id: Long, blockX: Int, blockY: Int, blockZ: Int): NbtCompound {
        val knot = NbtCompound()
        knot["identifier"] = NbtString(LEASH_KNOT)
        knot["definitions"] = NbtList<NbtString>()
        val pos = NbtList<NbtFloat>()
        pos.add(NbtFloat(blockX + 0.5f))
        pos.add(NbtFloat(blockY + 0.25f))
        pos.add(NbtFloat(blockZ + 0.5f))
        knot["Pos"] = pos
        val motion = NbtList<NbtFloat>()
        motion.add(NbtFloat(0f))
        motion.add(NbtFloat(0f))
        motion.add(NbtFloat(0f))
        knot["Motion"] = motion
        val rot = NbtList<NbtFloat>()
        rot.add(NbtFloat(0f))
        rot.add(NbtFloat(0f))
        knot["Rotation"] = rot
        knot["OnGround"] = NbtByte(1)
        knot["Sitting"] = NbtByte(0)
        knot["Tags"] = NbtList<NbtString>()
        knot["OwnerNew"] = NbtLong(-1L)
        knot["UniqueID"] = NbtLong(id)
        return knot
    }

    fun link(actor: NbtCompound, actorId: Long, vehicleId: Long, riderIds: List<Long>?) {
        val riders = riderIds?.size ?: 0
        if (vehicleId == 0L && riders == 0) return

        val links = NbtList<NbtCompound>()
        if (vehicleId != 0L) {
            links.add(createLinkEntry(rideId = actorId, vehicleId = vehicleId))
        }
        if (riderIds != null) {
            for (riderId in riderIds) {
                links.add(createLinkEntry(rideId = riderId, vehicleId = actorId))
            }
        }
        actor["LinksTag"] = links
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
        return when (val target = leash(entity) { uniqueIdFromUuid(it) }) {
            is LeashTarget.Block -> knotId(target.x.toLong(), target.y.toLong(), target.z.toLong())
            is LeashTarget.Holder -> target.uniqueId
            null -> 0L
        }
    }

    fun deterministicKnotId(x: Int, y: Int, z: Int): Long = knotId(x.toLong(), y.toLong(), z.toLong())

    fun createLinkEntry(rideId: Long, vehicleId: Long): NbtCompound {
        val entry = NbtCompound()
        entry["RideID"] = NbtLong(rideId)
        entry["VehicleID"] = NbtLong(vehicleId)
        return entry
    }
}
