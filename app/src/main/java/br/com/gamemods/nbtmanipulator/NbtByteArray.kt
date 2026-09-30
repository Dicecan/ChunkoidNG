package br.com.gamemods.nbtmanipulator

public data class NbtByteArray(var value: ByteArray): NbtTag() {

    override val stringValue: String
        get() = value.takeIf { it.isNotEmpty() }?.joinToString(prefix = "[", postfix = "]") ?: "[]"

    public constructor(): this(byteArrayOf())

    @Throws(IllegalArgumentException::class)
    public constructor(value: String): this(value
        .removeSurrounding("[", "]")
        .split(", ")
        .takeIf { it.size > 1 || it.firstOrNull()?.isNotEmpty() == true }
        ?.map { it.toByte() }
        ?.toByteArray()
        ?: byteArrayOf()
    )

    override fun toTechnicalString(): String {
        return "NbtByteArray$stringValue"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NbtByteArray

        if (!value.contentEquals(other.value)) return false

        return true
    }

    override fun hashCode(): Int {
        return value.contentHashCode()
    }

    override fun deepCopy(): NbtByteArray = copy(value = value.copyOf())
}
