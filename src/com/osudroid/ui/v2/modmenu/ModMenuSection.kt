package com.osudroid.ui.v2.modmenu

import com.reco1l.framework.forEachTolerant
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.data.ClipMode
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.ui.UIButton
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.UIScrollableContainer
import com.reco1l.verktex.ui.text.UIText

@Suppress("LeakingThis")
open class ModMenuSection(name: String, private val toggles: List<UIButton> = listOf()) : UILinearContainer() {

    protected val toggleContainer: UILinearContainer


    init {
        orientation = Orientation.Vertical
        height = Dimension.FillAvailable
        style = Style { theme ->
            width = 224f.dip
            backgroundColor = theme.palette.accent * 0.1f
            radius = theme.radius.lg
        }

        +UIText().apply {
            width = Dimension.FillAvailable
            text = name.uppercase()
            alignment = Anchor.Center
            style = Style { theme ->
                fontSize = theme.typography.smallCaption.size
                padding = Padding(12f.dip)
                color = theme.palette.accent
                alpha = 0.75f
            }
        }

        +UIScrollableContainer().apply {
            scrollAxes = Axis.Y
            width = Dimension.FillAvailable
            height = Dimension.FillAvailable
            clipping = ClipMode.Bounds

            +UILinearContainer().apply {
                width = Dimension.FillAvailable
                orientation = Orientation.Vertical
                padding = Padding(8f.dip, 0f.dip, 8f.dip, 8f.dip)
                spacing = 8f.dip

                toggles.forEachTolerant { +it }
                toggleContainer = this
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
