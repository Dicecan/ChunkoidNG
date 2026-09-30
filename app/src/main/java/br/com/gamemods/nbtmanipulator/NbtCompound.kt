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

    @Throws(ClassCastException::class)
    public fun getNullableByte(key: String): Byte? = this[key]?.let { it as NbtByte }?.signed

    @Throws(ClassCastException::class)
    public fun getNullableShort(key: String): Short? = this[key]?.let { it as NbtShort }?.value

    @Throws(ClassCastException::class)
    public fun getNullableInt(key: String): Int? = this[key]?.let { it as NbtInt }?.value

    @Throws(ClassCastException::class)
    public fun getNullableLong(key: String): Long? = this[key]?.let { it as NbtLong }?.value

    @Throws(ClassCastException::class)
    public fun getNullableFloat(key: String): Float? = this[key]?.let { it as NbtFloat }?.value

    @Throws(ClassCastException::class)
    public fun getNullableDouble(key: String): Double? = this[key]?.let { it as NbtDouble }?.value

    @Throws(ClassCastException::class)
    public fun getNullableByteArray(key: String): ByteArray? = this[key]?.let { it as NbtByteArray }?.value

    @Throws(ClassCastException::class)
    public fun getNullableString(key: String): String? = this[key]?.let { it as NbtString }?.value

    @Throws(ClassCastException::class)
    public fun getNullableIntArray(key: String): IntArray? = this[key]?.let { it as NbtIntArray }?.value

    @Throws(ClassCastException::class)
    public fun getNullableLongArray(key: String): LongArray? = this[key]?.let { it as NbtLongArray }?.value

    @Throws(ClassCastException::class)
    public fun getNullableCompound(key: String): NbtCompound? = this[key]?.let { it as NbtCompound }

    @Throws(ClassCastException::class)
    public fun getNullableList(key: String): NbtList<*>? = this[key]?.let { it as NbtList<*> }

    @Throws(ClassCastException::class, IllegalStateException::class)
    public fun getNullableByteList(key: String): NbtList<NbtByte>? = getNullableList(key)?.cast()

    @Throws(ClassCastException::class, IllegalStateException::class)
    public fun getNullableShortList(key: String): NbtList<NbtShort>? = getNullableList(key)?.cast()

    @Throws(ClassCastException::class, IllegalStateException::class)
    public fun getNullableIntList(key: String): NbtList<NbtInt>? = getNullableList(key)?.cast()

    @Throws(ClassCastException::class, IllegalStateException::class)
    public fun getNullableLongList(key: String): NbtList<NbtLong>? = getNullableList(key)?.cast()

    @Throws(ClassCastException::class, IllegalStateException::class)
    public fun getNullableFloatList(key: String): NbtList<NbtFloat>? = getNullableList(key)?.cast()

    @Throws(ClassCastException::class, IllegalStateException::class)
    public fun getNullableDoubleList(key: String): NbtList<NbtDouble>? = getNullableList(key)?.cast()

    @Throws(ClassCastException::class, IllegalStateException::class)
    public fun getNullableByteArrayList(key: String): NbtList<NbtByteArray>? = getNullableList(key)?.cast()

    @Throws(ClassCastException::class, IllegalStateException::class)
    public fun getNullableStringList(key: String): NbtList<NbtString>? = getNullableList(key)?.cast()

    @Throws(ClassCastException::class, IllegalStateException::class)
    public fun getNullableIntArrayList(key: String): NbtList<NbtIntArray>? = getNullableList(key)?.cast()

    @Throws(ClassCastException::class, IllegalStateException::class)
    public fun getNullableLongArrayList(key: String): NbtList<NbtLongArray>? = getNullableList(key)?.cast()

    @Throws(ClassCastException::class, IllegalStateException::class)
    public fun getNullableCompoundList(key: String): NbtList<NbtCompound>? = getNullableList(key)?.cast()

    @Throws(ClassCastException::class, IllegalStateException::class)
    public fun getNullableListOfList(key: String): NbtList<NbtList<*>>? = getNullableList(key)?.cast()

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
