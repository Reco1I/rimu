package com.osudroid.ui.v2.songselect

import com.reco1l.verktex.ui.borderWidth
import com.reco1l.verktex.ui.borderColor
import com.osudroid.data.*
import com.osudroid.ui.v2.*
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.px
import com.reco1l.verktex.math.interpolateColor
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.UIBadge
import com.reco1l.verktex.ui.container.UIClickableContainer
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.text.text
import ru.nsu.ccfit.zuev.osu.*

@Suppress("LeakingThis")
open class BeatmapPanel(private val beatmapSetPanel: BeatmapSetPanel, val beatmapInfo: BeatmapInfo) : UIClickableContainer() {

    private var isExpanded = false

    private var currentBorderWidth = BORDER_WIDTH.dip.toPixels()
    private var currentExtraWidth = 0f


    init {
        width = Dimension.FillAvailable
        anchor = Anchor.TopRight
        origin = Anchor.TopRight
        style = Style { theme ->
            radius = theme.radius.xl
            backgroundColor = (theme.palette.accent * 0.1f).copy(alpha = 0.6f)
            borderColor = theme.palette.accent.copy(alpha = 0.3f)
            borderWidth = BORDER_WIDTH.dip
        }

        linearContainer {
            orientation = UILinearContainer.Orientation.Horizontal
            padding = Padding(16f.dip)
            spacing = 8f.dip

            +StarRatingBadge().apply {
                rating = beatmapInfo.getStarRating().toDouble()
                density = UIBadge.Density.Small
                applyColorsInstantly()
            }

            text {
                text = beatmapInfo.version
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                style = Style { theme ->
                    fontSettings = theme.typography.h3
                    fontSize = theme.typography.body.size
                    color = theme.palette.accent
                }
            }
        }

        onActionUp = {
            if (SongSelect.selectedBeatmap == beatmapInfo && beatmapSetPanel.selectedPanel == this) {
                SongSelect.onBeatmapConfirmed(beatmapInfo)
            } else {
                SkinSounds["menuclick"].play()
            }

            beatmapSetPanel.selectedPanel = this
        }
    }

    override fun onUpdate(clock: FrameClock) {
        val accent = Verktex.theme.palette.accent

        val targetBorderColor = if (isExpanded) accent else accent.copy(alpha = 0.3f)
        val targetBorderWidth = (if (isExpanded) EXPANDED_BORDER_WIDTH else BORDER_WIDTH).dip.toPixels()
        val targetExtraWidth = if (isExpanded) EXPANDED_EXTRA_WIDTH.dip.toPixels() else 0f

        borderColor = clock.interpolateColor(0.1f, borderColor, targetBorderColor)

        currentBorderWidth = clock.interpolateWithSnap(0.1f, currentBorderWidth, targetBorderWidth, 0.5f)
        borderWidth = currentBorderWidth.px

        currentExtraWidth = clock.interpolateWithSnap(0.1f, currentExtraWidth, targetExtraWidth, 0.5f)
        translationX = (-currentExtraWidth).px

        super.onUpdate(clock)
    }


    fun expand() {
        isExpanded = true
    }

    fun collapse() {
        isExpanded = false
    }


    private companion object {
        const val BORDER_WIDTH = 1f
        const val EXPANDED_BORDER_WIDTH = 2f
        const val EXPANDED_EXTRA_WIDTH = 48f
    }
}
