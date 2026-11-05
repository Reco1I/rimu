package com.osudroid.resources

import kotlin.jvm.JvmInline

/**
 * A localized string, by the name of its resource. See [Res.string].
 */
@JvmInline
value class StringResource(val name: String)

/**
 * A localized array of strings, by the name of its resource. See [Res.array].
 */
@JvmInline
value class StringArrayResource(val name: String)
