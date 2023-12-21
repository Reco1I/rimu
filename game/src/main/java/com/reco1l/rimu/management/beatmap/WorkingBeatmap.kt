package com.reco1l.rimu.management.beatmap

import androidx.core.animation.doOnEnd
import com.reco1l.basskt.stream.AudioStream
import com.reco1l.toolkt.animation.animateTo
import com.reco1l.toolkt.forEachObserver
import com.rian.osu.beatmap.BeatmapData
import com.rian.osu.beatmap.timings.ControlPoint
import com.reco1l.rimu.IWithContext
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.constants.RimuSetting.MUSIC_VOLUME
import com.reco1l.rimu.data.Beatmap
import com.reco1l.rimu.management.time.ControlPointType.DIFFICULTY
import com.reco1l.rimu.management.time.ControlPointType.TIMING
import com.reco1l.rimu.data.asset.ExternalAssetBundle
import com.reco1l.rimu.mainThread
import com.reco1l.rimu.management.time.ControlPointCursor
import com.reco1l.rimu.management.time.GameClock
import com.reco1l.rimu.ui.layouts.DebugOverlay
import com.reco1l.rimu.updateThread
import com.reco1l.toolkt.roundBy
import com.rian.osu.beatmap.timings.TimingControlPoint

class WorkingBeatmap(override val ctx: MainContext, val source: Beatmap) : IWithContext
{

    /**
     * Determine if the working beatmap is in gameplay mode.
     *
     * If this is set to `true` the beatmap hit objets will be load.
     */
    var isInGameplay = false
        set(value)
        {
            // Decoding hit objects only if they weren't yet.
            if (value && data.hitObjects == null)
                data = onDecodeSource(true)

            field = value
        }

    /**
     * The current decoded data from this beatmap, it can never be null.
     */
    var data = onDecodeSource(false)
        private set


    /**
     * The current assets from the beatmap.
     */
    val assets = ExternalAssetBundle(ctx, source.parent)

    /**
     * The audio stream for this beatmap.
     */
    val stream = AudioStream().also {

        // Getting the audio file key, usually this contains the extension so we're removing it first.
        val key = source.audio.substringBeforeLast('.')

        it.source = assets.getAssetPath(key)
    }

    /**
     * The game clock that matches the audio stream time.
     */
    val clock = GameClock(ctx, stream)


    /**
     * The cursors for every control point type.
     */
    private val cursors = data.controlPoints.let {

        mutableMapOf(
            // Cursor for timing control points
            TIMING to ControlPointCursor(it.timing, ::onTimingControlPointChange),

            // Cursor for difficulty control points
            DIFFICULTY to ControlPointCursor(it.difficulty) { _, _, _ -> }
        )
    }

    private val onVolumeChange = { value: Any? -> stream.volume = value as Float }


    init
    {
        ctx.settings.bindObserver(MUSIC_VOLUME, observer = onVolumeChange)

        // Binding the cursors to the clock.
        cursors.values.forEach { clock.bindObserver(observer = it) }
    }


    private fun onDecodeSource(withHitObjects: Boolean): BeatmapData
    {
        return BeatmapDecoder().decode(source.toFile(ctx), withHitObjects)
    }


    /**
     * Called by the [BeatmapManager] when this beatmap is no longer the current one.
     */
    internal fun onRelease()
    {
        ctx.settings.unbindObserver(MUSIC_VOLUME, observer = onVolumeChange)

        updateThread {
            ctx.engine.unregisterUpdateHandler(clock)
        }

        mainThread {
            stream::volume.animateTo(0f, 300).doOnEnd {
                stream.free()
            }
        }
    }

    /**
     * Play the beatmap.
     */
    fun play(restart: Boolean = false, withAnimation: Boolean = true)
    {
        stream.volume = 0f
        stream.bufferLength = if (ctx.engine.isRunning) 0.1f else 0.5f
        stream.play(restart)

        updateThread {
            ctx.engine.registerUpdateHandler(clock)
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

    private fun onTimingControlPointChange(
        previous: TimingControlPoint,
        current: TimingControlPoint,
        next: TimingControlPoint
    )
    {
        ctx.layouts[DebugOverlay::class].setSection("TimingPoint", """
            current_start_time: ${(current.time / 1000.0).roundBy(3)}s
            current_beat_length: ${current.msPerBeat.roundBy(3)}s (BPM: ${current.BPM.roundBy(3)})
            current_time_signature: 1/${current.timeSignature}
            next_start_time: ${(next.time / 1000.0).roundBy(3)}s
        """.trimIndent())
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