package ru.nsu.ccfit.zuev.osu.scoring

import okio.Source

// Only compiled to check the common code, see build.gradle.kts. Reading and writing Java's object
// serialization outside the JVM is still to be written.

actual interface ReplaySerializable

actual fun openReplayOutput(path: String): ReplayOutput = throw UnsupportedOperationException("Replays can't be written on this platform yet.")

actual fun openReplayInput(source: Source): ReplayInput = throw UnsupportedOperationException("Replays can't be read on this platform yet.")
