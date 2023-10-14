package com.reco1l.framework.lang

import java.text.SimpleDateFormat
import java.util.TimeZone


fun intOf(value: Float) = value.toInt()

fun intOf(value: Double) = value.toInt()

fun longOf(value: Float) = value.toLong()

fun longOf(value: Double) = value.toLong()


fun dateFormatFor(ms: Long): SimpleDateFormat
{
    @Suppress("SimpleDateFormat")
    return SimpleDateFormat(if (ms > 3600 * 1000) "HH:mm:ss" else "mm:ss").apply {

        timeZone = TimeZone.getTimeZone("GMT+0")
    }
}
