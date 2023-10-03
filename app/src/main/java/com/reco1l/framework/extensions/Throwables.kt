package com.reco1l.framework.extensions


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

/**
 * Prettier try-catch on async thread.
 */
fun (() -> Any).orAsyncCatch(onException: ((e: Exception) -> Unit)?)
{
    async { try { this() } catch (e: Exception) { onException?.invoke(e) } }
}
