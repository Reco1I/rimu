package com.reco1l.andengine.component

import android.util.*
import android.view.*
import com.edlplan.framework.easing.Easing
import com.osudroid.*
import com.reco1l.andengine.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.theme.Size
import com.reco1l.andengine.ui.*
import com.rian.osu.math.Precision
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import com.reco1l.toolkt.kotlin.*
import com.rian.andengine.modifier.*
import com.rian.andengine.timing.IFrameBasedClock
import com.rian.osu.math.Precision
import org.anddev.andengine.collision.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.entity.*
import org.anddev.andengine.entity.scene.*
import org.anddev.andengine.entity.scene.Scene.*
import org.anddev.andengine.input.touch.*
import org.anddev.andengine.opengl.util.*
import org.anddev.andengine.util.*
import org.anddev.andengine.util.constants.Constants.*
import javax.microedition.khronos.opengles.*
import kotlin.math.max

/**
 * Entity with extended features.
 * @author Reco1l
 */
@Suppress("MemberVisibilityCanBePrivate")
abstract class UIComponent : Entity(0f, 0f), ITouchArea {

    //region Size related properties

    /**
     * Whether the component can shrink below its intrinsic size. By default it
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
        get() = (if (attachmentMode == AttachmentMode.Child) parent.padding.left else 0f) + anchorPositionX - originPositionX + x + translationX

    /**
     * The absolute position for the Y axis of the entity taking into account the
     * anchor and origin in the parent's coordinate system.
     */
    val absoluteY
        get() = (if (attachmentMode == AttachmentMode.Child) parent.padding.top else 0f) + anchorPositionY - originPositionY + y + translationY


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
            mRed = value.red
            mGreen = value.green
            mBlue = value.blue
            mAlpha = value.alpha
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
    var backgroundColor
        get() = background?.color ?: Color4.Transparent
        set(value) {
            if (background?.color != value) {
                initializeBackground()
                background!!.color = value
            }
        }

    /**
     * The border color of this component.
     */
    var borderColor
        get() = border?.color ?: Color4.Transparent
        set(value) {
            if (border?.color != value) {
                initializeBorder()
                border!!.color = value
            }
        }

    /**
     * The border width of this component.
     */
    var borderWidth
        get() = border?.lineWidth ?: 0f
        set(value) {
            if (border?.lineWidth != value) {
                initializeBorder()
                border!!.lineWidth = value
            }
        }

    /**
     * The corner radius of this component.
     */
    open var radius = 0f
        set(value) {
            if (field != value) {
                field = value
                background?.radius = value
                border?.radius = value
            }
        }

    /**
     * The box used to draw the background of this component.
     */
    protected var background: UIBox? = null
        private set

    /**
     * The box used to draw the border of this component.
     */
    protected var border: UIBox? = null
        private set

    //endregion

    //region Other properties

    val isAnimating
        get() = mEntityModifiers.size > 1

    /**
     * Whether the entity should be culled when it is outside the parent's bounds.
     */
    var cullingMode = CullingMode.Disabled

    /**
     * The mode in which the entity is attached to its parent.
     */
    var attachmentMode = AttachmentMode.None
        private set


    private var invalidationFlags = InvalidationFlag.All

    private val inputBindings = arrayOfNulls<UIComponent>(10)

    //endregion

    //region Invalidation

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

    private fun initializeBackground() {
        if (background == null) {
            background = UIBox().apply {
                setParent(this@UIComponent, AttachmentMode.Decorator)
                radius = this@UIComponent.radius
                color = Color4.Transparent
            }
        }
    }

    private fun initializeBorder() {
        if (border == null) {
            border = UIBox().apply {
                setParent(this@UIComponent, AttachmentMode.Decorator)
                paintStyle = PaintStyle.Outline
                radius = this@UIComponent.radius
                color = Color4.Transparent
            }
        }
    }

    //endregion

    //region Attachment

    /**
     * Called when the content of the entity has changed. This usually is called when a child is added or removed.
     */
    open fun onContentChanged() {}


    override fun detachSelf(): Boolean {

        if (parent == null) {
            return false
        }

        if (attachmentMode == AttachmentMode.Decorator) {
            parent = null
            onDetached()
            return true
        }

        return super.detachSelf()
    }

    fun setParent(entity: IEntity?, mode: AttachmentMode?) {

        when (val parent = parent) {
            is Scene -> parent.unregisterTouchArea(this)
            is UIComponent -> parent.onChildDetached(this)
        }

        super.setParent(entity)

        attachmentMode = if (entity == null) AttachmentMode.None else mode ?: AttachmentMode.Child

        when (entity) {
            is Scene -> entity.registerTouchArea(this)
            is UIComponent -> entity.onChildAttached(this)
        }

        if (attachmentMode == AttachmentMode.Decorator) {
            // Set color-inheritance to false for decorators by default, but allowing to
            // change this after attaching if needed.
            inheritAncestorsColor = false

            if (entity == null) {
                onDetached()
            } else {
                onAttached()
            }
        }
    }

    override fun setParent(parent: IEntity?) {
        setParent(parent, null)
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

        when (val parent = parent) {
            is UIComponent -> updateClock(parent.clock)
            is UIScene -> updateClock(parent.clock)
        }
    }

    override fun onDetached() {
        super.onDetached()

        updateClock(null)
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

    override fun onApplyTransformations(gl: GL10, camera: Camera) {
        val x = absoluteX
        val y = absoluteY

        if (x != 0f || y != 0f) {
            gl.glTranslatef(x, y, 0f)
        }

        if (mRotation != 0f) {
            val centerX = width * mRotationCenterX
            val centerY = height * mRotationCenterY

            if (centerX > 0f || centerY > 0f) {
                gl.glTranslatef(centerX, centerY, 0f)
                gl.glRotatef(mRotation, 0f, 0f, 1f)
                gl.glTranslatef(-centerX, -centerY, 0f)
            } else {
                gl.glRotatef(mRotation, 0f, 0f, 1f)
            }
        }

        if (mScaleX != 1f || mScaleY != 1f) {
            val centerX = width * mScaleCenterX
            val centerY = height * mScaleCenterY

            if (centerX > 0f || centerY > 0f) {
                gl.glTranslatef(centerX, centerY, 0f)
                gl.glScalef(mScaleX, mScaleY, 1f)
                gl.glTranslatef(-centerX, -centerY, 0f)
            } else {
                gl.glScalef(mScaleX, mScaleY, 1f)
            }
        }
    }

    fun onApplyColor(gl: GL10) {

        var red = mRed
        var green = mGreen
        var blue = mBlue
        var alpha = mAlpha
        var parent = parent
        var inheritColor = inheritAncestorsColor

        while (parent != null) {

            if (inheritColor) {
                red *= parent.red
                green *= parent.green
                blue *= parent.blue
            }
            alpha *= parent.alpha

            if (red == 0f && green == 0f && blue == 0f || alpha == 0f) {
                break
            }

            if (parent is UIComponent && !parent.inheritAncestorsColor) {
                inheritColor = false
            }

            parent = parent.parent
        }

        GLHelper.setColor(gl, red, green, blue, alpha)
    }

    override fun onDraw(gl: GL10, camera: Camera) {

        val isCulled = isCulled(camera)

        if (!isVisible || isCulled) {
            return
        }

        if (clipToBounds) {
            val wasScissorTestEnabled = GLHelper.isEnableScissorTest()
            GLHelper.enableScissorTest(gl)

            // Entity coordinates in screen's space.
            val (topLeftX, topLeftY) = camera.convertSceneToSurfaceCoordinates(convertLocalToSceneCoordinates(0f, 0f))
            val (topRightX, topRightY) = camera.convertSceneToSurfaceCoordinates(convertLocalToSceneCoordinates(width, 0f))
            val (bottomRightX, bottomRightY) = camera.convertSceneToSurfaceCoordinates(convertLocalToSceneCoordinates(width, height))
            val (bottomLeftX, bottomLeftY) = camera.convertSceneToSurfaceCoordinates(convertLocalToSceneCoordinates(0f, height))

            val minX = minOf(topLeftX, bottomLeftX, bottomRightX, topRightX)
            val minY = minOf(topLeftY, bottomLeftY, bottomRightY, topRightY)
            val maxX = maxOf(topLeftX, bottomLeftX, bottomRightX, topRightX)
            val maxY = maxOf(topLeftY, bottomLeftY, bottomRightY, topRightY)

            ScissorStack.pushScissor(minX, minY, maxX - minX, maxY - minY)
            onManagedDraw(gl, camera)
            ScissorStack.pop()

            if (!wasScissorTestEnabled) {
                GLHelper.disableScissorTest(gl)
            }
        } else {
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

        gl.glPushMatrix()
        onApplyTransformations(gl, camera)

        background?.setSize(width, height)
        background?.onHandleInvalidations()
        background?.onDraw(gl, camera)

        doDraw(gl, camera)
        onDrawChildren(gl, camera)

        border?.setSize(width, height)
        border?.onHandleInvalidations()
        border?.onDraw(gl, camera)

        if (BuildSettings.SHOW_ENTITY_BOUNDARIES && DEBUG_FOREGROUND != this && attachmentMode != AttachmentMode.Decorator) {
            DEBUG_FOREGROUND.setSize(width, height)
            DEBUG_FOREGROUND.onHandleInvalidations()
            DEBUG_FOREGROUND.onDraw(gl, camera)
        }

        gl.glPopMatrix()
    }

    open fun beginDraw(gl: GL10) {
        // We haven't done any culling implementation so we disable it globally for all buffered entities.
        GLHelper.disableCulling(gl)
        GLHelper.disableTextures(gl)
        GLHelper.disableTexCoordArray(gl)
        onApplyColor(gl)
    }

    override fun doDraw(gl: GL10, camera: Camera) {
        beginDraw(gl)
    }

    //endregion

    //region Update

    override fun onManagedUpdate(deltaTimeSec: Float) {
        customClock?.processFrame()
        background?.onManagedUpdate(deltaTimeSec)
        border?.onManagedUpdate(deltaTimeSec)

        modifierDelay = 0f

        super.onManagedUpdate(deltaTimeSec)
    }

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

    /**
     * Clears [UniversalModifier]s of the specified [ModifierType] from this [UIComponent].
     *
     * Unlike the vararg variant, this method avoids array allocation in cases where there is only one [ModifierType] to
     * remove.
     *
     * @param type The [ModifierType] to remove.
     */
    fun clearModifiers(type: ModifierType) {
        unregisterEntityModifiers { it is UniversalModifier && it.type == type }
    }

    /**
     * Clears [UniversalModifier]s of the specified [ModifierType]s from this [UIComponent].
     *
     * @param types The [ModifierType]s to remove.
     */
    fun clearModifiers(vararg types: ModifierType) {
        unregisterEntityModifiers { it is UniversalModifier && it.type in types }
    }

    /**
     * Starting time to use for new [UniversalModifier]s.
     */
    val modifierStartTime
        get() = (clock?.currentTime ?: 0f) + modifierDelay

    /**
     * Delay from the current time until new [UniversalModifier]s are started, in seconds.
     */
    protected var modifierDelay = 0f
        private set

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
     */
    inline fun beginModifierSequence(crossinline block: UniversalModifierSequence.() -> Unit) =
        UniversalModifierSequence.obtain(this).use { it.block() }

    private var savedModifierStartTime = 0f

    /**
     * Starts a sequence of [UniversalModifier]s from an absolute time value (adjusts [modifierStartTime]).
     *
     * @param newModifierStartTime The new value for [modifierStartTime].
     * @param propagateChildren Whether this should be applied to children. `true` by default.
     */
    @JvmOverloads
    fun beginAbsoluteSequence(newModifierStartTime: Float, propagateChildren: Boolean = true, block: UniversalModifierSequence.() -> Unit) {
        adjustAbsoluteSequenceTime(newModifierStartTime, propagateChildren)

        beginModifierSequence(block)

        restoreAbsoluteSequenceTime(propagateChildren)
    }

    private fun adjustAbsoluteSequenceTime(newModifierStartTime: Float, includeChildren: Boolean = true) {
        savedModifierStartTime = modifierStartTime
        modifierDelay += newModifierStartTime - modifierStartTime

        if (includeChildren) {
            mChildren?.fastForEach { child ->
                (child as? UIComponent)?.adjustAbsoluteSequenceTime(newModifierStartTime)
            }
        }
    }

    private fun restoreAbsoluteSequenceTime(includeChildren: Boolean = true) {
        restoreFromAbsoluteSequenceTime(savedModifierStartTime)

        if (includeChildren) {
            mChildren?.fastForEach { child ->
                (child as? UIComponent)?.restoreAbsoluteSequenceTime()
            }
        }
    }

    private fun restoreFromAbsoluteSequenceTime(savedTime: Float) {
        if (!Precision.almostEquals(savedTime, modifierStartTime)) {
            throw IllegalStateException(
                "${this::class.simpleName}'s modifierStartTime at the end of absolute sequence is " +
                        "not the same as at the beginning (begin=$savedTime end=$modifierStartTime)"
            )
        }

        modifierDelay += savedTime - modifierStartTime
    }

    /**
     * Starts a sequence of [UniversalModifier]s with a (cumulative) relative delay applied.
     *
     * @param delay The offset in seconds from current time. Note that this stacks with other nested sequences.
     * @param propagateChildren Whether this should be applied to children. `true` by default.
     */
    @JvmOverloads
    fun beginDelayedSequence(delay: Float, propagateChildren: Boolean = true, block: UniversalModifierSequence.() -> Unit) {
        addDelay(delay, propagateChildren)
        val oldDelay = modifierDelay

        beginModifierSequence(block)

        val newDelay = modifierDelay

        if (!Precision.almostEquals(oldDelay, newDelay)) {
            throw IllegalStateException("${this::class.simpleName}'s modifierDelay at the end of delayed sequence is" +
                    "not the same as at the beginning (begin=$oldDelay end=$newDelay)")
        }

        addDelay(-delay, propagateChildren)
    }

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

    //region Modifiers Registration

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

        registerEntityModifier(modifier)

        return modifier
    }

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
        background?.onStyle(theme)
        border?.onStyle(theme)

        style(theme)
    }

    //endregion

    //region Timekeeping

    private var _clock: IFrameBasedClock? = null
    private var customClock: IFrameBasedClock? = null

    /**
     * The [IFrameBasedClock] of this [UIComponent]. Used for keeping track of time across frames. By default, this is
     * inherited from [parent].
     *
     * If set, then the provided value is used as a custom clock and [parent]'s [IFrameBasedClock] is ignored.
     */
    var clock
        get() = _clock
        set(value) {
            customClock = value
            updateClock(value)
        }

    /**
     * The current frame's time as observed by this class' [clock].
     */
    val time
        get() = clock?.timeInfo

    /**
     * Updates the [IFrameBasedClock] to be used. Has no effect if this [UIComponent] uses a custom [IFrameBasedClock].
     */
    open fun updateClock(clock: IFrameBasedClock?) {
        this._clock = customClock ?: clock

        mChildren?.fastForEach {
            (it as? UIComponent)?.updateClock(this._clock)
        }
    }

    //endregion

    companion object {

        private val DEBUG_FOREGROUND by lazy {
            UIBox().apply {
                paintStyle = PaintStyle.Outline
                color = Color4.White
                lineWidth = 2f
            }
        }

        private val VERTICES_WRAPPER = FloatArray(8)
    }

}

/**
 * A function that applies a [Theme] to a [UIComponent].
 */
typealias ThemeApplier = UIComponent.(theme: Theme) -> Unit

/**
 * Combines two [ThemeApplier]s into one.
 *
 * The original [ThemeApplier] will be applied first if present, followed by [other].
 *
 * @param other The other [ThemeApplier] to combine with.
 * @return A new [ThemeApplier] that applies both the original and the other [ThemeApplier].
 */
operator fun ThemeApplier?.plus(other: ThemeApplier): ThemeApplier = { theme ->
    this@plus?.invoke(this, theme)
    other.invoke(this, theme)
}
