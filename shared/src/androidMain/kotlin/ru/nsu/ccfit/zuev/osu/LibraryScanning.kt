@file:JvmName("LibraryScanning")

package ru.nsu.ccfit.zuev.osu

import kotlinx.coroutines.runBlocking

/**
 * Scans the beatmap directory as [LibraryManager.scanDirectory] does, blocking the calling thread
 * until it's done, for `MainActivity`, which loads the library on a background thread.
 */
fun scanBeatmapDirectoryBlocking(forceUpdateDirectories: Collection<String>?) = runBlocking {
    LibraryManager.scanDirectory(forceUpdateDirectories)
}
