package com.reco1l.framework.kotlin


/**
 * Special block to ignore exceptions
 */
inline fun ignoreException(block: () -> Unit)
{
    try { block() } catch (_: Exception) {  }
}

/**
 * Prettier try-catch with result returning.
 */
inline fun <T> (() -> T).orCatch(onException: (e: Exception) -> T): T
{
    return try { this() } catch (e: Exception) { onException(e) }
}