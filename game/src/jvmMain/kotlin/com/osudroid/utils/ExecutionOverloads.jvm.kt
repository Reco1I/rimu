@file:JvmName("Execution")
@file:JvmMultifileClass

package com.osudroid.utils

import java.util.concurrent.CompletableFuture
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.future.future

// For the Java code, which waits on the returned futures.

/**
 * Wraps a [Job.cancelAndJoin] call inside a coroutine. Useful for asynchronously canceling and joining a [Job] with
 * Java interoperability in mind.
 *
 * Returns a [CompletableFuture] that completes when the job is canceled and joined.
 */
fun Job?.stopAsync(): CompletableFuture<Unit> =
    if (this?.isCompleted == false) CoroutineScope(EmptyCoroutineContext).future { this@stopAsync.cancelAndJoin() }
    else CompletableFuture.completedFuture(Unit)
