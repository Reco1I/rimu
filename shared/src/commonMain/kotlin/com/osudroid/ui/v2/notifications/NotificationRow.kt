package com.osudroid.ui.v2.notifications

import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Background
import com.reco1l.verktex.data.CornerRadius
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.ui.Button
import com.reco1l.verktex.ui.LoadingIndicator
import com.reco1l.verktex.ui.TextButton
import com.reco1l.verktex.ui.container.Container
import com.reco1l.verktex.ui.shape.Box
import com.reco1l.verktex.ui.themed
import com.reco1l.verktex.ui.toast.Toast

/**
 * A notification in the [NotificationCenter]: a [Toast] with what a task shows while it runs, a
 * loading indicator ([isIndeterminate]) or a progress bar ([progress]), and a button ([setAction]).
 */
class NotificationRow : Toast() {

    private val loadingIndicator = LoadingIndicator().apply {
        width = 20f.dip
        height = 20f.dip
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterLeft
        isVisible = false
    }

    private val progressFill = Box().apply {
        height = fill()
        width = fill(0f)
        color = themed { palette.accent }
        radius = CornerRadius(2f.dip)
    }

    private val progressTrack = Container().apply {
        width = fill()
        height = 4f.dip
        background = Background.Color(themed { palette.controlInactiveBackground })
        radius = CornerRadius(2f.dip)
        isVisible = false
        +progressFill
    }

    private val actionButton = TextButton().apply {
        anchor = Anchor.CenterRight
        origin = Anchor.CenterRight
        variant = Button.Variant.Ghost
        density = Button.Density.Small
        isVisible = false
    }

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
        // Before the text, and the button between it and the close button.
        add(0, loadingIndicator)
        body += progressTrack
        add(childCount - 1, actionButton)
    }


    /**
     * Shows a button labeled [text] after the text, which calls [onClick], or removes it when
     * [text] is null.
     */
    fun setAction(text: CharSequence?, onClick: () -> Unit = {}) {
        actionButton.isVisible = text != null
        actionButton.text = text ?: ""
        actionButton.onActionUp = { onClick() }
    }
}
