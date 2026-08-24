package com.reco1l.verktex.ui

import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Mat2
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.data.Vec4
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.ClipMode
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.LayoutConstraints
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.math.almostEquals
import com.reco1l.verktex.settings.Setting
import com.reco1l.verktex.settings.VerktexSettings
import com.reco1l.verktex.time.TickingClock
import kotlin.math.max

/**
 * The base class for all UI components.
 *
 * This class provides the basic functionality for size, position, transformations, drawing, updating,
 * input handling, and layout of UI components.
 *
 * All UI components should inherit from this class and implement the [onDraw], [onMeasure], and [onLayout]
 * methods to define their specific behavior.
 *
 * This class goes in hand with [UIContainer], which is a special type of component that can
 * contain other components and manage their layout.
 */
abstract class UIComponent {

    //region Size

    /**
     * The width specification of the component.
     *
     * This specifies the dimension and how should it be interpreted, but it doesn't provide the real
     * value of it. To get the final value in pixels use [bounds].
     *
     * @see Dimension
     */
    var width: Dimension = Dimension.WrapContent
        set(value) {
            if (field == value) return
            field = value
            invalidateMeasure()
        }

    /**
     * The height specification of the component.
     *
     * This specifies the dimension and how should it be interpreted, but it doesn't provide the real
     * value of it. To get the final value in pixels use [bounds].
     *
     * @see Dimension
     */
    var height: Dimension = Dimension.WrapContent
        set(value) {
            if (field == value) return
            field = value
            invalidateMeasure()
        }


    /**
     * The width of this component with transformations applied.
     */
    val transformedWidth
        get() = bounds.width * scaleX

    /**
     * The height of this component with transformations applied.
     */
    val transformedHeight
        get() = bounds.height * scaleY


    /**
     * Called when the size of this component changes.
     */
    protected open fun onSizeChanged() {}

    //endregion

    //region Position

    /**
     * The X position specification of the component.
     *
     * This specifies the dimension and how should it be interpreted, but it doesn't provide the real
     * value of it. To get the final value in pixels use [bounds].
     *
     * @see Dimension.ConstraintDriven
     */
    var x: Dimension.ConstraintDriven = Dimension.Zero
        set(value) {
            if (field == value) return
            field = value
            invalidateLayout()
        }

    /**
     * The Y position specification of the component.
     *
     * This specifies the dimension and how should it be interpreted, but it doesn't provide the real
     * value of it. To get the final value in pixels use [bounds].
     *
     * @see Dimension.ConstraintDriven
     */
    var y: Dimension.ConstraintDriven = Dimension.Zero
        set(value) {
            if (field == value) return
            field = value
            invalidateLayout()
        }

    /**
     * Where the component should be anchored in the parent.
     *
     * @see Anchor
     */
    var anchor = Anchor.TopLeft
        set(value) {
            if (field == value) return
            field = value
            invalidateLayout()
        }

    /**
     * Where the component should be anchored in itself. This is relative to the component's size.
     *
     * @see Anchor
     */
    var origin = Anchor.TopLeft
        set(value) {
            if (field == value) return
            field = value
            invalidateLayout()
        }

    /**
     * Where the component should be rotated around. This is relative to the component's size.
     */
    var rotationOrigin = Anchor.Center
        set(value) {
            if (field == value) return
            field = value
            invalidateTransformations()
        }

    /**
     * Where the component should be scaled around. This is relative to the component's size.
     */
    var scaleOrigin = Anchor.Center
        set(value) {
            if (field == value) return
            field = value
            invalidateTransformations()
        }


    /**
     * Called when the position of this component changes.
     */
    open fun onPositionChanged() {}

    //endregion

    //region Transformations

    /**
     * The scale in the X axis.
     */
    var scaleX = 0f
        set(value) {
            if (field.almostEquals(value)) return
            field = value
            invalidateTransformations()
        }

    /**
     * The scale in the Y axis.
     */
    var scaleY = 0f
        set(value) {
            if (field.almostEquals(value)) return
            field = value
            invalidateTransformations()
        }

    /**
     * The scale of the component. This is a convenience property that sets both [scaleX] and
     * [scaleY] to the same value.
     *
     * Its getter returns the maximum of [scaleX] and [scaleY], which can be useful for
     * determining the overall scale of the component.
     */
    var scale
        get() = max(scaleX, scaleY)
        set(value) {
            scaleX = value
            scaleY = value
        }

    /**
     * The translation in the X axis.
     *
     * Translations does not affect the layout system. Similarly to how CSS translate() works.
     */
    var translationX: Dimension.ConstraintDriven = Dimension.Zero
        set(value) {
            if (field == value) return
            field = value
            invalidateTransformations()
        }

    /**
     * The translation in the Y axis.
     *
     * Translations does not affect the layout system. Similarly to how CSS translate() works.
     */
    var translationY: Dimension.ConstraintDriven = Dimension.Zero
        set(value) {
            if (field == value) return
            field = value
            invalidateTransformations()
        }

    /**
     * The rotation of the component in degrees of the Z axis.
     */
    var rotationZ = 0f
        set(value) {
            if (field.almostEquals(value)) return
            field = value
            invalidateTransformations()
        }

    /**
     * The rotation of the component in degrees of the X axis.
     */
    var rotationX = 0f
        set(value) {
            if (field.almostEquals(value)) return
            field = value
            invalidateTransformations()
        }

    /**
     * The rotation of the component in degrees of the Y axis.
     */
    var rotationY = 0f
        set(value) {
            if (field.almostEquals(value)) return
            field = value
            invalidateTransformations()
        }


    /**
     * The transformation matrix from local space to parent space.
     *
     * **It is not recomended to update this [Mat2] outside [calculateTransformations] as it might
     * cause inconsistencies on the layout system.**
     */
    protected val localToParentMatrix = Mat2()

    /**
     * The transformation matrix from parent space to local space.
     *
     * **It is not recomended to update this [Mat2] outside [calculateTransformations] as it might
     * cause inconsistencies on the layout system.**
     */
    protected val parentToLocalMatrix = Mat2()

    /**
     * The transformation matrix from local space to scene space.
     *
     * **It is not recomended to update this [Mat2] outside [calculateTransformations] as it might
     * cause inconsistencies on the layout system.**
     */
    protected val localToSceneMatrix = Mat2()

    /**
     * The transformation matrix from scene space to local space.
     *
     * **It is not recomended to update this [Mat2] outside [calculateTransformations] as it might
     * cause inconsistencies on the layout system.**
     */
    protected val sceneToLocalMatrix = Mat2()


    /**
     * Calculates the transformations of the component into the transformation matrices:
     * * [localToParentMatrix]
     * * [parentToLocalMatrix]
     * * [localToSceneMatrix]
     * * [sceneToLocalMatrix]
     *
     * Default implementation will apply the following transformations:
     * [x], [y], [scaleX], [scaleY], [rotationX], [rotationY], [rotationZ]
     */
    open fun calculateTransformations() {
        localToParentMatrix.apply {
            identity()

            translate(bounds.x, bounds.y)

            if (scaleX != 0f || scaleY != 0f) {
                val scaleCenterX = bounds.width * scaleOrigin.x
                val scaleCenterY = bounds.height * scaleOrigin.y

                translate(-scaleCenterX, -scaleCenterY)
                scale(scaleX, scaleY)
                translate(scaleCenterX, scaleCenterY)
            }

            if (rotationZ != 0f) {
                val rotationCenterX = bounds.width * rotationOrigin.x
                val rotationCenterY = bounds.height * rotationOrigin.y

                translate(-rotationCenterX, -rotationCenterY)
                rotate(rotationZ)
                translate(rotationCenterX, rotationCenterY)
            }
        }

        parentToLocalMatrix.set(localToParentMatrix)
        parentToLocalMatrix.invert()

        val parent = parent

        localToSceneMatrix.set(localToParentMatrix)
        if (parent != null) localToSceneMatrix.concat(parent.localToSceneMatrix)

        sceneToLocalMatrix.set(localToSceneMatrix)
        sceneToLocalMatrix.invert()
    }

    /**
     * Converts a pair of coordinates to scene space.
     */
    fun toSceneSpace(x: Float, y: Float): Vec2 {
        return localToSceneMatrix.transform(x, y)
    }

    /**
     * Converts a pair of coordinates to parent's local space.
     */
    fun toParentSpace(x: Float, y: Float): Vec2 {
        return localToParentMatrix.transform(x, y)
    }

    /**
     * Converts a pair of coordinates to local space from a parent space.
     */
    fun toLocalSpaceFromParent(x: Float, y: Float): Vec2 {
        return parentToLocalMatrix.transform(x, y)
    }

    /**
     * Converts a pair of coordinates to local space from a scene space.
     */
    fun toLocalSpaceFromScene(x: Float, y: Float): Vec2 {
        return sceneToLocalMatrix.transform(x, y)
    }

    //endregion

    //region Cosmetic

    /**
     * The style of this component.
     */
    var style = Style {}
        set(value) {
            if (field != value) {
                field = value
                style()
            }
        }

    /**
     * The color of the component boxed in a [Color4] object.
     */
    var color = Color4.White

    /**
     * The alpha component of the compoent's color.
     */
    var alpha
        get() = color.alpha
        set(value) { color = color.copy(alpha = value) }

    /**
     * The red component of the compoent's color.
     */
    var red
        get() = color.red
        set(value) { color = color.copy(red = value) }

    /**
     * The green component of the compoent's color.
     */
    var green
        get() = color.green
        set(value) { color = color.copy(green = value) }

    /**
     * The blue component of the compoent's color.
     */
    var blue
        get() = color.blue
        set(value) { color = color.copy(blue = value) }

    /**
     * Determines how the component should be clipped.
     */
    var clipping: ClipMode = ClipMode.Disabled

    /**
     * The background color of this component.
     */
    var backgroundColor = Color4.Transparent

    /**
     * The border color of this component.
     */
    var borderColor = Color4.Transparent

    /**
     * The border width of this component.
     */
    var borderWidth: Dimension.Fixed = Dimension.Zero

    /**
     * The radius of the corners of this component. If the radius is greater than 0, the component will be drawn with rounded corners.
     */
    var radius: Dimension.Fixed = Dimension.Zero


    /**
     * Applies the current style to the component.
     */
    fun style() {
        onStyle(Verktex.theme)
    }

    /**
     * Called when the style of the component needs to be applied.
     *
     * Usually this calls the [style] lambda with the current theme.
     */
    open fun onStyle(theme: Theme) {
        style.apply(theme)
    }

    //endregion

    //region Invalidation

    /**
     * Whether the component's layout is invalid and needs to be processed next frame.
     */
    var layoutInvalid = false
        private set

    /**
     * Whether the component's measurement is invalid and needs to be processed next frame.
     */
    var measureInvalid = false
        private set

    /**
     * Whether the component's transformations are invalid and needs to be processed next frame.
     */
    var transformationsInvalid = false
        private set


    /**
     * Marks this component's layout as invalid.
     */
    fun invalidateLayout() {
        layoutInvalid = true
    }

    /**
     * Marks this component's measure as invalid.
     */
    fun invalidateMeasure() {
        if (measureInvalid) return
        measureInvalid = true
        parent?.invalidateMeasure()
    }

    /**
     * Marks this component's transformations as invalid.
     */
    fun invalidateTransformations() {
        transformationsInvalid = true
    }

    //endregion

    //region Attachment

    /**
     * The parent container of this component. This is usually set when the component is added to a container.
     *
     * Usually the root component of the UI tree will have a null parent.
     */
    var parent: UIContainer? = null
        set(value) {
            if (field == value) return
            field?.onChildDetached(this)
            field = value
            field?.onChildAttached(this)
        }


    /**
     * Called when the component is attached to a parent.
     * This is usually called when the component is added to a container.
     */
    open fun onAttached() {
        style()
    }

    /**
     * Called when the component is detached from a parent.
     * This is usually called when the component is removed from a container.
     */
    open fun onDetached() {}

    /**
     * Removes this component from its parent container. If the component has no parent, this method
     * does nothing.
     */
    fun removeSelf() {
        parent?.minusAssign(this)
    }

    //endregion

    //region Drawing

    /**
     * Whether the component is visible. If false, the component will not be drawn.
     *
     * Being invisible does not mean that the component is removed from the layout, it will still
     * occupy space and be updated, as well it will still intercept input events.
     */
    var isVisible = true

    /**
     * Draws the component using the given renderer. This method is responsible for setting up the
     * rendering context, applying transformations, and calling [onDraw] to perform the actual
     * drawing of the component.
     */
    fun draw(renderer: GraphicsRenderer) {

        if (!isVisible) {
            return
        }

        if (transformationsInvalid) {
            transformationsInvalid = false
            calculateTransformations()
        }

        renderer.pushMatrix(localToSceneMatrix)
        renderer.pushColor(color)

        if (clipping != ClipMode.Disabled) {
            val clippingBounds = if (clipping == ClipMode.Bounds)
                bounds
            else
                bounds.shrink(padding.toVec4())

            renderer.pushScissor(
                x = if (clipping == ClipMode.Bounds) 0f else paddingLeft.toPixels(),
                y = if (clipping == ClipMode.Bounds) 0f else paddingTop.toPixels(),
                width = clippingBounds.width,
                height = clippingBounds.height
            )
        }

        if (backgroundColor.alpha > 0f || (borderColor.alpha > 0f && borderWidth.value > 0f)) {
            renderer.drawRect(
                x = 0f,
                y = 0f,
                width = bounds.width,
                height = bounds.height,
                color = backgroundColor,
                borderWidth = borderWidth.toPixels(),
                borderColor = borderColor,
                radius = radius.toPixels()
            )
        }

        // Render component and children
        onDraw(renderer)

        // Debug outline
        if (showComponentBoundaries) {
            renderer.drawRect(0f, 0f, bounds.width, bounds.height, Color4.Transparent, 1f, Color4.White, 0f)
        }

        if (clipping != ClipMode.Disabled) renderer.popScissor()

        renderer.popColor()
        renderer.popMatrix()
    }

    /**
     * Called when the component needs to be drawn. This is where the actual rendering of the
     * component should happen.
     *
     * @param renderer The renderer to use for drawing.
     */
    protected abstract fun onDraw(renderer: GraphicsRenderer)

    //endregion

    //region Update

    /**
     * Updates the component. This method is called every update cycle and is responsible for
     * updating the component's state.
     */
    fun update(clock: TickingClock) {
        onUpdate(clock)
    }

    /**
     * Called when the component needs to be updated. This is where the actual update logic of the
     * component should happen.
     */
    protected open fun onUpdate(clock: TickingClock) {}

    //endregion

    //region Input

    open fun onInputEvent(event: InputEvent): Boolean {
        return false
    }

    //endregion

    //region Layout

    /**
     * The padding of the component.
     */
    var padding = Padding.Zero
        set(value) {
            if (field == value) return
            field = value
            invalidateLayout()
        }

    /**
     * The padding of the component on the left side.
     */
    var paddingLeft
        get() = padding.left
        set(value) {
            if (padding.left != value) {
                padding = padding.copy(left = value)
            }
        }

    /**
     * The padding of the component on the top side.
     */
    var paddingTop
        get() = padding.top
        set(value) {
            if (padding.top != value) {
                padding = padding.copy(top = value)
            }
        }

    /**
     * The padding of the component on the right side.
     */
    var paddingRight
        get() = padding.right
        set(value) {
            if (padding.right != value) {
                padding = padding.copy(right = value)
            }
        }

    /**
     * The padding of the component on the bottom side.
     */
    var paddingBottom
        get() = padding.bottom
        set(value) {
            if (padding.bottom != value) {
                padding = padding.copy(bottom = value)
            }
        }


    /**
     * The bounds of the component after the last layout pass. This is the area that the component
     * will occupy in its parent's coordinate system.
     *
     * This is used for layout and rendering, and is updated during the [layout] method.
     */
    var bounds = Vec4.Zero
        protected set

    /**
     * The measured size of the component after the last measure pass.
     * This is the size that the component will use for layout and rendering.
     */
    var measuredSize = Vec2.Zero
        protected set

    /**
     * Measures the component with the given layout constraints. This method will call [onMeasure]
     * to perform the actual measurement.
     */
    fun measure(parentContraints: LayoutConstraints): Vec2 {

        if (!measureInvalid) {
            return measuredSize
        }

        val contentConstraints = parentContraints
            .resolve(width, height)
            .shrink(padding)

        val oldMeasuredSize = measuredSize
        measuredSize = onMeasure(parentContraints, contentConstraints)

        if (oldMeasuredSize != measuredSize) {
            invalidateLayout()
            onSizeChanged()
        }

        return measuredSize
    }

    /**
     * Layouts the component within the given bounds. This method will call [onLayout] to perform
     * the actual layout of the component. The bounds are expected to be in the parent's coordinate system.
     */
    open fun layout(assignedBounds: Vec4) {
        if (!layoutInvalid) {
            return
        }

        val anchorOffset = Vec2(
            assignedBounds.width * anchor.x,
            assignedBounds.height * anchor.y
        )

        val originOffset = Vec2(
            measuredSize.x * origin.x,
            measuredSize.y * origin.y
        )

        val positionOffset = Vec2(
            x.resolve(assignedBounds.width),
            y.resolve(assignedBounds.height)
        )

        val oldBounds = bounds

        bounds = Vec4(
            x = assignedBounds.x + anchorOffset.x + positionOffset.x - originOffset.x,
            y = assignedBounds.y + anchorOffset.y + positionOffset.y - originOffset.y,
            z = measuredSize.x,
            w = measuredSize.y
        )

        onLayout(bounds)

        if (oldBounds != bounds) {
            calculateTransformations()

            if (oldBounds.x != bounds.x || oldBounds.y != bounds.y) {
                onPositionChanged()
            }
        }
    }

    /**
     * Called when the component needs to be measured. This is where the actual measurement logic of
     * the component should happen.
     *
     * The method receives the parent constraints and the content constraints, which are the
     * constraints after applying the padding.
     */
    protected open fun onMeasure(parentConstraints: LayoutConstraints, contentConstraints: LayoutConstraints): Vec2 {
        return contentConstraints.constrain(Vec2.Zero)
    }

    /**
     * Called when the component needs to be laid out. This is where the actual layout logic of
     * the component should happen.
     *
     * The method receives the bounds of the component in its parent's coordinate system.
     */
    protected open fun onLayout(bounds: Vec4) {}


    //endregion

    /**
     * A style is a complementary interface that allows to apply a theme to a component.
     * It is a functional interface that can be implemented using a lambda expression.
     *
     * The [apply] method is called with the current theme when the style needs to be applied due to
     * a theme change or when the component is attached to a parent.
     */
    fun interface Style {

        /**
         * Applies the style to the component using the given theme.
         */
        fun apply(theme: Theme)


        /**
         * Combines two styles into one. The resulting style will apply both styles in order.
         */
        operator fun plus(other: Style): Style {
            return Style { theme ->
                this.apply(theme)
                other.apply(theme)
            }
        }
    }


    companion object {
        private val showComponentBoundaries by Setting(VerktexSettings.ShowComponentBoundaries, false)
    }
}
