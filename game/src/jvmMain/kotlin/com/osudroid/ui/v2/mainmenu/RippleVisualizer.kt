package com.osudroid.ui.v2.mainmenu

import com.osudroid.RythimManager
import com.reco1l.framework.forEachTolerant
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.px
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.UICanvas
import java.lang.ref.WeakReference
import kotlin.math.max

class RippleVisualizer : UICanvas() {

    /**
     * The thickness of each ripple.
     */
    var rippleThickness: Dimension.Fixed = 2f.px


    private val referenceToThis = WeakReference(this)

    private var activeRipples = mutableListOf<RippleInfo>()


    override fun onAttached() {
        super.onAttached()

        RythimManager.addOnBeatChangeListener(referenceToThis) {
            if (RythimManager.isKiai) {
                spawnRipple()
            } else if (RythimManager.beatIndex == 0) {
                spawnRipple()
            }
        }
    }


    private fun spawnRipple() {
        activeRipples.add(RippleInfo(0f, 1f))
    }

    override fun onUpdate(clock: FrameClock) {
        activeRipples.forEachTolerant { info ->
            info.update(clock.elapsedFrameTime)
        }
        activeRipples.removeAll { it.alpha <= 0f }

        super.onUpdate(clock)
    }

    override fun onDraw(renderer: GraphicsRenderer) {
        val baseSize = max(bounds.width, bounds.height)
        val thickness = rippleThickness.toPixels()

        activeRipples.forEachTolerant { info ->
            val size = baseSize * (1f + info.scale)

            renderer.drawArcStroke(
                x = (bounds.width - size) / 2f,
                y = (bounds.height - size) / 2f,
                width = size,
                height = size,
                thickness = thickness,
                color = contentColor.alpha(info.alpha)
            )
        }
    }

    fun finalize() {
        RythimManager.removeOnBeatChangeListener(referenceToThis)
    }


    data class RippleInfo(
        var scale: Float,
        var alpha: Float
    ) {

        var velocity: Float = 0f


        fun update(deltaTimeSec: Float) {
            val fadeRate = if (RythimManager.isKiai) 0.5f else 0.25f
            alpha -= fadeRate * deltaTimeSec

            // The velocity is proportional to the alpha value
            velocity = max(0f, alpha * (if (RythimManager.isKiai) 1f else 0.5f))

            scale += velocity * deltaTimeSec
        }
    }

}
