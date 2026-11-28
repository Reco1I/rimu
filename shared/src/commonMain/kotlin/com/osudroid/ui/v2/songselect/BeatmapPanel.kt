package com.osudroid.ui.v2.songselect

import com.reco1l.verktex.data.Border
import com.reco1l.verktex.data.Background
import com.reco1l.verktex.data.CornerRadius
import com.osudroid.data.*
import com.osudroid.ui.v2.*
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Shadow
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.px
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.ui.animatableColor
import com.reco1l.verktex.ui.animatableFloat
import com.reco1l.verktex.ui.animate
import com.reco1l.verktex.ui.animateColor
import com.reco1l.verktex.ui.transitionSlot
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.Badge
import com.reco1l.verktex.ui.container.ClickableContainer
import com.reco1l.verktex.ui.container.LinearContainer
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.text.text
import com.reco1l.verktex.ui.themed

@Suppress("LeakingThis")
open class BeatmapPanel(val beatmapSetPanel: BeatmapSetPanel, val beatmapInfo: BeatmapInfo) : ClickableContainer() {

    private var isExpanded = false
        set(value) {
            if (field != value) {
                field = value
                animateExpansion(value)
            }
        }

    private var currentExtraWidth = 0f

    /**
     * The horizontal offset given by the curve of the carrousel.
     */
    var curveOffset = 0f
        set(value) {
            field = value
            updateTranslation()
        }

    private var glow = 0f


    init {
        width = Dimension.FillAvailable
        anchor = Anchor.TopRight
        origin = Anchor.TopRight
        radius = themed { CornerRadius(radius.xl) }
        background = Background.Color(themed { palette.surface.alpha(0.6f) })
        border = Border(1f.dip, themed { palette.accent.alpha(0.3f) })

        linearContainer {
            orientation = LinearContainer.Orientation.Horizontal
            padding = Padding(16f.dip)
            spacing = 8f.dip

            +StarRatingBadge().apply {
                rating = beatmapInfo.getStarRating().toDouble()
                density = Badge.Density.Small
                applyColorsInstantly()
            }

            text {
                text = beatmapInfo.version
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                fontSettings = themed { typography.h3 }
                fontSize = themed { typography.body.size }
                color = themed { palette.accent }
            }
        }

        onActionLongPress = { beatmapSetPanel.beatmapCarrousel.onBeatmapSetLongPress?.invoke(beatmapSetPanel) }

        onActionUp = {
            if (beatmapSetPanel.selectedPanel == this) {
                beatmapSetPanel.beatmapCarrousel.onBeatmapConfirmed?.invoke(beatmapInfo)
            } else {
                SkinSounds["menuclick"].play()
            }

            beatmapSetPanel.selectedPanel = this
        }
    }

    override fun onUpdate(clock: FrameClock) {
        // The overhang of the carrousel can change while scrolling.
        updateTranslation()
        super.onUpdate(clock)
    }

    private val expansion = transitionSlot()

    private val extraWidthProperty = animatableFloat({ currentExtraWidth }, {
        currentExtraWidth = it
        updateTranslation()
    })

    private val glowProperty = animatableFloat({ glow }, {
        glow = it
        val accent = Verktex.theme.palette.accent
        shadow = if (it > 0f) Shadow(accent.alpha(GLOW_ALPHA * it), blur = GLOW_BLUR.dip, cutout = true) else Shadow.None
    })

    private val borderColorProperty = animatableColor({ border.color }, { border = border.withColor(it) })

    private fun animateExpansion(expanded: Boolean) {
        val accent = Verktex.theme.palette.accent

        expansion.run {
            animateColor(borderColorProperty, if (expanded) accent else accent.alpha(0.3f), EXPANSION_DURATION, Easing.OutQuint)
            animate(extraWidthProperty, if (expanded) EXPANDED_EXTRA_WIDTH.dip.toPixels() else 0f, EXPANSION_DURATION, Easing.OutQuint)
            animate(glowProperty, if (expanded) 1f else 0f, EXPANSION_DURATION, Easing.OutQuint)
        }
    }


    private fun updateTranslation() {
        translationX = (curveOffset + beatmapSetPanel.beatmapCarrousel.panelOverhang - currentExtraWidth).px
    }


    fun expand() {
        isExpanded = true
    }

    fun collapse() {
        isExpanded = false
    }


    private companion object {
        const val EXPANDED_EXTRA_WIDTH = 96f
        const val GLOW_ALPHA = 0.15f
        const val GLOW_BLUR = 24f
        const val EXPANSION_DURATION = 0.1f
    }
}
