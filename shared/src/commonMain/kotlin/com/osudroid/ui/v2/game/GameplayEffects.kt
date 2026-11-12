package com.osudroid.ui.v2.game

import com.osudroid.settings.GameResolution
import com.reco1l.verktex.data.Background
import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.settings.Settings
import com.osudroid.mods.ModFlashlight
import com.osudroid.ui.v2.SkinSounds
import com.osudroid.ui.v2.SkinTextures
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.px
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.math.FloatInterpolation
import com.reco1l.verktex.ui.UIComponent
import com.reco1l.verktex.ui.UISprite
import com.reco1l.verktex.ui.choreograph
import com.reco1l.verktex.ui.clearTransforms
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.fadeTo
import com.reco1l.verktex.ui.scaleTo
import com.reco1l.verktex.ui.shape.UIBox
import com.osudroid.math.Vector2
import ru.nsu.ccfit.zuev.osu.Constants
import ru.nsu.ccfit.zuev.osu.Utils
import ru.nsu.ccfit.zuev.osu.game.GameObject
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import ru.nsu.ccfit.zuev.skins.OsuSkin
import kotlin.jvm.JvmOverloads

//region Flashlight

private const val FLASHLIGHT_BASE_SIZE = 6f
private const val FLASHLIGHT_AREA_CHANGE_DURATION = 0.8f
private const val FLASHLIGHT_SLIDER_DIM_ALPHA = 0.8f

/**
 * The area around the cursor that remains visible with the Flashlight mod.
 */
class FlashLightEntity(flashlight: ModFlashlight) : UIContainer() {

    private val followDelay = flashlight.followDelay
    private val sizeMultiplier = flashlight.sizeMultiplier
    private val comboBasedSize = flashlight.comboBasedSize

    private val mainSprite = UISprite(SkinTextures["flashlight_cursor"]).also {
        it.origin = Anchor.Center
        it.scale = FLASHLIGHT_BASE_SIZE
        +it
    }

    private val dimLayer = UIBox().also {
        val texture = mainSprite.textureRegion

        it.origin = Anchor.Center
        it.scale = FLASHLIGHT_BASE_SIZE
        it.background = Background.Color(Color4.Black)
        it.alpha = 0f
        it.setSize(texture?.width ?: 0f, texture?.height ?: 0f)
        +it
    }

    private var isTrackingSliders = false
    private var isBreak = false
    private var currentSize = FLASHLIGHT_BASE_SIZE
    private var nextX = 0f
    private var nextY = 0f


    init {
        setPosition(GameResolution.width / 2f, GameResolution.height / 2f)
    }


    private fun changeArea(fromScale: Float, toScale: Float) {
        mainSprite.clearTransforms()
        mainSprite.scale = fromScale * sizeMultiplier
        mainSprite.scaleTo(toScale * sizeMultiplier, FLASHLIGHT_AREA_CHANGE_DURATION)
    }

    fun onBreak(isBreak: Boolean) {
        if (this.isBreak == isBreak) {
            return
        }

        this.isBreak = isBreak

        val fromScale = if (isBreak) currentSize else 1.5f * FLASHLIGHT_BASE_SIZE
        val toScale = if (isBreak) 1.5f * FLASHLIGHT_BASE_SIZE else currentSize

        changeArea(fromScale, toScale)
    }

    fun onMouseMove(x: Float, y: Float) {
        nextX = x.coerceIn(0f, GameResolution.width.toFloat())
        nextY = y.coerceIn(0f, GameResolution.height.toFloat())
    }

    fun onTrackingSliders(isTrackingSliders: Boolean) {
        this.isTrackingSliders = isTrackingSliders
    }

    fun updateCombo(combo: Int) {
        updateDim()

        // Area stops shrinking at 200 combo
        if (!comboBasedSize || combo > 200 || combo % 100 != 0) {
            return
        }

        // For every 100 combo, the size is decreased by 10%
        val newSize = (1 - 0.1f * combo / 100f) * FLASHLIGHT_BASE_SIZE

        // Only change the area when gameplay is not in break period
        if (!isBreak) {
            changeArea(currentSize, newSize)
        }

        currentSize = newSize
    }

    private var isDimmed = false

    private fun updateDim() {
        if (isDimmed != isTrackingSliders) {
            isDimmed = isTrackingSliders

            dimLayer.clearTransforms()
            dimLayer.fadeTo(if (isDimmed) FLASHLIGHT_SLIDER_DIM_ALPHA else 0f, 0.05f)
        }
    }

    override fun onUpdate(clock: com.reco1l.verktex.time.FrameClock) {
        super.onUpdate(clock)

        if (followDelay == 0f) {
            setPosition(nextX, nextY)
            return
        }

        val deltaTime = minOf(clock.elapsedFrameTime, followDelay)

        setPosition(
            FloatInterpolation.floatAt(deltaTime, x.resolve(0f), nextX, 0f, followDelay, Easing.OutQuad),
            FloatInterpolation.floatAt(deltaTime, y.resolve(0f), nextY, 0f, followDelay, Easing.OutQuad)
        )
    }
}

//endregion

//region Breaks

/**
 * Shows the state of the player during breaks and fades the background in the meantime.
 */
class BreakAnimator(
    private val layer: UIContainer,
    private val stat: StatisticV2,
    private val hud: UIContainer
) : GameObject() {

    /**
     * Called when the warning arrows show up, a second before the break ends.
     */
    var onEnding: (() -> Unit)? = null

    private val arrows = Array(4) { i ->
        UISprite(SkinTextures["play-warningarrow"]).also { it.flippedHorizontal = i > 1 }
    }

    private var dimRectangle: UIComponent? = null
    private var dimBrightness = OsuSettings.backgroundBrightness

    private var length = 0f
    private var time = 0f
    private var arrowTime = 0f
    private var ending = ""
    private var passFail: UISprite? = null
    private var mark: UISprite? = null
    private var isBreak = false
    private var over = false


    init {
        val texture = arrows[0].textureRegion
        val arrowWidth = texture?.width ?: 0f
        val arrowHeight = texture?.height ?: 0f
        val margin = Utils.toRes(64).toFloat()
        val right = GameResolution.width - arrowWidth - margin
        val bottom = GameResolution.height - arrowHeight
        val top = Utils.toRes(72).toFloat()

        arrows[0].setPosition(margin, top)
        arrows[1].setPosition(margin, bottom)
        arrows[2].setPosition(right, top)
        arrows[3].setPosition(right, bottom)
    }


    fun setDimRectangle(dimRectangle: UIComponent?) {
        this.dimRectangle = dimRectangle
    }

    fun setDimBrightness(brightness: Float) {
        dimBrightness = brightness
    }

    fun isBreak() = isBreak

    /**
     * Whether a break just finished, it's only true the first time it's checked.
     */
    fun isOver(): Boolean {
        val wasOver = over
        over = false
        return wasOver
    }

    fun reset() {
        isBreak = false
        over = false
        length = 0f
        time = 0f

        removeSprites()
        resumeBgFade()
    }

    private fun removeSprites() {
        mark?.removeSelf()
        mark = null

        passFail?.removeSelf()
        passFail = null

        arrows.forEach { it.removeSelf() }
    }

    @JvmOverloads
    fun init(length: Float, initialTime: Float = 0f) {
        if (this.length > 0 && time < this.length) {
            return
        }

        isBreak = true
        over = false
        this.length = length
        time = initialTime
        arrowTime = 0f
        ending = if (stat.hp > 0.5f) "pass" else "fail"

        passFail = UISprite(SkinTextures["section-$ending"]).also {
            it.origin = Anchor.Center
            it.setPosition(GameResolution.width / 2f, GameResolution.height / 2f)
            it.isVisible = false
            hud.add(0, it)
        }

        // If we are seeking into a break that is already within 1 second of ending,
        // the warning arrows should be visible immediately.
        val arrowsVisible = length - initialTime <= 1

        arrows.forEach {
            it.isVisible = arrowsVisible
            layer.add(0, it)
        }

        val zeroWidth = SkinTextures[OsuSkin.get().scorePrefix, "0"]?.width ?: 0f

        mark = UISprite(rankingTexture()).also {
            it.setPosition(GameResolution.width - zeroWidth * 11f, 5f)
            it.scale = 1.2f
            hud.add(0, it)
        }

        // Apply the background brightness that matches the current position within the break.
        // The update() loop only uses crossing-event checks to set this, so we must prime it
        // here for seek cases where time starts past those thresholds.
        if (length > 1) {
            setBgFade(
                when {
                    initialTime < 0.5f -> initialTime * 2
                    length - initialTime < 0.5f -> (length - initialTime) * 2
                    else -> 1f
                }
            )
        }
    }

    private fun setBgFade(percent: Float) {
        if (!Settings[OsuSettings.NoChangeDimInBreaks]) {
            dimRectangle?.alpha = (1 - dimBrightness) * (1 - percent)
        }
    }

    private fun resumeBgFade() {
        if (!Settings[OsuSettings.NoChangeDimInBreaks]) {
            dimRectangle?.alpha = 1 - dimBrightness
        }
    }

    private fun rankingTexture() = SkinTextures["ranking-${stat.mark}-small"]

    override fun update(dt: Float) {
        if (length == 0f || time >= length) {
            return
        }

        time += dt

        if (length > 3 && time > (length - 1) / 2 && time - dt < (length - 1) / 2) {
            passFail?.let {
                it.isVisible = true
                it.choreograph {
                    at(it.currentClockTime)
                    delay(0.25f).fadeOut(0.025f)
                    then().delay(0.025f).fadeIn(0.025f)
                    then().delay(0.6725f).fadeOut(0.3f)
                }
            }

            SkinSounds.getBeatmap("section$ending", 1)?.play()
        }

        if (length - time <= 1 && length - time + dt > 1) {
            arrows.forEach { it.isVisible = true }

            onEnding?.invoke()
        }

        if (arrows[0].isVisible) {
            arrowTime = (arrowTime + dt) % ARROW_LOOP_DURATION

            val alpha = when {
                arrowTime < 0.05f -> arrowTime / 0.05f
                arrowTime < 0.15f -> 1f
                else -> 1 - (arrowTime - 0.15f) / 0.1f
            }

            arrows.forEach { it.alpha = alpha }
        }

        if (length > 1) {
            when {
                time < 0.5f -> setBgFade(time * 2)
                length - time < 0.5f -> setBgFade((length - time) * 2)
                time - dt < 0.5f -> setBgFade(1f)
            }
        }

        mark?.textureRegion = rankingTexture()

        if (time >= length) {
            isBreak = false
            over = true
            resumeBgFade()
            removeSprites()
        }
    }

    private companion object {
        const val ARROW_LOOP_DURATION = 0.25f
    }
}

//endregion

//region Combo bursts

/**
 * Slides an image in from the sides and plays a voice when reaching certain combos.
 */
class ComboBurst(private val rightX: Float, private val bottomY: Float) {

    private val sprites = mutableListOf<UISprite>()
    private val vocals = (listOf("comboburst") + List(10) { "comboburst-$it" }).mapNotNull(SkinSounds::getOrNull)

    private var nextKeyComboNum = 30
    private var fromX = 0f
    private var nextShowId = 0
    private var nextSoundId = 0


    init {
        for (name in listOf("comboburst") + List(10) { "comboburst-$it" }) {
            val texture = SkinTextures[name] ?: continue

            sprites += UISprite(texture).also { it.alpha = 0f }
        }
    }


    fun checkAndShow(currentCombo: Int) {
        if (!Settings[OsuSettings.ComboBurst] || currentCombo < nextKeyComboNum) {
            return
        }

        if (vocals.isNotEmpty()) {
            vocals[nextSoundId].play(0.8f)
            nextSoundId = (nextSoundId + 1) % vocals.size
        }

        if (sprites.isNotEmpty()) {
            show(sprites[nextShowId])
            nextShowId = (nextShowId + 1) % sprites.size
        }

        if (nextKeyComboNum == 30) {
            nextKeyComboNum = 60
            fromX = rightX
        } else if (nextKeyComboNum == 60) {
            nextKeyComboNum = 100
            fromX = -1f
        } else {
            nextKeyComboNum += 100
            fromX = if (nextKeyComboNum / 100 % 2 == 0) rightX else -1f
        }
    }

    private fun show(sprite: UISprite) {
        val texture = sprite.textureRegion ?: return
        val width = texture.width
        val toX: Float

        if (fromX > 0) {
            toX = fromX - width
        } else {
            fromX = -width
            toX = 0f
        }

        sprite.choreography?.clear()
        sprite.setPosition(0f, bottomY - texture.height)
        sprite.translationX = fromX.px
        sprite.alpha = 0f

        val startX = fromX

        sprite.choreograph {
            at(sprite.currentClockTime)
            fadeIn(0.5f)
            moveToX(toX, 0.5f, Easing.OutSine)
            then().delay(1f)
            fadeOut(0.5f)
            moveToX(startX, 0.5f, Easing.OutSine)
        }
    }

    fun breakCombo() {
        fromX = 0f
        nextKeyComboNum = 30
    }

    fun attachAll(layer: UIContainer) {
        sprites.forEach { layer += it }
    }

    fun detachAll() {
        sprites.forEach { it.removeSelf() }
    }
}

//endregion

//region Countdown

/**
 * The countdown shown before the first object of a beatmap.
 */
class Countdown(
    private var layer: UIContainer?,
    private val speed: Float,
    @Suppress("UNUSED_PARAMETER") offset: Float,
    time: Float
) : GameObject() {

    /**
     * Called when the countdown finishes.
     */
    var onFinish: (() -> Unit)? = null

    private var timePassed = -time + COUNTDOWN_LENGTH * speed

    private val center = Utils.trackToRealCoords(Vector2(Constants.MAP_WIDTH / 2f, Constants.MAP_HEIGHT / 2f))

    private val ready = createSprite("ready", center.x)
    private val count3 = createSprite("count3", (SkinTextures["count3"]?.width ?: 0f) / 2f)
    private val count2 = createSprite("count2", GameResolution.width - (SkinTextures["count2"]?.width ?: 0f) / 2f)
    private val count1 = createSprite("count1", center.x)
    private val go = createSprite("go", center.x)


    private fun createSprite(texture: String, x: Float) = UISprite(SkinTextures[texture]).also {
        it.origin = Anchor.Center
        it.setPosition(x, center.y)
        it.isVisible = false
        layer?.add(0, it)
    }

    private fun duration(fraction: Float) = COUNTDOWN_LENGTH * speed * fraction

    private fun showReady() {
        ready.rotationZ = -90f
        ready.alpha = 0f
        ready.isVisible = true
        ready.choreograph {
            at(ready.currentClockTime)
            fadeIn(duration(1 / 9f))
            rotateTo(0f, duration(1 / 9f))
            then().delay(duration(1 / 9f))
            fadeOut(duration(1 / 9f))
            scaleTo(1.5f, duration(1 / 9f))
        }
    }

    private fun showCount(sprite: UISprite, holdFraction: Float) {
        sprite.alpha = 0f
        sprite.isVisible = true
        sprite.choreograph {
            at(sprite.currentClockTime)
            fadeIn(duration(1 / 18f))
            then().delay(duration(holdFraction))
            fadeOut(duration(1 / 18f))
        }
    }

    private fun showGo() {
        go.rotationZ = -180f
        go.alpha = 0f
        go.isVisible = true
        go.choreograph {
            at(go.currentClockTime)
            fadeIn(duration(1 / 18f))
            rotateTo(0f, duration(1 / 18f))
            then().delay(duration(1 / 18f))
            fadeOut(duration(1 / 18f))
        }
    }

    private fun crossed(fraction: Float, dt: Float): Boolean {
        val threshold = duration(fraction)
        return timePassed >= threshold && timePassed - dt < threshold
    }

    override fun update(dt: Float) {
        if (layer == null) {
            return
        }

        timePassed += dt

        if (timePassed >= 0 && timePassed - dt < 0) {
            SkinSounds.getBeatmap("readys", 1)?.play()
            showReady()
        }

        if (crossed(2 / 6f, dt)) {
            SkinSounds.getBeatmap("count3s", 1)?.play()
            showCount(count3, 8 / 18f)
        }

        if (crossed(3 / 6f, dt)) {
            SkinSounds.getBeatmap("count2s", 1)?.play()
            showCount(count2, 5 / 18f)
        }

        if (crossed(4 / 6f, dt)) {
            SkinSounds.getBeatmap("count1s", 1)?.play()
            showCount(count1, 2 / 18f)
        }

        if (crossed(5 / 6f, dt)) {
            SkinSounds.getBeatmap("gos", 1)?.play()
            showGo()
        }

        if (crossed(1f, dt)) {
            layer = null
            onFinish?.invoke()

            ready.removeSelf()
            go.removeSelf()
            count1.removeSelf()
            count2.removeSelf()
            count3.removeSelf()
        }
    }

    companion object {
        const val COUNTDOWN_LENGTH = 3f
    }
}

//endregion
