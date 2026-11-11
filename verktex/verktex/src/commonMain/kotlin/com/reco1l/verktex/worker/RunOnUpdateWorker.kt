package com.reco1l.verktex.worker

/**
 * Indicates that the annotated method executes on the update thread. This annotation is used to mark
 * methods that should be called from the update thread, ensuring that they are executed in the
 * correct context for update operations.
 *
 * Caution should be exercised when calling methods annotated with [RunOnUpdateWorker] from other threads,
 * as it may lead to unexpected behavior or update issues.
 *
 * If a action should be executed on the update thread, it can be posted to the draw thread using
 * [com.reco1l.verktex.Verktex.scheduleOnUpdate].
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class RunOnUpdateWorker