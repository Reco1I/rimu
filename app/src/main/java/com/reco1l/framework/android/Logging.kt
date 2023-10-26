package com.reco1l.framework.android

import android.util.Log
import kotlin.reflect.KClass


object Logger
{

    fun i(clazz: KClass<*>, message: String) = Log.i(clazz.simpleName, message)

    fun w(clazz: KClass<*>, message: String) = Log.w(clazz.simpleName, message)

    fun e(clazz: KClass<*>, message: String) = Log.e(clazz.simpleName, message)

    fun e(clazz: KClass<*>, message: String, throwable: Throwable) = Log.e(clazz.simpleName, message, throwable)

}