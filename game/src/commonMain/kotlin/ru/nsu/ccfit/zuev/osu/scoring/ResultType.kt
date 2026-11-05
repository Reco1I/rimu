package ru.nsu.ccfit.zuev.osu.scoring

enum class ResultType(val id: Byte) {
    HIT300(4.toByte()),
    HIT100(3.toByte()),
    HIT50(2.toByte()),
    MISS(1.toByte())
}
