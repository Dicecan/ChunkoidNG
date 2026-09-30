@file:JvmName("NbtUtil")
@file:JvmMultifileClass

package br.com.gamemods.nbtmanipulator

public operator fun NbtList<NbtByte>.contains(value: Byte): Boolean = contains(NbtByte(value))

public operator fun NbtList<NbtShort>.contains(value: Short): Boolean = contains(NbtShort(value))

public operator fun NbtList<NbtInt>.contains(value: Int): Boolean = contains(NbtInt(value))

public operator fun NbtList<NbtLong>.contains(value: Long): Boolean = contains(NbtLong(value))

public operator fun NbtList<NbtFloat>.contains(value: Float): Boolean = contains(NbtFloat(value))

public operator fun NbtList<NbtDouble>.contains(value: Double): Boolean = contains(NbtDouble(value))

public operator fun NbtList<NbtString>.contains(value: String): Boolean = contains(NbtString(value))

public operator fun NbtList<NbtByteArray>.contains(value: ByteArray): Boolean = contains(NbtByteArray(value))

public operator fun NbtList<NbtByteArray>.contains(value: Array<Byte>): Boolean = contains(NbtByteArray(value.toByteArray()))

public operator fun NbtList<NbtIntArray>.contains(value: IntArray): Boolean = contains(NbtIntArray(value))

public operator fun NbtList<NbtIntArray>.contains(value: Array<Int>): Boolean = contains(NbtIntArray(value.toIntArray()))

public operator fun NbtList<NbtLongArray>.contains(value: LongArray): Boolean = contains(NbtLongArray(value))

public operator fun NbtList<NbtLongArray>.contains(value: Array<Long>): Boolean = contains(NbtLongArray(value.toLongArray()))

public operator fun NbtList<NbtCompound>.contains(value: Map<String, NbtTag>): Boolean = contains(NbtCompound(value))

public operator fun <T: NbtTag> NbtList<NbtList<T>>.contains(value: Iterable<T>): Boolean = contains(NbtList(value))

public operator fun <T: NbtTag> NbtList<NbtList<T>>.contains(value: Array<T>): Boolean = contains(NbtList(*value))

@JvmName("containsListOfList")
public operator fun NbtList<NbtList<NbtByte>>.contains(value: Array<Byte>): Boolean = contains(value.toNbtList())

@JvmName("containsListOfList")
public operator fun NbtList<NbtList<NbtByte>>.contains(value: ByteArray): Boolean = contains(value.toNbtList())

@JvmName("containsListOfListIterByte")
public operator fun NbtList<NbtList<NbtByte>>.contains(value: Iterable<Byte>): Boolean = contains(value.toNbtList())

@JvmName("containsListOfList")
public operator fun NbtList<NbtList<NbtShort>>.contains(value: Array<Short>): Boolean = contains(value.toNbtList())

@JvmName("containsListOfList")
public operator fun NbtList<NbtList<NbtShort>>.contains(value: ShortArray): Boolean = contains(value.toNbtList())

@JvmName("containsListOfListIterShort")
public operator fun NbtList<NbtList<NbtShort>>.contains(value: Iterable<Short>): Boolean = contains(value.toNbtList())

@JvmName("containsListOfList")
public operator fun NbtList<NbtList<NbtInt>>.contains(value: Array<Int>): Boolean = contains(value.toNbtList())

@JvmName("containsListOfList")
public operator fun NbtList<NbtList<NbtInt>>.contains(value: IntArray): Boolean = contains(value.toNbtList())

@JvmName("containsListOfListIterInt")
public operator fun NbtList<NbtList<NbtInt>>.contains(value: Iterable<Int>): Boolean = contains(value.toNbtList())

@JvmName("containsListOfList")
public operator fun NbtList<NbtList<NbtLong>>.contains(value: Array<Long>): Boolean = contains(value.toNbtList())

@JvmName("containsListOfList")
public operator fun NbtList<NbtList<NbtLong>>.contains(value: LongArray): Boolean = contains(value.toNbtList())

@JvmName("containsListOfListIterLong")
public operator fun NbtList<NbtList<NbtLong>>.contains(value: Iterable<Long>): Boolean = contains(value.toNbtList())

@JvmName("containsListOfList")
public operator fun NbtList<NbtList<NbtFloat>>.contains(value: Array<Float>): Boolean = contains(value.toNbtList())

@JvmName("containsListOfList")
public operator fun NbtList<NbtList<NbtFloat>>.contains(value: FloatArray): Boolean = contains(value.toNbtList())

@JvmName("containsListOfListIterFloat")
public operator fun NbtList<NbtList<NbtFloat>>.contains(value: Iterable<Float>): Boolean = contains(value.toNbtList())

@JvmName("containsListOfList")
public operator fun NbtList<NbtList<NbtString>>.contains(value: Array<String>): Boolean = contains(value.toNbtList())

@JvmName("containsListOfListIterString")
public operator fun NbtList<NbtList<NbtString>>.contains(value: Iterable<String>): Boolean = contains(value.toNbtList())

@JvmName("containsListOfList")
public operator fun NbtList<NbtList<NbtByteArray>>.contains(value: Array<ByteArray>): Boolean = contains(value.toNbtList())

@JvmName("containsListOfListIterByteArray")
public operator fun NbtList<NbtList<NbtByteArray>>.contains(value: Iterable<ByteArray>): Boolean = contains(value.toNbtList())

@JvmName("containsListOfList")
public operator fun NbtList<NbtList<NbtIntArray>>.contains(value: Array<IntArray>): Boolean = contains(value.toNbtList())

@JvmName("containsListOfListIterIntArray")
public operator fun NbtList<NbtList<NbtIntArray>>.contains(value: Iterable<IntArray>): Boolean = contains(value.toNbtList())

@JvmName("containsListOfList")
public operator fun NbtList<NbtList<NbtLongArray>>.contains(value: Array<LongArray>): Boolean = contains(value.toNbtList())

@JvmName("containsListOfListIterLongArray")
public operator fun NbtList<NbtList<NbtLongArray>>.contains(value: Iterable<LongArray>): Boolean = contains(value.toNbtList())

@JvmName("containsListOfList")
public operator fun NbtList<NbtList<NbtCompound>>.contains(value: Array<Map<String, NbtTag>>): Boolean = contains(value.toNbtList())

@JvmName("containsListOfListIterCompound")
public operator fun NbtList<NbtList<NbtCompound>>.contains(value: Iterable<Map<String, NbtTag>>): Boolean = add(value.toNbtList())
