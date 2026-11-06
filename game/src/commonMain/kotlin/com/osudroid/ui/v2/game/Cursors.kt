package com.osudroid.ui.v2.game

import com.osudroid.settings.GameResolution
import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.settings.Settings
import com.osudroid.ui.v2.SkinTextures
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

/**
 * The sprite of a cursor, which reacts to clicks and sliders.
 */
class CursorSprite : UISprite(), ISliderListener {

    val baseSize = OsuSettings.cursorSize * 2

    init {
        anchor = Anchor.Center
        origin = Anchor.Center
        scale = baseSize
        textureRegion = SkinTextures["cursor"]
    }


    override fun onAttached() {
        super.onAttached()
        startRotationLoop()
    }

    fun handleClick() {
        choreography?.clear()
        scale = baseSize

        choreograph {
            at(currentClockTime)
            scaleTo(baseSize * 1.25f, CLICK_ANIMATION_TIME)
            then()
            scaleTo(baseSize, CLICK_ANIMATION_TIME)
        }
    }

    fun update(secondsElapsed: Float) {
        if (scaleX > 2f) {
            scale = maxOf(baseSize, scaleX - baseSize * 0.75f * secondsElapsed)
        }
    }

    override fun onSliderStart() {}

    override fun onSliderTracking() {
        choreography?.clear()
        scale = baseSize
        choreograph { at(currentClockTime).scaleTo(baseSize * 1.25f, CLICK_ANIMATION_TIME) }
    }

    override fun onSliderEnd() {
        choreography?.clear()
        scale = baseSize * 1.25f
        choreograph { at(currentClockTime).scaleTo(baseSize, CLICK_ANIMATION_TIME) }
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
        const val CLICK_ANIMATION_TIME = 0.25f
    }
}

/**
 * A cursor in the playfield, optionally leaving a trail of particles behind.
 */
open class CursorEntity : UIContainer() {

    protected val cursorSprite = CursorSprite()

    private val trail = if (Settings[OsuSettings.UseParticles]) CursorTrail(cursorSprite) else null
    private var isShowing = false


    init {
        origin = Anchor.Center
        isVisible = false
        +cursorSprite
    }


    fun setShowing(showing: Boolean) {
        isShowing = showing
        isVisible = showing
    }

    fun click() = cursorSprite.handleClick()

    open fun update(secondsElapsed: Float) {
        if (isShowing) {
            cursorSprite.update(secondsElapsed)
            trail?.spawn(x.resolve(0f), y.resolve(0f), secondsElapsed)
        }
    }

    fun attachToScene(layer: UIContainer) {
        trail?.let { layer += it }
        layer += this
    }
}

/**
 * The particles left behind by a cursor.
 */
private class CursorTrail(private val cursor: CursorSprite) : UIParticleSystem(MAX_PARTICLES) {

    private val spawnInterval = 1f / (Verktex.graphics.window.refreshRate * 2)
    private var spawnTimer = 0f


    init {
        texture = SkinTextures["cursortrail"]
    }


    fun spawn(x: Float, y: Float, secondsElapsed: Float) {
        spawnTimer += secondsElapsed

        while (spawnTimer >= spawnInterval) {
            spawnTimer -= spawnInterval

            emit(
                x = x,
                y = y,
                lifetime = LIFETIME,
                startScale = cursor.baseSize,
                startAlpha = 1f,
                endAlpha = 0f,
                rotation = if (OsuSkin.get().isRotateCursorTrail) cursor.rotationZ else 0f
            )
        }
    }

    private companion object {
        const val MAX_PARTICLES = 256
        const val LIFETIME = 0.1f
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


    init {
        setPosition(GameResolution.width / 2f, GameResolution.height / 2f)
        setShowing(true)
    }


    override fun update(secondsElapsed: Float) {
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

    override fun onSliderStart() = cursorSprite.onSliderStart()

    override fun onSliderTracking() = cursorSprite.onSliderTracking()

    override fun onSliderEnd() = cursorSprite.onSliderEnd()
}
