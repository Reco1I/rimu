package com.osudroid.ui.v2.game

import kotlin.math.PI
import com.osudroid.ui.v2.skinFrames
import com.reco1l.verktex.data.LayoutConstraints
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.data.ScaleType
import com.osudroid.beatmaps.hitobjects.HitObject
import com.osudroid.utils.IPoolable
import com.osudroid.utils.SynchronizedPool
import com.osudroid.utils.updateThread
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.px
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.ui.UIAnimatedSprite
import com.reco1l.verktex.ui.OnTransformFinished
import com.reco1l.verktex.ui.choreograph
import com.reco1l.verktex.ui.choreography
import com.reco1l.verktex.ui.UISprite
import com.reco1l.verktex.ui.container.UIContainer
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.skins.OsuSkin
import kotlin.math.*
import kotlin.jvm.JvmStatic

object FollowPointConnection {


    private const val SPACING = 32

    private const val PREEMPT = 800

    private val pool = SynchronizedPool<IPoolable>(20)

    private fun obtainFollowPoint(): UISprite {
        val fp = pool.acquire() as PoolableFollowPoint? ?: PoolableFollowPoint()

        // A pooled one stopped on its last frame, its animation plays again from the start.
        fp.stop()
        fp.play()
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
    fun clearAll(scene: UIContainer) {
        for (i in scene.childCount - 1 downTo 0) {
            val child = scene[i]

            if (child is PoolableFollowPoint) {
                child.choreography?.clear()
                child.removeSelf()
                pool.release(child as IPoolable)
            }
        }
    }

    @JvmStatic
    fun renew(size: Int) {
        pool.clear()

        repeat(size) {
            pool.release(PoolableFollowPoint())
        }
    }

    @JvmStatic
    fun addConnection(scene: UIContainer, start: HitObject, end: HitObject) {

        // Reference: https://github.com/ppy/osu/blob/7bc8908ca9c026fed1d831eb6e58df7624a8d614/osu.Game.Rulesets.Osu/Objects/Drawables/Connections/FollowPointConnection.cs

        val scale = start.screenSpaceGameplayScale
        val startTime = (start.endTime / 1000f).toFloat()

        val startPosition = start.screenSpaceGameplayStackedEndPosition
        val endPosition = end.screenSpaceGameplayStackedPosition

        val distanceX = endPosition.x - startPosition.x
        val distanceY = endPosition.y - startPosition.y
        val rotation = atan2(distanceY, distanceX) * (180f / PI).toFloat()

        // HitObject.timeFadeIn may be modified by mods, but reference uses the unmodified version (which is 400ms).
        val endFadeInTime = 0.4f
        val duration = (end.startTime - start.endTime).toFloat() / 1000f

        // Preempt time can go below 800ms. Normally, this is achieved via the DT mod which uniformly speeds up all animations game wide regardless of AR.
        // This uniform speedup is hard to match 1:1, however we can at least make AR>10 (via mods) feel good by extending the upper linear preempt function.
        // Note that this doesn't exactly match the AR>10 visuals as they're classically known, but it feels good.
        val preempt = PREEMPT * min(1.0, start.timePreempt / HitObject.PREEMPT_MIN).toFloat() / 1000f

        // Since the unit of spacing is in osu!pixels, we cannot directly port the reference code. As such, we need to
        // approach it with another method. We use the distance between the start and end positions in osu!pixels to
        // determine the amount of sprites to spawn, and then map them into gameplay positions in pixels.
        val osuPixelsStartPosition = start.difficultyStackedEndPosition
        val osuPixelsEndPosition = end.difficultyStackedPosition

        val osuPixelsDistance = osuPixelsEndPosition.getDistance(osuPixelsStartPosition).toInt()

        var d = (SPACING * 1.5f).toInt()
        while (d < osuPixelsDistance - SPACING) {

            val fraction = d.toFloat() / osuPixelsDistance

            val pointStartX = startPosition.x + distanceX * (fraction - 0.1f)
            val pointStartY = startPosition.y + distanceY * (fraction - 0.1f)

            val pointEndX = startPosition.x + distanceX * fraction
            val pointEndY = startPosition.y + distanceY * fraction

            val fadeOutTime = startTime + fraction * duration
            val fadeInTime = fadeOutTime - preempt

            val fp = obtainFollowPoint()

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
     * A follow point, animated or not depending on the skin, which may change while it's pooled.
     */
    private class PoolableFollowPoint : UIAnimatedSprite(skinFrames("followpoint", orStatic = true), OsuSkin.get().animationFramerate), IPoolable {
        override var isRecycled = false

        init {
            isLoop = false
            // Cropping from the center is what this scale type does when the size is smaller than the texture.
            scaleType = ScaleType.Crop
        }

        // The size follows the texture, at most this.
        // Reference: https://github.com/ppy/osu/blob/0811de728e4205a45e485d53ccdaf19a937c6033/osu.Game.Rulesets.Osu/Skinning/Legacy/OsuLegacySkinTransformer.cs#L95-L97
        override fun onMeasure(parentConstraints: LayoutConstraints, contentConstraints: LayoutConstraints): Vec2 {
            val size = super.onMeasure(parentConstraints, contentConstraints)

            return Vec2(
                x = min(size.x, HitObject.OBJECT_RADIUS * 2),
                y = min(size.y, HitObject.OBJECT_RADIUS)
            )
        }
    }
}
