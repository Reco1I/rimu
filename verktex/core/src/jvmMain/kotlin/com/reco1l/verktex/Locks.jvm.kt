package com.reco1l.verktex

import kotlinx.atomicfu.locks.ReentrantLock

// atomicfu's lock is java.util.concurrent's on the JVM, which can be made fair.
internal actual fun fairReentrantLock(): ReentrantLock = java.util.concurrent.locks.ReentrantLock(true)
