package com.reco1l.framework

import kotlin.concurrent.atomics.AtomicArray

/**
 * A pool of reusable snapshot buffers backing [forEachTolerant] and [forEachIndexedTolerant].
 *
 * A plain index-based loop over the live list tolerates it shrinking (bounds check) but not
 * skipping or double-visiting elements. Removing the current element shifts the next one into its
 * slot and past the loop unseen, and reordering (e.g. moving an element to the end) can revisit a
 * slot. Snapshotting into a buffer first and iterating that instead avoids both.
 *
 * Every active call owns the buffer it took until it gives it back, so nested calls (an action
 * that iterates again, directly or through a chain of calls) and calls from other threads each
 * get their own. The pool keeps up to [TOLERANT_BUFFER_SLOTS] idle buffers in slots taken and
 * returned atomically, so it needs no lock; when every slot is empty a new buffer is allocated,
 * and when every slot is full a returned buffer is dropped.
 */
private const val TOLERANT_BUFFER_SLOTS = 16

private val tolerantBuffers = AtomicArray<ArrayList<Any?>?>(arrayOfNulls(TOLERANT_BUFFER_SLOTS))

@Suppress("UNCHECKED_CAST")
@PublishedApi
internal fun <T> acquireTolerantBuffer(): ArrayList<T> {
    for (i in 0 until TOLERANT_BUFFER_SLOTS) {
        val buffer = tolerantBuffers.loadAt(i) ?: continue
        if (tolerantBuffers.compareAndSetAt(i, buffer, null)) {
            return buffer as ArrayList<T>
        }
    }
    return ArrayList<Any?>() as ArrayList<T>
}

@Suppress("UNCHECKED_CAST")
@PublishedApi
internal fun releaseTolerantBuffer(buffer: ArrayList<*>) {
    // Cleared on release, not acquire, so an unused object isn't kept alive by a stale
    // snapshot reference until the buffer is reused.
    buffer.clear()
    buffer as ArrayList<Any?>
    for (i in 0 until TOLERANT_BUFFER_SLOTS) {
        if (tolerantBuffers.loadAt(i) == null && tolerantBuffers.compareAndSetAt(i, null, buffer)) {
            return
        }
    }
}

/**
 * Copies [source] into this buffer element by element.
 *
 * Unlike [ArrayList.addAll], this does not allocate the intermediate array
 * that [Collection.toArray] produces.
 */
@PublishedApi
internal fun <T> ArrayList<T>.copyFrom(source: List<T>) {
    if (source !is RandomAccess) {
        // Indexed access would be O(n) per element here.
        addAll(source)
        return
    }

    ensureCapacity(source.size)

    for (i in source.indices) {
        add(source[i])
    }
}

/**
 * Iterates over a snapshot of the receiver taken at call time, so that a structural mutation
 * caused by [action] (e.g. removing or reordering elements) can neither skip nor duplicate
 * elements, unlike a live index-based loop or [Iterable.forEach] (the latter also fails fast on
 * such mutation for [ArrayList]-backed collections).
 *
 * The snapshot is drawn from a pool rather than allocated per call.
 *
 * Only use this in place of [Iterable.forEach] where that mutation tolerance is actually needed. In addition to that,
 * do not "simplify" a call site back to [Iterable.forEach] without first confirming the callback can't structurally
 * mutate the same [List] it's iterating.
 */
inline fun <T> List<T>.forEachTolerant(action: (T) -> Unit) {
    val buffer = acquireTolerantBuffer<T>()

    try {
        buffer.copyFrom(this)

        for (i in buffer.indices) {
            action(buffer[i])
        }
    } finally {
        releaseTolerantBuffer(buffer)
    }
}

/**
 * Iterates over a snapshot of the receiver taken at call time.
 *
 * @see forEachTolerant
 */
inline fun <T> List<T>.forEachIndexedTolerant(action: (Int, T) -> Unit) {
    val buffer = acquireTolerantBuffer<T>()

    try {
        buffer.copyFrom(this)

        for (i in buffer.indices) {
            action(i, buffer[i])
        }
    } finally {
        releaseTolerantBuffer(buffer)
    }
}
