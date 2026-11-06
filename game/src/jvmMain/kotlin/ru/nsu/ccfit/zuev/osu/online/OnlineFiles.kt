@file:JvmName("OnlineFiles")

package ru.nsu.ccfit.zuev.osu.online

import kotlinx.coroutines.runBlocking

/**
 * Downloads [url] into [path] as [OnlineFileOperator.downloadFile] does, blocking the calling
 * thread, for Java code that runs on a background thread.
 */
fun downloadFileBlocking(url: String, path: String): Boolean = runBlocking {
    OnlineFileOperator.downloadFile(url, path)
}
