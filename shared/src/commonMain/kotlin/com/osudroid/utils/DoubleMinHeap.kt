package com.osudroid.utils

/**
 * A priority queue of [Double]s whose head is the smallest, as a binary heap over a growable array.
 *
 * @param initialCapacity How many values it holds before it grows.
 */
class DoubleMinHeap(initialCapacity: Int = 16) {

    private var values = DoubleArray(initialCapacity.coerceAtLeast(1))

    /**
     * How many values it holds.
     */
    var size = 0
        private set

    fun isEmpty() = size == 0

    /**
     * Adds [value].
     */
    fun add(value: Double) {
        if (size == values.size) {
            values = values.copyOf(values.size * 2)
        }

        // Sift up.
        var index = size++
        while (index > 0) {
            val parent = (index - 1) / 2
            if (values[parent] <= value) {
                break
            }
            values[index] = values[parent]
            index = parent
        }
        values[index] = value
    }

    /**
     * The smallest value.
     *
     * @throws NoSuchElementException If it's empty.
     */
    fun peek(): Double {
        if (size == 0) {
            throw NoSuchElementException("The heap is empty")
        }
        return values[0]
    }

    /**
     * Removes and returns the smallest value.
     *
     * @throws NoSuchElementException If it's empty.
     */
    fun poll(): Double {
        val smallest = peek()
        val last = values[--size]

        // Sift the last value down from the root.
        var index = 0
        while (true) {
            val left = index * 2 + 1
            if (left >= size) {
                break
            }
            val right = left + 1
            val child = if (right < size && values[right] < values[left]) right else left
            if (values[child] >= last) {
                break
            }
            values[index] = values[child]
            index = child
        }
        if (size > 0) {
            values[index] = last
        }

        return smallest
    }
}
