package com.osudroid.ui.v2.mainmenu

import com.osudroid.RythimManager
import com.osudroid.ui.v2.SkinTextures
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.rgb
import com.reco1l.verktex.graphics.CompareFunction
import com.reco1l.verktex.graphics.DepthState
import com.reco1l.verktex.math.FloatInterpolation
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.PaintStyle
import com.reco1l.verktex.ui.UIGradientBox
import com.reco1l.verktex.ui.container.UIClickableContainer
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.container.container
import com.reco1l.verktex.ui.shape.UICircle
import com.reco1l.verktex.ui.shape.circle
import com.reco1l.verktex.ui.sprite

class OsuLogo(withExternalEffects: Boolean = true) : UIClickableContainer() {

    /**
     * Whether to play music effects.
     */
    var playEffects = withExternalEffects


    private lateinit var inputFeedbackCircle: UICircle

    private val bounceContainer: UIContainer

    private var radialVisualizer: RadialVisualizer? = null
    private var rippleDispenser: RippleVisualizer? = null


    init {
        if (withExternalEffects) {
            +RippleVisualizer().apply {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                anchor = Anchor.Center
                origin = Anchor.Center
                alpha = 0.3f

                rippleDispenser = this
            }
        }

        bounceContainer = container {
            width = Dimension.FillAvailable
            height = Dimension.FillAvailable
            anchor = Anchor.Center
            origin = Anchor.Center

            if (withExternalEffects) {
                +RadialVisualizer().apply {
                    width = Dimension.FillAvailable
                    height = Dimension.FillAvailable
                    anchor = Anchor.Center
                    origin = Anchor.Center
                    alpha = 0.4f

                    radialVisualizer = this
                }
            }

            circle {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                anchor = Anchor.Center
                origin = Anchor.Center
                color = OSU_COLOR
                clearDepthBeforeDraw = true
                depthState = DepthState(enabled = true, writeEnabled = true, function = CompareFunction.Always)
            }

            +TrianglesDispenser().apply {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                depthState = DepthState(enabled = true, writeEnabled = true, function = CompareFunction.Equal)
                paintStyle = PaintStyle.Outline
                lineWidth = 4f.dip

                colorPalette = arrayOf(
                    OSU_COLOR.lighten(0.1f),
                    OSU_COLOR.darken(0.1f),
                    OSU_COLOR.darken(0.125f),
                    OSU_COLOR.darken(0.15f)
                )
            }

            +UIGradientBox().apply {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                colorStart = Color4.Transparent
                colorEnd = Color4.Black
                gradientAngle = 90f
                anchor = Anchor.Center
                origin = Anchor.Center
                alpha = 0.175f
                depthState = DepthState(enabled = true, writeEnabled = true, function = CompareFunction.Equal)
            }

            sprite {
                textureRegion = SkinTextures["logo"]
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                anchor = Anchor.Center
                origin = Anchor.Center
                scale = 1.075f
            }

            inputFeedbackCircle = circle {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                color = Color4.White
                alpha = 0f
            }
        }

    }

    fun playClickEffect() {
        inputFeedbackCircle.alpha = 0.9f
    }

    override fun onUpdate(clock: FrameClock) {
        val deltaTimeSec = clock.elapsedFrameTime

        if (playEffects) {
            val beatLengthSeconds = RythimManager.beatLength.toFloat() / 1000f

            if (RythimManager.beatElapsed / 1000f < beatLengthSeconds * 0.75f) {
                val threeQuarts = beatLengthSeconds * 0.75f
                bounceContainer.scale = FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, threeQuarts), bounceContainer.scaleX, 1f, 0f, threeQuarts)
            } else {
                val oneQuart = beatLengthSeconds * 0.25f
                bounceContainer.scale = FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, oneQuart), bounceContainer.scaleX, 0.9f, 0f, oneQuart)
            }
        } else {
            bounceContainer.scale = FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.1f), bounceContainer.scaleX, 1f, 0f, 0.1f)
            radialVisualizer?.alpha = FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.1f), radialVisualizer?.alpha ?: 0f, 0f, 0f, 0.1f)
            rippleDispenser?.alpha = FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.1f), rippleDispenser?.alpha ?: 0f, 0f, 0f, 0.1f)
        }

        inputFeedbackCircle.alpha = FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.1f), inputFeedbackCircle.alpha, 0f, 0f, 0.1f)

        super.onUpdate(clock)
    }


    companion object {

        /**
         * The official osu! pink color.
         */
        val OSU_COLOR = rgb(255, 101, 170)

    }
}
