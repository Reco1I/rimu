package com.osudroid.ui.v2.modmenu

import com.reco1l.verktex.*
import com.reco1l.verktex.theme.FontSize
import com.reco1l.verktex.theme.Radius
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.theme.srem
import com.reco1l.verktex.ui.*
import com.reco1l.verktex.ui.container.UIFillContainer
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.UIScrollableContainer
import com.reco1l.verktex.ui.text.UIText
import com.reco1l.toolkt.kotlin.*
import com.reco1l.verktex.data.Vec4
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Axis

@Suppress("LeakingThis")
open class ModMenuSection(name: String, private val toggles: List<UIButton> = listOf()) : UIFillContainer() {

    protected val toggleContainer: UILinearContainer


    init {
        orientation = Orientation.Vertical
        height = Dimension.FillAvailable
        cullingMode = CullingMode.ScreenBounds
        style = {
            width = 14f.rem
            backgroundColor = it.accentColor * 0.1f
            radius = Radius.LG
        }

        +UIText().apply {
            width = Dimension.FillAvailable
            text = name.uppercase()
            alignment = Anchor.Center
            style = {
                fontSize = FontSize.XS
                padding = Vec4(3f.srem)
                color = it.accentColor
                alpha = 0.75f
            }
        }

        +UIScrollableContainer().apply {
            scrollAxes = Axis.Y
            width = Dimension.FillAvailable
            height = Dimension.FillAvailable
            clipToBounds = true

            +UILinearContainer().apply {
                width = Dimension.FillAvailable
                orientation = Orientation.Vertical
                style = {
                    padding = Vec4(2f.srem, 0f, 2f.srem, 2f.srem)
                    spacing = 2f.srem
                }

                toggles.fastForEach { +it }
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

        toggles.fastForEach {
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