@file:OptIn(DelicateCoroutinesApi::class)

package com.reco1l.framework.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Run a task on asynchronous using global scope.
 */
fun async(block: () -> Unit) = GlobalScope.launch {
    block()
}

/**
 * Run a task ignoring exceptions on asynchronous using global scope.
 */
fun asyncIgnoreExceptions(block: () -> Unit) = GlobalScope.launch {
    try { block() } catch (e: Exception) { e.printStackTrace() }
}

/**
 * Run a delayed task on asynchronous using global scope.
 */
fun delayed(time: Long, block: () -> Unit) = GlobalScope.launch {
    delay(time)
    block()
}


// Coroutines

/**
 * Similarly to [launch] with inferred [join] call.
 */
@Suppress("SuspendFunctionOnCoroutineScope")
suspend fun CoroutineScope.join(block: suspend CoroutineScope.() -> Unit): Job
{
    return launch { block() }.apply { join() }
}
