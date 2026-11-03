package com.osudroid.ui.v2.songselect

import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.px
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.UIIcon
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.container
import com.reco1l.verktex.ui.control.UITextInput
import com.reco1l.verktex.ui.iconButton

class SongSelectFilterBar : UILinearContainer() {

    init {
        width = Dimension.FillAvailable
        anchor = Anchor.TopRight
        origin = Anchor.TopRight
        padding = Padding(32f.px, 16f.px)
        spacing = 14f.px

        iconButton {
            icon = FAIcon.Filter
        }

        container {
            width = Dimension.FillAvailable
            height = Dimension.FillAvailable

            +object : UITextInput("") {

                private var lastValueChangeMillis = -1L

                init {
                    key = "query"
                    width = Dimension.FillAvailable
                    height = Dimension.FillAvailable
                }

                override fun onValueChanged() {
                    super.onValueChanged()
                    lastValueChangeMillis = System.currentTimeMillis()
                }

                override fun onUpdate(clock: FrameClock) {
                    super.onUpdate(clock)

                    if (lastValueChangeMillis > 0L && System.currentTimeMillis() - lastValueChangeMillis > 300) {
                        lastValueChangeMillis = -1L
                        //submit()
                    }
                }
            }

            +UIIcon(FAIcon.MagnifyingGlass).apply {
                anchor = Anchor.CenterRight
                origin = Anchor.CenterRight
                x = (-12f).px
                style = Style { theme -> color = theme.palette.accent }
            }
        }
    }
}
