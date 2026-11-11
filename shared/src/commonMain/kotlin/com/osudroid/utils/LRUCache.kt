package com.osudroid.utils

/**
 * An implementation of least-recently-used cache using [LinkedHashMap]s.
 *
 * @param <K> The key of the cache.
 * @param <V> The value to cache.
 */
expect class LRUCache<K, V>(maxSize: Int) : MutableMap<K, V>
