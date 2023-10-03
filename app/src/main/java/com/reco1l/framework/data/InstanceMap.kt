package com.reco1l.framework.data

import kotlin.reflect.KClass


/**
 * Store instances from a class inheritors as singletons.
 */
fun <T : Any>instanceMapOf() = HashMap<KClass<out T>, T>()

