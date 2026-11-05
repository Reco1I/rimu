package com.osudroid.utils

import kotlin.math.ceil

// An access ordered LinkedHashMap, which removes the least recently used entry once it's full.
actual class LRUCache<K, V> actual constructor(
    private val maxSize: Int
) : LinkedHashMap<K, V>(ceil(maxSize / 0.75f).toInt(), 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>) = size > maxSize
}
