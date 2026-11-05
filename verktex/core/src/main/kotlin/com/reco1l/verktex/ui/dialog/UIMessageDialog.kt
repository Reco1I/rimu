package com.reco1l.verktex.ui.dialog

import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.ui.text.UIText

/**
 * A dialog showing a text message.
 */
open class UIMessageDialog : UIDialog<UIText>(
    innerContent = UIText().apply {
        width = fill()
        alignment = Anchor.TopCenter
        style = Style { theme -> padding = Padding(theme.gap.lg) }
    }
) {

    /**
     * The text of the dialog.
     */
    var text by innerContent::text

}
