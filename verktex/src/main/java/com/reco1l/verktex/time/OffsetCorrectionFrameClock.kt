package com.reco1l.verktex.time

/**
 * An [OffsetFrameClock] whose offset is in real time, so it doesn't change with the rate of the source.
 */
class OffsetCorrectionFrameClock(source: Clock?) : OffsetFrameClock(source) {

    private var realTimeOffset = 0f

    override var offset
        get() = realTimeOffset
        set(value) {
            if (realTimeOffset == value) {
                return
            }

            realTimeOffset = value
            updateOffset()
        }

    /**
     * The offset actually being applied to the time of the source.
     */
    val rateAdjustedOffset
        get() = super.offset

    override fun processFrame() {
        super.processFrame()
        updateOffset()
    }

    private fun updateOffset() {
        super.offset = offset * rate
    }
}
