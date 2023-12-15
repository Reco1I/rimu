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

/**
 * Iterates all over the list removing the elements from start or from the end depending of [reversed]
 * parameter.
 */
inline fun <T>MutableList<T>.forEachTrim(reversed: Boolean = false, block: (T) -> Unit)
{
    while (isNotEmpty())
        block(if (reversed) removeLast() else removeFirst())
}

/**
 * Iterates all over the list transforming the result and returning it at the end.
 */
inline fun <T, R : Any?>Array<T>.forEachLet(block: (T) -> R): R?
{
    var result: R? = null
    forEach { result = block(it) }
    return result
}

/**
 * Covers the same functions as [associateWith] with indices.
 */
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

/**
 * Safe check if an element is in a nullable array. If the array is null then the result is `false`.
 */
infix fun <T>T.safeIn(array: Array<T>?): Boolean = array != null && this in array
