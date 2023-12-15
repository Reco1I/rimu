package com.reco1l.framework.data

import okhttp3.OkHttpClient
import okhttp3.Request
import okio.BufferedSource


// NIO

/**
 * Build an OkHttps3 HTTP request.
 */
fun buildRequest(block: Request.Builder.() -> Unit) = Request.Builder().apply(block).build()


/**
 *  Build an OkHttps3 HTTP client.
 */
fun buildClient(block: OkHttpClient.Builder.() -> Unit) = OkHttpClient.Builder().apply(block).build()


// Buffers

/**
 * Reads all UTF8 encoded lines until there's nothing left to read.
 */
inline fun BufferedSource.readUTF8Lines(block: (String) -> Unit)
{
    while (readUtf8Line()?.also(block) != null) Unit
    close()
}

/**
 * Reads all UTF8 encoded lines until there's nothing left to read or the predicate isn't accomplished
 * anymore.
 */
inline fun BufferedSource.readUTF8LinesUntil(predicate: (String) -> Boolean, block: (String) -> Unit)
{
    while (readUtf8Line()?.takeUnless(predicate)?.also(block) != null) Unit
}