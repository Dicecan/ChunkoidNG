package br.com.gamemods.nbtmanipulator

public data class NbtByte(var signed: Byte) : NbtTag() {

    var unsigned: Int
        get() = signed.toInt() and 0xFF
        set(value) {
            this.signed = (value and 0xFF).toByte()
        }

    @Deprecated(
        "Deprecated in favor of signed and unsigned flavours. Replace with the signed property.",
        ReplaceWith("signed")
    )
    inline var value: Byte
        get() = signed
        set(value) {
            signed = value
        }

    override val stringValue: String
        get() = signed.toString()

    public constructor(value: Boolean): this(if (value) BYTE_TRUE else 0)

    @Throws(NumberFormatException::class)
    public constructor(unsigned: Int): this(unsigned.let {
        if(it < 0 || it > 255) {
            throw NumberFormatException("Expected an unsigned byte of range 0 to 255. Got $it.")
        }
        it.toByte()
    })

    @Throws(NumberFormatException::class)
    public constructor(signed: String): this(signed.toByte())

    override fun deepCopy(): NbtByte = copy()

    override fun toTechnicalString(): String {
        return "NbtByte($signed)"
    }

    public companion object {

        @JvmStatic
        public fun unsigned(unsigned: String): NbtByte = NbtByte(unsigned = unsigned.toInt())
    }
}
