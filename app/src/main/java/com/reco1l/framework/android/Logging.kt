package com.reco1l.framework.android

import android.util.Log

/**
 * As specified on [Log.i]
 */
fun String.logI(tag: Any) = Log.i(tag.toString(), this)

/**
 * As specified on [Log.w]
 */
fun String.logW(tag: Any) = Log.w(tag.toString(), this)

/**
 * As specified on [Log.e]
 */
fun String.logE(tag: Any) = Log.e(tag.toString(), this)

/**
 * As specified on [Log.e]
 */
fun String.logE(e: Throwable, tag: Any) = Log.e(tag.toString(), this, e)

