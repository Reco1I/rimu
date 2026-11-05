package ru.nsu.ccfit.zuev.osu

import kotlin.jvm.JvmField

object Constants {
    const val MAP_WIDTH = 512
    const val MAP_HEIGHT = 384
    const val MAP_ACTUAL_WIDTH_OLD = 820
    const val MAP_ACTUAL_HEIGHT_OLD = 570
    @JvmField
    val MAP_ACTUAL_HEIGHT = (Config.getRES_HEIGHT() * 0.8f).toInt()
    @JvmField
    val MAP_ACTUAL_WIDTH = MAP_ACTUAL_HEIGHT / 3 * 4
}
