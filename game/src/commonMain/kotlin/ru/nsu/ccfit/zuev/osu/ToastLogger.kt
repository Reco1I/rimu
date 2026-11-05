package ru.nsu.ccfit.zuev.osu

import com.osudroid.resources.StringResource
import kotlin.jvm.JvmStatic
import ru.nsu.ccfit.zuev.osu.helper.StringTable

class ToastLogger private constructor(private val implementation: Implementation) {
    private var debugLog = ArrayList<String>()
    private var percentage = 0f

    /**
     * Shows the messages on the platform.
     */
    fun interface Implementation {
        fun showText(message: String, showlong: Boolean)
    }

    companion object {
        private var instance: ToastLogger? = null

        @JvmStatic
        fun init(implementation: Implementation) {
            instance = ToastLogger(implementation)
        }

        @JvmStatic
        fun showText(message: String, showlong: Boolean) {
            val instance = instance ?: return

            instance.implementation.showText(message, showlong)
        }

        @JvmStatic
        fun showText(resource: StringResource, showlong: Boolean) {
            showText(StringTable.get(resource), showlong)
        }

        /**
         * Shows a string by its Android resource ID, for the code that is still part of the Android app.
         */
        @JvmStatic
        fun showText(resID: Int, showlong: Boolean) {
            showText(StringTable.get(resID), showlong)
        }

        @JvmStatic
        fun getLog(): ArrayList<String>? {
            val instance = instance ?: return null
            return instance.debugLog
        }

        @JvmStatic
        fun getPercentage(): Float {
            val instance = instance ?: return -1f
            return instance.percentage
        }

        @JvmStatic
        fun setPercentage(perc: Float) {
            val instance = instance ?: return
            instance.percentage = perc
        }
    }
}
