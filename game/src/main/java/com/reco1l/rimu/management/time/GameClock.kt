package com.reco1l.rimu.management.time

import android.util.Log
import com.reco1l.basskt.AudioState.PLAYING
import com.reco1l.basskt.stream.AudioStream
import com.reco1l.rimu.IWithContext
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.ui.layouts.DebugOverlay
import com.reco1l.toolkt.IObservable
import com.reco1l.toolkt.forEachObserver
import com.reco1l.toolkt.roundBy
import org.andengine.engine.handler.IUpdateHandler
import kotlin.math.abs

/**
 * Despite a normal [IUpdateHandler] it listens to time modifications in an [AudioStream].
 */
class GameClock(override val ctx: MainContext, var audioStream: AudioStream) :
    IObservable<IClockObserver>,
    IUpdateHandler,
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
        val isSeeking = abs(sTimeDifference) > 1.0

        // By default when seeking the rate will be forced to 6x.
        // When not seeking we compensate multiplying by the difference ratio.
        rate = if (isSeeking) 6f else {

            // 10 ms tolerance of difference to avoid unexpected jumps.
            if (sTimeDifference < sEngineDeltaTime)
                audioStream.speed + (abs(sTimeDifference) / sEngineDeltaTime).toFloat()
            else
                audioStream.speed

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
            rate: ${rate.roundBy(3)}x
            audio_elapsed_time: ${sAudioElapsedTime}s
            clock_elapsed_time: ${msElapsedTime.roundBy(3)}s
            clock_delta_time: ${sDeltaTime.roundBy(3)}s
            time_difference: ${sTimeDifference.roundBy(3)}s
        """.trimIndent())
    }

    override fun reset()
    {
        Log.v(javaClass.simpleName, "Game clock resetted.")

        msElapsedTime = 0.0
        rate = 1f
    }
}

