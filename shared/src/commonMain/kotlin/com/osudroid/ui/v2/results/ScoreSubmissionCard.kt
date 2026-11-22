package com.osudroid.ui.v2.results

import com.osudroid.resources.Res.string
import com.osudroid.utils.formatNumber
import com.osudroid.utils.updateThread
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.localization.formatString
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.container.LinearContainer
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.loadingIndicator
import com.reco1l.verktex.ui.text.Text
import com.reco1l.verktex.ui.text.text
import com.reco1l.verktex.ui.themed
import kotlin.math.abs
import kotlin.math.round

/**
 * The submission of a score to the server, with the player's ranking before it ([overallRank],
 * [score], [accuracy], [pp]): it says it's being submitted until [show] gives the ranking after it, or
 * [setFail] says it failed. Both can be called from any thread.
 */
class ScoreSubmissionCard(
    private val overallRank: Long,
    private val score: Long,
    private val accuracy: Float,
    private val pp: Float,
) : ResultsSection(string.results_submission, FAIcon.CloudArrowUp) {

    private lateinit var status: LinearContainer
    private lateinit var statusText: Text
    private lateinit var tiles: LinearContainer

    init {
        status = linearContainer {
            spacing = 10f.dip

            loadingIndicator {
                width = 18f.dip
                height = 18f.dip
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
            }

            statusText = text {
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                text = string.results_submitting
                fontSettings = themed { typography.body }
            }
        }

        tiles = linearContainer {
            width = fill()
            spacing = 8f.dip
            isVisible = false
        }
    }


    fun show(newMapRank: Long, newOverallRank: Long, newScore: Long, newAccuracy: Float, newPP: Float) {
        updateThread { showRanking(newMapRank, newOverallRank, newScore, newAccuracy, newPP) }
    }

    private fun showRanking(newMapRank: Long, newOverallRank: Long, newScore: Long, newAccuracy: Float, newPP: Float) {
        status.isVisible = false
        tiles.isVisible = true
        tiles.clearChildren()

        // The map rank is gold when the score beats the player's best on the beatmap.
        tiles += tile(string.results_map_rank, "#${formatNumber(newMapRank)}", null, if (newScore > score) Color4("#FFCC22") else null)

        tiles += tile(
            string.results_overall_rank,
            "#${formatNumber(newOverallRank)}",
            when {
                newOverallRank < overallRank -> "+${formatNumber(overallRank - newOverallRank)}"
                newOverallRank > overallRank -> "-${formatNumber(newOverallRank - overallRank)}"
                else -> null
            },
            null,
            isGain = newOverallRank < overallRank
        )

        tiles += tile(
            string.results_accuracy,
            formatString("%.2f%%", newAccuracy * 100f),
            if (abs(newAccuracy - accuracy) < 0.0001f) null else formatString("%+.2f%%", (newAccuracy - accuracy) * 100f),
            null,
            isGain = newAccuracy > accuracy
        )

        tiles += tile(
            string.results_ranked_score,
            formatNumber(newScore),
            if (newScore == score) null else (if (newScore > score) "+" else "-") + formatNumber(abs(newScore - score)),
            null,
            isGain = newScore > score
        )

        // Only significant changes of pp are shown.
        tiles += tile(
            string.results_performance,
            formatString("%.0fpp", round(newPP)),
            if (round(newPP) - round(pp) < 1) null else formatString("+%.0fpp", round(newPP) - round(pp)),
            null,
            isGain = true
        )
    }

    fun setFail() {
        updateThread { showFailure() }
    }

    private fun showFailure() {
        status.isVisible = true
        tiles.isVisible = false
        status.forEach { if (it !is Text) it.isVisible = false }
        statusText.text = string.results_submission_failed
        statusText.color = Color4("#ED1121")
    }

    private fun tile(label: CharSequence, value: String, change: String?, valueColor: Color4?, isGain: Boolean = false) =
        ResultsTile(label, value).apply {
            valueText.fontSettings = themed { typography.h3 }

            if (valueColor != null) {
                valueText.color = valueColor
            }

            if (change != null) {
                +Text().apply {
                    text = change
                    fontSettings = themed { typography.smallCaption }
                    color = if (isGain) Color4("#88DA20") else Color4("#ED1121")
                }
            }
        }
}
