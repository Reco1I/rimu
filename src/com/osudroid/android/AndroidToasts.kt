package com.osudroid.android

import android.app.Activity
import android.widget.Toast
import ru.nsu.ccfit.zuev.osu.ToastLogger

/**
 * Shows the [ToastLogger] messages as Android toasts.
 */
class AndroidToasts(private val activity: Activity) : ToastLogger.Implementation {
    override fun showText(message: String, showlong: Boolean) {
        activity.runOnUiThread {
            Toast.makeText(activity, message, if (showlong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
        }
    }
}
