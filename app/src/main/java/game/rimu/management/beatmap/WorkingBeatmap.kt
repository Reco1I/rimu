package game.rimu.management.beatmap

import androidx.core.animation.doOnEnd
import com.reco1l.basskt.stream.AudioStream
import com.reco1l.framework.animation.animateTo
import com.reco1l.framework.forEachObserver
import com.rian.osu.beatmap.BeatmapData
import com.rian.osu.beatmap.timings.ControlPoint
import game.rimu.IWithContext
import game.rimu.MainContext
import game.rimu.constants.RimuSetting.MUSIC_VOLUME
import game.rimu.data.Beatmap
import game.rimu.data.ControlPointType.DIFFICULTY
import game.rimu.data.ControlPointType.TIMING
import game.rimu.data.asset.ExternalAssetBundle
import game.rimu.management.time.ControlPointCursor
import game.rimu.management.time.IClockObserver

class WorkingBeatmap(override val ctx: MainContext, val source: Beatmap) :
    IWithContext,
    IClockObserver,
    (ControlPointCursor<out ControlPoint>) -> Unit
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
        val key = data.general.audioFilename.substringBeforeLast('.')

        it.source = assets.getAssetPath(key)
    }


    /**
     * The cursors for every control point type.
     */
    private val cursors = data.controlPoints.let {

        mutableMapOf(
            // Cursor for timing control points
            TIMING to ControlPointCursor(it.timing, this),

            // Cursor for difficulty control points
            DIFFICULTY to ControlPointCursor(it.difficulty, this)
        )
    }

    private val onVolumeChange = { value: Any? -> stream.volume = value as Float }


    init
    {
        ctx.settings.bindObserver(MUSIC_VOLUME, observer = onVolumeChange)
    }


    private fun onDecodeSource(withHitObjects: Boolean): BeatmapData
    {
        return ctx.beatmaps.decoder.decode(source.toFile(ctx), withHitObjects)
    }


    /**
     * Called by the [BeatmapManager] when this beatmap is no longer the current one.
     */
    internal fun onRelease()
    {
        ctx.settings.unbindObserver(MUSIC_VOLUME, observer = onVolumeChange)

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

        stream.play(restart)
        stream.bufferLength = if (ctx.engine.isRunning) 0.1f else 0.5f

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

    override fun onClockUpdate(msElapsedTime: Long, msDeltaTime: Long)
    {
        cursors.values.forEach { it.onClockUpdate(msElapsedTime, msDeltaTime) }
    }

    override fun invoke(cursor: ControlPointCursor<out ControlPoint>)
    {

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