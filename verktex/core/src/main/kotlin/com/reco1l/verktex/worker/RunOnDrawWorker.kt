package com.reco1l.verktex.worker

/**
 * Indicates that the annotated method executes on the draw thread. This annotation is used to mark
 * methods that should be called from the rendering thread, ensuring that they are executed in the
 * correct context for rendering operations.
 *
 * Caution should be exercised when calling methods annotated with [RunOnDrawWorker] from other threads,
 * as it may lead to unexpected behavior or rendering issues.
 *
 * If a action should be executed on the draw thread, it can be posted to the draw thread using
 * [com.reco1l.verktex.Verktex.scheduleOnDraw].
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class RunOnDrawWorker