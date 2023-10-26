package com.reco1l.framework.kotlin

import kotlin.reflect.KClass


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
    clampToBounds: Boolean = false
): T?
{
    var index = indexOf(element ?: return null) + 1

    index = if (clampToBounds)
        index.coerceAtMost(lastIndex)
    else
        if (index > lastIndex) 0 else index

    return getOrNull(index)
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

    return getOrNull(index)
}


inline fun <T>MutableList<T>.forEachTrim(reversed: Boolean = false, block: (T) -> Unit)
{
    while (isNotEmpty())
        block(if (reversed) removeLast() else removeFirst())
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
