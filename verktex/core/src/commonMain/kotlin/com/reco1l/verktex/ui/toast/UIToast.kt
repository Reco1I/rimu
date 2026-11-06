package com.reco1l.verktex.ui.toast

import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.ui.UIButton
import com.reco1l.verktex.ui.UILoadingIndicator
import com.reco1l.verktex.ui.UITextButton
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.shape.UIBox
import com.reco1l.verktex.ui.text.UIText
import com.reco1l.verktex.ui.themed

/**
 * A short message shown above the scene, see [Toasts]. It has a [text], an optional [title] above it,
 * optionally a progress bar ([progress]) or a loading indicator ([isIndeterminate]), and a button at
 * its end ([setAction]).
 */
open class UIToast : UILinearContainer() {

    private val loadingIndicator = UILoadingIndicator().apply {
        width = 20f.dip
        height = 20f.dip
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterLeft
        isVisible = false
    }

    private val titleText = UIText().apply {
        width = fill()
        fontSettings = themed { typography.caption }
        color = themed { palette.accent }
        isVisible = false
    }

    private val messageText = UIText().apply {
        width = fill()
        fontSettings = themed { typography.smallBody }
    }

    private val progressFill = UIBox().apply {
        height = fill()
        width = fill(0f)
        backgroundColor = themed { palette.accent }
        radius = 2f.dip
    }

    private val progressTrack = UIContainer().apply {
        width = fill()
        height = 4f.dip
        backgroundColor = themed { palette.controlInactiveBackground }
        radius = 2f.dip
        isVisible = false
        +progressFill
    }

    private val actionButton = UITextButton().apply {
        anchor = Anchor.CenterRight
        origin = Anchor.CenterRight
        variant = UIButton.Variant.Ghost
        density = UIButton.Density.Small
        isVisible = false
    }

    /**
     * The title above the text, none if empty.
     */
    var title: String = ""
        set(value) {
            field = value
            titleText.text = value
            titleText.isVisible = value.isNotEmpty()
        }

    /**
     * The message of the toast.
     */
    var text by messageText::text

    /**
     * The progress of a task, from 0 to 1, shown as a bar below the text, or null for no bar.
     */
    var progress: Float? = null
        set(value) {
            field = value
            progressTrack.isVisible = value != null
            progressFill.width = fill(value?.coerceIn(0f, 1f) ?: 0f)
        }

    /**
     * Whether a task runs without a known progress, shown as a loading indicator.
     */
    var isIndeterminate: Boolean
        get() = loadingIndicator.isVisible
        set(value) {
            loadingIndicator.isVisible = value
        }


    init {
        orientation = Orientation.Horizontal
        anchor = Anchor.TopCenter
        origin = Anchor.TopCenter
        width = 360f.dip
        maxWidth = fill(0.9f)
        spacing = themed { gap.md }
        padding = themed { Padding(gap.md) }
        backgroundColor = themed { palette.surface }
        radius = themed { radius.lg }

        +loadingIndicator
        +UILinearContainer().apply {
            orientation = Orientation.Vertical
            width = fill()
            spacing = themed { gap.xs }

            +titleText
            +messageText
            +progressTrack
        }
        +actionButton
    }


    /**
     * Shows a button labeled [text] at the end of the toast, which calls [onClick], or removes it
     * when [text] is null.
     */
    fun setAction(text: String?, onClick: () -> Unit = {}) {
        actionButton.isVisible = text != null
        actionButton.text = text ?: ""
        actionButton.onActionUp = { onClick() }
    }
}
