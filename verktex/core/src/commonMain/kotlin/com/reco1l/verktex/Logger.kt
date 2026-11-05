package com.reco1l.verktex

/**
 * A singleton object that provides logging functionality for the engine. It allows logging messages
 * at different levels (info, warning, error) and requires a platform-specific implementation to be
 * set before use.
 */
object Logger {

    /**
     * Sets the logger implementation to be used for logging messages. This usually should be set once
     * during the application initialization phase.
     * If not set, any logging attempts will throw an [IllegalStateException].
     *
     * An implementation of this must be provided by the platform or application using this engine,
     * as the engine itself does not provide a default logging mechanism.
     */
    lateinit var implementation: Implementation


    private fun checkImplementation() {
        if (!::implementation.isInitialized) {
            throw IllegalStateException("Logger implementation is not initialized. Please set the implementation before using the Logger.")
        }
    }


    /**
     * Logs an informational message with the specified tag and message. This is typically used for
     * general information about the application's operation.
     *
     * **Note: These messages are only dispatched to the console if the engine is running in debug
     * mode, to avoid cluttering the output in production environments.**
     */
    fun i(tag: String, message: String) {
        checkImplementation()
        implementation.i(tag, message)
    }

    /**
     * Logs a warning message with the specified tag and message. This is typically used for situations
     * that are not errors but may require attention or indicate potential issues.
     *
     * **Note: These messages are only dispatched to the console if the engine is running in debug
     * mode, to avoid cluttering the output in production environments.**
     */
    fun w(tag: String, message: String) {
        checkImplementation()
        implementation.w(tag, message)
    }

    /**
     * Logs an error message with the specified tag and message. This is typically used for situations
     * that indicate a failure or problem in the application.
     *
     * An optional [throwable] can be provided to include stack trace information.
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        checkImplementation()
        implementation.e(tag, message, throwable)
    }


    interface Implementation {
        fun i(tag: String, message: String)
        fun w(tag: String, message: String)
        fun e(tag: String, message: String, throwable: Throwable? = null)
    }
}