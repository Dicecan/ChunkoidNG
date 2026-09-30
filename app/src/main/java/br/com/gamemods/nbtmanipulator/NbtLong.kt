package br.com.gamemods.nbtmanipulator

public data class NbtLong(var value: Long) : NbtTag() {

    override val stringValue: String
        get() = value.toString()

    @Throws(NumberFormatException::class)
    public constructor(signed: String): this(signed.toLong())

    override fun deepCopy(): NbtLong = copy()
}
