package com.reco1l.rimu.management.time

import com.reco1l.basskt.AudioState.PLAYING
import com.reco1l.basskt.stream.AudioStream
import com.reco1l.rimu.IWithContext
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.UpdateHandler
import com.reco1l.rimu.ui.layouts.DebugOverlay
import com.reco1l.toolkt.IObservable
import com.reco1l.toolkt.forEachObserver
import com.reco1l.toolkt.roundBy
import kotlin.math.abs

/**
 * Despite a normal [UpdateHandler] it listens to time modifications in an [AudioStream].
 */
class GameClock(override val ctx: MainContext, var audioStream: AudioStream) :
    IObservable<IClockObserver>,
    UpdateHandler,
    IWithContext
{

    override val observers = mutableListOf<IClockObserver>()


    /**
     * The total elapsed time since clock start.
     */
    var msElapsedTime: Double = 0.0

    /**
     * The current clock rate defined by the audio playback speed.
     */
    var rate: Float = 1f
        private set

    /**
     * Determines if the clock is seeking to a specific time.
     */
    var isSeeking: Boolean = false
        private set


    override fun onUpdate(sEngineDeltaTime: Float)
    {
        if (audioStream.state != PLAYING)
            return

        val sAudioElapsedTime = audioStream.position / 1000.0

        // Computing the difference between the engine elapsed time and the audio elapsed time, this
        // is used to match elapsed time and synchronize both times.
        val sTimeDifference = msElapsedTime + sEngineDeltaTime - sAudioElapsedTime

        // Determining if the difference is significant enough to seek to the target time, this can
        // happen if for example user seeks the song.
        // sEngineDeltaTime * 1.15f is used to avoid unexpected jumps due to frame time variations.
        if (!isSeeking)
            isSeeking = abs(sTimeDifference) > 1.0 + ctx.engine.expectedFrameTime
        else if (sTimeDifference >= 0.0 && sTimeDifference < 1.0)
            isSeeking = false

        // By default when seeking the rate will be forced to 6x.
        // When not seeking we compensate multiplying by the difference ratio.
        rate = if (isSeeking) 6f else {

            val differenceRatio = (abs(sTimeDifference) / 1.0).toFloat()

            audioStream.speed + when
            {
                // The audio is ahead of the engine, we need to slow down the clock.
                sTimeDifference < 0.0 -> differenceRatio
                // The audio is behind the engine, we need to speed up the clock.
                sTimeDifference > 1.0 -> -differenceRatio

                else -> 0f
            }

        }.coerceAtLeast(0f)

        var sDeltaTime = sEngineDeltaTime * rate

        // In this case the clock is going backwards, we need to reverse the delta time.
        if (isSeeking && sTimeDifference > 0)
            sDeltaTime = -sDeltaTime

        msElapsedTime += sDeltaTime

        forEachObserver { it.onClockUpdate(msElapsedTime, sDeltaTime) }

        updateDebugOverlaySection(sAudioElapsedTime, sTimeDifference, sDeltaTime)
    }


    private fun updateDebugOverlaySection(
        sAudioElapsedTime: Double,
        sTimeDifference: Double,
        sDeltaTime: Float
    )
    {
        ctx.layouts[DebugOverlay::class].setSection("Clock", """
            rate: ${rate.roundBy(1)}x
            isSeeking: $isSeeking
            audio_elapsed_time: ${sAudioElapsedTime}s
            clock_elapsed_time: ${msElapsedTime.roundBy(1)}s
            clock_delta_time: ${sDeltaTime.roundBy(3)}s
            time_difference: ${sTimeDifference.roundBy(1)}s
        """.trimIndent())
    }

}

/**
 * Observable for clock update calls.
 */
fun interface IClockObserver
{
    /**
     * Called by the clock with the elapsed time and the delta time both multiplied with the rate.
     */
    fun onClockUpdate(sElapsedTime: Double, sDeltaTime: Float)
}