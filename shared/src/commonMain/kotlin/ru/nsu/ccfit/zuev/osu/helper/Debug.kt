package ru.nsu.ccfit.zuev.osu.helper

import com.reco1l.verktex.Logger
import kotlin.jvm.JvmStatic

/**
 * Logging shortcuts for the legacy code.
 */
object Debug {
    private const val TAG = "osu!droid"

    @JvmStatic
    fun i(message: String?) {
        Logger.i(TAG, message.toString())
    }

    @JvmStatic
    fun e(message: String?) {
        Logger.e(TAG, message.toString())
    }

    @JvmStatic
    fun e(throwable: Throwable) {
        Logger.e(TAG, throwable.message.toString(), throwable)
    }

    @JvmStatic
    fun e(message: String?, throwable: Throwable?) {
        Logger.e(TAG, message.toString(), throwable)
    }
}
