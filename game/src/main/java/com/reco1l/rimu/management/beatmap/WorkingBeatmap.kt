package com.reco1l.rimu.management.beatmap

import androidx.core.animation.doOnEnd
import com.reco1l.basskt.stream.AudioStream
import com.reco1l.toolkt.animation.animateTo
import com.reco1l.toolkt.forEachObserver
import com.reco1l.rimu.IWithContext
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.constants.RimuSetting.MUSIC_VOLUME
import com.reco1l.rimu.data.Beatmap
import com.reco1l.rimu.data.asset.ExternalAssetBundle
import com.reco1l.rimu.mainThread
import com.reco1l.rimu.management.time.GameClock
import com.reco1l.rimu.management.time.IClockObserver
import com.reco1l.rimu.ui.layouts.DebugOverlay
import com.reco1l.rimu.updateThread
import com.reco1l.toolkt.roundBy
import com.rian.osu.beatmap.parser.BeatmapDecoder
import com.rian.osu.beatmap.timings.ControlPoint
import com.rian.osu.beatmap.timings.TimingControlPoint

open class WorkingBeatmap(final override val ctx: MainContext, val source: Beatmap) :
    IWithContext,
    IClockObserver
{

    /**
     * The current assets from the beatmap.
     */
    val assets = ExternalAssetBundle(ctx, source.parent)

    /**
     * The audio stream for this beatmap.
     */
    val stream = AudioStream().also {

        // Getting the audio file key, usually this contains the extension so we're removing it first.
        it.source = assets.getAssetPath(source.audio.substringBeforeLast('.'))
    }

    /**
     * The game clock that matches the audio stream time.
     */
    val clock = GameClock(ctx, stream)


    /**
     * The current decoded data from this beatmap.
     */
    open var data = BeatmapDecoder().decode(source.toFile(ctx), false)

    /**
     * The cursors for every control point type.
     */
    open var controlPointTimeline = ControlPointTimeline(data.controlPoints, ::onControlPointChange)


    private val onVolumeChange = { value: Any? -> stream.volume = value as Float }


    init
    {
        ctx.settings.bindObserver(MUSIC_VOLUME, observer = onVolumeChange)

        clock.bindObserver(observer = this)
    }


    // Actions

    /**
     * Called by the [BeatmapManager] when this beatmap is no longer the current one.
     */
    fun onRelease()
    {
        ctx.settings.unbindObserver(MUSIC_VOLUME, observer = onVolumeChange)

        updateThread {
            ctx.engine.unregisterUpdateHandler(clock)
        }

        mainThread {
            stream::volume.animateTo(0f, 300).doOnEnd { stream.free() }
        }
    }

    override fun onClockUpdate(sElapsedTime: Double, sDeltaTime: Float)
    {
        controlPointTimeline.onClockUpdate(sElapsedTime, sDeltaTime)
    }


    // Music control

    /**
     * Play the beatmap.
     */
    fun play(restart: Boolean = false, withAnimation: Boolean = true)
    {
        stream.volume = 0f
        stream.bufferLength = if (ctx.engine.isRunning) 0.1f else 0.5f
        stream.play(restart)

        updateThread {
            //ctx.engine.registerUpdateHandler(clock)
        }

        if (ctx.beatmaps.current == this)
            ctx.beatmaps.forEachObserver { it.onMusicPlay() }

        if (!withAnimation)
        {
            stream.volume = ctx.settings[MUSIC_VOLUME]
            return
        }

        mainThread { stream::volume.animateTo(ctx.settings[MUSIC_VOLUME], 300) }
    }

    /**
     * Pause the beatmap.
     *
     * @param withAnimation If `true` the pause animation will be skipped.
     */
    fun pause(withAnimation: Boolean = true)
    {
        if (!withAnimation)
        {
            stream.pause()
            return
        }

        mainThread {
            stream::volume.animateTo(0f, 200).doOnEnd {
                stream.pause()

                if (ctx.beatmaps.current == this)
                    ctx.beatmaps.forEachObserver { it.onMusicPause() }
            }
        }
    }


    // Control points

    protected open fun onControlPointChange(
        previous: ControlPoint,
        current: ControlPoint,
        next: ControlPoint
    )
    {
        if (current is TimingControlPoint)
        {
            ctx.layouts[DebugOverlay::class].setSection("TimingPoint", """
                current_start_time: ${(current.time / 1000.0).roundBy(3)}s
                current_beat_length: ${current.msPerBeat.roundBy(3)}s (BPM: ${current.BPM.roundBy(3)})
                current_time_signature: 1/${current.timeSignature}
                next_start_time: ${(next.time / 1000.0).roundBy(3)}s
            """.trimIndent())
        }
    }
}


interface IBeatmapObserver
{
    fun onMusicChange(beatmap: WorkingBeatmap?) = Unit

    fun onMusicPause() = Unit

    fun onMusicPlay() = Unit

    fun onMusicStop() = Unit

    fun onMusicEnd() = Unit
}