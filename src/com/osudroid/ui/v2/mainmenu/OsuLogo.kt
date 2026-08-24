package com.osudroid.ui.v2.mainmenu

import android.opengl.GLES10
import com.osudroid.RythimManager
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.ui.circle
import com.reco1l.verktex.component.ClearInfo
import com.reco1l.verktex.component.DepthInfo
import com.reco1l.verktex.ui.container
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.PaintStyle
import com.reco1l.verktex.ui.shape.UICircle
import com.reco1l.verktex.ui.UIGradientBox
import com.reco1l.verktex.ui.sprite
import com.reco1l.verktex.theme.Colors
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.ui.container.UIClickableContainer
import com.reco1l.verktex.math.FloatInterpolation
import com.reco1l.verktex.data.rgb
import ru.nsu.ccfit.zuev.osu.ResourceManager

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
                clearInfo = ClearInfo.ClearDepthBuffer
                depthInfo = DepthInfo(test = true, mask = true, function = GLES10.GL_ALWAYS)
            }

            +TrianglesDispenser().apply {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                triangle.apply {
                    depthInfo = DepthInfo(test = true, mask = true, function = GLES10.GL_EQUAL)
                    paintStyle = PaintStyle.Outline
                }
                style = {
                    triangle.lineWidth = 0.25f.rem
                }

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
                colorStart = Colors.Transparent
                colorEnd = Colors.Black
                gradientAngle = 90f
                anchor = Anchor.Center
                origin = Anchor.Center
                alpha = 0.175f
                depthInfo = DepthInfo(test = true, mask = true, function = GLES10.GL_EQUAL)
            }

            sprite {
                textureRegion = ResourceManager.getInstance().getTexture("logo")
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                anchor = Anchor.Center
                origin = Anchor.Center

                setScale(1.075f)
            }

            inputFeedbackCircle = circle {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                color = Colors.White
                alpha = 0f
            }
        }

    }

    fun playClickEffect() {
        inputFeedbackCircle.alpha = 0.9f
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {

        if (playEffects) {
            val beatLengthSeconds = RythimManager.beatLength.toFloat() / 1000f

            if (RythimManager.beatElapsed / 1000f < beatLengthSeconds * 0.75f) {
                val threeQuarts = beatLengthSeconds * 0.75f
                bounceContainer.setScale(FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, threeQuarts), bounceContainer.scaleX, 1f, 0f, threeQuarts))
            } else {
                val oneQuart = beatLengthSeconds * 0.25f
                bounceContainer.setScale(FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, oneQuart), bounceContainer.scaleX, 0.9f, 0f, oneQuart))
            }
        } else {
            bounceContainer.setScale(FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.1f), bounceContainer.scaleX, 1f, 0f, 0.1f))
            radialVisualizer?.alpha = FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.1f), radialVisualizer?.alpha ?: 0f, 0f, 0f, 0.1f)
            rippleDispenser?.alpha = FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.1f), rippleDispenser?.alpha ?: 0f, 0f, 0f, 0.1f)
        }

        inputFeedbackCircle.alpha = FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.1f), inputFeedbackCircle.alpha, 0f, 0f, 0.1f)

        super.onManagedUpdate(deltaTimeSec)
    }


    companion object {

        /**
         * The official osu! pink color.
         */
        val OSU_COLOR = rgb(255, 101, 170)

    }
}