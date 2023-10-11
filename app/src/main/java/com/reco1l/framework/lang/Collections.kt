package com.reco1l.framework.lang

import kotlin.reflect.KClass


/**
 * Improved comparator with ascending boolean property.
 */
inline fun <T> compareBy(ascending: Boolean, crossinline selector: (T) -> Comparable<*>?) = Comparator<T> { a, b ->

    if (ascending)
        compareValuesBy(a, b, selector)
    else
        compareValuesBy(b, a, selector)
}

/**
 * Add an element if the passed element isn't null.
 */
fun <T : Any> MutableCollection<T>.addIfNotNull(element: T?) = element?.let { add(it) } ?: false

/**
 * Returns the next element from the passed element or `null` if there's no next element.
 *
 * Note: This will never return the same object than the passed in [element].
 *
 * @param clampToBounds If `true` when reaching the max bound it'll return `null` instead of truncating
 * to first element.
 */
fun <T>List<T>.nextOf(
    element: T?,
    clampToBounds: Boolean = true
): T?
{
    var index = indexOf(element ?: return null) + 1

    if (clampToBounds)
        index = index.coerceAtMost(lastIndex)
    else
        index %= size

    return get(index).takeUnless { it == element }
}

/**
 * Returns the previous element from the passed element or `null` if there's no previous element.
 *
 * Note: This will never return the same object than the passed in [element].
 *
 * @param clampToBounds If `true` when reaching the min bound it'll return `null` instead of skipping
 * to last element.
 */
fun <T>List<T>.previousOf(
    element: T?,
    clampToBounds: Boolean = true
): T?
{
    var index = indexOf(element ?: return null) - 1

    index = if (clampToBounds)
        index.coerceAtLeast(0)
    else
        if (index < 0) lastIndex else index

    return get(index).takeUnless { it == element }
}


inline fun <T>MutableList<T>.forEachTrim(block: (T) -> Unit)
{
    while (isNotEmpty())
        block(removeFirst())
}

inline fun <T>MutableList<T>.forEachTrimEnd(block: (T) -> Unit)
{
    while (isNotEmpty())
        block(removeLast())
}

inline fun <K, V> Iterable<K>.associateWithIndexed(valueSelector: (K, Int) -> V): Map<K, V>
{
    var index = 0
    return associateWith {
        val value = valueSelector(it, index)
        index++
        value
    }
}

/**
 * Store instances from a class inheritors as singletons.
 */
fun <T : Any>instanceMapOf() = HashMap<KClass<out T>, T>()


infix fun <T>T.safeIn(array: Array<T>?): Boolean = array != null && this in array

infix fun <T>T.safeIn(list: List<T>?): Boolean = list != null && this in list