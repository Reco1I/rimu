package com.osudroid.multiplayer

import com.reco1l.verktex.Platform
import com.osudroid.settings.GameFolders
import com.reco1l.verktex.Logger
import kotlin.concurrent.Volatile
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import okio.BufferedSink
import okio.Path.Companion.toPath
import okio.buffer

/**
 * Logger for multiplayer events.
 */
class MultiplayerLogger : AutoCloseable {
    // One at a time, so the entries are written in order.
    private val dispatcher = Dispatchers.Default.limitedParallelism(1)
    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    @Volatile
    private var isClosed = false

    private var writer: BufferedSink? = null

    /**
     * Initializes the logger by creating the log file and writing the initial log entry.
     */
    fun init() {
        val file = "${GameFolders.defaultCorePath}/Log".toPath() / "multi_log.txt"
        file.parent?.let { Platform.fileSystem.createDirectories(it) }

        // Replaces the log of the previous session.
        writer = Platform.fileSystem.sink(file).buffer()

        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        write("[${now.format(startFormat)}] Client ${Platform.application.versionName} started.")
    }

    // The time of an entry, in UTC.
    private fun timestamp() = Clock.System.now().toLocalDateTime(TimeZone.UTC).time.format(timestampFormat)

    /**
     * Logs a message to the log file.
     *
     * @param text The message to log.
     */
    fun log(text: String) {
        if (isClosed) {
            return
        }

        val timestamp = timestamp()

        Logger.i("Multiplayer", text)
        write("\n[$timestamp] $text")
    }

    /**
     * Logs a [Throwable] to the log file.
     *
     * @param e The [Throwable] to log.
     */
    fun log(e: Throwable) {
        if (isClosed) {
            return
        }

        val time = timestamp()
        val stacktrace = e.stackTraceToString()

        Logger.e("Multiplayer", "An exception has been thrown.", e)
        write("\n[$time] EXCEPTION: ${e::class.simpleName}\n$stacktrace")
    }

    /**
     * Flushes the underlying buffer.
     */
    fun flush() {
        if (isClosed) {
            return
        }

        scope.launch {
            try {
                writer?.flush()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun write(str: String) {
        if (isClosed) {
            return
        }

        scope.launch {
            try {
                writer?.writeUtf8(str)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    override fun close() {
        if (isClosed) {
            return
        }

        isClosed = true

        scope.launch {
            try {
                writer?.flush()
                writer?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private companion object {
        // yyyy/MM/dd hh:mm:ss, with a 12-hour clock.
        val startFormat = LocalDateTime.Format {
            year(); char('/'); monthNumber(); char('/'); day()
            char(' ')
            amPmHour(); char(':'); minute(); char(':'); second()
        }

        // HH:mm:ss
        val timestampFormat = LocalTime.Format {
            hour(); char(':'); minute(); char(':'); second()
        }
    }
}
