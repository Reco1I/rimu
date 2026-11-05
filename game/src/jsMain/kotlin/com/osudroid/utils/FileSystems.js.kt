package com.osudroid.utils

import okio.FileSystem
import okio.NodeJsFileSystem

// Only compiled to check the common code, see build.gradle.kts.
actual val SystemFileSystem: FileSystem = NodeJsFileSystem
