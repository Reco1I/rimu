package com.reco1l.verktex.ui.dialog

import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.theme.FontSize
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.ui.text.UIText
import com.reco1l.verktex.data.Vec4

open class UIMessageDialog : UIDialog<UIText>(
    innerContent = UIText().apply {
        width = Dimension.FillAvailable
        fontSize = FontSize.SM
        alignment = Anchor.Center
        padding = Vec4(24f)

        style = {
            color = it.accentColor
        }
    }
) {

    /**
     * The text of the dialog.
     */
    var text by innerContent::text

}