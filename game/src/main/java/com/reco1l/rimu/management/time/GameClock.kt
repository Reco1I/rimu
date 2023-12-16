package com.reco1l.rimu.management.time

import android.util.Log
import com.reco1l.basskt.AudioState.PLAYING
import com.reco1l.basskt.stream.AudioStream
import com.reco1l.rimu.IWithContext
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.ui.layouts.DebugOverlay
import com.reco1l.toolkt.IObservable
import com.reco1l.toolkt.forEachObserver
import org.andengine.engine.handler.IUpdateHandler
import kotlin.math.abs
import kotlin.math.absoluteValue
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
    var msElapsedTime: Long = 0

    /**
     * The current clock rate defined by the audio playback speed.
     */
    var rate: Float = 1f
        private set


    override fun onUpdate(sEngineDelta: Float)
    {
        if (audioStream.state != PLAYING)
            return

        // Current audio time in milliseconds.
        val msAudioElapsed = audioStream.position

        // Computing the difference between the engine elapsed time and the audio elapsed time, this
        // is used to match elapsed time and synchronize both times.
        val msDifference = msAudioElapsed - msElapsedTime + sEngineDelta * 1000

        // Determining if the difference is significant enough to seek to the target time, this can
        // happen if for example user seeks the song.
        val isSeeking = abs(msDifference) >= 1000

        // By default when seeking the rate will be forced to 4x.
        rate = if (isSeeking) 6f else audioStream.speed

        var msDeltaTime = (sEngineDelta * 1000 * rate).toLong()

        // In this case the clock is going backwards, we need to reverse the delta time.
        if (isSeeking && msDifference < 0)
            msDeltaTime = -msDeltaTime

        msElapsedTime += msDeltaTime

        ctx.layouts[DebugOverlay::class].setSection("Clock", """
            clock_elapsed_time: ${msElapsedTime}ms
            audio_elapsed_time: ${msAudioElapsed}ms
            delta_time: ${msDeltaTime}ms
            difference: ${msDifference.toInt()}ms
            rate: ${rate}x
        """.trimIndent())

        forEachObserver { it.onClockUpdate(msElapsedTime, msDeltaTime) }
    }

    override fun reset()
    {
        Log.v(javaClass.simpleName, "Game clock resetted.")

        msElapsedTime = 0
        rate = 1f
    }
}

