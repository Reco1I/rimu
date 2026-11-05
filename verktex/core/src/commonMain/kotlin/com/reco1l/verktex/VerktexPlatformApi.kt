package com.reco1l.verktex

/**
 * Marks an API of the core that is only meant for the platform implementations, such as
 * `:verktex:android`. They opt in for the whole module, applications shouldn't.
 */
@RequiresOptIn(
    message = "This API is meant for the platform implementations of the engine.",
    level = RequiresOptIn.Level.ERROR
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR
)
annotation class VerktexPlatformApi
