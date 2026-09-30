package br.com.gamemods.nbtmanipulator

public class NbtList<T: NbtTag> private constructor(private val tags: ArrayList<T>): NbtTag(), MutableList<T> by tags, RandomAccess {

    override val stringValue: String
        get() = tags.toString()

    public constructor(tags: Collection<T>): this(ArrayList(tags))

    public constructor(): this(emptyList())

    public constructor(vararg tags: T): this(tags.toList())

    public constructor(tags: Iterable<T>): this(tags.toList())

    public constructor(tags: Sequence<T>): this(tags.toList())

    public constructor(tags: NbtList<T>): this(tags as Collection<T>)

    @Suppress("UNCHECKED_CAST")
    @Throws(IllegalArgumentException::class)
    public constructor(value: String): this(NbtListStringParser(value).parseList() as ArrayList<T>)

    override fun add(element: T): Boolean {
        checkTagType(element)
        return tags.add(element)
    }

    override fun add(index: Int, element: T) {
        checkTagType(element)
        return tags.add(index, element)
    }

    override fun set(index: Int, element: T): T {
        if (size > 1) {
            checkTagType(element)
        }
        return tags.set(index, element)
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> {
        val subList = tags.subList(fromIndex, toIndex)
        return object : MutableList<T> by subList, RandomAccess {
            override fun set(index: Int, element: T): T {
                checkTagType(element)
                return subList.set(index, element)
            }

            override fun toString(): String {
                return tags.toString()
            }

            override fun equals(other: Any?): Boolean {
                return subList == other
            }

            override fun hashCode(): Int {
                return subList.hashCode()
            }
        }
    }

    private fun checkTagType(tag: NbtTag) {
        val childrenType = firstOrNull()?.javaClass ?: return
        require(childrenType == tag.javaClass) {
            "NbtList must have all children tags of the same type. \n" +
                    "Tried to add a ${tag.javaClass.simpleName} tag in a NbtList of ${childrenType.javaClass.simpleName}"
        }
    }

    override fun deepCopy(): NbtList<T> = NbtList(map {
        @Suppress("UNCHECKED_CAST")
        it.deepCopy() as T
    })

    override fun toTechnicalString(): String {
        if (tags.isEmpty()) {
            return "NbtList[]"
        }
        return tags.joinToString(prefix = "NbtList[", postfix = "]")
    }

    override fun equals(other: Any?): Boolean {
        return tags == other
    }

    override fun hashCode(): Int {
        return tags.hashCode()
    }

    public companion object {

        @JvmStatic
        public fun create(vararg tags: Byte): NbtList<NbtByte> = tags.toNbtList()

        @JvmStatic
        public fun create(vararg tags: Short): NbtList<NbtShort> = tags.toNbtList()

        @JvmStatic
        public fun create(vararg tags: Int): NbtList<NbtInt> = tags.toNbtList()

        @JvmStatic
        public fun create(vararg tags: Long): NbtList<NbtLong> = tags.toNbtList()

        @JvmStatic
        public fun create(vararg tags: Float): NbtList<NbtFloat> = tags.toNbtList()

        @JvmStatic
        public fun create(vararg tags: Double): NbtList<NbtDouble> = tags.toNbtList()

        @JvmStatic
        public fun create(vararg tags: String): NbtList<NbtString> = tags.toNbtList()

        @JvmStatic
        public fun create(vararg tags: ByteArray): NbtList<NbtByteArray> = tags.toNbtList()

        @JvmStatic
        public fun create(vararg tags: IntArray): NbtList<NbtIntArray> = tags.toNbtList()

        @JvmStatic
        public fun create(vararg tags: LongArray): NbtList<NbtLongArray> = tags.toNbtList()

        @JvmStatic
        public fun create(vararg tags: Map<String, NbtTag>): NbtList<NbtCompound> = tags.toNbtList()

        @JvmStatic
        public fun create(vararg tags: Iterable<NbtTag>): NbtList<NbtList<NbtTag>> = tags.toNbtList()

        @JvmStatic
        public fun createByteSublist(vararg tags: Iterable<Byte>): NbtList<NbtList<NbtByte>> = tags.toNbtList()

        @JvmStatic
        public fun createByteSublist(vararg tags: Array<Byte>): NbtList<NbtList<NbtByte>> = tags.toNbtList()

        @JvmStatic
        public fun createByteSublist(vararg tags: ByteArray): NbtList<NbtList<NbtByte>> = tags.map { it.asIterable() }.toNbtList()

        @JvmStatic
        public fun createShortSublist(vararg tags: Iterable<Short>): NbtList<NbtList<NbtShort>> = tags.toNbtList()

        @JvmStatic
        public fun createShortSublist(vararg tags: Array<Short>): NbtList<NbtList<NbtShort>> = tags.toNbtList()

        @JvmStatic
        public fun createShortSublist(vararg tags: ShortArray): NbtList<NbtList<NbtShort>> = tags.map { it.asIterable() }.toNbtList()

        @JvmStatic
        public fun createIntSublist(vararg tags: Iterable<Int>): NbtList<NbtList<NbtInt>> = tags.toNbtList()

        @JvmStatic
        public fun createIntSublist(vararg tags: Array<Int>): NbtList<NbtList<NbtInt>> = tags.toNbtList()

        @JvmStatic
        public fun createIntSublist(vararg tags: IntArray): NbtList<NbtList<NbtInt>> = tags.map { it.asIterable() }.toNbtList()

        @JvmStatic
        public fun createFloatSublist(vararg tags: Iterable<Float>): NbtList<NbtList<NbtFloat>> = tags.toNbtList()

        @JvmStatic
        public fun createFloatSublist(vararg tags: Array<Float>): NbtList<NbtList<NbtFloat>> = tags.toNbtList()

        @JvmStatic
        public fun createFloatSublist(vararg tags: FloatArray): NbtList<NbtList<NbtFloat>> = tags.map { it.asIterable() }.toNbtList()

        @JvmStatic
        public fun createDoubleSublist(vararg tags: Iterable<Double>): NbtList<NbtList<NbtDouble>> = tags.toNbtList()

        @JvmStatic
        public fun createDoubleSublist(vararg tags: Array<Double>): NbtList<NbtList<NbtDouble>> = tags.toNbtList()

        @JvmStatic
        public fun createDoubleSublist(vararg tags: DoubleArray): NbtList<NbtList<NbtDouble>> = tags.map { it.asIterable() }.toNbtList()

        @JvmStatic
        public fun createStringSublist(vararg tags: Iterable<String>): NbtList<NbtList<NbtString>> = tags.toNbtList()

        @JvmStatic
        public fun createStringSublist(vararg tags: Array<String>): NbtList<NbtList<NbtString>> = tags.toNbtList()

        @JvmStatic
        public fun createCompoundSublist(vararg tags: Iterable<Map<String, NbtTag>>): NbtList<NbtList<NbtCompound>> = tags.toNbtList()

        @JvmStatic
        public fun createCompoundSublist(vararg tags: Array<Map<String, NbtTag>>): NbtList<NbtList<NbtCompound>> = tags.toNbtList()

        @JvmStatic
        public fun createSublist(vararg tags: Iterable<Iterable<NbtTag>>): NbtList<NbtList<NbtList<NbtTag>>> = tags.toNbtList()
    }
}
