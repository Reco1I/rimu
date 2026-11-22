package com.osudroid.ui.v2.results

import com.osudroid.multiplayer.Multiplayer
import com.osudroid.multiplayer.api.data.WinCondition
import com.osudroid.scoring.Statistic
import com.osudroid.utils.formatNumber
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.data.Background
import com.reco1l.verktex.data.ClipMode
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.CornerRadius
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.data.wrap
import com.reco1l.verktex.localization.formatString
import com.reco1l.verktex.ui.Button
import com.reco1l.verktex.ui.container.LinearContainer
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.container.scrollableContainer
import com.reco1l.verktex.ui.text.text
import com.reco1l.verktex.ui.themed

/**
 * The scores of a multiplayer match, [selected] highlighted: pressing another shows its results
 * through [onSelect].
 */
class ResultsLeaderboard(
    stats: Array<Statistic>,
    private val selected: Statistic?,
    private val onSelect: (Statistic) -> Unit,
) : LinearContainer() {

    init {
        orientation = Orientation.Vertical
        width = 230f.dip
        height = fill()
        spacing = 8f.dip
        padding = Padding(12f.dip)
        radius = themed { CornerRadius(radius.lg) }
        background = Background.Color(themed { palette.surface.alpha(0.82f) })

        text {
            text = com.osudroid.resources.Res.string.results_match
            fontSettings = themed { typography.caption }
            color = themed { palette.accent.alpha(0.75f) }
        }

        scrollableContainer {
            scrollAxes = Axis.Y
            width = fill()
            height = fill()
            clipping = ClipMode.Bounds
            showVerticalIndicator = false

            linearContainer {
                orientation = Orientation.Vertical
                width = fill()
                spacing = 6f.dip

                stats.forEachIndexed { index, stat -> +Row(index, stat) }
            }
        }
    }


    private inner class Row(index: Int, private val stat: Statistic) : Button() {

        init {
            width = fill()
            variant = Variant.Surface
            height = wrap()
            isSelected = stat.playerName == selected?.playerName
            padding = Padding(10f.dip, 8f.dip)

            onActionUp = {
                if (stat !== selected) {
                    onSelect(stat)
                }
            }

            linearContainer {
                orientation = Orientation.Vertical
                width = fill()
                // Room for the position.
                paddingRight = 40f.dip

                text {
                    text = stat.playerName
                    wrapText = false
                    maxLines = 1
                    width = fill()
                    fontSettings = themed { typography.body }
                    // A player who failed is red.
                    color = themed { if (stat.isAlive) palette.accent else Color4("#ED1121") }
                }

                text {
                    text = formatNumber(stat.totalScoreWithMultiplier)
                    fontSettings = themed { typography.smallCaption }
                    color = themed { palette.accent.alpha(0.75f) }
                }

                text {
                    text = when (Multiplayer.room?.winCondition) {
                        WinCondition.Accuracy -> formatString("%.2f%%", stat.accuracy * 100f)
                        else -> "${formatNumber(stat.scoreMaxCombo)}x"
                    }
                    fontSettings = themed { typography.smallCaption }
                    color = themed { palette.accent.alpha(0.75f) }
                }
            }

            text {
                anchor = Anchor.CenterRight
                origin = Anchor.CenterRight
                text = "#${index + 1}"
                fontSettings = themed { typography.h3 }
                color = themed { palette.accent.alpha(0.5f) }
            }
        }
    }
}
