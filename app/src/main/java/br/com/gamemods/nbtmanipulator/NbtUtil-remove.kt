@file:JvmName("NbtUtil")
@file:JvmMultifileClass

package br.com.gamemods.nbtmanipulator

public fun NbtList<NbtByte>.remove(value: Byte): Boolean = remove(NbtByte(value))

public fun NbtList<NbtShort>.remove(value: Short): Boolean = remove(NbtShort(value))

public fun NbtList<NbtInt>.remove(value: Int): Boolean = remove(NbtInt(value))

public fun NbtList<NbtLong>.remove(value: Long): Boolean = remove(NbtLong(value))

public fun NbtList<NbtFloat>.remove(value: Float): Boolean = remove(NbtFloat(value))

public fun NbtList<NbtDouble>.remove(value: Double): Boolean = remove(NbtDouble(value))

public fun NbtList<NbtString>.remove(value: String): Boolean = remove(NbtString(value))

public fun NbtList<NbtByteArray>.remove(value: ByteArray): Boolean = remove(NbtByteArray(value))

public fun NbtList<NbtByteArray>.remove(value: Array<Byte>): Boolean = remove(NbtByteArray(value.toByteArray()))

public fun NbtList<NbtIntArray>.remove(value: IntArray): Boolean = remove(NbtIntArray(value))

public fun NbtList<NbtIntArray>.remove(value: Array<Int>): Boolean = remove(NbtIntArray(value.toIntArray()))

public fun NbtList<NbtLongArray>.remove(value: LongArray): Boolean = remove(NbtLongArray(value))

public fun NbtList<NbtLongArray>.remove(value: Array<Long>): Boolean = remove(NbtLongArray(value.toLongArray()))

public fun NbtList<NbtCompound>.remove(value: Map<String, NbtTag>): Boolean = remove(NbtCompound(value))

public fun <T: NbtTag> NbtList<NbtList<T>>.remove(value: Iterable<T>): Boolean = remove(NbtList(value))

public fun <T: NbtTag> NbtList<NbtList<T>>.remove(value: Array<T>): Boolean = remove(NbtList(*value))

@JvmName("removeListOfList")
public fun NbtList<NbtList<NbtByte>>.remove(value: Array<Byte>): Boolean = remove(value.toNbtList())

@JvmName("removeListOfList")
public fun NbtList<NbtList<NbtByte>>.remove(value: ByteArray): Boolean = remove(value.toNbtList())

@JvmName("removeListOfListIterByte")
public fun NbtList<NbtList<NbtByte>>.remove(value: Iterable<Byte>): Boolean = remove(value.toNbtList())

@JvmName("removeListOfList")
public fun NbtList<NbtList<NbtShort>>.remove(value: Array<Short>): Boolean = remove(value.toNbtList())

@JvmName("removeListOfList")
public fun NbtList<NbtList<NbtShort>>.remove(value: ShortArray): Boolean = remove(value.toNbtList())

@JvmName("removeListOfListIterShort")
public fun NbtList<NbtList<NbtShort>>.remove(value: Iterable<Short>): Boolean = remove(value.toNbtList())

@JvmName("removeListOfList")
public fun NbtList<NbtList<NbtInt>>.remove(value: Array<Int>): Boolean = remove(value.toNbtList())

@JvmName("removeListOfList")
public fun NbtList<NbtList<NbtInt>>.remove(value: IntArray): Boolean = remove(value.toNbtList())

@JvmName("removeListOfListIterInt")
public fun NbtList<NbtList<NbtInt>>.remove(value: Iterable<Int>): Boolean = remove(value.toNbtList())

@JvmName("removeListOfList")
public fun NbtList<NbtList<NbtLong>>.remove(value: Array<Long>): Boolean = remove(value.toNbtList())

@JvmName("removeListOfList")
public fun NbtList<NbtList<NbtLong>>.remove(value: LongArray): Boolean = remove(value.toNbtList())

@JvmName("removeListOfListIterLong")
public fun NbtList<NbtList<NbtLong>>.remove(value: Iterable<Long>): Boolean = remove(value.toNbtList())

@JvmName("removeListOfList")
public fun NbtList<NbtList<NbtFloat>>.remove(value: Array<Float>): Boolean = remove(value.toNbtList())

@JvmName("removeListOfList")
public fun NbtList<NbtList<NbtFloat>>.remove(value: FloatArray): Boolean = remove(value.toNbtList())

@JvmName("removeListOfListIterFloat")
public fun NbtList<NbtList<NbtFloat>>.remove(value: Iterable<Float>): Boolean = remove(value.toNbtList())

@JvmName("removeListOfList")
public fun NbtList<NbtList<NbtString>>.remove(value: Array<String>): Boolean = remove(value.toNbtList())

@JvmName("removeListOfListIterString")
public fun NbtList<NbtList<NbtString>>.remove(value: Iterable<String>): Boolean = remove(value.toNbtList())

@JvmName("removeListOfList")
public fun NbtList<NbtList<NbtByteArray>>.remove(value: Array<ByteArray>): Boolean = remove(value.toNbtList())

@JvmName("removeListOfListIterByteArray")
public fun NbtList<NbtList<NbtByteArray>>.remove(value: Iterable<ByteArray>): Boolean = remove(value.toNbtList())

@JvmName("removeListOfList")
public fun NbtList<NbtList<NbtIntArray>>.remove(value: Array<IntArray>): Boolean = remove(value.toNbtList())

@JvmName("removeListOfListIterIntArray")
public fun NbtList<NbtList<NbtIntArray>>.remove(value: Iterable<IntArray>): Boolean = remove(value.toNbtList())

@JvmName("removeListOfList")
public fun NbtList<NbtList<NbtLongArray>>.remove(value: Array<LongArray>): Boolean = remove(value.toNbtList())

@JvmName("removeListOfListIterLongArray")
public fun NbtList<NbtList<NbtLongArray>>.remove(value: Iterable<LongArray>): Boolean = remove(value.toNbtList())

@JvmName("removeListOfList")
public fun NbtList<NbtList<NbtCompound>>.remove(value: Array<Map<String, NbtTag>>): Boolean = remove(value.toNbtList())

@JvmName("removeListOfListIterCompound")
public fun NbtList<NbtList<NbtCompound>>.remove(value: Iterable<Map<String, NbtTag>>): Boolean = add(value.toNbtList())
