package com.osudroid.ui.v2.settings

import com.reco1l.verktex.ui.toast.Toasts

/**
 * The editor of the areas of the screen that ignore touches in gameplay (`BlockArea`s in the
 * database). It was an Android view and isn't written on Verktex yet.
 */
object BlockAreaEditor {

    /**
     * Shows the editor. Until it's written, it only tells that it's a work in progress.
     */
    fun show() {
        Toasts.show("The block area editor is a work in progress.")
    }
}
