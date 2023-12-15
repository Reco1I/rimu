package com.reco1l.framework.kotlin

import java.text.SimpleDateFormat
import java.util.TimeZone


/**
 * This converts the string to boolean allowing numeric booleans (`1` for `true` and `0` for `false`).
 */
fun String.toBooleanOrNull(): Boolean?
{
    if (length == 1 && (get(0) == '0' || get(0) == '1'))
        return get(0) == '1'

    return try { toBoolean() } catch (_: Exception) { null }
}


fun dateFormatFor(ms: Long): SimpleDateFormat
{
    @Suppress("SimpleDateFormat")
    return SimpleDateFormat(if (ms > 3600 * 1000) "HH:mm:ss" else "mm:ss").apply {

        timeZone = TimeZone.getTimeZone("GMT+0")
    }
}
