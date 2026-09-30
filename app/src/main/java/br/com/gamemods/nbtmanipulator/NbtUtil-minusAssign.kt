@file:JvmName("NbtUtil")
@file:JvmMultifileClass

package br.com.gamemods.nbtmanipulator

public operator fun NbtList<NbtByte>.minusAssign(value: Byte) { remove(value) }

public operator fun NbtList<NbtShort>.minusAssign(value: Short) { remove(value) }

public operator fun NbtList<NbtInt>.minusAssign(value: Int) { remove(value) }

public operator fun NbtList<NbtLong>.minusAssign(value: Long) { remove(value) }

public operator fun NbtList<NbtFloat>.minusAssign(value: Float) { remove(value) }

public operator fun NbtList<NbtDouble>.minusAssign(value: Double) { remove(value) }

public operator fun NbtList<NbtString>.minusAssign(value: String) { remove(value) }

public operator fun NbtList<NbtByteArray>.minusAssign(value: ByteArray) { remove(value) }

public operator fun NbtList<NbtByteArray>.minusAssign(value: Array<Byte>) { remove(value) }

public operator fun NbtList<NbtIntArray>.minusAssign(value: IntArray) { remove(value) }

public operator fun NbtList<NbtIntArray>.minusAssign(value: Array<Int>) { remove(value) }

public operator fun NbtList<NbtLongArray>.minusAssign(value: LongArray) { remove(value) }

public operator fun NbtList<NbtLongArray>.minusAssign(value: Array<Long>) { remove(value) }

public operator fun NbtList<NbtCompound>.minusAssign(value: Map<String, NbtTag>) { remove(value) }

public operator fun <T: NbtTag> NbtList<NbtList<T>>.minusAssign(value: Iterable<T>) { remove(value) }

public operator fun <T: NbtTag> NbtList<NbtList<T>>.minusAssign(value: Array<T>) { remove(value) }

@JvmName("minusAssignListOfList")
public operator fun NbtList<NbtList<NbtByte>>.minusAssign(value: Array<Byte>) { remove(value) }

@JvmName("minusAssignListOfList")
public operator fun NbtList<NbtList<NbtByte>>.minusAssign(value: ByteArray) { remove(value) }

@JvmName("minusAssignListOfListIterByte")
public operator fun NbtList<NbtList<NbtByte>>.minusAssign(value: Iterable<Byte>) { remove(value) }

@JvmName("minusAssignListOfList")
public operator fun NbtList<NbtList<NbtShort>>.minusAssign(value: Array<Short>) { remove(value) }

@JvmName("minusAssignListOfList")
public operator fun NbtList<NbtList<NbtShort>>.minusAssign(value: ShortArray) { remove(value) }

@JvmName("minusAssignListOfListIterShort")
public operator fun NbtList<NbtList<NbtShort>>.minusAssign(value: Iterable<Short>) { remove(value) }

@JvmName("minusAssignListOfList")
public operator fun NbtList<NbtList<NbtInt>>.minusAssign(value: Array<Int>) { remove(value) }

@JvmName("minusAssignListOfList")
public operator fun NbtList<NbtList<NbtInt>>.minusAssign(value: IntArray) { remove(value) }

@JvmName("minusAssignListOfListIterInt")
public operator fun NbtList<NbtList<NbtInt>>.minusAssign(value: Iterable<Int>) { remove(value) }

@JvmName("minusAssignListOfList")
public operator fun NbtList<NbtList<NbtLong>>.minusAssign(value: Array<Long>) { remove(value) }

@JvmName("minusAssignListOfList")
public operator fun NbtList<NbtList<NbtLong>>.minusAssign(value: LongArray) { remove(value) }

@JvmName("minusAssignListOfListIterLong")
public operator fun NbtList<NbtList<NbtLong>>.minusAssign(value: Iterable<Long>) { remove(value) }

@JvmName("minusAssignListOfList")
public operator fun NbtList<NbtList<NbtFloat>>.minusAssign(value: Array<Float>) { remove(value) }

@JvmName("minusAssignListOfList")
public operator fun NbtList<NbtList<NbtFloat>>.minusAssign(value: FloatArray) { remove(value) }

@JvmName("minusAssignListOfListIterFloat")
public operator fun NbtList<NbtList<NbtFloat>>.minusAssign(value: Iterable<Float>) { remove(value) }

@JvmName("minusAssignListOfList")
public operator fun NbtList<NbtList<NbtString>>.minusAssign(value: Array<String>) { remove(value) }

@JvmName("minusAssignListOfListIterString")
public operator fun NbtList<NbtList<NbtString>>.minusAssign(value: Iterable<String>) { remove(value) }

@JvmName("minusAssignListOfList")
public operator fun NbtList<NbtList<NbtByteArray>>.minusAssign(value: Array<ByteArray>) { remove(value) }

@JvmName("minusAssignListOfListIterByteArray")
public operator fun NbtList<NbtList<NbtByteArray>>.minusAssign(value: Iterable<ByteArray>) { remove(value) }

@JvmName("minusAssignListOfList")
public operator fun NbtList<NbtList<NbtIntArray>>.minusAssign(value: Array<IntArray>) { remove(value) }

@JvmName("minusAssignListOfListIterIntArray")
public operator fun NbtList<NbtList<NbtIntArray>>.minusAssign(value: Iterable<IntArray>) { remove(value) }

@JvmName("minusAssignListOfList")
public operator fun NbtList<NbtList<NbtLongArray>>.minusAssign(value: Array<LongArray>) { remove(value) }

@JvmName("minusAssignListOfListIterLongArray")
public operator fun NbtList<NbtList<NbtLongArray>>.minusAssign(value: Iterable<LongArray>) { remove(value) }

@JvmName("minusAssignListOfList")
public operator fun NbtList<NbtList<NbtCompound>>.minusAssign(value: Array<Map<String, NbtTag>>) { remove(value) }

@JvmName("minusAssignListOfListIterCompound")
public operator fun NbtList<NbtList<NbtCompound>>.minusAssign(value: Iterable<Map<String, NbtTag>>) { remove(value) }
