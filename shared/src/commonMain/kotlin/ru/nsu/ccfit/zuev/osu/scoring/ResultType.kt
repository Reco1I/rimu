package ru.nsu.ccfit.zuev.osu.scoring

enum class ResultType(val id: Byte) {
    HIT300(4.toByte()),
    HIT100(3.toByte()),
    HIT50(2.toByte()),
    MISS(1.toByte());

    companion object {

        /**
         * The result with the [id] replays store it with, null for none (0).
         */
        fun fromId(id: Byte) = entries.firstOrNull { it.id == id }
    }
}
