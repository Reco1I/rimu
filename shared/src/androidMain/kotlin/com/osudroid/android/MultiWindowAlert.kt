package com.osudroid.android

import com.osudroid.resources.Res
import com.osudroid.utils.updateThread
import com.reco1l.verktex.ui.dialog.UIMessageDialog

/**
 * The alert shown while the game is in multi-window mode, which it doesn't support. It can't be
 * dismissed, only left by exiting the game.
 */
object MultiWindowAlert {

    private var dialog: UIMessageDialog? = null


    /**
     * Shows the alert, whose button calls [onExit]. Any thread can call it.
     */
    @JvmStatic
    fun show(onExit: Runnable) = updateThread {
        val dialog = dialog ?: UIMessageDialog().apply {
            title = Res.string.multi_mode_window_alert_title
            text = Res.string.multi_mode_window_alert_message
            staticBackdrop = true

            addButton {
                text = Res.string.accessibility_detector_exit
                onActionUp = { onExit.run() }
            }
        }.also { dialog = it }

        if (!dialog.isShown) {
            dialog.show()
        }
    }

    /**
     * Hides the alert, if it's shown. Any thread can call it.
     */
    @JvmStatic
    fun hide() = updateThread {
        dialog?.hide()
    }
}
