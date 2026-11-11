package com.osudroid.utils

import java.util.PriorityQueue
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DoubleMinHeapTest {

    @Test
    fun testMatchesPriorityQueue() {
        val random = Random(7)
        val heap = DoubleMinHeap(1)
        val queue = PriorityQueue<Double>()

        repeat(50_000) {
            if (queue.isEmpty() || random.nextInt(3) != 0) {
                // Few distinct values, so there are ties.
                val value = random.nextInt(-50, 50) / 4.0
                heap.add(value)
                queue.add(value)
            } else {
                assertEquals(queue.peek(), heap.peek(), 0.0)
                assertEquals(queue.poll(), heap.poll(), 0.0)
            }
            assertEquals(queue.size, heap.size)
        }

        while (queue.isNotEmpty()) {
            assertEquals(queue.poll(), heap.poll(), 0.0)
        }
        assertTrue(heap.isEmpty())
    }

    @Test(expected = NoSuchElementException::class)
    fun testPeekEmpty() {
        DoubleMinHeap().peek()
    }

    @Test(expected = NoSuchElementException::class)
    fun testPollEmpty() {
        DoubleMinHeap().poll()
    }
}
