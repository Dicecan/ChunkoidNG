package br.com.gamemods.nbtmanipulator

public data class NbtDouble(var value: Double) : NbtTag() {

    override val stringValue: String
        get() = value.toString()

    @Throws(NumberFormatException::class)
    public constructor(signed: String): this(signed.toDouble())

    override fun deepCopy(): NbtDouble = copy()
}
