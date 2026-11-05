package com.reco1l.verktex

import kotlinx.atomicfu.locks.ReentrantLock
import kotlinx.atomicfu.locks.reentrantLock

// JavaScript runs on one thread, nothing waits on the lock.
internal actual fun fairReentrantLock(): ReentrantLock = reentrantLock()
