package game.rimu.management.beatmap

import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import com.reco1l.bassbinding.stream.AudioStream
import com.reco1l.framework.animation.animateTo
import com.reco1l.framework.management.forEachObserver
import com.rian.osu.beatmap.BeatmapData
import com.rian.osu.beatmap.timings.ControlPoint
import game.rimu.android.IWithContext
import game.rimu.android.RimuContext
import game.rimu.constants.RimuSetting.MUSIC_VOLUME
import game.rimu.data.Beatmap
import game.rimu.data.ControlPointType.DIFFICULTY
import game.rimu.data.ControlPointType.TIMING
import game.rimu.data.asset.ExternalAssetBundle
import game.rimu.management.time.ControlPointCursor
import game.rimu.management.time.IClockObserver

class WorkingBeatmap(override val ctx: RimuContext, val source: Beatmap) :
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


    private fun onDecodeSource(withHitObjects: Boolean): BeatmapData
    {
        return ctx.beatmaps.decoder.decode(source.toFile(ctx), withHitObjects)
    }


    /**
     * Called by the [BeatmapManager] when this beatmap is no longer the current one.
     */
    internal fun onRelease()
    {
        stream::volume.animateTo(0f, 300).doOnEnd {
            stream.free()
        }
    }

    /**
     * Play the beatmap.
     */
    fun play()
    {
        stream.volume = 0f

        stream::volume.animateTo(ctx.settings[MUSIC_VOLUME] ?: 1f, 300).doOnStart {
            stream.play()

            if (ctx.beatmaps.current == this)
                ctx.beatmaps.forEachObserver { it.onMusicPlay() }
        }
    }

    /**
     * Pause the beatmap.
     *
     * @param instantly If `true` the pause animation will be skipped.
     */
    fun pause(instantly: Boolean = false)
    {
        if (instantly)
        {
            stream.pause()
            return
        }

        stream::volume.animateTo(0f, 300).doOnEnd {
            stream.pause()

            if (ctx.beatmaps.current == this)
                ctx.beatmaps.forEachObserver { it.onMusicPause() }
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