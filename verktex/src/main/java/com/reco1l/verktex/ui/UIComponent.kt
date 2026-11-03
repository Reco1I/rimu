package com.reco1l.verktex.ui

import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Shadow
import com.reco1l.verktex.data.Border
import com.reco1l.verktex.data.Mat2
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.data.Vec4
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.ClipMode
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.LayoutConstraints
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.px
import com.reco1l.verktex.math.almostEquals
import com.reco1l.verktex.settings.Setting
import com.reco1l.verktex.settings.VerktexSettings
import com.reco1l.verktex.time.Clock
import com.reco1l.verktex.time.FrameClock
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
     * The minimum width of the component, either fixed or relative to the available width. It wins over [maxWidth].
     */
    var minWidth: Dimension.ConstraintDriven? = null
        set(value) {
            if (field == value) return
            field = value
            invalidateMeasure()
        }

    /**
     * The maximum width of the component, either fixed or relative to the available width.
     */
    var maxWidth: Dimension.ConstraintDriven? = null
        set(value) {
            if (field == value) return
            field = value
            invalidateMeasure()
        }

    /**
     * The minimum height of the component, either fixed or relative to the available height. It wins over [maxHeight].
     */
    var minHeight: Dimension.ConstraintDriven? = null
        set(value) {
            if (field == value) return
            field = value
            invalidateMeasure()
        }

    /**
     * The maximum height of the component, either fixed or relative to the available height.
     */
    var maxHeight: Dimension.ConstraintDriven? = null
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
            invalidateMeasure()
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
            invalidateMeasure()
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
    var scaleX = 1f
        set(value) {
            if (field.almostEquals(value)) return
            field = value
            invalidateTransformations()
        }

    /**
     * The scale in the Y axis.
     */
    var scaleY = 1f
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
        // Mat2 operations are applied after the existing ones, so the order here is the order in
        // which they affect a local point: scale, rotate and finally move to the layout position.
        localToParentMatrix.apply {
            identity()

            if (scaleX != 1f || scaleY != 1f) {
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

            // Fractions of the translation are relative to the component's own size, as in CSS.
            translate(bounds.x + translationX.resolve(bounds.width), bounds.y + translationY.resolve(bounds.height))
        }

        parentToLocalMatrix.set(localToParentMatrix)
        parentToLocalMatrix.invertOrNaN()

        val parent = parent

        // concat() applies the given matrix first, so the local transform goes into the parent's.
        if (parent != null) {
            localToSceneMatrix.set(parent.localToSceneMatrix)
            localToSceneMatrix.concat(localToParentMatrix)
        } else {
            localToSceneMatrix.set(localToParentMatrix)
        }

        sceneToLocalMatrix.set(localToSceneMatrix)
        sceneToLocalMatrix.invertOrNaN()
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
     * When true, this component's RGB is multiplied into its descendants' colours (the alpha always is).
     * The component's own content, background and border are never tinted by its own RGB. Only
     * containers have descendants, so it has no effect on other components. Default false.
     */
    var tintChildren: Boolean = false

    /**
     * The [color] without its alpha, used to draw the component's own content. The alpha is
     * already applied to everything the component draws through the renderer's color stack.
     */
    protected val contentColor
        get() = color.copy(alpha = 1f)

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
     * The border drawn along the inner edge of this component. Null for none.
     */
    var border: Border? = null

    /**
     * The shadow drawn below this component, following its shape.
     */
    var shadow: Shadow? = null

    /**
     * The radius of the corners of this component. If the radius is greater than 0, the component will be drawn with rounded corners.
     */
    var radius: Dimension.Fixed = Dimension.Zero


    /**
     * The theme of this component and its children, or null to use the one of its parent.
     *
     * Components are styled when constructed, so changing it styles them again, overriding the
     * values set after construction on themed properties, as a change of [Verktex.theme] does.
     */
    var theme: Theme? = null
        set(value) {
            if (field != value) {
                field = value
                applyTheme()
            }
        }

    /**
     * The theme this component is styled with: its own [theme], the one of the closest parent
     * with one, or [Verktex.theme].
     */
    val resolvedTheme: Theme
        get() = theme ?: parent?.resolvedTheme ?: Verktex.theme

    // The theme the style was last applied with.
    private var styledTheme: Theme? = null


    /**
     * Applies the current style to the component.
     */
    fun style() {
        val theme = resolvedTheme
        styledTheme = theme
        onStyle(theme)
    }

    // Styles the component and its children again after the resolved theme changed.
    private fun applyTheme() {
        style()
        if (this is UIContainer) propagate { it.onThemeChanged() } else onThemeChanged()
    }

    /**
     * Called when the theme or the font scale changed, after the style was applied again.
     * Cached values depending on them must be invalidated here.
     */
    open fun onThemeChanged() {
        invalidateMeasure()
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
    // Laying out is cheap, so it runs every frame and this flag is never reset. Scrolling and
    // animated positions rely on it. Only a static layout resets it, see [isLayoutStatic].
    var layoutInvalid = true
        private set

    /**
     * Whether this component keeps its layout until it is invalidated, instead of laying out every
     * frame. See [UIContainer.staticLayout].
     */
    protected open val isLayoutStatic
        get() = false

    /**
     * Whether the component's measurement is invalid and needs to be processed next frame.
     */
    var measureInvalid = true
        private set

    /**
     * Whether the component's transformations are invalid and needs to be processed next frame.
     */
    var transformationsInvalid = true
        private set


    /**
     * Marks this component's layout as invalid.
     */
    fun invalidateLayout() {
        layoutInvalid = true
        // A static ancestor skips its pass while valid, so the change has to reach it to lay this
        // component out again. Every other component is always invalid, there it changes nothing.
        parent?.invalidateLayout()
    }

    /**
     * Marks this component's measure as invalid.
     */
    fun invalidateMeasure() {
        measureInvalid = true
        // The parent's size can depend on this component's one.
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

            // The style is applied on construction, before the component has a parent. If it's
            // attached under a different theme it must be styled again with it.
            if (value != null && styledTheme !== resolvedTheme) {
                applyTheme()
            }
        }


    /**
     * Called when the component is attached to a parent.
     * This is usually called when the component is added to a container.
     */
    open fun onAttached() {
        // The style is already applied when assigned, applying it again here would override the
        // values set by the builder after construction.
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
        set(value) {
            if (field == value) return
            field = value
            invalidateMeasure()
        }

    /**
     * Whether the bounds of this component, transformed to scene space, overlap the given scene
     * rectangle. Anything drawn outside of the bounds is not considered.
     */
    open fun intersectsSceneRect(x: Float, y: Float, width: Float, height: Float): Boolean {
        if (transformationsInvalid) {
            transformationsInvalid = false
            calculateTransformations()
        }

        val (x0, y0) = localToSceneMatrix.transform(0f, 0f)
        val (x1, y1) = localToSceneMatrix.transform(bounds.width, 0f)
        val (x2, y2) = localToSceneMatrix.transform(bounds.width, bounds.height)
        val (x3, y3) = localToSceneMatrix.transform(0f, bounds.height)

        return minOf(x0, x1, x2, x3) <= x + width && maxOf(x0, x1, x2, x3) >= x
            && minOf(y0, y1, y2, y3) <= y + height && maxOf(y0, y1, y2, y3) >= y
    }

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

        val shadow = shadow
        if (shadow != null && shadow.color.alpha > 0f) {
            val blur = shadow.blur.toPixels()

            // Grown by the blur, so the soft edge fits around the component's shape.
            renderer.drawRect(
                x = -blur,
                y = shadow.offsetY.toPixels() - blur,
                width = bounds.width + blur * 2f,
                height = bounds.height + blur * 2f,
                color = shadow.color,
                radius = radius.toPixels(),
                softness = blur,
                inset = blur
            )
        }

        // The background is drawn before clipping, padding clipping must only affect the content.
        val border = border?.takeIf { it.isVisible }
        if (backgroundColor.alpha > 0f || border != null) {
            renderer.drawRect(
                x = 0f,
                y = 0f,
                width = bounds.width,
                height = bounds.height,
                color = backgroundColor,
                borderWidth = border?.width?.toPixels() ?: 0f,
                borderColor = border?.color ?: Color4.Transparent,
                radius = radius.toPixels(),
                borderColorBottom = border?.colorBottom?.takeIf { it != border.color }
            )
        }

        if (clipping != ClipMode.Disabled) {
            val clippingBounds = if (clipping == ClipMode.Bounds)
                bounds
            else
                bounds.shrink(padding.toVec4())

            renderer.pushScissor(
                x = if (clipping == ClipMode.Bounds) 0f else paddingLeft.toPixels(),
                y = if (clipping == ClipMode.Bounds) 0f else paddingTop.toPixels(),
                width = clippingBounds.width.coerceAtLeast(0f),
                height = clippingBounds.height.coerceAtLeast(0f)
            )
        }

        drawContent(renderer)

        // Debug outline
        if (showComponentBoundaries) {
            renderer.drawRect(0f, 0f, bounds.width, bounds.height, Color4.Transparent, 1f, Color4.White, 0f)
        }

        if (clipping != ClipMode.Disabled) renderer.popScissor()

        renderer.popColor()
        renderer.popMatrix()
    }

    /**
     * Draws the component and its children by calling [onDraw]. Subclasses can wrap it to set up
     * render state around the drawing, see [UICanvas].
     */
    protected open fun drawContent(renderer: GraphicsRenderer) {
        onDraw(renderer)
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
    fun update(clock: FrameClock) {
        if (ignoreUpdate) {
            return
        }

        engineClockTime = clock.time
        transforms?.update(this, clock.elapsedFrameTime)
        choreography?.update(currentClockTime)
        onUpdate(clock)
    }

    /**
     * The clock pacing the transforms and the choreography of this component and its children. When null it's
     * inherited from the parent, falling back to the clock of the engine.
     */
    var clock: Clock? = null

    /**
     * The clock actually pacing this component, null if it's the one of the engine.
     */
    val activeClock: Clock?
        get() = clock ?: parent?.activeClock

    /**
     * The current time of the clock pacing this component, in seconds.
     */
    val currentClockTime: Float
        get() = activeClock?.time ?: engineClockTime

    /**
     * The timeline of this component, see [Choreography].
     */
    var choreography: Choreography? = null

    /**
     * Whether a transform or a step of the choreography is still running.
     */
    val isAnimating: Boolean
        get() = transforms?.hasTransforms == true || choreography?.let { currentClockTime < it.endTime } == true

    /**
     * Whether this component and its children stop updating, which freezes them in place.
     */
    var ignoreUpdate = false

    private var engineClockTime = 0f

    /**
     * Removes all the transforms and the choreography, leaving the properties at their current values.
     */
    fun clearAnimations() {
        transforms?.clear()
        choreography?.clear()
    }

    /**
     * Sets the position to the given amount of pixels.
     */
    fun setPosition(x: Float, y: Float) {
        this.x = x.px
        this.y = y.px
    }

    /**
     * Sets the size to the given amount of pixels.
     */
    fun setSize(width: Float, height: Float) {
        this.width = width.px
        this.height = height.px
    }

    /**
     * Sets the color channels, leaving the alpha as it is.
     */
    fun setColor(red: Float, green: Float, blue: Float) {
        color = Color4(red, green, blue, alpha)
    }

    /**
     * The transforms running on this component, created when the first one is added.
     */
    internal var transforms: TransformTracker? = null
        private set

    internal fun transformTracker() = transforms ?: TransformTracker().also { transforms = it }

    /**
     * Called when the component needs to be updated. This is where the actual update logic of the
     * component should happen.
     */
    protected open fun onUpdate(clock: FrameClock) {}

    //endregion

    //region Input

    open fun onInputEvent(event: InputEvent): Boolean {
        return false
    }

    /**
     * Whether the given point in scene (window) space is inside this component's bounds.
     */
    fun containsScenePoint(x: Float, y: Float): Boolean {
        val local = toLocalSpaceFromScene(x, y)
        return local.x >= 0f && local.y >= 0f && local.x <= bounds.width && local.y <= bounds.height
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
            invalidateMeasure()
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

    private var lastMeasureConstraints: LayoutConstraints? = null

    private var lastLayoutBounds: Vec4? = null

    /**
     * Measures the component with the given layout constraints. This method will call [onMeasure]
     * to perform the actual measurement.
     */
    fun measure(requestedConstraints: LayoutConstraints): Vec2 {
        val parentContraints = adjustParentConstraints(requestedConstraints)

        if (!measureInvalid && parentContraints == lastMeasureConstraints) {
            return measuredSize
        }

        // Reset before measuring, invalidations made meanwhile must trigger another pass.
        measureInvalid = false
        lastMeasureConstraints = parentContraints

        // A measure pass means a size, position or child changed, which a static layout must apply.
        if (isLayoutStatic) {
            layoutInvalid = true
        }

        val ownConstraints = parentContraints.resolve(width, height).limit(
            minWidth,
            maxWidth,
            minHeight,
            maxHeight,
            parentContraints.availableWidth,
            parentContraints.availableHeight
        )
        val contentConstraints = ownConstraints.shrink(padding)

        val oldMeasuredSize = measuredSize

        val contentSize = onMeasure(parentContraints, contentConstraints)
        val widthFactor = (width as? Dimension.ContentSizeFraction)?.factor ?: 1f
        val heightFactor = (height as? Dimension.ContentSizeFraction)?.factor ?: 1f

        measuredSize = ownConstraints.constrain(Vec2(contentSize.x * widthFactor, contentSize.y * heightFactor))

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
    /**
     * Adjusts the constraints the parent measures this component with, e.g. to grow past its
     * padding as [com.reco1l.verktex.ui.container.UIFullscreenContainer] does.
     */
    protected open fun adjustParentConstraints(constraints: LayoutConstraints) = constraints

    open fun layout(assignedBounds: Vec4) {
        if (isLayoutStatic) {
            if (!layoutInvalid && assignedBounds == lastLayoutBounds) {
                return
            }

            // Reset before laying out, invalidations made meanwhile must trigger another pass.
            layoutInvalid = false
            lastLayoutBounds = assignedBounds
        } else if (!layoutInvalid) {
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
            z = assignedBounds.x + anchorOffset.x + positionOffset.x - originOffset.x + measuredSize.x,
            w = assignedBounds.y + anchorOffset.y + positionOffset.y - originOffset.y + measuredSize.y
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


//region Border compatibility

/**
 * The color of the [UIComponent.border], transparent without one. Setting it gives the border a
 * single color, creating a border without width if there was none.
 */
var UIComponent.borderColor: Color4
    get() = border?.color ?: Color4.Transparent
    set(value) {
        border = border?.copy(color = value, colorBottom = value) ?: Border(Dimension.Zero, value)
    }

/**
 * The width of the [UIComponent.border], zero without one. Setting it creates a transparent border
 * if there was none.
 */
var UIComponent.borderWidth: Dimension.Fixed
    get() = border?.width ?: Dimension.Zero
    set(value) {
        border = border?.copy(width = value) ?: Border(value, Color4.Transparent)
    }

//endregion
