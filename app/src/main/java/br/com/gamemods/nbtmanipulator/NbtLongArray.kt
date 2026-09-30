package br.com.gamemods.nbtmanipulator

public data class NbtLongArray(var value: LongArray) : NbtTag() {

    override val stringValue: String
        get() = value.takeIf { it.isNotEmpty() }?.joinToString(prefix = "[", postfix = "]") ?: "[]"

    public constructor(): this(longArrayOf())

    @Throws(IllegalArgumentException::class)
    public constructor(value: String): this(value
        .removeSurrounding("[", "]")
        .split(", ")
        .takeIf { it.size > 1 || it.firstOrNull()?.isNotEmpty() == true }
        ?.map { it.toLong() }
        ?.toLongArray()
        ?: longArrayOf()
    )

    override fun toTechnicalString(): String {
        return "NbtLongArray$stringValue"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NbtLongArray

        if (!value.contentEquals(other.value)) return false

        return true
    }

    override fun hashCode(): Int {
        return value.contentHashCode()
    }

    override fun deepCopy(): NbtLongArray = copy(value = value.copyOf())
}
