package com.reco1l.verktex.ui

import com.reco1l.verktex.*
import com.reco1l.verktex.theme.Radius
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.framework.*
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.ClipMode
import com.reco1l.verktex.data.Dimension
import com.rian.andengine.modifier.ModifierType
import com.rian.andengine.modifier.UniversalModifier
import org.anddev.andengine.input.touch.*

@Suppress("LeakingThis")
open class UIModal(

    /**
     * The content of the modal. This is where you should add your UI elements.
     */
    val card: UIContainer = UIContainer().apply {
        anchor = Anchor.Center
        origin = Anchor.Center
        clipping = ClipMode.Bounds
        scaleOrigin = Anchor.Center
        style = Style {
            backgroundColor = it.accentColor * 0.15f
            radius = Radius.LG
        }
    }

) : UIContainer() {


    /**
     * Whether the modal shouldn't hide when the user clicks outside of the content.
     */
    var staticBackdrop = false

    /**
     * Whether the modal should be detached from the scene when hidden.
     */
    var detachOnHide = false


    init {
        style = {
            backgroundColor = Color4.Black.copy(alpha = 0.3f)
        }

        width = Dimension.FillAvailable
        height = Dimension.FillAvailable

        isVisible = false
        alpha = 0f

        card.scaleX = 0.9f
        card.scaleY = 0.9f
        +card
    }


    //region Input

    override fun onInputEvent(event: InputEvent): Boolean {
        if (!isVisible) return false

        if (!super.onInputEvent(event)) {
            if (!staticBackdrop) hide()
        }
        return true
    }

    //endregion

    //region Visibilty state

    /**
     * Creates the show animation for the modal.
     */
    open fun createShowAnimation(): () -> UniversalModifier = {
        card.scaleTo(1f, 0.2f)
        fadeIn(0.2f)
    }

    /**
     * Creates the hide animation for the modal.
     */
    open fun createHideAnimation(): () -> UniversalModifier = {
        card.scaleTo(0.9f, 0.2f)
        fadeOut(0.2f)
    }


    /**
     * Called when [show] is called. This is where you should set up the modal's animations.
     */
    protected open fun onShow() {
        // If there's no parent previously set, attach to the current scene.
        if (parent == null) {
            Verktex.addOverlay(this)
        }
    }

    /**
     * Called after all show animations are finished.
     */
    protected open fun onShown() = Unit

    /**
     * Called when [hide] is called.
     */
    protected open fun onHide() = Unit

    /**
     * Called after all hide animations are finished.
     */
    protected open fun onHidden() {
        if (detachOnHide) {
            removeSelf()
        }
    }


    /**
     * Shows the modal.
     */
    fun show() {
        if (!isVisible) {
            onShow()
            isVisible = true

            clearModifiers(false, ModifierType.ScaleXY, ModifierType.Alpha)
            createShowAnimation()().after {
                onShown()
            }
        }
    }

    /**
     * Hides the modal.
     */
    fun hide() {
        if (isVisible) {
            onHide()

            clearModifiers(false, ModifierType.ScaleXY, ModifierType.Alpha)
            createHideAnimation()().after {
                isVisible = false
                onHidden()
            }
        }
    }

    //endregion
}


