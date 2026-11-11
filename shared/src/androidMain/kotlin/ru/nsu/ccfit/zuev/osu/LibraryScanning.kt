@file:JvmName("LibraryScanning")

package ru.nsu.ccfit.zuev.osu

import com.osudroid.beatmaps.LibraryImport
import kotlinx.coroutines.runBlocking

/**
 * Imports the archives waiting and loads the library as [LibraryImport.loadLibrary] does, blocking the
 * calling thread until it's done, for `MainActivity`, which loads the library on a background thread.
 */
fun loadLibraryBlocking(openedFile: String?) = runBlocking {
    LibraryImport.loadLibrary(openedFile)
}
