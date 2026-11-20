package com.osudroid.ui.v2.game

import com.osudroid.ui.v2.skin.SkinComponent
import com.osudroid.ui.v2.skin.SkinPieces
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.ui.fadeIn
import com.reco1l.verktex.ui.fadeOut
import com.reco1l.verktex.data.px
import com.reco1l.verktex.ui.scaleTo
import com.reco1l.verktex.ui.choreograph
import com.reco1l.verktex.ui.choreography

import com.osudroid.beatmaps.hitobjects.Slider
import com.osudroid.beatmaps.hitobjects.sliderobject.SliderTick
import com.osudroid.utils.IPoolable
import com.osudroid.utils.SynchronizedPool
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.ui.container.Container
import kotlin.math.min
import ru.nsu.ccfit.zuev.osu.*
import kotlin.jvm.JvmStatic

class SliderTickContainer : Container() {
    private var slider: Slider? = null

    // The scale of the ticks, as the objects'.
    private var tickScale = 1f

    // Whether the ticks fade out before they're hit.
    private var fadeOut = false

    /**
     * Shows the ticks of [beatmapSlider], scaled by [tickScale].
     *
     * @param fadeOut Whether the ticks fade out before they're hit, as Hidden fades the objects.
     */
    fun init(beatmapSlider: Slider, tickScale: Float, fadeOut: Boolean) {
        slider = beatmapSlider
        this.tickScale = tickScale
        this.fadeOut = fadeOut

        clearChildren()

        val position = beatmapSlider.gameplayStackedPosition

        x = position.x.px
        y = position.y.px

        for (i in 1 until beatmapSlider.nestedHitObjects.size) {

            val tick = beatmapSlider.nestedHitObjects[i] as? SliderTick ?: break
            val tickPosition = tick.gameplayStackedPosition

            val sprite = GameplaySliderTick.obtain()

            // We're subtracting the position of the slider because the tick container is
            // already at the position of the slider since it's a child of the slider's body.
            sprite.x = (tickPosition.x - position.x).px
            sprite.y = (tickPosition.y - position.y).px
            +sprite

            sprite.init(tick, tickScale, fadeOut)
        }
    }

    fun onNewSpan(newSpanIndex: Int) {
        if (slider == null) {
            return
        }

        val spanStartIndex =
            // Amount of slider ticks passed.
            newSpanIndex * childCount +
            // Amount of slider repeats passed.
            newSpanIndex +
            // The slider head.
            1

        for (i in spanStartIndex until spanStartIndex + childCount) {
            val tick = slider!!.nestedHitObjects[i] as? SliderTick ?: break

            // For reverse sliders, the ticks are in the opposite order.
            val sprite = this[
                if (newSpanIndex % 2 != 0) childCount - (i - spanStartIndex) - 1 else i - spanStartIndex
            ] as? GameplaySliderTick ?: break

            sprite.init(tick, tickScale, fadeOut)
        }
    }

    override fun onDetached() {
        super.onDetached()
        clearChildren()
    }

}


/**
 * A tick of a slider, as osu!lazer's `DrawableSliderTick`: it animates the skin's piece it shows, the same for every
 * skin.
 */
class GameplaySliderTick : Container(), IPoolable {
    override var isRecycled = false
    private var tick: SliderTick? = null
    private var baseScale = 1f

    init {
        origin = Anchor.Center

        +SkinPieces.current.create(SkinComponent.SliderTick).also {
            it.anchor = Anchor.Center
            it.origin = Anchor.Center
        }
    }

    /**
     * Initializes this [GameplaySliderTick] with the given [SliderTick].
     *
     * @param tick The [SliderTick] represented by this [GameplaySliderTick].
     * @param fadeOut Whether the tick fades out before it's hit.
     */
    fun init(tick: SliderTick, baseScale: Float, fadeOut: Boolean) {
        this.tick = tick
        this.baseScale = baseScale

        val startTime = (tick.startTime / 1000).toFloat()
        val timePreempt = (tick.timePreempt / 1000).toFloat()
        val fadeInStartTime = startTime - timePreempt

        choreography?.clear()

        alpha = 0f
        scale = 0.5f * baseScale

        choreograph {
            at(fadeInStartTime)
            scaleTo(baseScale, ANIM_DURATION * 4, Easing.OutElasticHalf)
            fadeIn(ANIM_DURATION)

            if (fadeOut) {
                val fadeOutDuration = min(timePreempt - ANIM_DURATION, 1f)

                at(startTime - fadeOutDuration)
                fadeOut(fadeOutDuration)
            }
        }
    }

    /**
     * Called when the [SliderTick] that this [GameplaySliderTick] represents is hit.
     *
     * @param isSuccessful Whether the hit resulted in a successful hit.
     */
    fun onHit(isSuccessful: Boolean) {
        val tick = tick ?: return

        choreography?.clear()

        choreograph {
            at(tick.startTime.toFloat() / 1000)
            fadeOut(ANIM_DURATION, Easing.OutQuint)

            if (isSuccessful) {
                scaleTo(1.5f * baseScale, ANIM_DURATION, Easing.OutQuad)
            }
        }

        this.tick = null
    }

    override fun onDetached() {
        super.onDetached()
        choreography?.clear()
        pool.release(this)
    }

    companion object {
        private const val ANIM_DURATION = 0.15f
        private val pool = SynchronizedPool<GameplaySliderTick>(20)

        /**
         * Renews the [GameplaySliderTick] pool with fresh instances, with the pieces of the skin in use.
         *
         * @param size The number of [GameplaySliderTick] instances to pre-populate the pool with.
         */
        @JvmStatic
        fun renew(size: Int) {
            pool.clear()
            repeat(size) { pool.release(GameplaySliderTick()) }
        }

        /**
         * Obtains a [GameplaySliderTick] from the pool, or creates a new one if the pool is empty.
         */
        @JvmStatic
        fun obtain(): GameplaySliderTick {
            return pool.acquire() ?: GameplaySliderTick()
        }
    }

}
