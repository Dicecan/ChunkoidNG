package br.com.gamemods.nbtmanipulator

public data class NbtInt(var value: Int) : NbtTag() {

    override val stringValue: String
        get() = value.toString()

    @Throws(NumberFormatException::class)
    public constructor(signed: String): this(signed.toInt())

    override fun deepCopy(): NbtInt = copy()
}
