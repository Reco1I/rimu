package com.reco1l.verktex.localization

import kotlin.concurrent.Volatile

/**
 * Text that depends on the [Localization.locale]. Components showing it resolve it again when the
 * locale changes, see [com.reco1l.verktex.ui.UIComponent.bindLocale].
 */
interface Localized : CharSequence


/**
 * The string with the given [key] in the current locale, resolved whenever it's read. Without
 * [args] it's the string as is, otherwise the string is the pattern they're formatted with (see
 * [formatString]); an argument can be a [LocalizedString] too.
 *
 * Use [toString] to take the string in the current locale.
 */
class LocalizedString private constructor(
    val key: String,
    private val args: Array<out Any?>?,
) : Localized {

    constructor(key: String) : this(key, null)


    private class Resolved(val version: Int, val value: String)

    @Volatile
    private var resolved: Resolved? = null


    /**
     * This string formatted with [args].
     */
    fun format(vararg args: Any?) = LocalizedString(key, args)


    override fun toString(): String {
        val version = Localization.version
        resolved?.let { if (it.version == version) return it.value }

        val pattern = Localization.get(key)
        val value = if (args == null) pattern else formatString(pattern, *args)

        resolved = Resolved(version, value)
        return value
    }

    override val length get() = toString().length

    override fun get(index: Int) = toString()[index]

    override fun subSequence(startIndex: Int, endIndex: Int) = toString().subSequence(startIndex, endIndex)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LocalizedString || key != other.key) return false
        if (args == null || other.args == null) return args == null && other.args == null
        return args.contentEquals(other.args)
    }

    override fun hashCode() = 31 * key.hashCode() + (args?.contentHashCode() ?: 0)
}

/**
 * The array of strings with the given [key] in the current locale, resolved whenever it's read.
 */
class LocalizedStringArray(val key: String) {

    /**
     * The strings in the current locale.
     */
    val values: List<String>
        get() = Localization.getArray(key)

    val size get() = values.size

    operator fun get(index: Int) = values[index]

    fun toTypedArray() = values.toTypedArray()

    override fun equals(other: Any?) = other is LocalizedStringArray && key == other.key

    override fun hashCode() = key.hashCode()

    override fun toString() = values.toString()
}
