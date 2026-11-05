package com.osudroid.utils

import okio.FileSystem

/**
 * The file system of the platform the game runs on.
 */
expect val SystemFileSystem: FileSystem
