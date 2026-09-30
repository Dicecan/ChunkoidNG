package br.com.gamemods.nbtmanipulator

public sealed class NbtTag {

    public abstract val stringValue: String

    public abstract fun deepCopy(): NbtTag

    protected open fun toTechnicalString(): String = "${this::class.java.simpleName}($stringValue)"

    final override fun toString(): String = toTechnicalString()
}
