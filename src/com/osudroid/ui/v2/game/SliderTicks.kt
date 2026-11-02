package com.osudroid.ui.v2.game

import com.osudroid.ui.v2.SkinTextures
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.ui.clearTransforms
import com.reco1l.verktex.ui.fadeIn
import com.reco1l.verktex.ui.fadeOut
import com.reco1l.verktex.data.px
import com.reco1l.verktex.ui.scaleTo
import com.reco1l.verktex.ui.transformSequence

import com.osudroid.beatmaps.hitobjects.Slider
import com.osudroid.beatmaps.hitobjects.sliderobject.SliderTick
import com.osudroid.utils.IPoolable
import com.osudroid.utils.SynchronizedPool
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.ui.UISprite
import com.reco1l.verktex.ui.container.UIContainer
import kotlin.math.min
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.game.GameHelper

class SliderTickContainer : UIContainer() {
    private var slider: Slider? = null

    fun init(beatmapSlider: Slider) {
        slider = beatmapSlider

        clearChildren()

        val position = beatmapSlider.screenSpaceGameplayStackedPosition

        x = position.x.px
        y = position.y.px

        for (i in 1 until beatmapSlider.nestedHitObjects.size) {

            val tick = beatmapSlider.nestedHitObjects[i] as? SliderTick ?: break
            val tickPosition = tick.screenSpaceGameplayStackedPosition

            val sprite = SliderTickSprite.obtain()

            // We're subtracting the position of the slider because the tick container is
            // already at the position of the slider since it's a child of the slider's body.
            sprite.x = (tickPosition.x - position.x).px
            sprite.y = (tickPosition.y - position.y).px
            +sprite

            sprite.init(tick)
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
            ] as? SliderTickSprite ?: break

            sprite.init(tick)
        }
    }

    override fun onDetached() {
        super.onDetached()
        clearChildren()
    }

}


class SliderTickSprite : UISprite(), IPoolable {
    override var isRecycled = false
    private var tick: SliderTick? = null

    init {
        textureRegion = SkinTextures["sliderscorepoint"]
        origin = Anchor.Center
    }

    /**
     * Initializes this [SliderTickSprite] with the given [SliderTick].
     *
     * @param tick The [SliderTick] represented by this [SliderTickSprite].
     */
    fun init(tick: SliderTick) {
        this.tick = tick

        val startTime = (tick.startTime / 1000).toFloat()
        val timePreempt = (tick.timePreempt / 1000).toFloat()
        val fadeInStartTime = startTime - timePreempt

        clearTransforms()

        alpha = 0f
        scale = 0.5f

        transformSequence(fadeInStartTime - gameplayTime()) {
            scaleTo(1f, ANIM_DURATION * 4, Easing.OutElasticHalf)
            fadeIn(ANIM_DURATION)
        }

        if (GameHelper.isHidden() && !GameHelper.getHidden().onlyFadeApproachCircles) {
            val fadeOutDuration = min(timePreempt - ANIM_DURATION, 1f)
            val fadeOutStartTime = startTime - fadeOutDuration

            transformSequence(fadeOutStartTime - gameplayTime()) {
                fadeOut(fadeOutDuration)
            }
        }
    }

    /**
     * Called when the [SliderTick] that this [SliderTickSprite] represents is hit.
     *
     * @param isSuccessful Whether the hit resulted in a successful hit.
     */
    fun onHit(isSuccessful: Boolean) {
        val tick = tick ?: return

        clearTransforms()

        transformSequence(tick.startTime.toFloat() / 1000 - gameplayTime()) {
            fadeOut(ANIM_DURATION, Easing.OutQuint)

            if (isSuccessful) {
                scaleTo(1.5f, ANIM_DURATION, Easing.OutQuad)
            }
        }

        this.tick = null
    }

    private fun gameplayTime() = GlobalManager.getInstance().gameScene.elapsedTime

    override fun onDetached() {
        super.onDetached()
        clearTransforms()
        pool.release(this)
    }

    companion object {
        private const val ANIM_DURATION = 0.15f
        private val pool = SynchronizedPool<SliderTickSprite>(20).apply { release(SliderTickSprite()) }

        /**
         * Renews the [SliderTickSprite] pool with fresh instances.
         *
         * @param size The number of [SliderTickSprite] instances to pre-populate the pool with.
         */
        @JvmStatic
        fun renew(size: Int) {
            pool.clear()
            repeat(size) { pool.release(SliderTickSprite()) }
        }

        /**
         * Obtains a [SliderTickSprite] from the pool, or creates a new one if the pool is empty.
         */
        @JvmStatic
        fun obtain(): SliderTickSprite {
            val sprite = pool.acquire() ?: return SliderTickSprite()
            sprite.textureRegion = SkinTextures["sliderscorepoint"]
            return sprite
        }
    }

}
