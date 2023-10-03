package com.reco1l.framework.extensions


/**
 * Improved comparator with ascending boolean property.
 */
inline fun <T> compareBy(ascending: Boolean, crossinline selector: (T) -> Comparable<*>?) = Comparator<T> { a, b ->

    if (ascending)
    {
        compareValuesBy(a, b, selector)
    }
    else compareValuesBy(b, a, selector)
}

/**
 * Add an element if the passed element isn't null.
 */
fun <T : Any> MutableCollection<T>.addIfNotNull(element: T?) = element?.let { add(it) } ?: false

/**
 * Returns the next element from the passed element or `null` if there's no next element.
 */
fun <T>List<T>.nextOf(element: T?) = element?.let { getOrNull(indexOf(it) + 1) }

/**
 * Returns the previous element from the passed element or `null` if there's no previous element.
 */
fun <T>List<T>.previousOf(element: T?) = element?.let { getOrNull(indexOf(it) - 1) }


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

