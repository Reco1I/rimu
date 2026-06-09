package com.reco1l.andengine.component

import android.util.*
import android.view.*
import com.edlplan.framework.easing.Easing
import com.osudroid.*
import com.osudroid.math.Precision
import com.reco1l.andengine.*
import com.reco1l.andengine.buffered.QuadRenderer
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.theme.Size
import com.reco1l.andengine.ui.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import com.reco1l.toolkt.kotlin.*
import com.rian.andengine.modifier.*
import com.rian.andengine.timing.IClockProvider
import com.rian.andengine.timing.IClockReceiver
import com.rian.andengine.timing.IFrameBasedClock
import org.anddev.andengine.collision.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.entity.*
import org.anddev.andengine.entity.scene.*
import org.anddev.andengine.entity.scene.Scene.*
import org.anddev.andengine.input.touch.*
import org.anddev.andengine.util.*
import org.anddev.andengine.util.constants.Constants.*
import java.util.function.Consumer
import javax.microedition.khronos.opengles.*
import kotlin.math.max
import kotlin.math.min

/**
 * Entity with extended features.
 * @author Reco1l
 */
@Suppress("MemberVisibilityCanBePrivate")
abstract class UIComponent : Entity(0f, 0f),
    ITouchArea, IClockProvider<IFrameBasedClock?>, IClockReceiver<IFrameBasedClock?> {

    //region Size related properties

    /**
     * Whether the component can shrink below its intrinsic size. By default, it
     * is true in order to pair default CSS's `flex-shrink` behavior.
     */
    var shrink = true


    /**
     * Represents the unprocessed width, works as a backing field for [width].
     */
    var rawWidth = 0f
        private set

    /**
     * Represents the unprocessed height, works as a backing field for [height].
     */
    var rawHeight = 0f
        private set


    /**
     * The width of the entity.
     */
    var width: Float
        get() = let {
            if (rawWidth == Size.Auto)
                return@let intrinsicWidth

            if (rawWidth >= Size.relativeSizeRange.start && rawWidth <= Size.relativeSizeRange.endInclusive)
                return@let (parent?.innerWidth ?: 0f) * (rawWidth - Size.relativeSizeRange.start)

            return@let rawWidth
        }.coerceAtMost(maxWidth).coerceAtLeast(minWidth)
        set(value) {
            if (!Precision.almostEquals(rawWidth, value)) {
                rawWidth = value
                invalidate(InvalidationFlag.Content or InvalidationFlag.Size)
            }
        }

    /**
     * The height of the entity.
     */
    var height: Float
        get() = let {
            if (rawHeight == Size.Auto)
                return@let intrinsicHeight

            if (rawHeight >= Size.relativeSizeRange.start && rawHeight <= Size.relativeSizeRange.endInclusive)
                return@let (parent?.innerHeight ?: 0f) * (rawHeight - Size.relativeSizeRange.start)

            return@let rawHeight
        }.coerceAtMost(maxHeight).coerceAtLeast(minHeight)
        set(value) {
            if (!Precision.almostEquals(rawHeight, value)) {
                rawHeight = value
                invalidate(InvalidationFlag.Content or InvalidationFlag.Size)
            }
        }

    /**
     * The minimum width of the entity.
     */
    var minWidth = 0f
        get() = if (shrink) field else max(intrinsicWidth, field)
        set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Content or InvalidationFlag.Size)
            }
        }

    /**
     * The minimum height of the entity.
     */
    var minHeight = 0f
        get() = if (shrink) field else max(intrinsicHeight, field)
        set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Content or InvalidationFlag.Size)
            }
        }


    /**
     * The maximum width of the entity.
     */
    var maxWidth = Float.MAX_VALUE
        set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Content or InvalidationFlag.Size)
            }
        }

    /**
     * The maximum height of the entity.
     */
    var maxHeight = Float.MAX_VALUE
        set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Content or InvalidationFlag.Size)
            }
        }


    /**
     * The width of the content inside the entity.
     */
    var contentWidth = 0f
        protected set(value) {
            if (!Precision.almostEquals(field, value)) {
                field = value
                invalidate(InvalidationFlag.Content or InvalidationFlag.Size)
            }
        }

    /**
     * The height of the content inside the entity.
     */
    var contentHeight = 0f
        protected set(value) {
            if (!Precision.almostEquals(field, value)) {
                field = value
                invalidate(InvalidationFlag.Content or InvalidationFlag.Size)
            }
        }


    /**
     * The padding of the entity.
     */
    var padding = Vec4.Zero
        set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Content or InvalidationFlag.Size)
            }
        }


    /**
     * The inner width of the component, which is the width minus the horizontal padding.
     * It can be equivalent to [contentWidth] if the component's width is set to [Size.Auto].
     */
    val innerWidth
        get() = max(0f, width - padding.horizontal)

    /**
     * The inner height of the component, which is the height minus the vertical padding.
     * It can be equivalent to [contentHeight] if the component's height is set to [Size.Auto].
     */
    val innerHeight
        get() = max(0f, height - padding.vertical)

    /**
     * The intrinsic width of the entity, which is the content width plus the horizontal padding.
     */
    val intrinsicWidth
        get() = max(contentWidth + padding.horizontal, 0f)

    /**
     * The intrinsic height of the entity, which is the content height plus the vertical padding.
     */
    val intrinsicHeight
        get() = max(contentHeight + padding.vertical, 0f)

    /**
     * The width of this component with transformations applied.
     */
    val transformedWidth
        get() = width * scaleX

    /**
     * The height of this component with transformations applied.
     */
    val transformedHeight
        get() = height * scaleY

    //endregion

    //region Position related properties

    fun setX(value: Float) {
        if (!Precision.almostEquals(mX, value)) {
            mX = value
            invalidate(InvalidationFlag.Position)
        }
    }

    fun setY(value: Float) {
        if (!Precision.almostEquals(mY, value)) {
            mY = value
            invalidate(InvalidationFlag.Position)
        }
    }

    /**
     * Where the entity should be anchored in the parent.
     */
    var anchor = Anchor.TopLeft
        set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Position)
            }
        }

    /**
     * Where the entity's origin should be.
     */
    var origin = Anchor.TopLeft
        set(value) {
            if (field != value) {
                field = value
                mRotationCenterX = value.x
                mRotationCenterY = value.y
                mScaleCenterX = value.x
                mScaleCenterY = value.y
                invalidate(InvalidationFlag.Position)
            }
        }

    /**
     * The translation in the X axis, translation does not trigger any kind of invalidation
     * nor re-layout of the parent container. It is considered as a transformation.
     */
    var translationX = 0f
        set(value) {
            if (!Precision.almostEquals(field, value)) {
                field = value
                invalidate(InvalidationFlag.Transformations)
            }
        }

    /**
     * The translation in the Y axis, translation does not trigger any kind of invalidation
     * nor re-layout of the parent container. It is considered as a transformation.
     */
    var translationY = 0f
        set(value) {
            if (!Precision.almostEquals(field, value)) {
                field = value
                invalidate(InvalidationFlag.Transformations)
            }
        }

    /**
     * The calculated anchor position for the X axis in the parent's coordinate system.
     * This will be always 0 if the component has no parent.
     */
    val anchorPositionX
        get() = (parent?.innerWidth ?: 0f) * anchor.x

    /**
     * The calculated anchor position for the Y axis in the parent's coordinate system.
     * This will be always 0 if the component has no parent.
     */
    val anchorPositionY
        get() = (parent?.innerHeight ?: 0f) * anchor.y

    /**
     * The calculated origin position for the X axis in the component's coordinate system.
     */
    val originPositionX
        get() = width * origin.x

    /**
     * The calculated origin position for the Y axis in the component's coordinate system.
     */
    val originPositionY
        get() = height * origin.y

    /**
     * The absolute position for the X axis of the entity taking into account the
     * anchor and origin in the parent's coordinate system.
     */
    val absoluteX
        get() = parent.padding.left + anchorPositionX - originPositionX + x + translationX

    /**
     * The absolute position for the Y axis of the entity taking into account the
     * anchor and origin in the parent's coordinate system.
     */
    val absoluteY
        get() = parent.padding.top + anchorPositionY - originPositionY + y + translationY


    //endregion

    //region Cosmetic properties

    /**
     * The style of this component.
     */
    var style: StyleApplier = {}
        set(value) {
            if (field != value) {
                field = value
                applyStyle()
            }
        }

    /**
     * Applies the current style to the component.
     */
    fun applyStyle() {
        onStyle(Theme.current)
    }

    /**
     * The color of the entity boxed in a [Color4] object.
     */
    var color: Color4
        get() = Color4(mRed, mGreen, mBlue, mAlpha)
        set(value) {
            if (value.red == mRed && value.green == mGreen && value.blue == mBlue && value.alpha == mAlpha) {
                return
            }
            mRed = value.red
            mGreen = value.green
            mBlue = value.blue
            mAlpha = value.alpha
            invalidate(InvalidationFlag.Content)
        }

    /**
     * Whether the entity's color should be multiplied by the color of its ancestor entities.
     */
    var inheritAncestorsColor = true

    /**
     * Whether the entity should clip its children.
     */
    var clipToBounds = false


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
    var borderWidth = 0f

    /**
     * The radius of the corners of this component. If the radius is greater than 0, the component will be drawn with rounded corners.
     */
    var radius = 0f

    //endregion

    //region Other properties

    /**
     * Whether this component is currently running any modifiers or animations.
     */
    val isAnimating
        get() = !mEntityModifiers.isNullOrEmpty() || universalModifierTrackers.any { !it.modifiers.isEmpty() }

    /**
     * Currently bound components to input pointeres.
     */
    val inputBindings = arrayOfNulls<UIComponent>(10)

    /**
     * Whether the entity should be culled when it is outside the parent's bounds.
     */
    var cullingMode = CullingMode.Disabled

    //endregion

    //region Invalidation

    /**
     * The flags that are being invalidated next frame.
     */
    var invalidationFlags = InvalidationFlag.All
        private set

    /**
     * Whether to ignore invalidations.
     */
    var ignoreInvlidations = false

    /**
     * Adds the given flag to the invalidation list. Depending on each flag it will trigger a different action.
     *
     * @see InvalidationFlag
     */
    fun invalidate(flag: Int) {
        invalidationFlags = invalidationFlags or flag
    }

    //endregion

    //region Attachment

    /**
     * Called when the content of the entity has changed. This usually is called when a child is added or removed.
     */
    open fun onContentChanged() {}


    override fun setParent(entity: IEntity?) {
        when (val parent = parent) {
            is Scene -> parent.unregisterTouchArea(this)
            is UIComponent -> parent.onChildDetached(this)
        }
        super.setParent(entity)
        when (entity) {
            is Scene -> entity.registerTouchArea(this)
            is UIComponent -> entity.onChildAttached(this)
        }
    }

    /**
     * Called when a child is attached to this entity.
     */
    open fun onChildAttached(child: IEntity) {
        invalidate(InvalidationFlag.Content)
    }

    /**
     * Called when a child is detached from this entity.
     */
    open fun onChildDetached(child: IEntity) {
        invalidate(InvalidationFlag.Content)
    }

    override fun onAttached() {
        applyStyle()
        updateClock((parent as? IClockProvider<*>)?.clock as? IFrameBasedClock)
    }

    override fun onDetached() {
        updateClock(null)

        super.onDetached()
    }

    //endregion

    //region Size

    /**
     * Called when the size of this entity changes.
     */
    open fun onSizeChanged() {}

    /**
     * Sets the size of the entity.
     */
    fun setSize(x: Float, y: Float) {
        width = x
        height = y
    }

    //endregion

    //region Position

    /**
     * Whether the entity is outside the camera's or parent's bounds.
     */
    open fun isCulled(camera: Camera): Boolean {

        when (cullingMode) {

            CullingMode.CameraBounds -> {
                val (x1, y1) = convertLocalToSceneCoordinates(0f, 0f)
                val (x2, y2) = convertLocalToSceneCoordinates(width, height)

                return x2 < camera.minX || y2 < camera.minY || x1 > camera.maxX || y1 > camera.maxY
            }

            CullingMode.ParentBounds -> {

                val parent = parent
                if (parent !is UIComponent && parent !is UIScene) {
                    return false
                }

                val x1 = absoluteX
                val y1 = absoluteY
                val x2 = x1 + width
                val y2 = y1 + height

                return x2 < 0f || y2 < 0f || x1 > parent.width || y1 > parent.height
            }

            else -> return false
        }

    }

    /**
     * Called when the position of this entity changes.
     */
    open fun onPositionChanged() {}

    /**
     * Sets the position of the entity.
     */
    override fun setPosition(x: Float, y: Float) {
        if (!Precision.almostEquals(mX, x) || !Precision.almostEquals(mY, y)) {
            mX = x
            mY = y
            invalidate(InvalidationFlag.Position)
        }
    }

    //endregion

    //region Drawing

    override fun onDraw(gl: GL10, camera: Camera) {
        if (isVisible && !isCulled(camera)) {
            onManagedDraw(gl, camera)
        }
    }

    /**
     * Called when invalidations needs to be run.
     */
    open fun onHandleInvalidations(flags: Int = this.invalidationFlags) {

        val parent = parent as? UIComponent
        var propagateToChildrenFlags = 0
        var propagateToParentFlags = 0

        if (flags and InvalidationFlag.Content != 0) {
            onContentChanged()
            propagateToParentFlags = InvalidationFlag.Content
        }

        if (flags and InvalidationFlag.Size != 0) {
            onSizeChanged()
            propagateToChildrenFlags = InvalidationFlag.Size
            propagateToParentFlags = InvalidationFlag.Content
        }

        if (flags and InvalidationFlag.Position != 0) {
            onPositionChanged()
            propagateToParentFlags = InvalidationFlag.Content
        }

        // Transformations have and special case since they are affected by position and size changes as well
        // but not always, as an example scale and rotation do not trigger Position or Size flags but they're
        // still transformations.
        if (flags and InvalidationFlag.Transformations != 0 || flags and InvalidationFlag.Position != 0 || flags and InvalidationFlag.Size != 0) {
            onInvalidateTransformations()
            propagateToChildrenFlags = propagateToChildrenFlags or InvalidationFlag.Transformations
        }

        if (flags and InvalidationFlag.InputBindings != 0) {
            onInvalidateInputBindings()
            propagateToChildrenFlags = propagateToChildrenFlags or InvalidationFlag.InputBindings
        }

        if (propagateToParentFlags != 0) {
            val parent = parent
            if (parent is UIComponent) {
                parent.onHandleInvalidations(propagateToParentFlags)
            }
        }

        if (propagateToChildrenFlags != 0) {
            forEach { child ->
                if (child is UIComponent) {
                    child.onHandleInvalidations(propagateToChildrenFlags)
                }
            }
        }

        if (invalidationFlags == flags) {
            invalidationFlags = 0
        }
    }

    override fun onManagedDraw(gl: GL10, camera: Camera) {

        if (!ignoreInvlidations) {
            onHandleInvalidations()
        }

        TransformationStack.push(localToSceneTransformation)
        ColorStack.push(color, inheritAncestorsColor)

        if (clipToBounds) {
            val (topLeftX, topLeftY) = camera.convertSceneToSurfaceCoordinates(convertLocalToSceneCoordinates(0f, 0f))
            val (topRightX, topRightY) = camera.convertSceneToSurfaceCoordinates(convertLocalToSceneCoordinates(width, 0f))
            val (bottomRightX, bottomRightY) = camera.convertSceneToSurfaceCoordinates(convertLocalToSceneCoordinates(width, height))
            val (bottomLeftX, bottomLeftY) = camera.convertSceneToSurfaceCoordinates(convertLocalToSceneCoordinates(0f, height))

            val minX = min(topLeftX, min(bottomLeftX, min(bottomRightX, topRightX)))
            val minY = min(topLeftY, min(bottomLeftY, min(bottomRightY, topRightY)))
            val maxX = max(topLeftX, max(bottomLeftX, max(bottomRightX, topRightX)))
            val maxY = max(topLeftY, max(bottomLeftY, max(bottomRightY, topRightY)))

            ScissorStack.push(minX, minY, maxX - minX, maxY - minY)
        }

        UIRenderer.activeScissor = ScissorStack.peek()
        UIRenderer.activeTexture = null

        // Render background quad
        if (backgroundColor.alpha > 0f) {
            ColorStack.push(backgroundColor, false)
            QuadRenderer.renderQuad(0f, 0f, width, height, radius)
            ColorStack.pop()
        }

        // Render component and children
        doDraw(gl, camera)
        onDrawChildren(gl, camera)

        // Render border quad
        if (borderColor.alpha > 0f && borderWidth > 0f) {
            ColorStack.push(borderColor, false)
            QuadRenderer.renderQuad(0f, 0f, width, height, radius, PaintStyle.Outline, borderWidth)
            ColorStack.pop()
        }

        // Debug outline
        if (BuildSettings.SHOW_ENTITY_BOUNDARIES) {
            ColorStack.push(Color4.White, false)
            QuadRenderer.renderQuad(0f, 0f, width, height, 0f, PaintStyle.Outline)
            ColorStack.pop()
        }

        if (clipToBounds) ScissorStack.pop()
        ColorStack.pop()
        TransformationStack.pop()
    }

    override fun doDraw(gl: GL10, camera: Camera) = Unit

    //endregion

    //region Update

    final override fun onUpdate(deltaTimeSec: Float) {
        if (loadState == LoadState.NotLoaded) {
            return
        }

        if (processCustomClock) {
            customClock?.processFrame()
        }

        if (loadState == LoadState.Ready) {
            loadState = LoadState.Loaded
            onLoadComplete()
        }

        if (!isIgnoreUpdate) {
            // Fallback to parent or engine-provided delta time in case clock is not present.
            onManagedUpdate(clock?.elapsedFrameTime ?: deltaTimeSec)
        }
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {
        updateModifiers()
        super.onManagedUpdate(deltaTimeSec)
    }

    private var loadState = LoadState.NotLoaded

    /**
     * Whether this [UIComponent] is currently loaded and part of the active update loop.
     *
     * This becomes `true` after [onLoadComplete] is called and stays `true` until this [UIComponent] is unloaded.
     */
    val isLoaded
        get() = loadState == LoadState.Loaded

    /**
     * Called when this [UIComponent] is fully loaded and ready for use.
     *
     * This is invoked when [onUpdate] is called for the first time after this [UIComponent] receives a valid [clock]
     * **and** before [onManagedUpdate]. It is safe to start animations and modifiers here.
     *
     * Note that this is called regardless of [isIgnoreUpdate], and can be called multiple times during this
     * [UIComponent]'s lifetime if it is detached and re-attached.
     */
    protected open fun onLoadComplete() {}

    /**
     * Called when this [UIComponent] is being unloaded.
     *
     * This is invoked when this [UIComponent] loses its [clock], usually when it is detached from its [parent].
     * If this [UIComponent] is re-attached, [onLoadComplete] will be called again.
     *
     * Subclasses should use this to clear persistent modifiers or stop animations if needed (e.g., if this
     * [UIComponent] is intended to be pooled and re-used).
     */
    protected open fun onUnload() {}

    //endregion

    //region Collision

    override fun contains(x: Float, y: Float): Boolean {

        if (width == 0f || height == 0f) {
            return false
        }

        VERTICES_WRAPPER[0 + VERTEX_INDEX_X] = 0f
        VERTICES_WRAPPER[0 + VERTEX_INDEX_Y] = 0f

        VERTICES_WRAPPER[2 + VERTEX_INDEX_X] = width
        VERTICES_WRAPPER[2 + VERTEX_INDEX_Y] = 0f

        VERTICES_WRAPPER[4 + VERTEX_INDEX_X] = width
        VERTICES_WRAPPER[4 + VERTEX_INDEX_Y] = height

        VERTICES_WRAPPER[6 + VERTEX_INDEX_X] = 0f
        VERTICES_WRAPPER[6 + VERTEX_INDEX_Y] = height

        if (parent is Scene) {
            localToSceneTransformation.transform(VERTICES_WRAPPER)
        } else {
            localToParentTransformation.transform(VERTICES_WRAPPER)
        }

        return ShapeCollisionChecker.checkContains(VERTICES_WRAPPER, VERTICES_WRAPPER.size, x, y)
    }

    //endregion

    //region Transformations

    open fun onInvalidateTransformations() {
        mLocalToParentTransformationDirty = true
        mParentToLocalTransformationDirty = true

        // This recreates and calculates the transformation matrices.
        localToParentTransformation
        parentToLocalTransformation
    }


    override fun setRotation(pRotation: Float) {
        if (mRotation != pRotation) {
            mRotation = pRotation
            invalidate(InvalidationFlag.Transformations)
        }
    }

    override fun setRotationCenterX(pRotationCenterX: Float) = setRotationCenter(pRotationCenterX, mRotationCenterY)
    override fun setRotationCenterY(pRotationCenterY: Float) = setRotationCenter(mRotationCenterX, pRotationCenterY)
    override fun setRotationCenter(pRotationCenterX: Float, pRotationCenterY: Float) {
        if (mRotationCenterX != pRotationCenterX || mRotationCenterY != pRotationCenterY) {
            mRotationCenterX = pRotationCenterX
            mRotationCenterY = pRotationCenterY
            invalidate(InvalidationFlag.Transformations)
        }
    }

    override fun setScaleCenterX(pScaleCenterX: Float) = setScaleCenter(pScaleCenterX, mScaleCenterY)
    override fun setScaleCenterY(pScaleCenterY: Float) = setScaleCenter(mScaleCenterX, pScaleCenterY)
    override fun setScaleCenter(pScaleCenterX: Float, pScaleCenterY: Float) {
        if (mScaleCenterX != pScaleCenterX || mScaleCenterY != pScaleCenterY) {
            mScaleCenterX = pScaleCenterX
            mScaleCenterY = pScaleCenterY
            invalidate(InvalidationFlag.Transformations)
        }
    }

    override fun setScaleX(pScaleX: Float) = setScale(pScaleX, mScaleY)
    override fun setScaleY(pScaleY: Float) = setScale(mScaleX, pScaleY)
    override fun setScale(pScale: Float) = setScale(pScale, pScale)
    override fun setScale(pScaleX: Float, pScaleY: Float) {
        if (!Precision.almostEquals(mScaleX, pScaleX) || !Precision.almostEquals(mScaleY, pScaleY)) {
            mScaleX = pScaleX
            mScaleY = pScaleY
            invalidate(InvalidationFlag.Transformations)
        }
    }

    override fun getLocalToParentTransformation(): Transformation {

        if (mLocalToParentTransformation == null) {
            mLocalToParentTransformation = Transformation()
        }

        if (mLocalToParentTransformationDirty) {
            mLocalToParentTransformation.setToIdentity()

            if (mScaleX != 1f || mScaleY != 1f) {
                val centerX = width * mScaleCenterX
                val centerY = height * mScaleCenterY

                mLocalToParentTransformation.postTranslate(-centerX, -centerY)
                mLocalToParentTransformation.postScale(mScaleX, mScaleY)
                mLocalToParentTransformation.postTranslate(centerX, centerY)
            }

            if (rotation != 0f) {
                val centerX = width * mRotationCenterX
                val centerY = height * mRotationCenterY

                mLocalToParentTransformation.postTranslate(-centerX, -centerY)
                mLocalToParentTransformation.postRotate(mRotation)
                mLocalToParentTransformation.postTranslate(centerX, centerY)
            }

            mLocalToParentTransformation.postTranslate(absoluteX, absoluteY)
            mLocalToParentTransformationDirty = false
        }

        return mLocalToParentTransformation
    }

    override fun getParentToLocalTransformation(): Transformation {

        if (mParentToLocalTransformation == null) {
            mParentToLocalTransformation = Transformation()
        }

        if (mParentToLocalTransformationDirty) {
            mParentToLocalTransformation.setToIdentity()
            mParentToLocalTransformation.postTranslate(-absoluteX, -absoluteY)

            if (mRotation != 0f) {
                val centerX = width * mRotationCenterX
                val centerY = height * mRotationCenterY

                mParentToLocalTransformation.postTranslate(-centerX, -centerY)
                mParentToLocalTransformation.postRotate(-mRotation)
                mParentToLocalTransformation.postTranslate(centerX, centerY)
            }

            if (mScaleX != 1f || mScaleY != 1f) {
                val centerX = width * mScaleCenterX
                val centerY = height * mScaleCenterY

                mParentToLocalTransformation.postTranslate(-centerX, -centerY)
                mParentToLocalTransformation.postScale(1 / mScaleX, 1 / mScaleY)
                mParentToLocalTransformation.postTranslate(centerX, centerY)
            }

            mParentToLocalTransformationDirty = false
        }

        return mParentToLocalTransformation
    }

    //endregion

    //region Modifiers

    private val universalModifierTrackers = mutableListOf<UniversalModifierTargetTracker>()

    /**
     * Appends a [UniversalModifier] to this [UIComponent]. The [UniversalModifier] will be tracked and
     * automatically.
     *
     * @param modifier The [UniversalModifier] to append.
     */
    fun appendModifier(modifier: UniversalModifier) {
        getTrackerFor(modifier.type, true)!!.add(modifier)
    }

    /**
     * Removes a [UniversalModifier] from this [UIComponent].
     *
     * @param modifier The [UniversalModifier] to remove.
     * @return Whether the [UniversalModifier] was removed.
     */
    fun removeModifier(modifier: UniversalModifier): Boolean {
        if (modifier.target != this) {
            return false
        }

        return getTrackerFor(modifier.type)?.remove(modifier) ?: false
    }

    private inline fun appendModifier(
        type: ModifierType,
        duration: Float,
        easing: Easing,
        crossinline block: UniversalModifier.() -> Unit
    ): UniversalModifier {
        val modifier = UniversalModifier.GlobalPool.acquire() ?: UniversalModifier()

        modifier.target = this
        modifier.type = type
        modifier.startTime = modifierStartTime
        modifier.duration = duration
        modifier.eased(easing)
        modifier.block()

        appendModifier(modifier)

        return modifier
    }

    /**
     * Obtains the [UniversalModifierTargetTracker] for the specified [ModifierType].
     *
     * @param type The [ModifierType] to get the [UniversalModifierTargetTracker] for.
     * @param createIfNotExisting Whether to create the [UniversalModifierTargetTracker] if it does not exist.
     * @return The [UniversalModifierTargetTracker] for [type], `null` if it did not exist and [createIfNotExisting] was
     * `false`.
     */
    private fun getTrackerFor(type: ModifierType, createIfNotExisting: Boolean = false): UniversalModifierTargetTracker? {
        for (i in universalModifierTrackers.indices) {
            val tracker = universalModifierTrackers[i]

            if (tracker.targetMember == type.targetMember) {
                return tracker
            }
        }

        if (!createIfNotExisting) {
            return null
        }

        val tracker = UniversalModifierTargetTracker(type.targetMember, this)
        universalModifierTrackers += tracker

        return tracker
    }

    /**
     * Resets [modifierDelay] and processes updates to [UniversalModifier]s.
     */
    protected fun updateModifiers() {
        modifierDelay = 0f

        updateModifiers(time?.current ?: return)
    }

    private fun updateModifiers(time: Float) {
        universalModifierTrackers.fastForEach { it.update(time) }
    }

    override fun clearEntityModifiers() {
        super.clearEntityModifiers()

        universalModifierTrackers.fastForEach { it.clear() }
    }

    /**
     * Clears [UniversalModifier]s of the specified [ModifierType] from this [UIComponent].
     *
     * Unlike the vararg variant, this method avoids array allocation in cases where there is only one [ModifierType] to
     * remove.
     *
     * @param type The [ModifierType] to clear.
     * @param propagateChildren Whether to also clear [UniversalModifier]s of children. Defaults to `false`.
     */
    @JvmOverloads
    fun clearModifiers(type: ModifierType, propagateChildren: Boolean = false) {
        universalModifierTrackers.fastForEach { it.clear(type) }

        if (propagateChildren) {
            mChildren?.fastForEach { (it as? UIComponent)?.clearModifiers(type, true) }
        }
    }

    /**
     * Clears [UniversalModifier]s of the specified [ModifierType]s from this [UIComponent].
     *
     * @param propagateChildren Whether to also clear [UniversalModifier]s of children. Defaults to `false`.
     * @param types The [ModifierType]s to remove.
     */
    @JvmOverloads
    fun clearModifiers(propagateChildren: Boolean = false, vararg types: ModifierType) {
        universalModifierTrackers.fastForEach { it.clear(*types) }

        if (propagateChildren) {
            mChildren?.fastForEach { (it as? UIComponent)?.clearModifiers(true, *types) }
        }
    }

    /**
     * Clears [UniversalModifier]s that start after [time].
     *
     * @param time The time to clear [UniversalModifier]s after.
     * @param propagateChildren Whether to also clear such [UniversalModifier]s of children. Defaults to `false`.
     */
    @JvmOverloads
    fun clearModifiersAfter(time: Float, propagateChildren: Boolean = false) {
        universalModifierTrackers.fastForEach { it.clearAfter(time) }

        if (propagateChildren) {
            mChildren?.fastForEach { (it as? UIComponent)?.clearModifiersAfter(time, true) }
        }
    }

    /**
     * Clears [UniversalModifier]s with the given [ModifierType] that start after [time].
     *
     * @param time The time to clear [UniversalModifier]s after.
     * @param type The [ModifierType] to clear.
     * @param propagateChildren Whether to also clear such [UniversalModifier]s of children. Defaults to `false`.
     */
    @JvmOverloads
    fun clearModifiersAfter(time: Float, type: ModifierType, propagateChildren: Boolean = false) {
        universalModifierTrackers.fastForEach { it.clearAfter(time, type) }

        if (propagateChildren) {
            mChildren?.fastForEach { (it as? UIComponent)?.clearModifiersAfter(time, type, true) }
        }
    }

    /**
     * Clears [UniversalModifier]s with the given [ModifierType]s that start after [time].
     *
     * @param time The time to clear [UniversalModifier]s after.
     * @param types The [ModifierType]s to clear.
     * @param propagateChildren Whether to also clear such [UniversalModifier]s of children. Defaults to `false`.
     */
    @JvmOverloads
    fun clearModifiersAfter(time: Float, propagateChildren: Boolean = false, vararg types: ModifierType) {
        universalModifierTrackers.fastForEach { it.clearAfter(time, *types) }

        if (propagateChildren) {
            mChildren?.fastForEach { (it as? UIComponent)?.clearModifiersAfter(time, true, *types) }
        }
    }

    /**
     * Finishes all [UniversalModifier]s, using their [UniversalModifier.finalValues] and calling their
     * [UniversalModifier.onFinished] callbacks.
     *
     * @param propagateChildren Whether to also finish [UniversalModifier]s of children. Defaults to `false`.
      * @param type The [ModifierType] to finish, or `null` to finish all [UniversalModifier]s regardless of their type.
     */
    @JvmOverloads
    fun finishModifiers(propagateChildren: Boolean = false, type: ModifierType? = null) {
        if (type != null) {
            getTrackerFor(type)?.finish()
        } else {
            universalModifierTrackers.fastForEach { it.finish() }
        }

        if (propagateChildren) {
            mChildren?.fastForEach { (it as? UIComponent)?.finishModifiers(true, type) }
        }
    }

    /**
     * Starting time to use for new [UniversalModifier]s.
     */
    val modifierStartTime
        get() = (clock?.currentTime ?: 0f) + modifierDelay

    /**
     * Delay from the current time until new [UniversalModifier]s are started, in seconds. **This is made public so that
     * [beginAbsoluteSequence] and [beginDelayedSequence] can be inlined for performance and should not be altered
     * externally**.
     */
    var modifierDelay = 0f

    /**
     * Adds a delay duration to [modifierDelay], in seconds.
     *
     * @param duration The delay duration to add.
     * @param propagateChildren Whether we also delay down the child.
     */
    @JvmOverloads
    open fun addDelay(duration: Float, propagateChildren: Boolean = false) {
        if (duration == 0f) {
            return
        }

        modifierDelay += duration

        if (propagateChildren) {
            mChildren?.fastForEach { (it as? UIComponent)?.addDelay(duration, true) }
        }
    }

    /**
     * Starts a sequence of [UniversalModifier]s. The block will be provided with a [UniversalModifierSequence] that can
     * be used to add [UniversalModifier]s.
     *
     * @param block The block to execute with the [UniversalModifierSequence] to add [UniversalModifier]s to.
     */
    @JvmSynthetic
    inline fun beginModifierSequence(crossinline block: UniversalModifierSequence.() -> Unit) =
        UniversalModifierSequence.obtain(this).use { it.block() }

    /**
     * Starts a sequence of [UniversalModifier]s. The block will be provided with a [UniversalModifierSequence] that can
     * be used to add [UniversalModifier]s.
     *
     * This is a Java-friendly overload of [beginModifierSequence] that accepts a [Consumer].
     *
     * @param block The block to execute with the [UniversalModifierSequence] to add [UniversalModifier]s to.
     */
    fun beginModifierSequence(block: Consumer<UniversalModifierSequence>) =
        beginModifierSequence { block.accept(this) }

    /**
     * Starts a sequence of [UniversalModifier]s from an absolute time value (adjusts [modifierStartTime]).
     *
     * @param newModifierStartTime The new value for [modifierStartTime].
     * @param propagateChildren Whether this should be applied to children. `true` by default.
     * @param block The block to execute with the [UniversalModifierSequence] to add [UniversalModifier]s to.
     */
    @JvmSynthetic
    inline fun beginAbsoluteSequence(
        newModifierStartTime: Float,
        propagateChildren: Boolean = true,
        crossinline block: UniversalModifierSequence.() -> Unit
    ) {
        val prevModifierStartTime = modifierStartTime

        adjustAbsoluteSequenceTime(newModifierStartTime, propagateChildren)

        try {
            beginModifierSequence(block)
        } finally {
            restoreAbsoluteSequenceTime(prevModifierStartTime, propagateChildren)
        }
    }

    /**
     * Starts a sequence of [UniversalModifier]s from an absolute time value (adjusts [modifierStartTime]).
     *
     * This is a Java-friendly overload of [beginAbsoluteSequence] that accepts a [Consumer].
     *
     * @param newModifierStartTime The new value for [modifierStartTime].
     * @param propagateChildren Whether this should be applied to children. `true` by default.
     * @param block The block to execute with the [UniversalModifierSequence] to add [UniversalModifier]s to.
     */
    @JvmOverloads
    fun beginAbsoluteSequence(
        newModifierStartTime: Float,
        propagateChildren: Boolean = true,
        block: Consumer<UniversalModifierSequence>
    ) = beginAbsoluteSequence(newModifierStartTime, propagateChildren) { block.accept(this) }

    /**
     * Adjusts [modifierStartTime] to a new absolute time value. **This is used internally for [beginAbsoluteSequence]
     * but is made public for inlining, and should not be used externally**.
     *
     * @param newModifierStartTime The new [modifierStartTime].
     * @param propagateChildren Whether to propagate [newModifierStartTime] to children. Defaults to `true`.
     */
    fun adjustAbsoluteSequenceTime(newModifierStartTime: Float, propagateChildren: Boolean = true) {
        modifierDelay += newModifierStartTime - modifierStartTime

        if (propagateChildren) {
            mChildren?.fastForEach { child ->
                (child as? UIComponent)?.adjustAbsoluteSequenceTime(newModifierStartTime)
            }
        }
    }

    /**
     * Restores [modifierStartTime] to its previous absolute time value. **This is used internally for
     * [beginAbsoluteSequence] but is made public for inlining, and should not be used externally**.
     *
     * @param prevModifierStartTime The previous [modifierStartTime].
     * @param propagateChildren Whether to propagate [prevModifierStartTime]. Defaults to `true`.
     */
    fun restoreAbsoluteSequenceTime(prevModifierStartTime: Float, propagateChildren: Boolean = true) {
        modifierDelay += prevModifierStartTime - modifierStartTime

        if (!Precision.almostEquals(prevModifierStartTime, modifierStartTime)) {
            throw IllegalStateException(
                "${this::class.simpleName}'s modifierStartTime at the end of absolute sequence is " +
                        "not the same as at the beginning (begin=$prevModifierStartTime end=$modifierStartTime)"
            )
        }

        if (propagateChildren) {
            mChildren?.fastForEach { child ->
                (child as? UIComponent)?.restoreAbsoluteSequenceTime(prevModifierStartTime)
            }
        }
    }

    /**
     * Starts a sequence of [UniversalModifier]s with a (cumulative) relative delay applied.
     *
     * @param delay The offset in seconds from current time. Note that this stacks with other nested sequences.
     * @param propagateChildren Whether this should be applied to children. `true` by default.
     * @param block The block to execute with the [UniversalModifierSequence] to add [UniversalModifier]s to.
     */
    @JvmSynthetic
    inline fun beginDelayedSequence(
        delay: Float,
        propagateChildren: Boolean = true,
        crossinline block: UniversalModifierSequence.() -> Unit
    ) {
        addDelay(delay, propagateChildren)
        val oldDelay = modifierDelay

        try {
            beginModifierSequence(block)

            val newDelay = modifierDelay

            if (!Precision.almostEquals(oldDelay, newDelay)) {
                throw IllegalStateException("${this::class.simpleName}'s modifierDelay at the end of delayed sequence " +
                        "is not the same as at the beginning (begin=$oldDelay end=$newDelay)")
            }
        } finally {
            addDelay(-delay, propagateChildren)
        }
    }

    /**
     * Starts a sequence of [UniversalModifier]s with a (cumulative) relative delay applied.
     *
     * This is a Java-friendly overload of [beginDelayedSequence] that accepts a [Consumer].
     *
     * @param delay The offset in seconds from current time. Note that this stacks with other nested sequences.
     * @param propagateChildren Whether this should be applied to children. `true` by default.
     * @param block The block to execute with the [UniversalModifierSequence] to add [UniversalModifier]s to.
     */
    @JvmOverloads
    fun beginDelayedSequence(
        delay: Float,
        propagateChildren: Boolean = true,
        block: Consumer<UniversalModifierSequence>
    ) = beginDelayedSequence(delay, propagateChildren) { block.accept(this) }

    //endregion

    //region Modifiers - Translation

    /**
     * Smoothly adjusts this [UIComponent]'s [translationX] and [translationY] over time.
     *
     * @param x The final [translationX] to reach at the end of the [UniversalModifier].
     * @param y The final [translationY] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun translateTo(x: Float, y: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        appendModifier(ModifierType.TranslateXY, duration, easing) {
            finalValues[0] = x
            finalValues[1] = y
        }

    /**
     * Smoothly adjusts this [UIComponent]'s [translationX] over time.
     *
     * @param value The final [translationX] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun translateToX(value: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        appendModifier(ModifierType.TranslateX, duration, easing) { finalValues[0] = value }

    /**
     * Smoothly adjusts this [UIComponent]'s [translationY] over time.
     *
     * @param value The final [translationY] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun translateToY(value: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        appendModifier(ModifierType.TranslateY, duration, easing) { finalValues[0] = value }

    //endregion

    //region Modifiers - Move

    /**
     * Smoothly adjusts this [UIComponent]'s [mX] and [mY] over time.
     *
     * @param x The final [mX] to reach at the end of the [UniversalModifier].
     * @param y The final [mY] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun moveTo(x: Float, y: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        appendModifier(ModifierType.MoveXY, duration, easing) {
            finalValues[0] = x
            finalValues[1] = y
        }

    /**
     * Smoothly adjusts this [UIComponent]'s [mX] over time.
     *
     * @param value The final [mX] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun moveToX(value: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        appendModifier(ModifierType.MoveX, duration, easing) { finalValues[0] = value }

    /**
     * Smoothly adjusts this [UIComponent]'s [mY] over time.
     *
     * @param value The final [mY] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun moveToY(value: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        appendModifier(ModifierType.MoveY, duration, easing) { finalValues[0] = value }

    //endregion

    //region Modifiers - Scale

    /**
     * Smoothly adjusts this [UIComponent]'s [mScaleX] and [mScaleY] over time.
     *
     * @param value The final [mScaleX] and [mScaleY] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun scaleTo(value: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        appendModifier(ModifierType.ScaleXY, duration, easing) {
            finalValues[0] = value
            finalValues[1] = value
        }

    /**
     * Smoothly adjusts this [UIComponent]'s [mScaleX] over time.
     *
     * @param value The final [mScaleY] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun scaleToX(value: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        appendModifier(ModifierType.ScaleX, duration, easing) { finalValues[0] = value }

    /**
     * Smoothly adjusts this [UIComponent]'s [mScaleY] over time.
     *
     * @param value The final [mScaleY] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun scaleToY(value: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        appendModifier(ModifierType.ScaleY, duration, easing) { finalValues[0] = value }

    //endregion

    //region Modifiers - Alpha

    /**
     * Smoothly adjusts this [UIComponent]'s [alpha] over time.
     *
     * @param value The final [alpha] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun fadeTo(value: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        appendModifier(ModifierType.Alpha, duration, easing) { finalValues[0] = value }

    /**
     * Smoothly adjusts this [UIComponent]'s [alpha] to 1 over time.
     *
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun fadeIn(duration: Float = 0f, easing: Easing = Easing.None) = fadeTo(1f, duration, easing)

    /**
     * Smoothly adjusts this [UIComponent]'s [alpha] from 0 to 1 over time.
     *
     * @param duration The duration of the [UniversalModifier]. Defaults to 0 seconds.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun fadeInFromZero(duration: Float = 0f, easing: Easing = Easing.None) =
        appendModifier(ModifierType.Alpha, duration, easing) {
            hasInitialValues = true
            initialValues[0] = 0f
            finalValues[0] = 1f
        }

    /**
     * Smoothly adjusts this [UIComponent]'s [alpha] to 0 over time.
     *
     * @param duration The duration of the [UniversalModifier]. Defaults to 0 seconds.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun fadeOut(duration: Float = 0f, easing: Easing = Easing.None) = fadeTo(0f, duration, easing)

    /**
     * Smoothly adjusts this [UIComponent]'s [alpha] from 1 to 0 over time.
     *
     * @param duration The duration of the [UniversalModifier]. Defaults to 0 seconds.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun fadeOutFromOne(duration: Float = 0f, easing: Easing = Easing.None) =
        appendModifier(ModifierType.Alpha, duration, easing) {
            hasInitialValues = true
            initialValues[0] = 1f
            finalValues[0] = 0f
        }

    //endregion

    //region Modifiers - Color

    /**
     * Smoothly adjusts this [UIComponent]'s [color] over time.
     *
     * @param color The final color to reach at the end of the [UniversalModifier], in 0xRRGGBB format.
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun colorTo(color: Long, duration: Float = 0f, easing: Easing = Easing.None) =
        colorTo(
            red = ((color ushr 16) and 0xFF) / 255f,
            green = ((color ushr 8) and 0xFF) / 255f,
            blue = (color and 0xFF) / 255f,
            duration = duration,
            easing = easing
        )

    /**
     * Smoothly adjusts this [UIComponent]'s [color] over time.
     *
     * @param color The final [Color4] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun colorTo(color: Color4, duration: Float = 0f, easing: Easing = Easing.None) =
        colorTo(color.red, color.green, color.blue, duration, easing)

    /**
     * Smoothly adjusts this [UIComponent]'s [color] over time.
     *
     * @param red The final red component of [color] to reach at the end of the [UniversalModifier], in the
     * range [0, 1].
     * @param green The final green component of [color] to reach at the end of the [UniversalModifier], in
     * the range [0, 1].
     * @param blue The final blue component of [color] to reach at the end of the [UniversalModifier], in
     * the range [0, 1].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun colorTo(red: Float, green: Float, blue: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        appendModifier(ModifierType.Color, duration, easing) {
            finalValues[0] = red
            finalValues[1] = green
            finalValues[2] = blue
        }

    //endregion

    //region Modifiers - Rotation

    /**
     * Smoothly adjusts this [UIComponent]'s [mRotation] over time.
     *
     * @param value The final [mRotation] to reach at the end of the [UniversalModifier], in degrees.
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun rotateTo(value: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        appendModifier(ModifierType.Rotation, duration, easing) { finalValues[0] = value }

    //endregion

    //region Modifiers - Size

    /**
     * Smoothly adjusts this [UIComponent]'s [width] and [height] over time.
     *
     * @param width The final [width] to reach at the end of the [UniversalModifier].
     * @param height The final [height] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun sizeTo(width: Float, height: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        appendModifier(ModifierType.Size, duration, easing) {
            finalValues[0] = width
            finalValues[1] = height
        }

    /**
     * Smoothly adjusts this [UIComponent]'s [width] over time.
     *
     * @param width The final [width] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun widthTo(width: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        appendModifier(ModifierType.Width, duration, easing) { finalValues[0] = width }

    /**
     * Smoothly adjusts this [UIComponent]'s [height] over time.
     *
     * @param height The final [height] to reach at the end of the [UniversalModifier].
     * @param duration The duration of the [UniversalModifier], in seconds. Defaults to 0.
     * @param easing The easing function to apply to the [UniversalModifier]. Defaults to [Easing.None].
     * @return The added [UniversalModifier].
     */
    @JvmOverloads
    fun heightTo(height: Float, duration: Float = 0f, easing: Easing = Easing.None) =
        appendModifier(ModifierType.Height, duration, easing) { finalValues[0] = height }

    //endregion

    //region Input

    /**
     * Propagates a touch event to the entity.
     */
    protected fun propagateTouchEvent(action: Int, pointerIndex: Int = 0, localX: Float = 0f, localY: Float = 0f): Boolean {

        val motionEvent = MotionEvent.obtain(0, 0, action, 0f, 0f, 0)
        val touchEvent = TouchEvent.obtain(0f, 0f, action, pointerIndex, motionEvent)

        val result = onAreaTouched(touchEvent, localX, localY)

        touchEvent.recycle()
        motionEvent.recycle()
        return result
    }

    /**
     * Called when input bindings are invalidated and needs to be removed.
     */
    open fun onInvalidateInputBindings() {
        inputBindings.fastForEachIndexed { index, binding ->
            if (binding != null) {
                propagateTouchEvent(MotionEvent.ACTION_CANCEL, index)
            }
        }
        inputBindings.fill(null)
    }

    /**
     * Called when a key is pressed.
     */
    open fun onKeyPress(keyCode: Int, event: KeyEvent): Boolean {
        return false
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

        val inputBinding = inputBindings.getOrNull(event.pointerID)

        if (inputBinding is IEntity && inputBinding.parent == this) {
            inputBinding.onAreaTouched(event, localX - inputBinding.absoluteX, localY - inputBinding.absoluteY)

            if (event.isActionUp) {
                inputBindings[event.pointerID] = null
            }
            return true
        } else {
            inputBindings[event.pointerID] = null
        }

        try {
            for (i in childCount - 1 downTo 0) {
                val child = getChild(i)
                if (child is UIComponent && child.contains(localX, localY)) {
                    if (child.onAreaTouched(event, localX - child.absoluteX, localY - child.absoluteY)) {
                        inputBindings[event.pointerID] = child
                        return true
                    }
                }
            }
        } catch (e: IndexOutOfBoundsException) {
            Log.e("UIComponent", "A child entity was removed during touch event propagation.", e)
        }

        return false
    }

    //endregion

    //region Cosmetic functions

    open fun onStyle(theme: Theme) {
        style(theme)
    }

    //endregion

    //region Timekeeping

    /**
     * Whether [IFrameBasedClock.processFrame] should be automatically invoked on this [UIComponent]'s [customClock] in
     * [onUpdate]. This should only be set to false in scenarios where the clock is updated elsewhere.
     */
    var processCustomClock = true

    private var customClock: IFrameBasedClock? = null
    // Cache inherited clock here to avoid parent tree climbing.
    private var inheritedClock: IFrameBasedClock? = null

    /**
     * The [IFrameBasedClock] of this [UIComponent]. Used for keeping track of time across frames. By default, this is
     * inherited from [parent].
     *
     * If set, then the provided value is used as a custom clock and [parent]'s [IFrameBasedClock] is ignored.
     */
    override var clock: IFrameBasedClock?
        get() = customClock ?: inheritedClock
        set(value) {
            customClock = value
            updateClock(inheritedClock)
        }

    /**
     * The current frame's time as observed by this class' [clock].
     */
    val time
        get() = clock?.timeInfo

    override fun updateClock(clock: IFrameBasedClock?) {
        inheritedClock = clock
        val currentClock = this.clock

        if (currentClock != null) {
            if (loadState == LoadState.NotLoaded) {
                loadState = LoadState.Ready
            }
        } else {
            if (loadState == LoadState.Loaded) {
                onUnload()
            }

            loadState = LoadState.NotLoaded
        }

        mChildren?.fastForEach {
            @Suppress("UNCHECKED_CAST")
            (it as? IClockReceiver<IFrameBasedClock?>)?.updateClock(currentClock)
        }
    }

    //endregion

    companion object {
        private val VERTICES_WRAPPER = FloatArray(8)
    }

}