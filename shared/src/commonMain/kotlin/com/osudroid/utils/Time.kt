package com.osudroid.utils

import com.reco1l.verktex.localization.formatString
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime

/**
 * The current time, in milliseconds since the Unix epoch, as `System.currentTimeMillis` returns it.
 *
 * It's the wall clock, which can jump when the device's time changes. To measure how long something
 * takes, use [kotlin.time.TimeSource.Monotonic] instead.
 */
fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

/**
 * Formats the time of day of [epochMillis] in the device's time zone, as `HH:mm:ss`.
 */
fun formatTimeOfDay(epochMillis: Long): String =
    Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault()).time.format(timeOfDayFormat)

private val timeOfDayFormat = LocalTime.Format {
    hour(); char(':'); minute(); char(':'); second()
}

/**
 * Formats the date and time of [epochMillis] in the device's time zone, as `yyyy/MM/dd HH:mm:ss`.
 */
fun formatDateTime(epochMillis: Long): String =
    Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault()).format(dateTimeFormat)

private val dateTimeFormat = LocalDateTime.Format {
    year(); char('/'); monthNumber(); char('/'); day()
    char(' ')
    hour(); char(':'); minute(); char(':'); second()
}

/**
 * Formats a duration of [millis] as `mm:ss`, or `HH:mm:ss` if it's longer than an hour.
 */
fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = totalSeconds / 60 % 60
    val seconds = totalSeconds % 60

    return if (millis > 3600 * 1000) formatString("%02d:%02d:%02d", hours, minutes, seconds)
    else formatString("%02d:%02d", minutes, seconds)
}
