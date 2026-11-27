package com.osudroid.ui.v2.results

import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Background
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.CornerRadius
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.ui.container.Container
import com.reco1l.verktex.ui.container.LinearContainer
import com.reco1l.verktex.ui.container.container
import com.reco1l.verktex.ui.shape.box
import com.reco1l.verktex.ui.text.Text
import com.reco1l.verktex.ui.text.faSolidIcon
import com.reco1l.verktex.ui.text.text
import com.reco1l.verktex.ui.themed

inline fun Container.resultsSection(title: CharSequence, icon: Int, block: ResultsSection.() -> Unit) =
    ResultsSection(title, icon).apply(block).also(::plusAssign)

/**
 * A titled panel of the results, its content a column.
 */
open class ResultsSection(title: CharSequence, icon: Int) : LinearContainer() {

    init {
        orientation = Orientation.Vertical
        width = fill()
        spacing = 10f.dip
        padding = Padding(16f.dip, 14f.dip)
        radius = themed { CornerRadius(radius.lg) }
        background = Background.Color(themed { palette.surface.alpha(0.82f) })

        text {
            text = title
            leadingIcon = faSolidIcon(icon)
            iconSpacing = 8f.dip
            fontSettings = themed { typography.caption }
            color = themed { palette.accent.alpha(0.75f) }
        }
    }
}

inline fun Container.resultsTile(label: CharSequence, value: CharSequence, block: ResultsTile.() -> Unit = {}) =
    ResultsTile(label, value).apply(block).also(::plusAssign)

/**
 * A value of the results under its label, filling the width it's given.
 */
open class ResultsTile(label: CharSequence, value: CharSequence) : LinearContainer() {

    val labelText: Text

    val valueText: Text

    init {
        orientation = Orientation.Vertical
        width = fill()
        spacing = 2f.dip
        padding = Padding(12f.dip, 10f.dip)
        radius = themed { CornerRadius(radius.md) }
        background = Background.Color(themed { palette.surfaceRaised.alpha(0.7f) })

        labelText = text {
            text = label
            fontSettings = themed { typography.smallCaption }
            color = themed { palette.accent.alpha(0.65f) }
        }

        valueText = text {
            text = value
            fontSettings = themed { typography.h2 }
            color = themed { palette.accent }
            wrapText = false
        }
    }
}

/**
 * The count of a judgement, its label in the judgement's color, over a bar of its share of the notes.
 */
class JudgementTile(label: String, count: Int, share: Float, judgementColor: Color4) : ResultsTile(label, "${count}x") {

    init {
        labelText.color = judgementColor
        labelText.fontSettings = themed { typography.caption }

        container {
            width = fill()
            height = 4f.dip
            radius = CornerRadius(2f.dip)
            background = Background.Color(Color4.White.alpha(0.08f))

            box {
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                width = fill(share.coerceIn(0f, 1f))
                height = fill()
                radius = CornerRadius(2f.dip)
                color = judgementColor
            }
        }
    }
}

/**
 * A horizontal bar [fraction] full of [color].
 */
fun Container.progressBar(fraction: Float, color: Color4) = container {
    width = fill()
    height = 6f.dip
    radius = CornerRadius(3f.dip)
    background = Background.Color(Color4.White.alpha(0.08f))

    box {
        width = fill(fraction.coerceIn(0f, 1f))
        height = fill()
        radius = CornerRadius(3f.dip)
        this.color = color
    }
}

/**
 * Gives a tile standing on its own the surface of the sections around it.
 */
fun ResultsTile.onSurface() {
    background = Background.Color(themed { palette.surface.alpha(0.82f) })
    radius = themed { CornerRadius(radius.lg) }
    padding = Padding(16f.dip, 12f.dip)
}
