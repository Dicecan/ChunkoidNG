package com.noches.chunkoidng.core.leveldb

class BitArray(
    val bitsPerBlock: Int,
    val size: Int = 4096,
    val words: IntArray = IntArray(calculateWordCount(bitsPerBlock, size))
) {
    val blocksPerWord: Int = 32 / bitsPerBlock
    private val mask: Int = (1 shl bitsPerBlock) - 1

    init {
        require(bitsPerBlock in VALID_BITS_PER_BLOCK) {
            "Unsupported bitsPerBlock: $bitsPerBlock. Must be one of $VALID_BITS_PER_BLOCK"
        }
        val expectedWordCount = calculateWordCount(bitsPerBlock, size)
        require(words.size >= expectedWordCount) {
            "Words array size (${words.size}) too small, expected at least $expectedWordCount for bitsPerBlock=$bitsPerBlock, size=$size"
        }
    }

    operator fun set(index: Int, value: Int) {
        require(index in 0 until size) { "Index out of bounds: $index (size=$size)" }
        require(value in 0..mask) { "Value $value out of range for $bitsPerBlock bits (max=$mask)" }

        val wordIndex = index / blocksPerWord
        val bitOffset = (index % blocksPerWord) * bitsPerBlock

        words[wordIndex] = (words[wordIndex] and (mask shl bitOffset).inv()) or ((value and mask) shl bitOffset)
    }

    operator fun get(index: Int): Int {
        require(index in 0 until size) { "Index out of bounds: $index (size=$size)" }

        val wordIndex = index / blocksPerWord
        val bitOffset = (index % blocksPerWord) * bitsPerBlock

        return (words[wordIndex] ushr bitOffset) and mask
    }

    companion object {
        val VALID_BITS_PER_BLOCK = listOf(1, 2, 3, 4, 5, 6, 8, 16)

        fun calculateWordCount(bitsPerBlock: Int, size: Int = 4096): Int {
            val bpw = 32 / bitsPerBlock
            return (size + bpw - 1) / bpw
        }

        fun getOptimalBitsPerBlock(paletteSize: Int): Int {
            for (bits in VALID_BITS_PER_BLOCK) {
                if ((1 shl bits) >= paletteSize) {
                    return bits
                }
            }
            return 16
        }
    }
}
