package com.reco1l.framework.extensions

/**
 * Run a block if the receiver is not `null`.
 */
inline fun <T : Any> T?.ifNotNull(block: (T) -> Unit)
{
    if (this != null)
        block(this)
}

/**
 * Returns `true` if the receiver equals `null`.
 */
fun Any?.isNull() = this == null