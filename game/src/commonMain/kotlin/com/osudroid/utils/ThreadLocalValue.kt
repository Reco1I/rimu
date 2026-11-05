package com.osudroid.utils

/**
 * A value each thread has its own instance of, created by [initial] the first time the thread
 * [get]s it.
 */
expect class ThreadLocalValue<T>(initial: () -> T) {
    fun get(): T
}
