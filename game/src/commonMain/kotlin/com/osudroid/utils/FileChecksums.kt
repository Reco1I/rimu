@file:JvmName("FileChecksums")

package com.osudroid.utils

import kotlin.jvm.JvmName
import okio.use
import okio.HashingSink
import okio.IOException
import okio.Path.Companion.toPath
import okio.blackholeSink
import okio.buffer
import ru.nsu.ccfit.zuev.osu.helper.Debug

/**
 * Computes the MD5 checksum of the file at [path], as lowercase hexadecimal digits, or an empty string
 * if it can't be read.
 */
fun md5Checksum(path: String): String = checksum(path) { HashingSink.md5(it) }

/**
 * Computes the SHA-256 checksum of the file at [path], as lowercase hexadecimal digits, or an empty string
 * if it can't be read.
 */
fun sha256Checksum(path: String): String = checksum(path) { HashingSink.sha256(it) }

private fun checksum(path: String, createSink: (okio.Sink) -> HashingSink): String {
    return try {
        val hashingSink = createSink(blackholeSink())
        SystemFileSystem.source(path.toPath()).buffer().use { source ->
            hashingSink.buffer().use { source.readAll(it) }
        }
        hashingSink.hash.hex()
    } catch (e: IOException) {
        Debug.e("getFileChecksum " + e.message, e)
        ""
    }
}
