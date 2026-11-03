package com.reco1l.verktex.ui.control

import com.reco1l.verktex.*
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.ui.UIComponent
import com.reco1l.verktex.ui.UITextButton
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.UIScrollableContainer
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.data.ClipMode
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.PointerEvent
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.math.FloatAnimation
import com.reco1l.verktex.math.interpolateFloat
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.ui.UIButton
import com.reco1l.verktex.ui.UIScene
import com.reco1l.verktex.ui.container.UILinearContainer.Orientation
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.data.px
import com.reco1l.verktex.data.wrap
import com.reco1l.verktex.time.FrameClock
import kotlin.math.*

class UIDropdown(var trigger: UIComponent) : UIScrollableContainer() {

    /**
     * Whether the dropdown menu is currently expanded or not.
     */
    val isExpanded
        get() = parentScene != null

    /**
     * A callback that is invoked when the dropdown menu is expanded.
     */
    var onExpand: (() -> Unit)? = null

    /**
     * A callback that is invoked when the dropdown menu is collapsed.
     */
    var onCollapse: (() -> Unit)? = null


    private var parentScene: UIScene? = null

    @Volatile
    private var isRemovalScheduled = false

    private val menuScale = FloatAnimation(0f, 0.2f)

    private var menuX = 0f
    private var menuY = 0f


    private val optionsContainer: UILinearContainer

    private val wrapper = object : UIContainer() {

        init {
            width = fill()
            height = fill()
        }


        override fun onInputEvent(event: InputEvent): Boolean {

            // While closing the menu must not block the scene below.
            if (!isExpanded) {
                return false
            }

            if (event is PointerEvent) {

                // The menu is modal, presses outside of it must not reach the scene below.
                if (event.action == PointerEvent.Action.Down) {
                    super.onInputEvent(event)
                    return true
                }

                if (!super.onInputEvent(event) && !this@UIDropdown.bounds.contains(event.x, event.y)) {
                    hide()
                }

                return true
            }

            return super.onInputEvent(event)
        }

    }



    init {
        width = Dimension.WrapContent
        height = Dimension.WrapContent
        scrollAxes = Axis.Y
        clipping = ClipMode.Bounds

        style = Style { theme ->
            radius = theme.radius.md
            backgroundColor = theme.palette.surface
        }

        scaleOrigin = Anchor.TopCenter
        alpha = 0f
        scaleY = 0f

        optionsContainer = linearContainer {
            width = fill()
            orientation = Orientation.Vertical
            style = Style { theme ->
                spacing = 4f.dip
                padding = Padding(4f.dip)
            }
        }

        wrapper += this
    }

    override fun onUpdate(clock: FrameClock) {

        if (isExpanded) {
            val (triggerTopLeftX, triggerTopLeftY) = trigger.toSceneSpace(0f, 0f)
            val (triggerBottomRightX, triggerBottomRightY) = trigger.toSceneSpace(trigger.bounds.width, trigger.bounds.height)

            // The menu is at least as wide as the trigger, so it reads as coming out of it.
            minWidth = (triggerBottomRightX - triggerTopLeftX).px

            val spaceBelow = Verktex.scene.bounds.height - triggerBottomRightY
            val expandUpwards = triggerTopLeftY > spaceBelow && spaceBelow < optionsContainer.measuredSize.y

            // Height of the dropdown menu is limited to the available space below or above the trigger,
            // depending on the expansion direction.
            height = min(optionsContainer.measuredSize.y, if (expandUpwards) triggerTopLeftY else spaceBelow).px

            // Position of the dropdown menu is clamped to the screen bounds, so it doesn't go off.
            // Essentially it will always be aligned to the trigger, but if the trigger is too close
            // to the right edge of the screen, the dropdown will be shifted to the left to fit within
            // the screen.
            val targetX = triggerTopLeftX + min(0f, Verktex.scene.bounds.width - triggerTopLeftX - optionsContainer.bounds.width)

            val targetY = if (expandUpwards) {
                triggerTopLeftY - min(optionsContainer.bounds.height, triggerTopLeftY)
            } else {
                triggerBottomRightY
            }

            // Bounds are only updated on the draw worker, so the position is tracked separately.
            if (alpha == 0f) {
                menuX = targetX
                menuY = targetY
            }
            menuX = clock.interpolateFloat(0.2f, menuX, targetX)
            menuY = clock.interpolateFloat(0.2f, menuY, targetY)

            x = menuX.px
            y = menuY.px

            // Expansion animation
            scaleOrigin = if (expandUpwards) Anchor.BottomCenter else Anchor.TopCenter
            alpha = clock.interpolateFloat(0.2f, alpha, 1f)
            menuScale.animateTo(1f, 0.4f, Easing.OutBounce)
            scale = menuScale.update(clock)

        } else {
            alpha = clock.interpolateFloat(0.2f, alpha, 0f)
            menuScale.animateTo(0f, 0.2f, Easing.OutExpo)
            scale = menuScale.update(clock)

            // Scheduled once, the update worker runs more often than the draw worker.
            if (alpha == 0f && !isRemovalScheduled) {
                isRemovalScheduled = true

                Verktex.scheduleOnDraw {
                    // It might have been shown again meanwhile.
                    if (!isExpanded) {
                        wrapper.removeSelf()
                    }
                    isRemovalScheduled = false
                }
            }
        }

        super.onUpdate(clock)
    }

    //region Buttons

    fun addButton(block: UITextButton.() -> Unit): UITextButton {
        val button = UITextButton().apply {
            width = Dimension.FillAvailable
            variant = UIButton.Variant.Ghost
            alignment = Anchor.CenterLeft
            block()
        }

        optionsContainer += button
        return button
    }

    fun clearButtons() {
        optionsContainer.clearChildren()
    }

    fun forEachButton(action: (UITextButton) -> Unit) {
        optionsContainer.forEach { action(it as UITextButton) }
    }

    //endregion

    //region Visibility

    fun show() {
        val wasExpanded = isExpanded

        parentScene = Verktex.scene

        wrapper.removeSelf()
        Verktex.scene += wrapper

        if (!wasExpanded) {
            onExpand?.invoke()
        }
    }

    fun hide() {
        parentScene = null
    }

    //endregion

}