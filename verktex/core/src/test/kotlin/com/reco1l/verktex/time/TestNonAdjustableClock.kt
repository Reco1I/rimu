package com.reco1l.verktex.time

class TestNonAdjustableClock : Clock() {
    override var time = 0f
    override var rate = 1f
    override val isRunning = true
}