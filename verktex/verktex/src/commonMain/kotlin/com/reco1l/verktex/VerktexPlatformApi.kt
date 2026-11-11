package com.reco1l.verktex

/**
 * Marks an API of the common code that is only meant for the platform sources of the engine
 * (`androidMain`, `desktopMain`). The module opts in as a whole, applications shouldn't.
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
