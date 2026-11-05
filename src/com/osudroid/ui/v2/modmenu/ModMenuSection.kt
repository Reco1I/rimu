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
import com.reco1l.verktex.ui.UIButton
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.UIScrollableContainer
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.container.scrollableContainer
import com.reco1l.verktex.ui.text.UIText
import com.reco1l.verktex.ui.text.text
import com.reco1l.verktex.ui.themed

@Suppress("LeakingThis")
open class ModMenuSection(name: String, private val toggles: List<UIButton> = listOf()) : UILinearContainer() {

    protected lateinit var toggleContainer: UILinearContainer


    init {
        height = fill()
        width = 220f.dip
        orientation = Orientation.Vertical

        text {
            width = fill()
            text = name.uppercase()
            alignment = Anchor.Center
            padding = Padding(8f.dip)
            fontSettings = themed { typography.smallCaption }
        }

        scrollableContainer {
            scrollAxes = Axis.Y
            width = fill()
            height = fill()
            clipping = ClipMode.Bounds

            toggleContainer = linearContainer {
                width = fill()
                orientation = Orientation.Vertical
                padding = verticalPadding(8f.dip)
                spacing = 8f.dip

                toggles.forEachTolerant { +it }
            }
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
