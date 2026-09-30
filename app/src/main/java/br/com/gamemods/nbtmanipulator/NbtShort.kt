package br.com.gamemods.nbtmanipulator

public data class NbtShort(var value: Short) : NbtTag() {

    override val stringValue: String
        get() = value.toString()

    @Throws(NumberFormatException::class)
    public constructor(signed: String): this(signed.toShort())

    override fun deepCopy(): NbtShort = copy()
}
