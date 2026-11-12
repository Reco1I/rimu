package com.osudroid.ui.v2.hud.elements

import com.reco1l.verktex.ui.borderWidth
import com.reco1l.verktex.data.Background
import com.reco1l.verktex.data.CornerRadius
import com.reco1l.verktex.ui.borderColor
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.data.px
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.clearTransforms
import com.reco1l.verktex.ui.container.container
import com.reco1l.verktex.ui.moveTo
import com.reco1l.verktex.ui.shape.box
import com.reco1l.verktex.ui.shape.circle

import com.osudroid.math.Vector2
import com.osudroid.math.toRadians
import com.osudroid.ui.v2.hud.HUDElement
import com.osudroid.utils.*
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.shape.UIBox
import com.reco1l.verktex.data.Color4
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Shows where circle and slider head judgements land relative to the object's center.
 *
 * Positions are normalized against the direction from the previous object to the current one, so a consistent aim bias
 * shows up as a consistent offset regardless of approach angle.
 */
class HUDAimErrorMeter : HUDElement() {
    private val greatColor = Color4(70, 180, 220)
    private val okColor = Color4(100, 220, 40)
    private val mehColor = Color4(200, 180, 110)
    private val missColor = Color4(255, 9, 9)

    private val expiredIndicators = SynchronizedPool<Indicator>(30)
    private val activeIndicators = mutableListOf<Indicator>()

    // Still attached as a normal child (so its `moveTo` transform keeps ticking via the regular update
    // traversal), but manually re-drawn after the hit indicators in `onDraw` - see there for why.
    private val averageMarker = UIContainer().apply {
        anchor = Anchor.Center
        origin = Anchor.Center
        width = (AVERAGE_MARKER_SIZE).px
        height = (AVERAGE_MARKER_SIZE).px
        box {
            anchor = Anchor.Center
            origin = Anchor.Center
            width = (AVERAGE_MARKER_SIZE).px
            height = (AVERAGE_MARKER_THICKNESS).px
            background = Background.Color(Color4.White)
        }

        box {
            anchor = Anchor.Center
            origin = Anchor.Center
            width = (AVERAGE_MARKER_SIZE).px
            height = (AVERAGE_MARKER_THICKNESS).px
            rotationZ = 90f
            background = Background.Color(Color4.White)
        }
    }

    // Scratch fields reused every judgement to avoid Vec2/Vector2 allocations in this hot path.
    private var hitPositionX = 0f
    private var hitPositionY = 0f

    private var hasAveragePosition = false
    private var averagePositionX = 0f
    private var averagePositionY = 0f

    private var hasLastObjectPosition = false
    private var lastObjectPositionX = 0f
    private var lastObjectPositionY = 0f


    init {
        width = (SIZE).px
        height = (SIZE).px
        circle {
            anchor = Anchor.Center
            origin = Anchor.Center
            width = (SIZE * INNER_PORTION).px
            height = (SIZE * INNER_PORTION).px
            color = Color4(128, 128, 128)
            alpha = 0.3f
        }

        box {
            anchor = Anchor.Center
            origin = Anchor.Center
            width = (SIZE * INNER_PORTION).px
            height = (SIZE * INNER_PORTION).px
            radius = CornerRadius((SIZE * INNER_PORTION / 2f).px)
            borderWidth = LINE_THICKNESS.px
            borderColor = Color4.White
        }

        // Cross background
        // Cardinal guides at higher opacity
        +createCrossGuide(0f, LINE_THICKNESS, 0.5f)
        +createCrossGuide(90f, LINE_THICKNESS, 0.5f)
        // Diagonal guides at lower opacity
        +createCrossGuide(45f, LINE_THICKNESS / 2f, 0.2f)
        +createCrossGuide(135f, LINE_THICKNESS / 2f, 0.2f)

        // Arrow background
        // A fixed 45-degree reference axis for the normalized flow direction, with a chevron tip at one end.
        val shaftLength = SIZE * (INNER_PORTION + 0.2f)

        container {
            anchor = Anchor.Center
            origin = Anchor.Center
            width = (SIZE).px
            height = (SIZE).px
            rotationZ = 45f

            box {
                anchor = Anchor.Center
                origin = Anchor.Center
                width = (LINE_THICKNESS).px
                height = (shaftLength).px
                background = Background.Color(Color4.White)
                alpha = 0.7f
            }

            // Chevron tip
            // Each wing hangs down from the shaft's tip point (its own top-center, used as the rotation
            // pivot) and is splayed outwards by rotating around that point.
            box {
                anchor = Anchor.Center
                origin = Anchor.TopCenter
                width = (LINE_THICKNESS).px
                height = (ARROW_TIP_LENGTH).px
                y = (-shaftLength / 2f).px
                rotationOrigin = Anchor.TopCenter
                rotationZ = -45f
                background = Background.Color(Color4.White)
                alpha = 0.7f
            }

            box {
                anchor = Anchor.Center
                origin = Anchor.TopCenter
                width = (LINE_THICKNESS).px
                height = (ARROW_TIP_LENGTH).px
                y = (-shaftLength / 2f).px
                rotationOrigin = Anchor.TopCenter
                rotationZ = 45f
                background = Background.Color(Color4.White)
                alpha = 0.7f
            }
        }

        +averageMarker
    }

    private fun createCrossGuide(rotationDegrees: Float, thickness: Float, guideAlpha: Float) = UIBox().apply {
        anchor = Anchor.Center
        origin = Anchor.Center
        width = (thickness).px
        height = (SIZE * INNER_PORTION * 0.9f).px
        rotationZ = rotationDegrees
        background = Background.Color(Color4.White)
        alpha = guideAlpha
    }

    override fun onAimJudgement(objectPosition: Vector2, cursorPosition: Vector2, objectRadius: Float) {
        if (objectRadius <= 0f) {
            return
        }

        computeHitPosition(objectPosition, cursorPosition, objectRadius)

        val distance = hypot(hitPositionX, hitPositionY)

        val color = when {
            distance >= 0.5f * INNER_PORTION -> missColor
            distance >= 0.35f * INNER_PORTION -> mehColor
            distance >= 0.2f * INNER_PORTION -> okColor
            else -> greatColor
        }

        val indicator = expiredIndicators.acquire() ?: Indicator(0f, 0f, Color4.White, 0f)

        indicator.x = hitPositionX * SIZE
        indicator.y = hitPositionY * SIZE
        indicator.color = color
        indicator.alpha = 1f

        activeIndicators.add(indicator)

        if (hasAveragePosition) {
            averagePositionX = hitPositionX * 0.1f + averagePositionX * 0.9f
            averagePositionY = hitPositionY * 0.1f + averagePositionY * 0.9f
        } else {
            averagePositionX = hitPositionX
            averagePositionY = hitPositionY
            hasAveragePosition = true
        }

        averageMarker.moveTo(
            averagePositionX * SIZE,
            averagePositionY * SIZE,
            AVERAGE_MARKER_MOVE_DURATION,
            Easing.OutQuint
        )

        lastObjectPositionX = objectPosition.x
        lastObjectPositionY = objectPosition.y
        hasLastObjectPosition = true
    }

    /**
     * Rotates the hit offset so that the previous-to-current object flow direction always points along a fixed
     * 45-degree axis, falling back to a raw (unrotated) offset for the very first judgement of a play.
     */
    private fun computeHitPosition(objectPosition: Vector2, cursorPosition: Vector2, objectRadius: Float) {
        if (hasLastObjectPosition) {
            val angle1 = atan2(objectPosition.y - cursorPosition.y, cursorPosition.x - objectPosition.x)
            val angle2 = atan2(objectPosition.y - lastObjectPositionY, lastObjectPositionX - objectPosition.x)
            val finalAngle = angle2 - angle1

            val normalizedDistance =
                hypot(cursorPosition.x - objectPosition.x, cursorPosition.y - objectPosition.y) / objectRadius

            val rotatedAngle = finalAngle - 45f.toRadians()

            hitPositionX = -normalizedDistance * cos(rotatedAngle) * (INNER_PORTION / 2f)
            hitPositionY = -normalizedDistance * sin(rotatedAngle) * (INNER_PORTION / 2f)
        } else {
            hitPositionX = (cursorPosition.x - objectPosition.x) / objectRadius * (INNER_PORTION / 2f)
            hitPositionY = (cursorPosition.y - objectPosition.y) / objectRadius * (INNER_PORTION / 2f)
        }

        hitPositionX = hitPositionX.coerceIn(-0.5f, 0.5f)
        hitPositionY = hitPositionY.coerceIn(-0.5f, 0.5f)
    }

    override fun onSeek() {
        // Note that this only clears current active indicators. While reconstructing indicators that should still be
        // active is possible, it is not worth the complexity.
        activeIndicators.forEach {
            if (!it.isRecycled) {
                expiredIndicators.release(it)
            }
        }

        activeIndicators.clear()

        hasAveragePosition = false
        hasLastObjectPosition = false

        // Snap back to center immediately rather than animating from the pre-seek position (which is now stale).
        averageMarker.clearTransforms()
        averageMarker.translationX = 0f.px
        averageMarker.translationY = 0f.px
    }

    //region Indicator update & draw

    override fun onDraw(renderer: GraphicsRenderer) {
        // averageMarker is hidden for this pass and re-drawn manually below (after the hit indicators) so
        // it always renders on top of them.
        averageMarker.isVisible = false
        super.onDraw(renderer)
        averageMarker.isVisible = true

        val centerX = bounds.width / 2f
        val centerY = bounds.height / 2f

        // Half of the marker's length on each axis, as its bars are rotated 45 degrees.
        val offset = HIT_MARKER_SIZE / 2f * cos(45f.toRadians())

        activeIndicators.forEach {
            val x = centerX + it.x
            val y = centerY + it.y
            val color = it.color.alpha(it.alpha)

            renderer.drawLine(x - offset, y - offset, x + offset, y + offset, HIT_MARKER_THICKNESS, color)
            renderer.drawLine(x - offset, y + offset, x + offset, y - offset, HIT_MARKER_THICKNESS, color)
        }

        averageMarker.draw(renderer)
    }

    override fun onUpdate(clock: FrameClock) {
        activeIndicators.forEach(Indicator::update)

        super.onUpdate(clock)
    }

    //endregion

    private inner class Indicator(var x: Float, var y: Float, var color: Color4, var alpha: Float) : IPoolable {
        override var isRecycled = false

        fun update() {
            if (alpha > 0f) {
                alpha -= ALPHA_DECAY
            }

            if (alpha <= 0f) {
                alpha = 0f
                expiredIndicators.release(this)

                updateThread {
                    activeIndicators.remove(this)
                }
            }
        }
    }

    companion object {
        private const val SIZE = 125f
        private const val INNER_PORTION = 0.85f
        private const val LINE_THICKNESS = 2.5f
        private const val ARROW_TIP_LENGTH = 12f

        private const val HIT_MARKER_SIZE = 9f
        private const val HIT_MARKER_THICKNESS = 2f

        private const val AVERAGE_MARKER_SIZE = 15f
        private const val AVERAGE_MARKER_THICKNESS = 3f
        private const val AVERAGE_MARKER_MOVE_DURATION = 0.8f

        // Fades a marker out over ~5 seconds, assuming a 60 FPS update rate.
        private const val ALPHA_DECAY = 1f / 300f
    }
}
