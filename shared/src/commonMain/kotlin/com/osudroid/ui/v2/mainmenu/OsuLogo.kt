package com.osudroid.ui.v2.mainmenu

import com.osudroid.ui.v2.SkinTextures
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.rgb
import com.reco1l.verktex.graphics.CompareFunction
import com.reco1l.verktex.graphics.DepthState
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.math.interpolateFloat
import com.reco1l.verktex.ui.fadeOut
import com.reco1l.verktex.ui.scaleTo
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.GradientBox
import com.reco1l.verktex.ui.container.ClickableContainer
import com.reco1l.verktex.ui.container.Container
import com.reco1l.verktex.ui.container.container
import com.reco1l.verktex.ui.shape.Circle
import com.reco1l.verktex.ui.shape.circle
import com.reco1l.verktex.ui.sprite

class OsuLogo(withExternalEffects: Boolean = true) : ClickableContainer() {

    /**
     * Whether to play music effects.
     */
    var playEffects = withExternalEffects
        set(value) {
            if (field != value) {
                field = value
                if (!value) stopEffects()
            }
        }


    private lateinit var inputFeedbackCircle: Circle

    private val bounceContainer: Container

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
                isOutlined = true
                lineWidth = 4f.dip

                colorPalette = arrayOf(
                    OSU_COLOR.lighten(0.1f),
                    OSU_COLOR.darken(0.1f),
                    OSU_COLOR.darken(0.125f),
                    OSU_COLOR.darken(0.15f)
                )
            }

            +GradientBox().apply {
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
        inputFeedbackCircle.fadeOut(0.4f, Easing.OutQuint)
    }

    // Stops the music effects, fading them out and settling the bounce.
    private fun stopEffects() {
        bounceContainer.scaleTo(1f, 0.4f, Easing.OutQuint)
        radialVisualizer?.fadeOut(0.4f, Easing.OutQuint)
        rippleDispenser?.fadeOut(0.4f, Easing.OutQuint)
    }

    override fun onUpdate(clock: FrameClock) {
        if (playEffects) {
            // The bounce follows the position in the beat of the music, so it's smoothed towards it: it shrinks in
            // the last quarter of the beat and grows back over the rest.
            val beatLengthSeconds = RythimManager.beatLength.toFloat() / 1000f
            val isInLastQuarter = RythimManager.beatElapsed / 1000f >= beatLengthSeconds * 0.75f
            val smoothing = beatLengthSeconds * (if (isInLastQuarter) 0.25f else 0.75f) * 5f

            bounceContainer.scale = clock.interpolateFloat(smoothing, bounceContainer.scaleX, if (isInLastQuarter) 0.9f else 1f)
        }

        super.onUpdate(clock)
    }


    companion object {

        /**
         * The official osu! pink color.
         */
        val OSU_COLOR = rgb(255, 101, 170)

    }
}
