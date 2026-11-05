package com.reco1l.verktex

import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * A singleton object that provides logging functionality for the engine. It allows logging messages
 * at different levels (info, warning, error) through a platform-specific implementation, printing to
 * the standard output until one is set.
 */
object Logger {

    /**
     * Sets the logger implementation to be used for logging messages. This usually should be set once
     * during the application initialization phase.
     * Until then messages are printed to the standard output, see [Implementation.Console], which
     * lets code that logs run without a platform, as unit tests do.
     */
    var implementation: Implementation = Implementation.Console


    /**
     * Logs an informational message with the specified tag and message. This is typically used for
     * general information about the application's operation.
     *
     * **Note: These messages are only dispatched to the console if the engine is running in debug
     * mode, to avoid cluttering the output in production environments.**
     */
    @JvmStatic
    fun i(tag: String, message: String) {
        implementation.i(tag, message)
    }

    /**
     * Logs a warning message with the specified tag and message. This is typically used for situations
     * that are not errors but may require attention or indicate potential issues.
     *
     * **Note: These messages are only dispatched to the console if the engine is running in debug
     * mode, to avoid cluttering the output in production environments.**
     */
    @JvmStatic
    fun w(tag: String, message: String) {
        implementation.w(tag, message)
    }

    /**
     * Logs an error message with the specified tag and message. This is typically used for situations
     * that indicate a failure or problem in the application.
     *
     * An optional [throwable] can be provided to include stack trace information.
     */
    @JvmStatic
    @JvmOverloads
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        implementation.e(tag, message, throwable)
    }


    interface Implementation {
        fun i(tag: String, message: String)
        fun w(tag: String, message: String)
        fun e(tag: String, message: String, throwable: Throwable? = null)

        /**
         * Prints the messages to the standard output.
         */
        object Console : Implementation {
            override fun i(tag: String, message: String) = println("I/$tag: $message")
            override fun w(tag: String, message: String) = println("W/$tag: $message")

            override fun e(tag: String, message: String, throwable: Throwable?) {
                println("E/$tag: $message")
                throwable?.let { println(it.stackTraceToString()) }
            }
        }
    }
}