package com.osudroid.utils

/**
 * A least-recently-used cache: a map of up to [maxSize] entries that removes the least recently
 * used one when an entry is added to it full. Reading an entry with [get] or writing it with [put]
 * counts as using it.
 *
 * It isn't thread-safe, as the access-ordered `LinkedHashMap` it replaces wasn't.
 *
 * @param K The key of the cache.
 * @param V The value to cache.
 */
class LRUCache<K, V>(
    private val maxSize: Int
) : MutableMap<K, V> {

    // Insertion ordered: the least recently used entry is the first one, as using an entry moves it
    // to the end.
    private val map = LinkedHashMap<K, V>()

    override val size get() = map.size
    override val keys get() = map.keys
    override val values get() = map.values
    override val entries get() = map.entries

    override fun isEmpty() = map.isEmpty()
    override fun containsKey(key: K) = map.containsKey(key)
    override fun containsValue(value: V) = map.containsValue(value)

    override fun get(key: K): V? {
        if (!map.containsKey(key)) {
            return null
        }

        // Moved to the end, as the most recently used.
        @Suppress("UNCHECKED_CAST")
        val value = map.remove(key) as V
        map[key] = value
        return value
    }

    override fun put(key: K, value: V): V? {
        val previous = map.remove(key)
        map[key] = value

        if (map.size > maxSize) {
            val eldest = map.keys.iterator()
            eldest.next()
            eldest.remove()
        }

        return previous
    }

    override fun putAll(from: Map<out K, V>) = from.forEach { (key, value) -> put(key, value) }
    override fun remove(key: K) = map.remove(key)
    override fun clear() = map.clear()

    override fun equals(other: Any?) = map == other
    override fun hashCode() = map.hashCode()
    override fun toString() = map.toString()
}
