package com.osudroid.ui.v2.mainmenu

import com.reco1l.framework.forEachIndexedTolerant
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.math.FloatInterpolation
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.Theme
import com.reco1l.verktex.ui.UICanvas
import ru.nsu.ccfit.zuev.osu.GlobalManager
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class RadialVisualizer : UICanvas() {

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
     * The gap between each bar (angular gap in radians for radial mode).
     */
    var gap: Dimension.Fixed = 0.6f.dip

    /**
     * The multiplier applied to the FFT magnitude values.
     */
    var magnitudeMultiplier = 2f


    private val music = GlobalManager.getInstance().music

    private var bars = mutableListOf<BarInfo>()
    private var visualizerRadius = 0f
    private var shiftOffset = 0f


    private fun initializeBars() {
        // The measured size, this runs from onSizeChanged() while measuring, before the bounds
        // are laid out with the new size.
        visualizerRadius = min(measuredSize.x, measuredSize.y) / 2f

        // Circumference = 2 * PI * radius
        val circumference = 2f * Math.PI.toFloat() * visualizerRadius
        val barCount = (circumference / (barThickness.toPixels() + gap.toPixels())).toInt()

        bars = MutableList(barCount) { i ->
            // Preserve existing bar heights if possible
            if (i < bars.size) bars[i] else BarInfo()
        }
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

        val fft = music.getAudioFFT(bars.size / 6)

        // Synchronize rotation speed with the beat
        val beatLength = RythimManager.beatLength
        val rotationsPerBeat = if (RythimManager.isKiai) 0.25f else 0.1f

        // Calculate rotation speed based on BPM
        // rotationsPerSecond = rotationsPerBeat * beatsPerSecond
        val rotationsPerSecond = if (beatLength > 0) {
            rotationsPerBeat * (1000f / beatLength.toFloat())
        } else {
            0f
        }

        val fftSize = fft?.size ?: 1

        // Accumulate rotation over time (doesn't reset each beat)
        shiftOffset += rotationsPerSecond * fftSize * deltaTimeSec

        // Keep offset in reasonable range
        if (shiftOffset >= fftSize) {
            shiftOffset %= fftSize.toFloat()
        }

        bars.forEachIndexedTolerant { index, barInfo ->
            val exactIndex = (shiftOffset + index) % fftSize
            val fftIndex1 = exactIndex.toInt()
            val fftIndex2 = (fftIndex1 + 1) % fftSize
            val fraction = exactIndex - fftIndex1

            // Interpolate between two consecutive FFT values for smooth transition
            val value1 = fft?.getOrNull(fftIndex1) ?: 0f
            val value2 = fft?.getOrNull(fftIndex2) ?: 0f
            val interpolatedValue = value1 * (1f - fraction) + value2 * fraction

            barInfo.targetHeight = interpolatedValue * magnitudeMultiplier
            barInfo.update(deltaTimeSec)
        }

        super.onUpdate(clock)
    }

    override fun onDraw(renderer: GraphicsRenderer) {
        val bars = bars

        val centerX = bounds.width / 2f
        val centerY = bounds.height / 2f
        val thickness = barThickness.toPixels()
        val color = contentColor

        // Each bar starts at the center and points outwards, being the first one the one pointing up.
        bars.forEachIndexedTolerant { index, barInfo ->
            val angle = (index.toFloat() / bars.size) * 2f * PI.toFloat()
            val length = visualizerRadius * (1f + 2f * barInfo.currentHeight.coerceIn(0f, 1f))

            renderer.drawLine(
                centerX,
                centerY,
                centerX + sin(angle) * length,
                centerY - cos(angle) * length,
                thickness,
                color
            )
        }
    }


    data class BarInfo(
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
