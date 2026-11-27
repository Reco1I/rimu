package com.osudroid.ui.v2.modmenu

import com.osudroid.mods.*
import com.reco1l.verktex.data.Background
import com.reco1l.verktex.data.CornerRadius
import com.osudroid.ui.ISkinnable
import com.osudroid.ui.v2.skinTexture
import com.reco1l.verktex.ui.Sprite
import com.osudroid.ui.v2.*
import com.osudroid.utils.allModsInstances
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.data.pct
import com.reco1l.verktex.data.px
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.Icon
import com.reco1l.verktex.ui.container.Container
import com.reco1l.verktex.ui.text.Text
import com.reco1l.verktex.ui.themed


/**
 * The icon for a mod: the skin's texture of the mod when the skin (the beatmap's or the custom one) has it, otherwise
 * the mod's glyph ([modIconGlyph]) over its acronym, drawn by the engine.
 */
class ModIcon(val mod: Mod) : Container(), ISkinnable {

    private var shouldUpdateTexture = true

    // The glyph and the acronym, when the icon is drawn rather than the skin's texture.
    private var glyph: Icon? = null
    private var acronym: Text? = null

    /**
     * Whether the icon is the skin's texture of the mod when it has one, otherwise always drawn by the engine.
     */
    var useSkinTexture = true
        set(value) {
            if (field == value) return
            field = value
            shouldUpdateTexture = true
        }

    constructor(acronym: String): this(allModsInstances.find { it.acronym.equals(acronym, ignoreCase = true) }!!)


    private fun setupContent() {
        clearChildren()
        glyph = null
        acronym = null

        // The game has no textures of its own for mods, only skins do.
        val texture = if (useSkinTexture) SkinTextures[mod.iconTextureName] else null

        if (texture != null) {
            background = Background.Color(Color4.Transparent)

            +Sprite(skinTexture(mod.iconTextureName)).apply {
                width = fill()
                height = fill()
            }
        } else {
            background = Background.Color(Verktex.theme.palette.surfaceRaised)

            // Both are drawn at a fixed size and scaled to the icon's in onUpdate, as the icon's size changes when
            // it's animated and fonts are made for each size.
            glyph = Icon(modIconGlyph(mod)).apply {
                anchor = Anchor.Center
                origin = Anchor.Center
                y = (-0.12f).pct
                color = themed { palette.accent }
            }.also { +it }

            acronym = Text().apply {
                anchor = Anchor.Center
                origin = Anchor.Center
                y = 0.28f.pct
                text = mod.acronym
                fontSize = themed { typography.smallBody.size }
                color = themed { palette.accent }
            }.also { +it }
        }

        shouldUpdateTexture = false
    }


    override fun onAttached() {
        if (shouldUpdateTexture) {
            setupContent()
        }

        super.onAttached()
    }

    override fun onUpdate(clock: FrameClock) {
        if (shouldUpdateTexture) {
            setupContent()
        }

        val glyph = glyph
        if (glyph != null && glyph.measuredSize.y > 0f) {
            glyph.scale = bounds.height * GLYPH_HEIGHT / glyph.measuredSize.y
        }

        val acronym = acronym
        if (acronym != null && acronym.measuredSize.y > 0f) {
            acronym.scale = bounds.height * ACRONYM_HEIGHT / acronym.measuredSize.y
        }

        radius = CornerRadius((bounds.height * 0.2f).px)

        super.onUpdate(clock)
    }

    override fun onSkinChanged() {
        shouldUpdateTexture = true
    }


    private companion object {
        // The heights of the glyph and the acronym, relative to the icon's.
        const val GLYPH_HEIGHT = 0.5f
        const val ACRONYM_HEIGHT = 0.3f
    }
}


/**
 * The Font Awesome glyph (an [FAIcon], of the solid family) that stands for [mod] where the skin has no texture for it.
 */
fun modIconGlyph(mod: Mod): Int = when (mod) {
    is ModApproachDifferent -> FAIcon.Bullseye
    is ModAutopilot -> FAIcon.Plane
    is ModAutoplay -> FAIcon.Robot
    is ModCustomSpeed -> FAIcon.GaugeHigh
    is ModDifficultyAdjust -> FAIcon.Sliders
    is ModDoubleTime -> FAIcon.ForwardFast
    is ModEasy -> FAIcon.Feather
    is ModFlashlight -> FAIcon.Lightbulb
    is ModFreezeFrame -> FAIcon.Snowflake
    is ModHalfTime -> FAIcon.HourglassHalf
    is ModHardRock -> FAIcon.HandBackFist
    is ModHidden -> FAIcon.EyeSlash
    is ModMirror -> FAIcon.ArrowsLeftRight
    is ModMuted -> FAIcon.VolumeXmark
    is ModNightCore -> FAIcon.Drum
    is ModNoFail -> FAIcon.ShieldHalved
    is ModPerfect -> FAIcon.Star
    is ModPrecise -> FAIcon.Crosshairs
    is ModRandom -> FAIcon.Shuffle
    is ModReallyEasy -> FAIcon.Baby
    is ModRelax -> FAIcon.MugHot
    is ModReplayV6 -> FAIcon.ClockRotateLeft
    is ModScoreV2 -> FAIcon.Trophy
    is ModSmallCircle -> FAIcon.Compress
    is ModSuddenDeath -> FAIcon.Skull
    is ModSynesthesia -> FAIcon.Palette
    is ModTraceable -> FAIcon.Ghost
    is ModWindDown -> FAIcon.ArrowTrendDown
    is ModWindUp -> FAIcon.ArrowTrendUp
    else -> FAIcon.Question
}
