package com.reco1l.verktex.desktop

import com.reco1l.verktex.Logger

/**
 * Logs to the console, warnings and errors to the standard error.
 */
class DesktopLogger : Logger.Implementation {

    override fun i(tag: String, message: String) {
        println("I/$tag: $message")
    }

    override fun w(tag: String, message: String) {
        System.err.println("W/$tag: $message")
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        System.err.println("E/$tag: $message")
        throwable?.printStackTrace()
    }
}
