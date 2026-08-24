package com.reco1l.verktex.ui.dialog

import com.reco1l.verktex.ui.UIModal
import com.reco1l.verktex.ui.UITextButton

open class UIConfirmDialog : UIMessageDialog() {

    /**
     * Callback invoked when the user confirms the dialog.
     */
    var onConfirm: (() -> Unit)? = null

    /**
     * Callback invoked when the user cancels the dialog.
     */
    var onCancel: (() -> Unit)? = null


    init {
        addButton(UITextButton().apply {
            text = "Yes"
            isSelected = true
            onActionUp = {
                onConfirm?.invoke()
                UIModal.hide()
            }
        })

        addButton(UITextButton().apply {
            text = "No"
            onActionUp = {
                onCancel?.invoke()
                UIModal.hide()
            }
        })
    }

}