package com.osudroid.ui.v2.mainmenu

import com.osudroid.OsuGame
import com.reco1l.framework.forEachIndexedTolerant
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.graphics.Renderer
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.math.FloatInterpolation
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.Canvas

/**
 * The spectrum of the music playing, as bars across the width growing from an edge: from the top
 * one down when [fromTop], from the bottom one up otherwise. A bar at its loudest fills the height.
 */
class LinearVisualizer : Canvas() {

    /**
     * Whether the bars hang from the top edge instead of rising from the bottom one.
     */
    var fromTop = false

    /**
     * The thickness of each bar.
     */
    var barThickness: Dimension.Fixed = 4f.dip
        set(value) {
            if (field != value) {
                field = value
                initializeBars()
            }
        }

    /**
     * The gap between each bar.
     */
    var gap: Dimension.Fixed = 2f.dip
        set(value) {
            if (field != value) {
                field = value
                initializeBars()
            }
        }

    /**
     * The multiplier applied to the FFT magnitude values.
     */
    var magnitudeMultiplier = 2f


    private val music = OsuGame.music

    private var bars = mutableListOf<BarInfo>()


    private fun initializeBars() {
        // The measured size, this runs from onSizeChanged() while measuring, before the bounds
        // are laid out with the new size.
        val barCount = (measuredSize.x / (barThickness.toPixels() + gap.toPixels())).toInt().coerceAtLeast(0)

        bars = MutableList(barCount) { i -> if (i < bars.size) bars[i] else BarInfo() }
    }


    override fun onSizeChanged() {
        super.onSizeChanged()
        initializeBars()
    }

    override fun onThemeChanged() {
        super.onThemeChanged()
        initializeBars()
    }

    override fun onUpdate(clock: FrameClock) {
        val deltaTimeSec = clock.elapsedFrameTime
        val bars = bars

        // Fewer values than bars, the lower frequencies are the ones that move: each bar
        // interpolates between the two values around its position.
        val fft = music.getAudioFFT((bars.size / 3).coerceAtLeast(1))
        val fftSize = fft?.size ?: 0

        bars.forEachIndexedTolerant { index, barInfo ->
            barInfo.targetHeight = if (fft == null || fftSize == 0) 0f else {
                val exactIndex = index.toFloat() / bars.size * (fftSize - 1)
                val index1 = exactIndex.toInt()
                val index2 = (index1 + 1).coerceAtMost(fftSize - 1)
                val fraction = exactIndex - index1

                (fft[index1] * (1f - fraction) + fft[index2] * fraction) * magnitudeMultiplier
            }
            barInfo.update(deltaTimeSec)
        }

        super.onUpdate(clock)
    }

    override fun onDraw(renderer: Renderer) {
        val bars = bars
        if (bars.isEmpty()) {
            return
        }

        val height = bounds.height
        val thickness = barThickness.toPixels()
        val step = bounds.width / bars.size
        val color = contentColor

        bars.forEachIndexedTolerant { index, barInfo ->
            val length = height * barInfo.currentHeight.coerceIn(0f, 1f)
            if (length <= 0f) {
                return@forEachIndexedTolerant
            }
            val x = step * (index + 0.5f)

            if (fromTop) {
                renderer.drawLine(x, 0f, x, length, thickness, color)
            } else {
                renderer.drawLine(x, height, x, height - length, thickness, color)
            }
        }
    }


    private class BarInfo(
        // Heights are in relative units (0 to 1)
        var targetHeight: Float = 0f,
        var currentHeight: Float = 0f
    ) {
        fun update(deltaTimeSec: Float) {
            currentHeight = FloatInterpolation.floatAt(
                deltaTimeSec.coerceIn(0f, 0.3f),
                currentHeight,
                targetHeight,
                0f,
                0.3f,
                Easing.OutQuint
            )
        }
    }
}
