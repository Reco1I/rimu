package com.reco1l.framework.android

import android.content.Context


inline fun <reified T : Any> Context.getSystemService(): T = getSystemService(T::class.java)