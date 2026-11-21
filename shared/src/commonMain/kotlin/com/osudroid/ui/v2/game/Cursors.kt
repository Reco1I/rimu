package com.osudroid.ui.v2.game

import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.settings.Settings
import com.osudroid.ui.v2.skin.CursorTrailStyle
import com.osudroid.ui.v2.skin.SkinComponent
import com.osudroid.ui.v2.skin.SkinPieces
import com.osudroid.ui.v2.skinTexture
import com.reco1l.verktex.Verktex
import com.osudroid.game.Playfield
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.LayoutConstraints
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.ui.ParticleSystem
import com.reco1l.verktex.ui.Sprite
import com.reco1l.verktex.ui.choreograph
import com.reco1l.verktex.ui.container.Container
import com.reco1l.verktex.ui.rotateTo
import com.reco1l.verktex.ui.scaleTo
import com.osudroid.game.objects.GameObject
import com.osudroid.game.objects.GameplayCursors
import com.osudroid.game.objects.GameplaySpinner
import ru.nsu.ccfit.zuev.skins.OsuSkin
import kotlin.math.sqrt
import com.reco1l.verktex.graphics.BlendMode

/**
 * A cursor's look, the skin's [CursorPiece], which grows while it's pressed.
 */
class CursorSprite : Container() {

    val baseSize = OsuSettings.cursorSize * 2

    // Whether the skin lets it grow while pressed.
    private val expands = OsuSkin.get().isExpandCursor

    private var isPressed = false

    private val piece = SkinPieces.current.create(SkinComponent.Cursor)

    /**
     * Where the cursor is placed from its position: its centre, or its top left corner for skins that ask for it.
     */
    val cursorOrigin = if (piece.isCentred) Anchor.Center else Anchor.TopLeft

    init {
        anchor = Anchor.Center
        origin = cursorOrigin
        scaleOrigin = cursorOrigin
        scale = baseSize
        +piece
    }


    override fun onAttached() {
        super.onAttached()
        startRotationLoop()
    }

    /**
     * Grows the cursor while it's pressed, unless the skin says it doesn't.
     *
     * Reference: https://github.com/ppy/osu/blob/master/osu.Game.Rulesets.Osu/UI/Cursor/OsuCursor.cs
     */
    fun press() {
        if (!expands || isPressed) {
            return
        }
        isPressed = true

        choreography?.clear()
        scale = baseSize
        choreograph { at(currentClockTime).scaleTo(baseSize * PRESSED_SCALE, EXPAND_DURATION, Easing.OutElasticHalf) }
    }

    /**
     * Shrinks the cursor back once it's released.
     */
    fun release() {
        if (!isPressed) {
            return
        }
        isPressed = false

        choreography?.clear()
        choreograph { at(currentClockTime).scaleTo(baseSize, EXPAND_DURATION, Easing.OutQuad) }
    }

    private fun startRotationLoop() {
        if (OsuSkin.get().isRotateCursor) {
            rotateTo(360f, 14f).after {
                rotationZ = 0f
                startRotationLoop()
            }
        }
    }

    private companion object {
        const val PRESSED_SCALE = 1.2f
        const val EXPAND_DURATION = 0.4f
    }
}

/**
 * A cursor in the playfield, leaving a trail behind if the skin has one. It's placed in osu!pixels and sized for
 * osu!droid's 1280x720 screen, scaled by [Playfield.SCREEN_PIXEL].
 */
open class CursorEntity : Container() {

    protected val cursorSprite = CursorSprite()

    // Drawn over the cursor, it neither rotates nor grows. Only texture skins have it.
    private val middleSprite = Sprite().apply {
        anchor = Anchor.Center
        origin = cursorSprite.cursorOrigin
        scale = cursorSprite.baseSize
        texture = skinTexture("cursormiddle")
    }

    private val trail = SkinPieces.current.createOrNull(SkinComponent.CursorTrail)
        ?.takeIf { it.texture != null }
        ?.let { CursorTrail(cursorSprite, it) }
    private var isShowing = false


    init {
        origin = Anchor.Center
        isVisible = false
        scale = Playfield.SCREEN_PIXEL
        +cursorSprite
        +middleSprite
    }


    // It keeps its size whatever the size of the layer, the cursor can be bigger than the playfield.
    override fun adjustParentConstraints(constraints: LayoutConstraints) =
        LayoutConstraints(0f, Float.POSITIVE_INFINITY, 0f, Float.POSITIVE_INFINITY)

    fun setShowing(showing: Boolean) {
        if (!showing) {
            trail?.breakPath()
        }

        isShowing = showing
        isVisible = showing
    }

    /**
     * Whether the cursor is pressed, it grows while it is.
     */
    fun setPressed(pressed: Boolean) {
        if (pressed) cursorSprite.press() else cursorSprite.release()
    }

    open fun update(secondsElapsed: Float) {
        if (isShowing) {
            trail?.spawn(x.resolve(0f), y.resolve(0f), secondsElapsed)
        }
    }

    fun attachToScene(layer: Container) {
        trail?.let { layer += it }
        layer += this
    }
}

/**
 * The trail left behind by a cursor, as osu!stable's, in the skin's [style]: separate images every frame, or a
 * continuous trail along the path of the cursor.
 *
 * Reference: https://github.com/ppy/osu/blob/master/osu.Game.Rulesets.Osu/Skinning/Legacy/LegacyCursorTrail.cs
 */
private class CursorTrail(private val cursor: CursorSprite, private val style: CursorTrailStyle) : ParticleSystem(MAX_PARTICLES) {

    // The scale of the parts, the cursor's in the playfield.
    private val partScale = cursor.baseSize * Playfield.SCREEN_PIXEL

    private val isContinuous = style.isContinuous

    private val lifetime = if (isContinuous) CONTINUOUS_LIFETIME else SEPARATE_LIFETIME

    private val isCentred = style.isCentred

    private var timeSinceLast = SEPARATE_INTERVAL

    // Where the last part of a continuous trail was put, NaN when it starts again.
    private var lastX = Float.NaN
    private var lastY = Float.NaN


    init {
        texture = style.texture

        if (isContinuous) {
            blendMode = BlendMode.Additive
        }
    }


    /**
     * Makes the next part start the trail again, e.g. when the cursor was hidden.
     */
    fun breakPath() {
        lastX = Float.NaN
        lastY = Float.NaN
    }

    fun spawn(x: Float, y: Float, secondsElapsed: Float) {
        val texture = texture ?: return

        if (!isContinuous) {
            timeSinceLast += secondsElapsed

            if (timeSinceLast >= SEPARATE_INTERVAL) {
                timeSinceLast = 0f
                emitPart(x, y, texture.displayWidth, texture.displayHeight)
            }
            return
        }

        if (lastX.isNaN()) {
            lastX = x
            lastY = y
            emitPart(x, y, texture.displayWidth, texture.displayHeight)
            return
        }

        // Parts along the way, overlapping so they look like one.
        val interval = texture.displayWidth * partScale / 2.5f
        val dx = x - lastX
        val dy = y - lastY
        var distance = sqrt(dx * dx + dy * dy)

        if (interval <= 0f || distance < interval) {
            return
        }

        val directionX = dx / distance
        val directionY = dy / distance

        while (distance >= interval) {
            lastX += directionX * interval
            lastY += directionY * interval
            distance -= interval
            emitPart(lastX, lastY, texture.displayWidth, texture.displayHeight)
        }
    }

    private fun emitPart(x: Float, y: Float, width: Float, height: Float) {
        // Particles are centred, a trail with its origin at the top left is offset by half their size.
        val offsetX = if (isCentred) 0f else width * partScale / 2f
        val offsetY = if (isCentred) 0f else height * partScale / 2f

        emit(
            x = x + offsetX,
            y = y + offsetY,
            lifetime = lifetime,
            startScale = partScale,
            startAlpha = 1f,
            endAlpha = 0f,
            rotation = if (style.rotates) cursor.rotationZ else 0f
        )
    }

    private companion object {
        const val MAX_PARTICLES = 512

        const val SEPARATE_INTERVAL = 1f / 60f
        const val SEPARATE_LIFETIME = 0.15f
        const val CONTINUOUS_LIFETIME = 0.5f
    }
}

/**
 * The cursor used in autoplay, moving by itself from object to object, from [startX] and [startY] in the playfield.
 */
class AutoCursor(startX: Float, startY: Float) : CursorEntity() {

    /**
     * The ID of the object that the cursor is currently active on.
     */
    private var currentObjectId = -1

    private var isMoving = false
    private var moveElapsed = 0f
    private var moveDuration = 0f
    private var moveStartX = 0f
    private var moveStartY = 0f
    private var moveEndX = 0f
    private var moveEndY = 0f

    // How long until the press of a hit is released, held while on a slider.
    private var releaseTimer = 0f
    private var isOnSlider = false


    init {
        setPosition(startX, startY)
        setShowing(true)
    }


    override fun update(secondsElapsed: Float) {
        if (releaseTimer > 0f) {
            releaseTimer -= secondsElapsed

            if (releaseTimer <= 0f && !isOnSlider) {
                setPressed(false)
            }
        }

        if (isMoving) {
            moveElapsed += secondsElapsed

            val progress = Easing.OutQuad.interpolate((moveElapsed / moveDuration).coerceAtMost(1f))

            setPosition(
                moveStartX + (moveEndX - moveStartX) * progress,
                moveStartY + (moveEndY - moveStartY) * progress
            )

            isMoving = moveElapsed < moveDuration
        }

        super.update(secondsElapsed)
    }

    /**
     * Cancels the current movement, leaving the cursor where it is.
     */
    fun stopMoving() {
        isMoving = false
    }

    /**
     * Directly moves the cursor to the specified position.
     */
    fun setPosition(x: Float, y: Float, listener: GameplayCursors) {
        stopMoving()
        setPosition(x, y)
        listener.onUpdatedAutoCursor(x, y)
    }

    /**
     * Moves the cursor to the specified object.
     *
     * @param secPassed The amount of seconds that have passed since the game has started.
     */
    fun moveToObject(gameObject: GameObject?, secPassed: Float, listener: GameplayCursors) {
        if (gameObject == null || currentObjectId == gameObject.getId()) {
            return
        }

        val isSpinner = gameObject is GameplaySpinner
        val position = gameObject.getPosition()
        val moveX = position.x
        // Below the centre of a spinner, by 50 pixels of osu!droid's 1280x720 screen.
        val moveY = position.y + if (isSpinner) 50 * Playfield.SCREEN_PIXEL else 0f
        var deltaTime = gameObject.getHitTime() - secPassed

        currentObjectId = gameObject.getId()

        if (deltaTime < 0.085f && !isSpinner) {
            deltaTime = 0.085f
        }

        if (deltaTime <= 0f) {
            setPosition(moveX, moveY, listener)
            click()
        } else {
            moveStartX = x.resolve(0f)
            moveStartY = y.resolve(0f)
            moveEndX = moveX
            moveEndY = moveY
            moveElapsed = 0f
            moveDuration = deltaTime
            isMoving = true
        }

        listener.onUpdatedAutoCursor(moveX, moveY)
    }

    /**
     * Presses the cursor for a moment, as autoplay hits.
     */
    fun click() {
        setPressed(true)
        releaseTimer = KEY_UP_DELAY
    }

    fun onSliderStart() {
        isOnSlider = true
        setPressed(true)
    }

    fun onSliderTracking() = onSliderStart()

    fun onSliderEnd() {
        isOnSlider = false
        setPressed(false)
    }

    private companion object {
        // How long autoplay holds a key for a hit, as osu!lazer's.
        const val KEY_UP_DELAY = 0.05f
    }
}
