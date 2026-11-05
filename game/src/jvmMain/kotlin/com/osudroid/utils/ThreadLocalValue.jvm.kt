package com.osudroid.utils

actual class ThreadLocalValue<T> actual constructor(initial: () -> T) {
    private val local = ThreadLocal.withInitial(initial)

    actual fun get(): T = local.get()
}
