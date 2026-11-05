package com.osudroid.utils

// Only compiled to check the common code, see build.gradle.kts.
actual class LRUCache<K, V> actual constructor(maxSize: Int) : MutableMap<K, V> by LinkedHashMap()
