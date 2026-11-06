package com.reco1l.verktex.io

import okio.FileSystem

/**
 * The file system of the device, see [com.reco1l.verktex.Platform.fileSystem].
 */
internal expect val systemFileSystem: FileSystem
