package com.osudroid.ui.v2.game

import com.osudroid.settings.GameResolution
import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.settings.Settings
import com.osudroid.ui.v2.SkinTextures
import com.osudroid.ui.v2.skinTexture
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.ui.UIParticleSystem
import com.reco1l.verktex.ui.UISprite
import com.reco1l.verktex.ui.choreograph
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.rotateTo
import com.reco1l.verktex.ui.scaleTo
import ru.nsu.ccfit.zuev.osu.game.GameObject
import ru.nsu.ccfit.zuev.osu.game.GameObjectListener
import ru.nsu.ccfit.zuev.osu.game.GameplaySpinner
import ru.nsu.ccfit.zuev.osu.game.ISliderListener
import ru.nsu.ccfit.zuev.skins.OsuSkin
import kotlin.math.sqrt
import com.reco1l.verktex.graphics.BlendMode

/**
 * The sprite of a cursor, which grows while it's pressed.
 */
class CursorSprite : UISprite() {

    val baseSize = OsuSettings.cursorSize * 2

    // Whether the skin lets it grow while pressed.
    private val expands = OsuSkin.get().isExpandCursor

    private var isPressed = false

    init {
        anchor = Anchor.Center
        origin = cursorOrigin()
        scaleOrigin = cursorOrigin()
        scale = baseSize
        texture = skinTexture("cursor")
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
 * A cursor in the playfield, leaving a trail behind if the skin has one.
 */
open class CursorEntity : UIContainer() {

    protected val cursorSprite = CursorSprite()

    // Drawn over the cursor, it neither rotates nor grows.
    private val middleSprite = UISprite().apply {
        anchor = Anchor.Center
        origin = cursorOrigin()
        scale = cursorSprite.baseSize
        texture = skinTexture("cursormiddle")
    }

    private val trail = if (SkinTextures["cursortrail"] != null) CursorTrail(cursorSprite) else null
    private var isShowing = false


    init {
        origin = Anchor.Center
        isVisible = false
        +cursorSprite
        +middleSprite
    }


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

    fun attachToScene(layer: UIContainer) {
        trail?.let { layer += it }
        layer += this
    }
}

/**
 * Where a cursor's textures are placed from its position: their centre, or their top left corner for skins
 * that ask for it.
 */
private fun cursorOrigin() = if (OsuSkin.get().isCentreCursor) Anchor.Center else Anchor.TopLeft

/**
 * The trail left behind by a cursor, as osu!stable's: separate images every frame for skins without a
 * `cursormiddle`, a continuous trail along the path of the cursor for skins with one.
 *
 * Reference: https://github.com/ppy/osu/blob/master/osu.Game.Rulesets.Osu/Skinning/Legacy/LegacyCursorTrail.cs
 */
private class CursorTrail(private val cursor: CursorSprite) : UIParticleSystem(MAX_PARTICLES) {

    private val isContinuous = SkinTextures["cursormiddle"] != null

    private val lifetime = if (isContinuous) CONTINUOUS_LIFETIME else SEPARATE_LIFETIME

    private val isCentred = isContinuous || OsuSkin.get().isCentreCursor

    private var timeSinceLast = SEPARATE_INTERVAL

    // Where the last part of a continuous trail was put, NaN when it starts again.
    private var lastX = Float.NaN
    private var lastY = Float.NaN


    init {
        texture = SkinTextures["cursortrail"]

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
        val interval = texture.displayWidth * cursor.baseSize / 2.5f
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
        val offsetX = if (isCentred) 0f else width * cursor.baseSize / 2f
        val offsetY = if (isCentred) 0f else height * cursor.baseSize / 2f

        emit(
            x = x + offsetX,
            y = y + offsetY,
            lifetime = lifetime,
            startScale = cursor.baseSize,
            startAlpha = 1f,
            endAlpha = 0f,
            rotation = if (OsuSkin.get().isRotateCursorTrail) cursor.rotationZ else 0f
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
 * The cursor used in autoplay, moving by itself from object to object.
 */
class AutoCursor : CursorEntity(), ISliderListener {

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
        setPosition(GameResolution.width / 2f, GameResolution.height / 2f)
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
    fun setPosition(x: Float, y: Float, listener: GameObjectListener) {
        stopMoving()
        setPosition(x, y)
        listener.onUpdatedAutoCursor(x, y)
    }

    /**
     * Moves the cursor to the specified object.
     *
     * @param secPassed The amount of seconds that have passed since the game has started.
     */
    fun moveToObject(gameObject: GameObject?, secPassed: Float, listener: GameObjectListener) {
        if (gameObject == null || currentObjectId == gameObject.getId()) {
            return
        }

        val isSpinner = gameObject is GameplaySpinner
        val moveX = gameObject.getPosition().x
        val moveY = gameObject.getPosition().y + if (isSpinner) 50f else 0f
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

    override fun onSliderStart() {
        isOnSlider = true
        setPressed(true)
    }

    override fun onSliderTracking() = onSliderStart()

    override fun onSliderEnd() {
        isOnSlider = false
        setPressed(false)
    }

    private companion object {
        // How long autoplay holds a key for a hit, as osu!lazer's.
        const val KEY_UP_DELAY = 0.05f
    }
}
