package com.reco1l.verktex.time

/**
 * The clock of a song: it advances smoothly between the updates of the audio track, can run while the track
 * can't (such as before its start) and applies the offsets set by the user.
 *
 * @param applyOffsets Whether to apply the user offsets.
 * @param requireDecoupling Whether this clock can run while the track isn't.
 * @param source The audio track, or null to use one following real time.
 */
class SongClock(
    applyOffsets: Boolean,
    requireDecoupling: Boolean = false,
    source: Clock? = null
) : FrameClock(), IFrameProcessableClock, ISourceChangeableClock, IAdjustableClock {

    private val decoupledTrack = DecouplingFrameClock(source).apply { allowDecoupling = requireDecoupling }
    private val interpolatedTrack = InterpolatingFrameClock(decoupledTrack).apply { driftRecoveryHalfLife = 0.08f }

    private val globalOffsetClock: OffsetCorrectionFrameClock?
    private val beatmapOffsetClock: OffsetFrameClock?

    private val finalClock: FrameClock


    init {
        if (applyOffsets) {
            val global = OffsetCorrectionFrameClock(interpolatedTrack)
            val beatmap = OffsetFrameClock(global)

            globalOffsetClock = global
            beatmapOffsetClock = beatmap
            finalClock = beatmap
        } else {
            globalOffsetClock = null
            beatmapOffsetClock = null
            finalClock = interpolatedTrack
        }
    }


    /**
     * The offset set by the user for all songs, in seconds of real time.
     */
    var userGlobalOffset
        get() = globalOffsetClock?.offset ?: 0f
        set(value) {
            globalOffsetClock?.offset = value
        }

    /**
     * The offset set by the user for this song, in seconds.
     */
    var userBeatmapOffset
        get() = beatmapOffsetClock?.offset ?: 0f
        set(value) {
            beatmapOffsetClock?.offset = value
        }

    /**
     * The total offset being applied to the time of the track.
     */
    val totalAppliedOffset
        get() = (globalOffsetClock?.rateAdjustedOffset ?: 0f) + (beatmapOffsetClock?.offset ?: 0f)

    override val time
        get() = finalClock.time

    override val lastFrameTime
        get() = finalClock.lastFrameTime

    override val elapsedFrameTime
        get() = finalClock.elapsedFrameTime

    override val isRunning
        get() = finalClock.isRunning

    override val source
        get() = decoupledTrack.source

    override var rate
        get() = decoupledTrack.rate
        set(value) {
            decoupledTrack.rate = value
        }


    override fun processFrame() {
        (finalClock as IFrameProcessableClock).processFrame()
    }

    override fun reset() {
        decoupledTrack.reset()
        processFrame()
    }

    override fun start() {
        decoupledTrack.start()
        processFrame()
    }

    override fun pause() {
        decoupledTrack.pause()
        processFrame()
    }

    override fun seek(to: Float): Boolean {
        val success = decoupledTrack.seek(to - totalAppliedOffset)
        processFrame()
        return success
    }

    override fun resetSpeedAdjustments() = decoupledTrack.resetSpeedAdjustments()

    override fun changeSource(source: Clock?) = decoupledTrack.changeSource(source)
}
