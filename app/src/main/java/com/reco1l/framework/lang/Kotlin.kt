package com.reco1l.framework.lang

/**
 * Run a block if the receiver is not `null`.
 */
inline fun <T : Any> T?.ifNotNull(block: (T) -> Unit)
{
    if (this != null)
        block(this)
}