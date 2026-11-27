package com.osudroid.ui.v2.mainmenu

import com.osudroid.OsuGame
import com.reco1l.verktex.BuiltIn
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.fonts.Font
import com.reco1l.verktex.math.ColorInterpolation
import com.reco1l.verktex.math.interpolateFloat
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.container.Container
import com.reco1l.verktex.ui.text.Text
import com.reco1l.verktex.ui.text.TextEffect
import com.reco1l.verktex.ui.themed

/**
 * The game's name where the menu's logo is: a text tinted by the theme's accent with a thin
 * outline, and a glow under it that pulses with the music's beats, brighter through kiai sections.
 *
 * The glow is a text of its own under the one shown: [Text.effectsAlpha] fades every effect of a
 * text, the outline would pulse with it.
 */
class RimuLogo(withEffects: Boolean = true) : Container() {

    /**
     * Whether the glow follows the music. Without it, it fades out.
     */
    var playEffects = withEffects


    private val glow = logoText().apply {
        effectsAlpha = if (withEffects) IDLE_GLOW else 0f
    }

    private val label = logoText().apply {
        color = themed { ColorInterpolation.colorAt(TINT, Color4.White, palette.accent, 0f, 1f) }
    }


    init {
        +glow
        +label
        updateEffects()
    }


    private fun logoText() = Text().apply {
        text = "rimu!"
        fontFamily = BuiltIn.Fonts.Fredoka
        fontWeight = Font.Weight.Bold
        fontSize = 96f.dip
        color = Color4.White
        wrapText = false

        // The effects are drawn past the glyphs, the padding keeps them inside the bounds.
        padding = Padding(GLOW_RADIUS)
    }

    private fun updateEffects() {
        val accent = Verktex.theme.palette.accent
        glow.effects = listOf(TextEffect.Glow(accent, GLOW_RADIUS, 1.5f))
        label.effects = listOf(TextEffect.Outline(Verktex.theme.palette.contentSubtle.alpha(0.35f), OUTLINE_WIDTH))
    }

    override fun onThemeChanged() {
        super.onThemeChanged()
        updateEffects()
    }

    override fun onUpdate(clock: FrameClock) {
        val target = when {
            !playEffects -> 0f
            !OsuGame.music.isPlaying || RythimManager.beatLength <= 0.0 -> IDLE_GLOW

            else -> {
                // Brightest on the beat, dimming until the next one.
                val progress = (RythimManager.beatElapsed / RythimManager.beatLength).toFloat().coerceIn(0f, 1f)
                val low = if (RythimManager.isKiai) KIAI_LOW_GLOW else LOW_GLOW
                val falloff = 1f - progress
                low + (1f - low) * falloff * falloff
            }
        }

        // Smoothed so the jump on each beat is a quick flash rather than a cut.
        glow.effectsAlpha = clock.interpolateFloat(if (playEffects) 0.05f else 0.4f, glow.effectsAlpha, target)

        super.onUpdate(clock)
    }


    private companion object {
        val GLOW_RADIUS = 24f.dip
        val OUTLINE_WIDTH = 1.5f.dip

        // How far the text's color goes from white to the accent.
        const val TINT = 0.25f

        const val IDLE_GLOW = 0.5f
        const val LOW_GLOW = 0.35f
        const val KIAI_LOW_GLOW = 0.6f
    }
}
