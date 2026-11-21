package com.osudroid.ui.v2.game

import com.reco1l.verktex.data.LayoutConstraints
import com.reco1l.verktex.data.Vec4
import com.osudroid.ui.v2.SkinTextures
import com.osudroid.ui.v2.skinTexture
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.px
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.ui.Sprite
import com.reco1l.verktex.ui.choreographAt
import com.reco1l.verktex.ui.container.Container
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The spins per minute of a spinner, shown under it as osu!stable does: the skin's `spinner-rpm` with the
 * number in its score font. It slides in when the spinner starts and out when it ends.
 *
 * Positions are osu!stable's at 1024x768, scaled to the height of the part of the playfield that's shown and
 * centered in it, see [layout].
 *
 * Reference: https://github.com/ppy/osu/blob/master/osu.Game.Rulesets.Osu/Skinning/Legacy/LegacySpinner.cs
 */
class SpinnerRpmCounter {

    // osu!stable's 1024x768, placed and scaled in the playfield from its top center. It keeps its size, bigger than the
    // playfield.
    private val screen = object : Container() {
        override fun adjustParentConstraints(constraints: LayoutConstraints) =
            LayoutConstraints(0f, Float.POSITIVE_INFINITY, 0f, Float.POSITIVE_INFINITY)
    }.apply {
        setSize(STABLE_WIDTH, STABLE_HEIGHT)
        origin = Anchor.TopCenter
        scaleOrigin = Anchor.TopCenter
    }

    // What slides in and out, in the pixels of osu!stable's screen.
    private val container = Container().apply {
        setSize(STABLE_WIDTH, STABLE_HEIGHT)
    }

    private val background = Sprite().apply {
        origin = Anchor.TopLeft
        texture = skinTexture("spinner-rpm")
        setPosition(BACKGROUND_X, BACKGROUND_Y)
    }

    // Right aligned.
    private val number = ScoreNumber(NUMBER_RIGHT, NUMBER_Y, "0", 1f, isRightAligned = true)

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
        screen += container
    }


    /**
     * Places the counter over the part of the playfield that's shown, [visibleArea] in osu!pixels.
     */
    fun layout(visibleArea: Vec4) {
        screen.setPosition((visibleArea.x + visibleArea.z) / 2f, visibleArea.y)
        screen.scale = visibleArea.height / STABLE_HEIGHT
    }

    /**
     * Adds the counter to [scene], a layer of the playfield, for a spinner from [startTime] to [endTime], in seconds
     * of gameplay. It's placed by [layout].
     */
    fun attach(scene: Container, startTime: Float, endTime: Float) {
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

        container.clearAnimations()
        container.alpha = 0f
        container.translationY = HIDE_OFFSET.px

        container.choreographAt(startTime) { sequence -> sequence
            .fadeIn(SLIDE_DURATION)
            .moveToY(0f, SLIDE_DURATION, Easing.OutQuint) }

        container.choreographAt(endTime) { sequence -> sequence
            .fadeOut(SLIDE_DURATION)
            .moveToY(HIDE_OFFSET, SLIDE_DURATION, Easing.OutQuint) }

        scene += screen
    }

    /**
     * Measures the spins from the angle the spinner is shown at, in degrees, at [time] in seconds of gameplay.
     */
    fun update(time: Float, angle: Float) {
        if (screen.parent == null) {
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
        screen.removeSelf()
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
