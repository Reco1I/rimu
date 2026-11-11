package com.osudroid.utils

import com.reco1l.verktex.localization.formatString

/**
 * Formats [value] with its thousands grouped by commas, e.g. `1,234,567`, as
 * `NumberFormat.getNumberInstance(Locale.US)` does.
 */
fun formatNumber(value: Long): String = formatString("%,d", value)

/**
 * Formats [value] with its thousands grouped by commas, e.g. `1,234,567`.
 */
fun formatNumber(value: Int): String = formatNumber(value.toLong())
