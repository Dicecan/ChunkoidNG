@file:JvmName("NbtUtil")
@file:JvmMultifileClass

package br.com.gamemods.nbtmanipulator

public operator fun NbtList<NbtByte>.plusAssign(value: Byte) { add(value) }

public operator fun NbtList<NbtShort>.plusAssign(value: Short) { add(value) }

public operator fun NbtList<NbtInt>.plusAssign(value: Int) { add(value) }

public operator fun NbtList<NbtLong>.plusAssign(value: Long) { add(value) }

public operator fun NbtList<NbtFloat>.plusAssign(value: Float) { add(value) }

public operator fun NbtList<NbtDouble>.plusAssign(value: Double) { add(value) }

public operator fun NbtList<NbtString>.plusAssign(value: String) { add(value) }

public operator fun NbtList<NbtByteArray>.plusAssign(value: ByteArray) { add(value) }

public operator fun NbtList<NbtByteArray>.plusAssign(value: Array<Byte>) { add(value) }

public operator fun NbtList<NbtIntArray>.plusAssign(value: IntArray) { add(value) }

public operator fun NbtList<NbtIntArray>.plusAssign(value: Array<Int>) { add(value) }

public operator fun NbtList<NbtLongArray>.plusAssign(value: LongArray) { add(value) }

public operator fun NbtList<NbtLongArray>.plusAssign(value: Array<Long>) { add(value) }

public operator fun NbtList<NbtCompound>.plusAssign(value: Map<String, NbtTag>) { add(value) }

public operator fun <T: NbtTag> NbtList<NbtList<T>>.plusAssign(value: Iterable<T>) { add(value) }

public operator fun <T: NbtTag> NbtList<NbtList<T>>.plusAssign(value: Array<T>) { add(value) }

@JvmName("plusAssignListOfList")
public operator fun NbtList<NbtList<NbtByte>>.plusAssign(value: Array<Byte>) { add(value) }

@JvmName("plusAssignListOfList")
public operator fun NbtList<NbtList<NbtByte>>.plusAssign(value: ByteArray) { add(value) }

@JvmName("plusAssignListOfListIterByte")
public operator fun NbtList<NbtList<NbtByte>>.plusAssign(value: Iterable<Byte>) { add(value) }

@JvmName("plusAssignListOfList")
public operator fun NbtList<NbtList<NbtShort>>.plusAssign(value: Array<Short>) { add(value) }

@JvmName("plusAssignListOfList")
public operator fun NbtList<NbtList<NbtShort>>.plusAssign(value: ShortArray) { add(value) }

@JvmName("plusAssignListOfListIterShort")
public operator fun NbtList<NbtList<NbtShort>>.plusAssign(value: Iterable<Short>) { add(value) }

@JvmName("plusAssignListOfList")
public operator fun NbtList<NbtList<NbtInt>>.plusAssign(value: Array<Int>) { add(value) }

@JvmName("plusAssignListOfList")
public operator fun NbtList<NbtList<NbtInt>>.plusAssign(value: IntArray) { add(value) }

@JvmName("plusAssignListOfListIterInt")
public operator fun NbtList<NbtList<NbtInt>>.plusAssign(value: Iterable<Int>) { add(value) }

@JvmName("plusAssignListOfList")
public operator fun NbtList<NbtList<NbtLong>>.plusAssign(value: Array<Long>) { add(value) }

@JvmName("plusAssignListOfList")
public operator fun NbtList<NbtList<NbtLong>>.plusAssign(value: LongArray) { add(value) }

@JvmName("plusAssignListOfListIterLong")
public operator fun NbtList<NbtList<NbtLong>>.plusAssign(value: Iterable<Long>) { add(value) }

@JvmName("plusAssignListOfList")
public operator fun NbtList<NbtList<NbtFloat>>.plusAssign(value: Array<Float>) { add(value) }

@JvmName("plusAssignListOfList")
public operator fun NbtList<NbtList<NbtFloat>>.plusAssign(value: FloatArray) { add(value) }

@JvmName("plusAssignListOfListIterFloat")
public operator fun NbtList<NbtList<NbtFloat>>.plusAssign(value: Iterable<Float>) { add(value) }

@JvmName("plusAssignListOfList")
public operator fun NbtList<NbtList<NbtString>>.plusAssign(value: Array<String>) { add(value) }

@JvmName("plusAssignListOfListIterString")
public operator fun NbtList<NbtList<NbtString>>.plusAssign(value: Iterable<String>) { add(value) }

@JvmName("plusAssignListOfList")
public operator fun NbtList<NbtList<NbtByteArray>>.plusAssign(value: Array<ByteArray>) { add(value) }

@JvmName("plusAssignListOfListIterByteArray")
public operator fun NbtList<NbtList<NbtByteArray>>.plusAssign(value: Iterable<ByteArray>) { add(value) }

@JvmName("plusAssignListOfList")
public operator fun NbtList<NbtList<NbtIntArray>>.plusAssign(value: Array<IntArray>) { add(value) }

@JvmName("plusAssignListOfListIterIntArray")
public operator fun NbtList<NbtList<NbtIntArray>>.plusAssign(value: Iterable<IntArray>) { add(value) }

@JvmName("plusAssignListOfList")
public operator fun NbtList<NbtList<NbtLongArray>>.plusAssign(value: Array<LongArray>) { add(value) }

@JvmName("plusAssignListOfListIterLongArray")
public operator fun NbtList<NbtList<NbtLongArray>>.plusAssign(value: Iterable<LongArray>) { add(value) }

@JvmName("plusAssignListOfList")
public operator fun NbtList<NbtList<NbtCompound>>.plusAssign(value: Array<Map<String, NbtTag>>) { add(value) }

@JvmName("plusAssignListOfListIterCompound")
public operator fun NbtList<NbtList<NbtCompound>>.plusAssign(value: Iterable<Map<String, NbtTag>>) { add(value) }
