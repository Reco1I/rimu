package com.osudroid.utils

/**
 * A growable set of bits, as `java.util.BitSet` for the operations the game uses.
 */
class BitSet {
    private var words = LongArray(1)

    /**
     * Gets the bit at [index].
     */
    operator fun get(index: Int): Boolean {
        require(index >= 0) { "index < 0: $index" }
        val word = index ushr 6
        return word < words.size && (words[word] and (1L shl index)) != 0L
    }

    /**
     * Sets the bit at [index] to [value].
     */
    @kotlin.jvm.JvmOverloads
    fun set(index: Int, value: Boolean = true) {
        require(index >= 0) { "index < 0: $index" }
        val word = index ushr 6

        if (value) {
            if (word >= words.size) {
                words = words.copyOf(maxOf(words.size * 2, word + 1))
            }
            words[word] = words[word] or (1L shl index)
        } else if (word < words.size) {
            words[word] = words[word] and (1L shl index).inv()
        }
    }

    /**
     * Clears all bits.
     */
    fun clear() {
        words.fill(0)
    }

    /**
     * The index of the highest set bit plus one, or zero if no bit is set.
     */
    fun length(): Int {
        for (i in words.indices.reversed()) {
            if (words[i] != 0L) {
                return i * 64 + (64 - words[i].countLeadingZeroBits())
            }
        }
        return 0
    }

    /**
     * Whether no bit is set.
     */
    fun isEmpty() = words.all { it == 0L }

    /**
     * Creates a copy of this [BitSet].
     */
    fun copy() = BitSet().also { it.words = words.copyOf() }

    override fun equals(other: Any?) = other is BitSet && length() == other.length() &&
        (0 until length()).all { get(it) == other[it] }

    override fun hashCode(): Int {
        var hash = 1
        for (i in 0 until length()) {
            if (get(i)) hash = hash * 31 + i
        }
        return hash
    }
}
