package com.osudroid.ui.v2.hud.elements

import ru.nsu.ccfit.zuev.osu.game.GameScene
import com.reco1l.verktex.data.Background
import com.reco1l.verktex.data.CornerRadius
import com.reco1l.verktex.data.px
import com.reco1l.verktex.graphics.CompareFunction
import com.reco1l.verktex.graphics.DepthState
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.time.FrameClock

import com.reco1l.verktex.ui.shape.UIBox
import com.reco1l.verktex.data.Color4
import com.osudroid.utils.*
import com.osudroid.beatmaps.constants.HitObjectType
import com.reco1l.verktex.data.Anchor
import kotlin.math.abs

class HUDBarHitErrorMeter(game: GameScene) : HUDHitErrorMeter(game) {

    private val expiredIndicators = SynchronizedPool<Indicator>(20)
    private val activeIndicators = mutableListOf<Indicator>()

    init {
        width = (WIDTH).px
        height = (INDICATOR_HEIGHT).px
        val mehWindow = UIBox().apply {
            anchor = Anchor.Center
            origin = Anchor.Center
            background = Background.Color(mehColor)
            radius = CornerRadius((BAR_HEIGHT / 2).px)
            width = (WIDTH).px
            height = (BAR_HEIGHT).px
            depthState = DepthState(enabled = true, writeEnabled = true, function = CompareFunction.Less)
        }

        val okWindow = UIBox().apply {
            anchor = Anchor.Center
            origin = Anchor.Center
            background = Background.Color(okColor)
            width = (WIDTH * (hitWindow.okWindow / hitWindow.mehWindow).toFloat()).px
            height = (BAR_HEIGHT).px
            depthState = DepthState(enabled = true, writeEnabled = true, function = CompareFunction.Less)
        }

        val greatWindow = UIBox().apply {
            anchor = Anchor.Center
            origin = Anchor.Center
            width = (WIDTH * (hitWindow.greatWindow / hitWindow.mehWindow).toFloat()).px
            height = (BAR_HEIGHT).px
            background = Background.Color(greatColor)

            clearDepthBeforeDraw = true
            depthState = DepthState(enabled = true, writeEnabled = true, function = CompareFunction.Less)
        }

        +mehWindow
        add(0, okWindow)
        add(0, greatWindow)

        // Indicator
        +UIBox().apply {
            anchor = Anchor.Center
            origin = Anchor.Center
            width = (INDICATOR_WIDTH / 2).px
            height = (INDICATOR_HEIGHT).px
            background = Background.Color(Color4.White)
        }

        mehWindow.alpha = 0.6f
        okWindow.alpha = 0.6f
        greatWindow.alpha = 0.6f
    }

    override fun addResult(type: HitObjectType, accuracy: Float, color: Color4) {
        if (type == HitObjectType.Spinner) {
            return
        }

        val accuracyMs = accuracy * 1000

        if (abs(accuracyMs) > hitWindow.mehWindow) {
            return
        }

        val indicator = expiredIndicators.acquire() ?: Indicator(0f, 0f, Color4.White)

        indicator.x = (WIDTH / 2f) * (accuracyMs / hitWindow.mehWindow.toFloat())
        indicator.alpha = 1f
        indicator.color = color

        activeIndicators.add(indicator)
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
    }

    //region Indicator update & draw

    override fun onDraw(renderer: GraphicsRenderer) {
        super.onDraw(renderer)

        val centerX = bounds.width / 2f
        val centerY = bounds.height / 2f
        val halfHeight = (INDICATOR_HEIGHT - 2f) / 2f

        activeIndicators.forEach {
            val x = centerX + it.x

            renderer.drawLine(x, centerY - halfHeight, x, centerY + halfHeight, INDICATOR_WIDTH, it.color.alpha(it.alpha))
        }
    }

    override fun onUpdate(clock: FrameClock) {
        activeIndicators.forEach(Indicator::update)
        super.onUpdate(clock)
    }

    //endregion


    private inner class Indicator(var x: Float, var alpha: Float, var color: Color4) : IPoolable {
        override var isRecycled = false

        fun update() {
            if (alpha > 0f) {
                alpha -= 0.005f
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

        private const val WIDTH = 400f
        private const val BAR_HEIGHT = 10f
        private const val INDICATOR_HEIGHT = BAR_HEIGHT + 14f
        private const val INDICATOR_WIDTH = 4f

    }
}