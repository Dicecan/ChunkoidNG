package br.com.gamemods.nbtmanipulator

public object NbtEnd : NbtTag() {

    override val stringValue: String
        get() = ""

    override fun toTechnicalString(): String {
        return "NbtEnd"
    }

    override fun deepCopy(): NbtEnd = NbtEnd
}
