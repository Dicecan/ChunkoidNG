package br.com.gamemods.nbtmanipulator

public class NbtCompound private constructor(private val value: LinkedHashMap<String, NbtTag>) : NbtTag(), MutableMap<String, NbtTag> by value {

    override val stringValue: String
        get() = value.takeIf { it.isNotEmpty() }?.entries?.joinToString(prefix = "{", postfix = "}") { (key, tag) ->
            '"' + key.replace("\\", "\\\\").replace("\"", "\\\"") + "\"=" + tag
        } ?: "{}"

    public constructor(value: Map<String, NbtTag>): this(LinkedHashMap(value))

    public constructor(): this(emptyMap())

    public constructor(vararg tags: Pair<String, NbtTag>): this(mapOf(*tags))

    public constructor(tags: Iterable<Pair<String, NbtTag>>): this(tags.toMap())

    @Throws(IllegalArgumentException::class)
    public constructor(value: String): this(NbtCompoundStringParser(value).parseCompound())

    public operator fun set(key: String, value: NbtTag) {
        put(key, value)
    }

    public operator fun set(key: String, value: Boolean): Unit = set(key, if (value) BYTE_TRUE else 0)

    public operator fun set(key: String, value: Byte): Unit = set(key, NbtByte(value))

    public operator fun set(key: String, value: Short): Unit = set(key, NbtShort(value))

    public operator fun set(key: String, value: Int): Unit = set(key, NbtInt(value))

    public operator fun set(key: String, value: Long): Unit = set(key, NbtLong(value))

    public operator fun set(key: String, value: Float): Unit = set(key, NbtFloat(value))

    public operator fun set(key: String, value: Double): Unit = set(key, NbtDouble(value))

    public operator fun set(key: String, value: ByteArray): Unit = set(key, NbtByteArray(value))

    public operator fun set(key: String, value: String): Unit = set(key, NbtString(value))

    public operator fun set(key: String, value: IntArray): Unit = set(key, NbtIntArray(value))

    public operator fun set(key: String, value: LongArray): Unit = set(key, NbtLongArray(value))

    @Throws(ClassCastException::class)
    public fun getNullableBooleanByte(key: String): Boolean = getNullableByte(key) == BYTE_TRUE

    @Throws(ClassCastException::class, NoSuchElementException::class)
    public fun getBooleanByte(key: String): Boolean = getByte(key) == BYTE_TRUE

    @Throws(NoSuchElementException::class)
    public fun require(key: String): NbtTag = get(key) ?: throw NoSuchElementException(key)

    @Throws(ClassCastException::class, NoSuchElementException::class)
    public fun getByte(key: String): Byte = (require(key) as NbtByte).signed

    @Throws(ClassCastException::class, NoSuchElementException::class)
    public fun getShort(key: String): Short = (require(key) as NbtShort).value

    @Throws(ClassCastException::class, NoSuchElementException::class)
    public fun getInt(key: String): Int = (require(key) as NbtInt).value

    @Throws(ClassCastException::class, NoSuchElementException::class)
    public fun getLong(key: String): Long = (require(key) as NbtLong).value

    @Throws(ClassCastException::class, NoSuchElementException::class)
    public fun getFloat(key: String): Float = (require(key) as NbtFloat).value

    @Throws(ClassCastException::class, NoSuchElementException::class)
    public fun getDouble(key: String): Double = (require(key) as NbtDouble).value

    @Throws(ClassCastException::class, NoSuchElementException::class)
    public fun getByteArray(key: String): ByteArray = (require(key) as NbtByteArray).value

    @Throws(ClassCastException::class, NoSuchElementException::class)
    public fun getString(key: String): String = (require(key) as NbtString).value

    @Throws(ClassCastException::class, NoSuchElementException::class)
    public fun getIntArray(key: String): IntArray = (require(key) as NbtIntArray).value

    @Throws(ClassCastException::class, NoSuchElementException::class)
    public fun getLongArray(key: String): LongArray = (require(key) as NbtLongArray).value

    @Throws(ClassCastException::class, NoSuchElementException::class)
    public fun getCompound(key: String): NbtCompound = require(key) as NbtCompound

    @Throws(ClassCastException::class, NoSuchElementException::class)
    public fun getList(key: String): NbtList<*> = require(key) as NbtList<*>

    @Throws(ClassCastException::class, NoSuchElementException::class, IllegalStateException::class)
    public fun getByteList(key: String): NbtList<NbtByte> = getList(key).cast()

    @Throws(ClassCastException::class, NoSuchElementException::class, IllegalStateException::class)
    public fun getShortList(key: String): NbtList<NbtShort> = getList(key).cast()

    @Throws(ClassCastException::class, NoSuchElementException::class, IllegalStateException::class)
    public fun getIntList(key: String): NbtList<NbtInt> = getList(key).cast()

    @Throws(ClassCastException::class, NoSuchElementException::class, IllegalStateException::class)
    public fun getLongList(key: String): NbtList<NbtLong> = getList(key).cast()

    @Throws(ClassCastException::class, NoSuchElementException::class, IllegalStateException::class)
    public fun getFloatList(key: String): NbtList<NbtFloat> = getList(key).cast()

    @Throws(ClassCastException::class, NoSuchElementException::class, IllegalStateException::class)
    public fun getDoubleList(key: String): NbtList<NbtDouble> = getList(key).cast()

    @Throws(ClassCastException::class, NoSuchElementException::class, IllegalStateException::class)
    public fun getByteArrayList(key: String): NbtList<NbtByteArray> = getList(key).cast()

    @Throws(ClassCastException::class, NoSuchElementException::class, IllegalStateException::class)
    public fun getStringList(key: String): NbtList<NbtString> = getList(key).cast()

    @Throws(ClassCastException::class, NoSuchElementException::class, IllegalStateException::class)
    public fun getIntArrayList(key: String): NbtList<NbtIntArray> = getList(key).cast()

    @Throws(ClassCastException::class, NoSuchElementException::class, IllegalStateException::class)
    public fun getLongArrayList(key: String): NbtList<NbtLongArray> = getList(key).cast()

    @Throws(ClassCastException::class, NoSuchElementException::class, IllegalStateException::class)
    public fun getCompoundList(key: String): NbtList<NbtCompound> = getList(key).cast()

    @Throws(ClassCastException::class, NoSuchElementException::class, IllegalStateException::class)
    public fun getListOfList(key: String): NbtList<NbtList<*>> = getList(key).cast()

    public fun getNullableByte(key: String): Byte? = when (val t = this[key]) {
        is NbtByte -> t.signed
        is NbtShort -> t.value.toByte()
        is NbtInt -> t.value.toByte()
        is NbtLong -> t.value.toByte()
        else -> null
    }

    public fun getNullableShort(key: String): Short? = when (val t = this[key]) {
        is NbtShort -> t.value
        is NbtByte -> t.signed.toShort()
        is NbtInt -> t.value.toShort()
        is NbtLong -> t.value.toShort()
        else -> null
    }

    public fun getNullableInt(key: String): Int? = when (val t = this[key]) {
        is NbtInt -> t.value
        is NbtByte -> t.signed.toInt()
        is NbtShort -> t.value.toInt()
        is NbtLong -> t.value.toInt()
        else -> null
    }

    public fun getNullableLong(key: String): Long? = when (val t = this[key]) {
        is NbtLong -> t.value
        is NbtInt -> t.value.toLong()
        is NbtShort -> t.value.toLong()
        is NbtByte -> t.signed.toLong()
        else -> null
    }

    public fun getNullableFloat(key: String): Float? = when (val t = this[key]) {
        is NbtFloat -> t.value
        is NbtDouble -> t.value.toFloat()
        is NbtInt -> t.value.toFloat()
        is NbtShort -> t.value.toFloat()
        is NbtByte -> t.signed.toFloat()
        else -> null
    }

    public fun getNullableDouble(key: String): Double? = when (val t = this[key]) {
        is NbtDouble -> t.value
        is NbtFloat -> t.value.toDouble()
        is NbtInt -> t.value.toDouble()
        is NbtLong -> t.value.toDouble()
        else -> null
    }

    public fun getNullableByteArray(key: String): ByteArray? = (this[key] as? NbtByteArray)?.value

    public fun getNullableString(key: String): String? = (this[key] as? NbtString)?.value

    public fun getNullableIntArray(key: String): IntArray? = (this[key] as? NbtIntArray)?.value

    public fun getNullableLongArray(key: String): LongArray? = (this[key] as? NbtLongArray)?.value

    public fun getNullableCompound(key: String): NbtCompound? = this[key] as? NbtCompound

    public fun getNullableList(key: String): NbtList<*>? = this[key] as? NbtList<*>

    public fun getNullableByteList(key: String): NbtList<NbtByte>? = try { getNullableList(key)?.cast() } catch (_: Exception) { null }

    public fun getNullableShortList(key: String): NbtList<NbtShort>? = try { getNullableList(key)?.cast() } catch (_: Exception) { null }

    public fun getNullableIntList(key: String): NbtList<NbtInt>? = try { getNullableList(key)?.cast() } catch (_: Exception) { null }

    public fun getNullableLongList(key: String): NbtList<NbtLong>? = try { getNullableList(key)?.cast() } catch (_: Exception) { null }

    public fun getNullableFloatList(key: String): NbtList<NbtFloat>? = try { getNullableList(key)?.cast() } catch (_: Exception) { null }

    public fun getNullableDoubleList(key: String): NbtList<NbtDouble>? = try { getNullableList(key)?.cast() } catch (_: Exception) { null }

    public fun getNullableByteArrayList(key: String): NbtList<NbtByteArray>? = try { getNullableList(key)?.cast() } catch (_: Exception) { null }

    public fun getNullableStringList(key: String): NbtList<NbtString>? = try { getNullableList(key)?.cast() } catch (_: Exception) { null }

    public fun getNullableIntArrayList(key: String): NbtList<NbtIntArray>? = try { getNullableList(key)?.cast() } catch (_: Exception) { null }

    public fun getNullableLongArrayList(key: String): NbtList<NbtLongArray>? = try { getNullableList(key)?.cast() } catch (_: Exception) { null }

    public fun getNullableCompoundList(key: String): NbtList<NbtCompound>? = try { getNullableList(key)?.cast() } catch (_: Exception) { null }

    public fun getNullableListOfList(key: String): NbtList<NbtList<*>>? = try { getNullableList(key)?.cast() } catch (_: Exception) { null }

    public fun copyFrom(other: NbtCompound, tagKey: String, default: NbtTag? = null) {
        val tag = other[tagKey] ?: default
        if (tag != null) {
            this[tagKey] = tag
        }
    }

    public fun copyTo(other: NbtCompound, tagKey: String, default: NbtTag? = null) {
        val tag = this[tagKey] ?: default
        if (tag != null) {
            other[tagKey] = tag
        }
    }

    override fun deepCopy(): NbtCompound = NbtCompound(mapValues { it.value.deepCopy() })

    override fun toTechnicalString(): String {
        if (value.isEmpty()) {
            return "NbtCompound{}"
        }
        return "NbtCompound$stringValue"

    }

    override fun equals(other: Any?): Boolean {
        return value == other
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

}
