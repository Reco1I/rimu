package ru.nsu.ccfit.zuev.osu.helper

import com.osudroid.resources.StringArrayResource
import com.osudroid.resources.StringResource
import kotlin.jvm.JvmStatic

/**
 * The localized strings of the game, which the platform provides through [implementation].
 */
object StringTable {

    /**
     * The implementation of the current platform.
     */
    lateinit var implementation: Implementation


    @JvmStatic
    fun get(resource: StringResource): String {
        return implementation.get(resource)
    }

    @JvmStatic
    fun getArray(resource: StringArrayResource): Array<String> {
        return implementation.getArray(resource)
    }

    /**
     * Gets a string by its Android resource ID, for the code that is still part of the Android app.
     */
    @JvmStatic
    fun get(resid: Int): String {
        return implementation.get(resid)
    }


    @JvmStatic
    fun format(resource: StringResource, vararg objects: Any?): String {
        return format(get(resource), *objects)
    }

    /**
     * Formats a string by its Android resource ID, for the code that is still part of the Android app.
     */
    @JvmStatic
    fun format(resid: Int, vararg objects: Any?): String {
        return format(get(resid), *objects)
    }

    @JvmStatic
    fun format(format: String, vararg objects: Any?): String {
        return implementation.format(format, *objects)
    }


    interface Implementation {

        /**
         * Gets the string of [resource] in the current language.
         */
        fun get(resource: StringResource): String

        /**
         * Gets the array of strings of [resource] in the current language.
         */
        fun getArray(resource: StringArrayResource): Array<String>

        /**
         * Gets a string by its Android resource ID, for the code that is still part of the Android app.
         */
        fun get(resid: Int): String

        /**
         * Formats [args] as [format] says, as `java.util.Formatter` does in the current locale.
         */
        fun format(format: String, vararg args: Any?): String
    }
}
