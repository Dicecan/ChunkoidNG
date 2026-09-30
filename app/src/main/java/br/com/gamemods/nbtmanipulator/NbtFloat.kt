package br.com.gamemods.nbtmanipulator

public data class NbtFloat(var value: Float) : NbtTag() {

    override val stringValue: String
        get() = value.toString()

    @Throws(NumberFormatException::class)
    public constructor(signed: String): this(signed.toFloat())

    override fun deepCopy(): NbtFloat = copy()
}
