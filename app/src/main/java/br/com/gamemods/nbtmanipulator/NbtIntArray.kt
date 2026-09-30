package br.com.gamemods.nbtmanipulator

public data class NbtIntArray(var value: IntArray): NbtTag() {

    override val stringValue: String
        get() = value.takeIf { it.isNotEmpty() }?.joinToString(prefix = "[", postfix = "]") ?: "[]"

    public constructor(): this(intArrayOf())

    @Throws(IllegalArgumentException::class)
    public constructor(value: String): this(value
        .removeSurrounding("[", "]")
        .split(", ")
        .takeIf { it.size > 1 || it.firstOrNull()?.isNotEmpty() == true }
        ?.map { it.toInt() }
        ?.toIntArray()
        ?: intArrayOf()
    )

    override fun toTechnicalString(): String {
        return "NbtIntArray$stringValue"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NbtIntArray

        if (!value.contentEquals(other.value)) return false

        return true
    }

    override fun hashCode(): Int {
        return value.contentHashCode()
    }

    override fun deepCopy(): NbtIntArray = copy(value = value.copyOf())
}
