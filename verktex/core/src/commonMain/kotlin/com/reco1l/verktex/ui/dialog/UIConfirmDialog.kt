package com.reco1l.verktex.ui.dialog

import com.reco1l.verktex.ui.UIButton

/**
 * A message dialog asking the user to confirm or cancel.
 */
open class UIConfirmDialog : UIMessageDialog() {

    /**
     * Callback invoked when the user confirms the dialog.
     */
    var onConfirm: (() -> Unit)? = null

    /**
     * Callback invoked when the user cancels the dialog, including closing it by its backdrop.
     */
    var onCancel: (() -> Unit)? = null


    private var isConfirmed = false


    init {
        addButton {
            text = "Yes"
            variant = UIButton.Variant.Solid
            onActionUp = {
                isConfirmed = true
                onConfirm?.invoke()
                hide()
            }
        }

        addButton {
            text = "No"
            onActionUp = { hide() }
        }
    }

    override fun onShow() {
        super.onShow()
        isConfirmed = false
    }

    override fun onHide() {
        super.onHide()
        if (!isConfirmed) {
            onCancel?.invoke()
        }
    }

}
