package com.osudroid.ui.v2.hud.elements

import com.reco1l.verktex.data.px
import com.reco1l.verktex.graphics.GraphicsRenderer

import com.reco1l.verktex.data.Color4
import com.osudroid.beatmaps.constants.HitObjectType

class HUDColorHitErrorMeter : HUDHitErrorMeter() {
    private val indicators = MutableList(20) { Indicator(it, 0f, Color4.Black) }
    private var currentIndicatorIndex = 0

    init {
        // Extra 2 to indicator spacing for left and right padding.
        val totalWidth = INDICATOR_SIZE * indicators.size + INDICATOR_SPACING * (indicators.size + 2)

        width = totalWidth.px

        height = (INDICATOR_SIZE).px
        backgroundColor = Color4.Black.alpha(0.6f)
    }

    override fun addResult(type: HitObjectType, accuracy: Float, color: Color4) {
        indicators.forEach { it.alpha = (it.alpha - ALPHA_RAMP).coerceAtLeast(0f) }

        val indicator = indicators[currentIndicatorIndex]

        indicator.alpha = 1f
        indicator.color = color

        currentIndicatorIndex = (currentIndicatorIndex + 1) % indicators.size
    }

    override fun onSeek() {
        // Note that this only clears current active indicators. While reconstructing indicators that should still be
        // active is possible, it is not worth the complexity.
        indicators.forEach { it.alpha = 0f }
    }

    override fun onDraw(renderer: GraphicsRenderer) {
        super.onDraw(renderer)

        val y = (bounds.height - INDICATOR_SIZE) / 2f

        indicators.forEach {
            renderer.drawRect(
                x = INDICATOR_SPACING + it.index * (INDICATOR_SIZE + INDICATOR_SPACING),
                y = y,
                width = INDICATOR_SIZE,
                height = INDICATOR_SIZE,
                color = it.color.alpha(it.alpha)
            )
        }
    }

    private data class Indicator(val index: Int, var alpha: Float, var color: Color4)

    companion object {
        private const val INDICATOR_SIZE = 30f
        private const val INDICATOR_SPACING = 3f
        private const val ALPHA_RAMP = 0.08f
    }
}