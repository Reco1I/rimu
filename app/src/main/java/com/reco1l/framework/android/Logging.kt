package com.reco1l.framework.android

import android.util.Log
import java.lang.Exception
import kotlin.reflect.KClass

/**
 * As specified on [Log.i]
 */
infix fun String.logI(message: String) = Log.i(this, message)

infix fun KClass<*>.logI(message: String) = Log.i(simpleName, message)

/**
 * As specified on [Log.w]
 */
infix fun String.logW(message: String) = Log.w(this, message)

infix fun KClass<*>.logW(message: String) = Log.w(simpleName, message)

/**
 * As specified on [Log.e]
 */
infix fun String.logE(message: String) = Log.e(this, message)

infix fun String.logE(exception: Exception) = Log.e(this, "", exception)


infix fun KClass<*>.logE(message: String) = Log.e(simpleName, message)

infix fun KClass<*>.logE(pair: Pair<String, Throwable>) = Log.e(simpleName, pair.first, pair.second)