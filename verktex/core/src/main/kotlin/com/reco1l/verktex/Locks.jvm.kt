package com.reco1l.verktex

import kotlinx.atomicfu.locks.ReentrantLock

/**
 * A reentrant lock granted in the order it was requested, so the threads taking turns on it (e.g. the
 * draw and update workers) don't starve each other.
 */
internal fun fairReentrantLock(): ReentrantLock = java.util.concurrent.locks.ReentrantLock(true)
