package com.osudroid.ui

import com.osudroid.ui.v2.SkinTextures
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.px
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.PointerEvent
import com.reco1l.verktex.ui.UISprite
import com.reco1l.verktex.ui.clearTransforms
import com.reco1l.verktex.ui.container.UILinearContainer.Orientation
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.moveTo
import com.reco1l.verktex.ui.text.UIText
import kotlin.math.abs
import kotlin.math.round
import com.reco1l.verktex.localization.formatString

/**
 * The panel that slides down from the top of the results screen with the changes in the player's ranking.
 */
class SendingPanel(
    private val overallRank: Long,
    private val score: Long,
    private val accuracy: Float,
    private val pp: Float
) : UIContainer() {

    private val dismissButton = DismissButton().also {
        it.anchor = Anchor.BottomCenter
        it.origin = Anchor.TopCenter
        it.setText("Sending...")
    }

    private val mapRankColumn = Column("Map Rank")
    private val overallRankColumn = Column("Overall Rank")
    private val accuracyColumn = Column("Accuracy")
    private val scoreColumn = Column("Score")
    private val ppColumn = Column("Performance")


    init {
        anchor = Anchor.TopCenter
        origin = Anchor.TopCenter
        minWidth = 800f.px
        minHeight = 200f.px
        backgroundColor = Color4(0f, 0f, 0f, 0.7f)

        +UILinearContainer().apply {
            orientation = Orientation.Vertical
            anchor = Anchor.TopCenter
            origin = Anchor.TopCenter
            spacing = 10f.dip
            padding = Padding(10f.dip)

            +UIText().apply {
                anchor = Anchor.TopCenter
                origin = Anchor.TopCenter
                text = "Overall Ranking"
            }

            +UILinearContainer().apply {
                spacing = 10f.px

                +mapRankColumn
                +overallRankColumn
                +accuracyColumn
                +scoreColumn
                +ppColumn
            }
        }

        +dismissButton

        // Hidden above the screen until the results arrive.
        translationY = HIDDEN_POSITION.px
    }


    fun show(newMapRank: Long, newOverallRank: Long, newScore: Long, newAccuracy: Float, newPP: Float) {
        dismissButton.canBeDismissed = true

        // Map rank column is special, as its color is different when ranking up
        mapRankColumn.update("#$newMapRank", 0f)

        if (newScore > score) {
            mapRankColumn.valueColor = Color4(1f, 1f, 0f, 0.8f)
        }

        overallRankColumn.update(
            when {
                newOverallRank == overallRank -> "#$newOverallRank"
                newOverallRank < overallRank -> "#$newOverallRank\n(+${overallRank - newOverallRank})"
                else -> "#$newOverallRank\n(-${newOverallRank - overallRank})"
            },
            (overallRank - newOverallRank).toFloat()
        )

        accuracyColumn.update(
            when {
                abs(newAccuracy - accuracy) < 0.0001f -> formatString("%.2f%%", newAccuracy * 100f)
                newAccuracy < accuracy -> formatString("%.2f%%\n(%.2f%%)", newAccuracy * 100f, (newAccuracy - accuracy) * 100f)
                else -> formatString("%.2f%%\n(+%.2f%%)", newAccuracy * 100f, (newAccuracy - accuracy) * 100f)
            },
            newAccuracy - accuracy
        )

        scoreColumn.update(
            when {
                newScore == score -> formatScore(newScore)
                newScore < score -> "${formatScore(newScore)}\n(${formatScore(newScore - score)})"
                else -> "${formatScore(newScore)}\n(+${formatScore(newScore - score)})"
            },
            (newScore - score).toFloat()
        )

        ppColumn.update(
            // For PP, we only want to show significant changes.
            when {
                round(newPP) - round(pp) < 1 -> formatString("%.0fpp", round(newPP))
                else -> formatString("%.0fpp\n(+%.0fpp)", round(newPP), round(newPP) - round(pp))
            },
            round(newPP - pp)
        )

        dismissButton.setText("Dismiss")

        clearTransforms()
        moveTo(0f, 0f, 0.5f)
    }

    fun setFail() {
        dismissButton.setText("Failed")
    }

    private fun dismiss() {
        clearTransforms()
        moveTo(0f, HIDDEN_POSITION, 0.5f)
    }

    private fun formatScore(score: Long) = score
        .toString()
        // Reverse the order
        .reversed()
        // Split into 3 characters each
        .chunked(3)
        // Join them
        .joinToString(" ") { it }
        // Reverse back to maintain original order
        .reversed()


    private inner class DismissButton : UIContainer() {

        var canBeDismissed = false

        private val text = UIText().also {
            it.anchor = Anchor.Center
            it.origin = Anchor.Center
        }

        init {
            +UISprite(SkinTextures["ranking_button"])
            +text
        }

        fun setText(value: String) {
            Verktex.withTreeLock { text.text = value }
        }

        override fun onInputEvent(event: InputEvent): Boolean {
            if (canBeDismissed && event is PointerEvent && event.action == PointerEvent.Action.Up && containsScenePoint(event.x, event.y)) {
                canBeDismissed = false
                dismiss()
                return true
            }

            return event is PointerEvent && canBeDismissed && containsScenePoint(event.x, event.y)
        }
    }

    private class Column(caption: String) : UILinearContainer() {

        private val valueText = UIText().also {
            it.anchor = Anchor.Center
            it.origin = Anchor.Center
            it.alignment = Anchor.Center
        }

        private val valueBox = UIContainer().also {
            it.width = Dimension.FillAvailable
            it.padding = Padding(4f.dip)
            it.backgroundColor = Color4(0.4f, 0.4f, 0.4f, 0.8f)
            it.minHeight = 40f.dip
            it += valueText
        }

        var valueColor: Color4
            get() = valueBox.backgroundColor
            set(value) {
                valueBox.backgroundColor = value
            }

        init {
            orientation = Orientation.Vertical
            minWidth = 100f.px

            +UIText().apply {
                anchor = Anchor.TopCenter
                origin = Anchor.TopCenter
                text = caption
            }

            +valueBox
        }

        fun update(value: String, difference: Float) {
            valueText.text = value

            valueColor = when {
                difference > 0 -> Color4(0f, 1f, 0f, 0.5f)
                difference < 0 -> Color4(1f, 0f, 0f, 0.5f)
                else -> Color4(0.4f, 0.4f, 0.4f, 0.8f)
            }
        }
    }


    private companion object {
        const val HIDDEN_POSITION = -1000f
    }
}
