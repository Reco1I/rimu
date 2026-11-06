@file:JvmName("Execution")
@file:JvmMultifileClass

package com.osudroid.utils

import com.osudroid.OsuPlatform
import com.reco1l.verktex.Verktex
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A [Runnable] intended specifically for Java interoperability with Kotlin coroutines.
 */
interface CoroutineRunnable {
    operator fun invoke(scope: CoroutineScope)
}

/**
 * Scope used for fire-and-forget coroutines launched through [async] and [delayed].
 */
private val executionScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, e ->
    if (OsuPlatform.isInitialized) {
        OsuPlatform.implementation.handleUncaughtException(e)
    } else {
        throw e
    }
})

// Lots of overloads here, but they are necessary for Java interoperability...

/**
 * Run a task on asynchronous using [executionScope].
 */
fun async(block: Runnable) = executionScope.launch { block.run() }

/**
 * Run a task on asynchronous using [executionScope]. The block is a coroutine, it can call suspend
 * functions such as the requests of [com.osudroid.online.httpClient].
 */
fun async(block: suspend CoroutineScope.() -> Unit) = executionScope.launch(block = block)

/**
 * Run a task on asynchronous using [executionScope].
 */
fun async(block: CoroutineRunnable) = executionScope.launch { block(this) }


/**
 * Run a block of code ignoring any exceptions.
 */
fun runSafe(block: Runnable) = try {
    block.run()
} catch (e: Exception) {
    e.printStackTrace()
}


/**
 * Run a delayed task on asynchronous using [executionScope].
 */
fun delayed(time: Long, block: Runnable) = executionScope.launch {
    delay(time.milliseconds)
    block.run()
}

/**
 * Run a delayed task on asynchronous using [executionScope].
 */
fun delayed(time: Long, block: suspend CoroutineScope.() -> Unit) = executionScope.launch {
    delay(time.milliseconds)
    block()
}

/**
 * Run a delayed task on asynchronous using [executionScope].
 */
fun delayed(time: Long, block: CoroutineRunnable) = executionScope.launch {
    delay(time.milliseconds)
    block(this)
}

/**
 * Run a task on the main thread.
 */
fun mainThread(block: Runnable) {
    OsuPlatform.implementation.runOnMainThread { block.run() }
}

/**
 * Run a task on the update thread.
 */
fun updateThread(block: Runnable) {
    Verktex.scheduleOnUpdate { block.run() }
}
