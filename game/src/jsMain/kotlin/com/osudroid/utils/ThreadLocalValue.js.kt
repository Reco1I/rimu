package com.osudroid.utils

// Only compiled to check the common code, see build.gradle.kts. JavaScript has a single thread.
actual class ThreadLocalValue<T> actual constructor(initial: () -> T) {
    private val value by lazy(initial)

    actual fun get(): T = value
}
