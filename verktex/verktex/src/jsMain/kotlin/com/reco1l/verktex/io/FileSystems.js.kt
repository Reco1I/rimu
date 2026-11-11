package com.reco1l.verktex.io

import okio.FileSystem
import okio.NodeJsFileSystem

// Only compiled to check the common code, see build.gradle.kts.
internal actual val systemFileSystem: FileSystem = NodeJsFileSystem
