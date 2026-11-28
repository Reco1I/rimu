package com.osudroid.ui.v2.game

import kotlin.math.PI
import com.osudroid.ui.v2.skin.SkinComponent
import com.osudroid.ui.v2.skin.SkinPieces
import com.reco1l.verktex.data.LayoutConstraints
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.data.ScaleType
import com.osudroid.beatmaps.hitobjects.HitObject
import com.osudroid.beatmaps.hitobjects.Spinner
import kotlin.jvm.JvmOverloads
import com.osudroid.utils.IPoolable
import com.osudroid.utils.SynchronizedPool
import com.osudroid.utils.updateThread
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.px
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.ui.AnimatedSprite
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.OnTransformFinished
import com.reco1l.verktex.ui.choreograph
import com.reco1l.verktex.ui.choreography
import com.reco1l.verktex.ui.Sprite
import com.reco1l.verktex.ui.container.Container
import ru.nsu.ccfit.zuev.osu.*
import kotlin.math.*
import kotlin.jvm.JvmStatic

object FollowPointConnection {


    private const val SPACING = 32

    private const val PREEMPT = 800

    /**
     * How long a follow point takes to fade in and to fade out, in seconds.
     */
    const val FADE_DURATION = 0.4f

    private val pool = SynchronizedPool<IPoolable>(20)

    /**
     * The follow points created since the pool was renewed, as it had none left.
     */
    var misses = 0
        private set

    private fun obtainFollowPoint(animationStartTime: Float): PoolableFollowPoint {
        val fp = pool.acquire() as PoolableFollowPoint? ?: PoolableFollowPoint().also { misses++ }
        fp.piece.animationStartTime = animationStartTime
        return fp
    }

    private val expire = OnTransformFinished { fp ->
        updateThread {
            val poolable = fp as IPoolable

            // `clearAll` may have already recycled this follow point, so we need to check if it's already recycled
            // before releasing it again.
            if (poolable.isRecycled) {
                return@updateThread
            }

            fp.removeSelf()
            pool.release(poolable)
        }
    }


    @JvmStatic
    fun clearAll(scene: Container) {
        for (i in scene.childCount - 1 downTo 0) {
            val child = scene[i]

            if (child is PoolableFollowPoint) {
                child.choreography?.clear()
                child.removeSelf()
                pool.release(child as IPoolable)
            }
        }
    }

    /**
     * Fills the pool with [size] follow points, with the pieces of the skin in use. Unless [keep], the ones it had are
     * dropped first, as the skin changed.
     */
    @JvmStatic
    @JvmOverloads
    fun renew(size: Int, keep: Boolean = false) {
        if (!keep) {
            pool.clear()
        }
        pool.ensureCapacity(size)
        misses = 0

        repeat(size - pool.count) {
            pool.release(PoolableFollowPoint())
        }
    }

    /**
     * Whether follow points go from [start] to [end], the object after it.
     */
    @JvmStatic
    fun connects(start: HitObject, end: HitObject) = start !is Spinner && end !is Spinner && !start.isLastInCombo

    /**
     * How many follow points go from [start] to [end], see [addConnection].
     */
    @JvmStatic
    fun pointCount(start: HitObject, end: HitObject): Int {
        val distance = end.gameplayStackedPosition.getDistance(start.gameplayStackedEndPosition).toInt()
        val first = (SPACING * 1.5f).toInt()
        val last = distance - SPACING

        return if (first < last) (last - first + SPACING - 1) / SPACING else 0
    }

    /**
     * The time, in seconds, the follow points from [start] start to fade in at: none of them does before. They're
     * added by then, which can be before [start] itself spawns: added later, they'd show halfway through their fade.
     */
    @JvmStatic
    fun fadeInStartTime(start: HitObject) = (start.endTime / 1000f).toFloat() - preempt(start)

    // Preempt time can go below 800ms. Normally, this is achieved via the DT mod which uniformly speeds up all animations game wide regardless of AR.
    // This uniform speedup is hard to match 1:1, however we can at least make AR>10 (via mods) feel good by extending the upper linear preempt function.
    // Note that this doesn't exactly match the AR>10 visuals as they're classically known, but it feels good.
    private fun preempt(start: HitObject) = PREEMPT * min(1.0, start.timePreempt / HitObject.PREEMPT_MIN).toFloat() / 1000f

    @JvmStatic
    fun addConnection(scene: Container, start: HitObject, end: HitObject) {

        // Reference: https://github.com/ppy/osu/blob/7bc8908ca9c026fed1d831eb6e58df7624a8d614/osu.Game.Rulesets.Osu/Objects/Drawables/Connections/FollowPointConnection.cs

        val scale = end.gameplayScale
        val startTime = (start.endTime / 1000f).toFloat()

        // In osu!pixels, as the playfield the follow points are added to.
        val startPosition = start.gameplayStackedEndPosition
        val endPosition = end.gameplayStackedPosition

        val distanceX = endPosition.x - startPosition.x
        val distanceY = endPosition.y - startPosition.y
        val rotation = atan2(distanceY, distanceX) * (180f / PI).toFloat()

        // osu!stable's HitObjectManager.TimeFadeIn, a constant: HitObject.timeFadeIn isn't used, as mods such as
        // Hidden change it.
        val endFadeInTime = FADE_DURATION
        val duration = (end.startTime - start.endTime).toFloat() / 1000f

        val preempt = preempt(start)

        // The gameplay stacked positions: the difficulty stacked ones may be stacked differently and give another
        // amount of points.
        val distance = endPosition.getDistance(startPosition).toInt()

        var d = (SPACING * 1.5f).toInt()
        while (d < distance - SPACING) {

            val fraction = d.toFloat() / distance

            val pointStartX = startPosition.x + distanceX * (fraction - 0.1f)
            val pointStartY = startPosition.y + distanceY * (fraction - 0.1f)

            val pointEndX = startPosition.x + distanceX * fraction
            val pointEndY = startPosition.y + distanceY * fraction

            val fadeOutTime = startTime + fraction * duration
            val fadeInTime = fadeOutTime - preempt

            val fp = obtainFollowPoint(fadeInTime)

            fp.choreography?.clear()
            fp.translationX = pointStartX.px
            fp.translationY = pointStartY.px
            fp.scaleOrigin = Anchor.Center
            fp.scale = 1.5f * scale
            fp.origin = Anchor.Center
            fp.rotationZ = rotation
            fp.alpha = 0f

            scene.add(0, fp)

            fp.choreograph {
                at(fadeInTime)
                fadeIn(endFadeInTime)
                scaleTo(scale, endFadeInTime, Easing.OutQuad)
                moveTo(pointEndX, pointEndY, endFadeInTime, Easing.OutQuad)

                delay(fadeOutTime - fadeInTime)
                fadeOut(endFadeInTime)
                then()
                after(expire)
            }

            d += SPACING
        }
    }

    /**
     * A follow point, showing the skin's piece, as osu!lazer's `FollowPoint`: it's placed, turned and faded here,
     * whatever the skin.
     */
    private class PoolableFollowPoint : Container(), IPoolable {
        override var isRecycled = false

        val piece = SkinPieces.current.create(SkinComponent.FollowPoint).apply {
            anchor = Anchor.Center
            origin = Anchor.Center
        }

        init {
            +piece
        }
    }
}
