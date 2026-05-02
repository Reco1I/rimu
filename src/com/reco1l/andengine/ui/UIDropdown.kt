package com.reco1l.andengine.ui

import com.edlplan.framework.easing.Easing
import com.osudroid.utils.*
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.theme.Radius
import com.reco1l.andengine.theme.Size
import com.reco1l.andengine.theme.srem
import com.reco1l.framework.math.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.input.touch.*
import javax.microedition.khronos.opengles.*
import kotlin.math.*
import org.anddev.andengine.entity.scene.Scene

class UIDropdown(var trigger: UIComponent) : UIScrollableContainer() {
    private var currentScene: Scene? = null

    /**
     * Whether the dropdown menu is currently expanded or not.
     */
    val isExpanded
        get() = currentScene != null

    /**
     * A callback that is invoked when the dropdown menu is expanded.
     */
    var onExpand: (() -> Unit)? = null

    /**
     * A callback that is invoked when the dropdown menu is collapsed.
     */
    var onCollapse: (() -> Unit)? = null


    private val wrapper = object : UIContainer() {
        init {
            width = Size.Full
            height = Size.Full
        }

        override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
            if (!event.isActionDown) {
                return super.onAreaTouched(event, localX, localY)
            }

            if (!super.onAreaTouched(event, localX, localY) && !this@UIDropdown.contains(localX, localY)) {
                hide()
            }

            return true
        }
    }

    private val optionsContainer: UILinearContainer


    init {
        width = Size.Auto
        height = Size.Auto
        scrollAxes = Axes.Y
        clipToBounds = true

        style = {
            radius = Radius.LG
            backgroundColor = it.accentColor * 0.175f
        }

        scaleCenter = Anchor.TopCenter
        alpha = 0f
        scaleY = 0f

        optionsContainer = linearContainer {
            width = Size.Full
            orientation = Orientation.Vertical
            shrink = false
            style = {
                spacing = 0.5f.srem
                padding = Vec4(1f.srem)
            }
        }

        wrapper.attachChild(this)
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {

        if (isExpanded) {
            if (currentScene == UIEngine.current.scene) {
                var minWidth = trigger.width

                optionsContainer.forEach { it as UITextButton
                    minWidth = max(minWidth, it.contentWidth + it.padding.horizontal + optionsContainer.padding.horizontal)
                }

                optionsContainer.minWidth = max(minWidth, width)
            } else {
                // Scene was changed - hide the dropdown.
                hide()
            }
        }

        super.onManagedUpdate(deltaTimeSec)
    }

    override fun onManagedDraw(gl: GL10, camera: Camera) {

        if (isExpanded) {
            val (sceneSpaceX, sceneSpaceY) = trigger.convertLocalToSceneCoordinates(0f, trigger.height)
            val (_, triggerTopY) = trigger.convertLocalToSceneCoordinates(0f, 0f)

            val spaceBelow = parent.height - sceneSpaceY
            val expandUpwards = triggerTopY > spaceBelow && spaceBelow < optionsContainer.height

            x = sceneSpaceX

            if (expandUpwards) {
                y = triggerTopY - min(optionsContainer.height, triggerTopY)
                scaleCenter = Anchor.BottomCenter
                maxHeight = min(optionsContainer.height, triggerTopY)
            } else {
                y = sceneSpaceY
                scaleCenter = Anchor.TopCenter
                maxHeight = min(optionsContainer.height, spaceBelow)
            }

            minWidth = trigger.width
        }

        super.onManagedDraw(gl, camera)
    }


    //region Buttons

    fun addButton(block: UITextButton.() -> Unit): UITextButton {
        val button = UITextButton().apply {
            width = Size.Full
            alignment = Anchor.CenterLeft
            colorVariant = ColorVariant.Tertiary
            block()
        }

        optionsContainer += button
        return button
    }

    fun clearButtons() {
        optionsContainer.detachChildren()
    }

    fun forEachButton(action: (UITextButton) -> Unit) {
        optionsContainer.forEach { action(it as UITextButton) }
    }

    //endregion

    //region Visibility

    fun show() {
        if (!isExpanded) {
            clearModifiers(ModifierType.Alpha, ModifierType.ScaleXY)
            fadeTo(1f, 0.2f)
            scaleToY(1f, 0.3f, Easing.OutBounce)

            wrapper.detachSelf()

            // Workaround to ensure that the wrapper is always on the top while also ensuring that it does not leak
            // across scenes.
            currentScene = UIEngine.current.scene
            UIEngine.current.overlay.attachChild(wrapper)

            onExpand?.invoke()
        }
    }

    fun hide() {
        if (isExpanded) {
            currentScene = null
            clearModifiers(ModifierType.Alpha, ModifierType.ScaleXY)
            scaleToY(0f, 0.2f, Easing.OutExpo)
            fadeTo(0f, 0.2f).after {
                updateThread {
                    wrapper.detachSelf()
                }
            }

            onCollapse?.invoke()
        }
    }

    //endregion

}