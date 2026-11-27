package com.osudroid.ui.v2.results

import com.reco1l.verktex.BuiltIn
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.fonts.Font
import com.reco1l.verktex.ui.text.Text
import com.reco1l.verktex.ui.text.TextEffect
import com.reco1l.verktex.ui.text.TextGradient

/**
 * The rank of a score ([com.osudroid.scoring.Statistic.mark]: XH, SH, X, S, A, B, C or D), drawn as
 * text in the rank's color with an outline, a glow and a shadow.
 */
class RankText(mark: String, size: Dimension.Fixed) : Text() {

    init {
        val rankColor = rankColor(mark)

        text = rankLabel(mark)
        fontFamily = BuiltIn.Fonts.Fredoka
        fontWeight = Font.Weight.Bold
        fontSize = size
        wrapText = false

        // Room for the effects, which are drawn around the glyphs.
        padding = Padding(24f.dip)

        // From almost white at the top to the rank's color.
        val top = Color4(
            rankColor.red + (1f - rankColor.red) * 0.7f,
            rankColor.green + (1f - rankColor.green) * 0.7f,
            rankColor.blue + (1f - rankColor.blue) * 0.7f,
        )

        fillGradient = TextGradient(top, rankColor)
        effects = listOf(
            TextEffect.Shadow(Color4(0f, 0f, 0f, 0.55f), offsetY = 6f.dip, softness = 10f.dip),
            TextEffect.Glow(rankColor, 22f.dip, 1.3f),
            TextEffect.Outline(rankColor.darken(1.1f), 2f.dip),
        )
    }
}

/**
 * The rank as it's shown: the perfect ranks X and XH are SS.
 */
fun rankLabel(mark: String) = when (mark) {
    "X", "XH" -> "SS"
    "SH" -> "S"
    else -> mark
}

/**
 * The color of a rank: gold for SS and S, silver for their Hidden or Flashlight variants.
 */
fun rankColor(mark: String) = when (mark) {
    "X", "S" -> Color4("#FFCC22")
    "XH", "SH" -> Color4("#D6E0EB")
    "A" -> Color4("#88DA20")
    "B" -> Color4("#2F9EEA")
    "C" -> Color4("#A85CF0")
    else -> Color4("#ED1121")
}
