package game.rimu.management.time

import com.reco1l.basskt.AudioState.PLAYING
import com.reco1l.basskt.stream.AudioStream
import com.reco1l.framework.IObservable
import com.reco1l.framework.forEachObserver
import org.andengine.engine.handler.IUpdateHandler
import kotlin.math.abs

/**
 * Despite a normal [IUpdateHandler] it listens to time modifications in an [AudioStream].
 */
class GameClock(var audioStream: AudioStream) :
    IObservable<IClockObserver>,
    IUpdateHandler
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


    private var msPreviousAudioTime: Long = 0


    override fun onUpdate(sEngineDelta: Float)
    {
        if (audioStream.state != PLAYING)
            return

        // Current audio time in milliseconds.
        val msAudioElapsed = audioStream.position

        // Engine time elapsed in milliseconds.
        val msEngineDelta = (sEngineDelta * 1000).toLong()

        // Audio time elapsed in milliseconds.
        val msAudioDelta = msAudioElapsed - msPreviousAudioTime
        msPreviousAudioTime = msAudioElapsed

        // Computing the difference between the engine elapsed time and the audio elapsed time, this
        // is used to match elapsed time and synchronize both times.
        val msDifference = msAudioDelta - msEngineDelta

        // Computing clock rate at current frame with the audio playback speed as base rate.
        rate = audioStream.speed

        // Here we ignoring unimportant differences lower than 5ms.
        if (abs(msDifference) > 5)
            rate += msDifference / msEngineDelta.toFloat()

        // Computing delta time in milliseconds.
        val msDeltaTime = (msEngineDelta * rate).toLong()

        // Computing elapsed time since clock start.
        msElapsedTime += msDeltaTime

        // Notifying clock update.
        forEachObserver { it.onClockUpdate(msElapsedTime, msDeltaTime) }
    }

    override fun reset()
    {
        msElapsedTime = 0
        msPreviousAudioTime = 0
        rate = 1f
    }
}