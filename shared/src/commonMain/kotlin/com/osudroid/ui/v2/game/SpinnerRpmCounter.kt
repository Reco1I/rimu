package com.osudroid.ui.v2.game

import com.osudroid.settings.GameResolution
import com.osudroid.ui.v2.SkinTextures
import com.osudroid.ui.v2.skinTexture
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.px
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.ui.UISprite
import com.reco1l.verktex.ui.choreographAt
import com.reco1l.verktex.ui.container.UIContainer
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The spins per minute of a spinner, shown under it as osu!stable does: the skin's `spinner-rpm` with the
 * number in its score font. It slides in when the spinner starts and out when it ends.
 *
 * Positions are osu!stable's at 1024x768, scaled to the height of the screen.
 *
 * Reference: https://github.com/ppy/osu/blob/master/osu.Game.Rulesets.Osu/Skinning/Legacy/LegacySpinner.cs
 */
class SpinnerRpmCounter {

    private val scale = GameResolution.height / STABLE_HEIGHT

    private val container = UIContainer()

    private val background = UISprite().apply {
        origin = Anchor.TopLeft
        texture = skinTexture("spinner-rpm")
        this.scale = this@SpinnerRpmCounter.scale
        setPosition(GameResolution.width / 2f + (BACKGROUND_X - STABLE_WIDTH / 2) * this@SpinnerRpmCounter.scale, BACKGROUND_Y * this@SpinnerRpmCounter.scale)
    }

    private val number = ScoreNumber(0f, NUMBER_Y * scale, "0", scale)

    // The rotation added up over time, sampled each update, to measure the rotations of the last moments.
    private val sampleTimes = FloatArray(SAMPLE_COUNT)
    private val sampleRotations = FloatArray(SAMPLE_COUNT)
    private var sampleCount = 0
    private var firstSample = 0

    private var totalRotation = 0f
    private var lastAngle = Float.NaN
    private var shownRpm = -1
    private var lastShownTime = Float.NEGATIVE_INFINITY


    init {
        container += background
        container += number
    }


    /**
     * Adds the counter to [scene] for a spinner from [startTime] to [endTime], in seconds of gameplay.
     */
    fun attach(scene: UIContainer, startTime: Float, endTime: Float) {
        // Only for skins that have it.
        if (SkinTextures["spinner-rpm"] == null) {
            return
        }

        sampleCount = 0
        firstSample = 0
        totalRotation = 0f
        lastAngle = Float.NaN
        lastShownTime = Float.NEGATIVE_INFINITY
        show(0)

        val hideOffset = HIDE_OFFSET * scale

        container.clearAnimations()
        container.alpha = 0f
        container.translationY = hideOffset.px

        container.choreographAt(startTime) { sequence -> sequence
            .fadeIn(SLIDE_DURATION)
            .moveToY(0f, SLIDE_DURATION, Easing.OutQuint) }

        container.choreographAt(endTime) { sequence -> sequence
            .fadeOut(SLIDE_DURATION)
            .moveToY(hideOffset, SLIDE_DURATION, Easing.OutQuint) }

        scene += container
    }

    /**
     * Measures the spins from the angle the spinner is shown at, in degrees, at [time] in seconds of gameplay.
     */
    fun update(time: Float, angle: Float) {
        if (container.parent == null) {
            return
        }

        if (!lastAngle.isNaN()) {
            // The shortest way between both angles, a frame is never half a turn long.
            var delta = (angle - lastAngle) % 360f
            if (delta > 180f) delta -= 360f
            if (delta < -180f) delta += 360f
            totalRotation += abs(delta)
        }
        lastAngle = angle

        addSample(time, totalRotation)

        // Samples older than the window are dropped, but the oldest one in it is kept as the reference.
        while (sampleCount > 1 && time - sampleTimes[(firstSample + 1) % SAMPLE_COUNT] >= WINDOW) {
            firstSample = (firstSample + 1) % SAMPLE_COUNT
            sampleCount--
        }

        val elapsed = time - sampleTimes[firstSample]

        if (elapsed > 0f && time - lastShownTime >= REFRESH_INTERVAL) {
            val rotations = (totalRotation - sampleRotations[firstSample]) / 360f
            show((rotations / elapsed * 60).roundToInt())
            lastShownTime = time
        }
    }

    fun detach() {
        container.clearAnimations()
        container.removeSelf()
    }


    private fun addSample(time: Float, rotation: Float) {
        if (sampleCount == SAMPLE_COUNT) {
            firstSample = (firstSample + 1) % SAMPLE_COUNT
            sampleCount--
        }

        val index = (firstSample + sampleCount) % SAMPLE_COUNT
        sampleTimes[index] = time
        sampleRotations[index] = rotation
        sampleCount++
    }

    private fun show(rpm: Int) {
        if (rpm == shownRpm) {
            return
        }
        shownRpm = rpm

        number.setText(rpm.toString())

        // Right aligned.
        number.x = (GameResolution.width / 2f + (NUMBER_RIGHT - STABLE_WIDTH / 2) * scale - number.textWidth).px
    }


    private companion object {
        const val STABLE_WIDTH = 1024f
        const val STABLE_HEIGHT = 768f

        const val BACKGROUND_X = 373f
        const val BACKGROUND_Y = 712f
        const val NUMBER_RIGHT = 640f
        const val NUMBER_Y = 717f

        // How far below its place it is while hidden.
        const val HIDE_OFFSET = 80f
        const val SLIDE_DURATION = 0.2f

        // How long the spins are measured over, as osu!lazer does.
        const val WINDOW = 0.595f
        const val SAMPLE_COUNT = 512

        // How often the number changes at most.
        const val REFRESH_INTERVAL = 0.05f
    }
}
