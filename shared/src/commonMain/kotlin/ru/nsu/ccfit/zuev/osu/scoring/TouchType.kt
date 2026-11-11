package ru.nsu.ccfit.zuev.osu.scoring

import kotlin.jvm.JvmStatic

enum class TouchType(val id: Byte) {
    DOWN(0.toByte()),
    MOVE(1.toByte()),
    UP(2.toByte());

    companion object {
        private val byID = HashMap<Byte, TouchType>()

        init {
            for (v in entries) {
                byID[v.id] = v
            }
        }

        @JvmStatic
        fun getByID(id: Byte): TouchType? {
            return byID[id]
        }
    }
}
