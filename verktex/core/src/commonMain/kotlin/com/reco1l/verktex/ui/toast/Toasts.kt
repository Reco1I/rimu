package com.reco1l.verktex.ui.toast

import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.fadeOut
import com.reco1l.verktex.ui.themed
import com.reco1l.verktex.ui.transformSequence
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Shows [UIToast]s at the bottom of the window, above the scene and the other overlays. They can be
 * shown and changed from any thread.
 *
 * Nothing is shown until the engine is initialized, so code that shows toasts also runs without it
 * (e.g. in tests).
 */
object Toasts {

    /**
     * How long a short message stays on screen.
     */
    val Short = 2000.milliseconds

    /**
     * How long a long message stays on screen.
     */
    val Long = 3500.milliseconds

    private const val FADE_DURATION = 0.2f

    private val container = UILinearContainer().apply {
        orientation = UILinearContainer.Orientation.Vertical
        width = fill()
        anchor = Anchor.BottomCenter
        origin = Anchor.BottomCenter
        spacing = themed { gap.sm }
        padding = Padding(bottom = 48f.dip)
    }


    /**
     * Shows [text] for [Short].
     */
    @JvmStatic
    fun show(text: String) = show(text, Short)

    /**
     * Shows [text] for [Long].
     */
    @JvmStatic
    fun showLong(text: String) = show(text, Long)

    /**
     * Shows [text] for [duration], see [Short] and [Long].
     *
     * @param action The label of a button at the end of the toast, which calls [onAction] and hides
     * the toast, none if null.
     */
    fun show(text: String, duration: Duration, action: String? = null, onAction: () -> Unit = {}): Toast {
        val toast = Toast(text, title = "", duration)
        if (action != null) {
            toast.setAction(action) {
                onAction()
                toast.dismiss()
            }
        }
        return toast.also(::add)
    }

    /**
     * Shows a toast that stays until [Toast.dismiss] is called, e.g. for the progress of a task.
     */
    @JvmStatic
    @JvmOverloads
    fun showPersistent(text: String, title: String = "", isIndeterminate: Boolean = false): Toast {
        return Toast(text, title, duration = null).also {
            it.isIndeterminate = isIndeterminate
            add(it)
        }
    }

    private fun add(toast: Toast) = update {
        // Toasts stay above what was added since, e.g. a dialog.
        container.removeSelf()
        Verktex.addOverlay(container)

        container += toast.component
        toast.component.alpha = 0f

        val sequence = toast.component.transformSequence { fadeIn(FADE_DURATION, Easing.OutQuad) }
        val duration = toast.duration ?: return@update

        sequence.then().delay(duration.inWholeMilliseconds / 1000f).fadeOut(FADE_DURATION).after { remove(toast) }
    }

    private fun remove(toast: Toast) {
        container -= toast.component
        if (container.childCount == 0) {
            container.removeSelf()
        }
    }

    internal fun dismiss(toast: Toast) = update {
        toast.component.fadeOut(FADE_DURATION, Easing.OutQuad).after { remove(toast) }
    }

    /**
     * Changes the components from any thread, holding the tree lock as the engine's workers do.
     */
    internal fun update(block: () -> Unit) {
        if (!Verktex.isInitialized) return
        Verktex.withTreeLock(block)
    }


    /**
     * A toast on screen. Its properties can be changed from any thread.
     */
    class Toast internal constructor(text: String, title: String, internal val duration: Duration?) {

        // Created on first use, so nothing of the UI is built while the engine isn't initialized.
        internal val component by lazy {
            UIToast().apply {
                this.text = text
                this.title = title
            }
        }

        /**
         * The message of the toast.
         */
        var text: String = text
            set(value) {
                field = value
                update { component.text = value }
            }

        /**
         * The title above the message, none if empty.
         */
        var title: String = title
            set(value) {
                field = value
                update { component.title = value }
            }

        /**
         * The progress of a task, from 0 to 1, or null for no progress bar.
         */
        var progress: Float? = null
            set(value) {
                field = value
                update { component.progress = value }
            }

        /**
         * Whether a task runs without a known progress, shown as a loading indicator.
         */
        var isIndeterminate = false
            set(value) {
                field = value
                update { component.isIndeterminate = value }
            }

        /**
         * Shows a button labeled [text] at the end of the toast, see [UIToast.setAction].
         */
        fun setAction(text: String?, onClick: () -> Unit = {}) = update { component.setAction(text, onClick) }

        /**
         * Hides the toast.
         */
        fun dismiss() = dismiss(this)
    }
}
