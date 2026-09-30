package br.com.gamemods.nbtmanipulator

public data class NbtFile @JvmOverloads constructor(
    var name: String,
    var tag: NbtTag,
    var version: Int? = null,
    var length: Int? = null,
    var isCompressed: Boolean? = null,
    var isLittleEndian: Boolean? = null
) {
    @Suppress("MemberVisibilityCanBePrivate")
    public var compound: NbtCompound
        @Throws(ClassCastException::class)
        get() = tag as NbtCompound
        set(value) {
            tag = value
        }
}
