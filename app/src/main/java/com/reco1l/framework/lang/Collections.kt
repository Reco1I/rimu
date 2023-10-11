package com.reco1l.framework.lang

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


inline fun <T>MutableList<T>.forEachTrim(block: (T) -> Unit, reversed: Boolean = false)
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

/**
 * Just an alias for function [to].
 */
infix fun <T1, T2>T1.with(that: T2) = this to that