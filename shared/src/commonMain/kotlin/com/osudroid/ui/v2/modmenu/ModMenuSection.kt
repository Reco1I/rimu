package com.osudroid.ui.v2.modmenu

import com.reco1l.framework.forEachTolerant
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.data.ClipMode
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.data.verticalPadding
import com.reco1l.verktex.ui.Button
import com.reco1l.verktex.ui.container.LinearContainer
import com.reco1l.verktex.ui.container.LinearContainer.Orientation
import com.reco1l.verktex.ui.container.ScrollableContainer
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.container.scrollableContainer
import com.reco1l.verktex.ui.text.Text
import com.reco1l.verktex.ui.text.text
import com.reco1l.verktex.ui.themed

@Suppress("LeakingThis")
open class ModMenuSection(name: String, private val toggles: List<Button> = listOf()) : ScrollableContainer() {

    protected val toggleContainer: LinearContainer


    init {
        scrollAxes = Axis.Y
        width = 220f.dip
        height = fill()
        clipping = ClipMode.Bounds

        toggleContainer = linearContainer {
            width = fill()
            orientation = Orientation.Vertical
            spacing = 8f.dip
            padding = verticalPadding(8f.dip)

            text {
                width = fill()
                text = name.uppercase()
                alignment = Anchor.Center
                padding = Padding(8f.dip, 8f.dip, 8f.dip, 2f.dip)
                fontSettings = themed { typography.smallCaption }
            }

            toggles.forEachTolerant { +it }
        }

        updateVisibility()
    }

    fun updateVisibility() {
        isVisible = toggles.any { it.isVisible }
    }

    open fun onSearchTermUpdate(searchTerm: String) {
        // Not using updateVisibility() here to avoid iterating over the toggles twice.
        var anyVisible = false

        toggles.forEach {
            if (it is ModMenuToggle) {
                it.updateVisibility(searchTerm)
            }

            if (it.isVisible) {
                anyVisible = true
            }
        }

        isVisible = anyVisible
    }

}
