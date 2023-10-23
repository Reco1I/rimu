@file:Suppress("DEPRECATION")

package game.rimu.engine.surface

import android.animation.ValueAnimator
import android.view.View.MeasureSpec
import android.view.WindowManager
import com.reco1l.framework.android.getSystemService
import com.reco1l.framework.animation.Ease
import game.rimu.android.IWithContext
import game.rimu.android.RimuContext
import game.rimu.constants.RimuSetting.UI_SCALE
import org.andengine.engine.options.resolutionpolicy.IResolutionPolicy
import org.andengine.engine.options.resolutionpolicy.IResolutionPolicy.Callback

class EngineSurface(override val ctx: RimuContext) :
    IResolutionPolicy,
    IWithContext
{

    private val display = ctx.getSystemService<WindowManager>().defaultDisplay!!


    /**
     * The final scale factor, this is a composition of [ratio] times [factor].
     */
    val scale
        get() = ratio * factor

    /**
     * The scale factor set by user.
     */
    var factor = 1f
        private set

    /**
     * The app window width.
     */
    var width = display.width.toFloat()
        private set

    /**
     * The app window height.
     */
    var height = display.height.toFloat()
        private set

    /**
     * The scaling ratio, see [RatioFunction.LEGACY].
     */
    var ratio: Float = 1f
        private set


    private val scaleAnimator = ValueAnimator.ofFloat().apply {

        duration = 300
        interpolator = Ease.DECELERATE

        addUpdateListener { ctx.layouts.onApplyScale(it.animatedValue as Float) }
    }


    init
    {
        ctx.settings.bindObserver(UI_SCALE) {

            val oldScale = scale
            factor = it as Float

            scaleAnimator.setFloatValues(oldScale, scale)
            scaleAnimator.start()

        }

        ctx.initializationTree!!.add {

            ctx.layouts.onApplyScale(scale)
        }
    }


    override fun onMeasure(callback: Callback, rawWidth: Int, rawHeight: Int)
    {
        var width = MeasureSpec.getSize(rawWidth).toFloat()
        var height = MeasureSpec.getSize(rawHeight).toFloat()

        ratio = RatioFunction.LEGACY(width, height)

        // Comparing desired ratio with real screen ratio so we can apply properly the ratio to
        // the measured dimensions.
        if (width / height < ratio)
            height = width / ratio
        else
            width = height * ratio

        this.width = width
        this.height = height

        callback.onResolutionChanged(width.toInt(), height.toInt())

        // Setting dimensions to the engine main camera to handle resolution changes.
        ctx.engine.camera.onMeasureSurface(width, height)
    }


    object RatioFunction
    {
        /**
         * The legacy resolution ratio function.
         *
         * Note: This is not the exact same as the osu!droid one, this one accounts for aspect
         * ratio instead of force 16:9 like the original function.
         *
         * [osu!droid code snippet](https://github.com/osudroid/osu-droid/blob/522716f870701f4b3728bfb912e18dd264f8fa0c/src/ru/nsu/ccfit/zuev/osu/Config.java#L269-L272)
         */
        val LEGACY = { width: Float, height: Float -> 1280f / (1280f * height / width) }
    }
}