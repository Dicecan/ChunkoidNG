package br.com.gamemods.nbtmanipulator

public data class NbtString(var value: String): NbtTag() {

    override val stringValue: String
        get() = value

    override fun toTechnicalString(): String {
        return buildString {
            append("NbtString(\"")
            append(
                value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
            )
            append("\")")
        }
    }

    override fun deepCopy(): NbtString = copy()
}
