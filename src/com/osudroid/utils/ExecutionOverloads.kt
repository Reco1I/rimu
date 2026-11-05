@file:JvmName("Execution")

package com.osudroid.utils

import android.os.Handler
import android.os.Looper
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Verktex

import java.util.concurrent.CompletableFuture
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.future
import kotlinx.coroutines.launch
import ru.nsu.ccfit.zuev.osu.GlobalManager

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
    // Without the main activity (e.g. in the UI test browser) a task may reach something only the
    // game sets up, which must not take the whole process down.
    if (GlobalManager.getInstance().isHeadless) {
        Logger.e("Execution", "Uncaught exception in a task while headless", e)
    } else {
        Thread.currentThread().let { it.uncaughtExceptionHandler?.uncaughtException(it, e) ?: throw e }
    }
})

// Lots of overloads here, but they are necessary for Java interoperability...

/**
 * Run a task on asynchronous using [executionScope].
 */
fun async(block: Runnable) = executionScope.launch { block.run() }

/**
 * Run a task on asynchronous using [executionScope].
 */
fun async(block: CoroutineScope.() -> Unit) = executionScope.launch(block = block)

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
fun delayed(time: Long, block: CoroutineScope.() -> Unit) = executionScope.launch {
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
    if (Looper.myLooper() == Looper.getMainLooper()) block.run() else mainHandler.post(block)
}

private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

/**
 * Run a task on the update thread.
 */
fun updateThread(block: Runnable) {
    Verktex.scheduleOnUpdate { block.run() }
}

/**
 * Wraps a [Job.cancelAndJoin] call inside a coroutine. Useful for asynchronously canceling and joining a [Job] with
 * Java interoperability in mind.
 *
 * Returns a [CompletableFuture] that completes when the job is canceled and joined.
 */
fun Job?.stopAsync(): CompletableFuture<Unit> =
    if (this?.isCompleted == false) CoroutineScope(EmptyCoroutineContext).future { this@stopAsync.cancelAndJoin() }
    else CompletableFuture.completedFuture(Unit)
