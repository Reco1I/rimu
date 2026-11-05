package com.reco1l.verktex.android

import com.reco1l.verktex.Logger

class AndroidLogger : Logger.Implementation {
    override fun i(tag: String, message: String) {
        android.util.Log.i(tag, message)
    }

    override fun w(tag: String, message: String) {
        android.util.Log.w(tag, message)
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        android.util.Log.e(tag, message, throwable)
    }
}