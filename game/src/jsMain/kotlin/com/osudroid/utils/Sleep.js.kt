package com.osudroid.utils

// Only compiled to check the common code, see build.gradle.kts. JavaScript can't block its thread.
actual fun sleepBlocking(millis: Long) = Unit
