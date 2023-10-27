@file:Suppress("DEPRECATION")

package game.rimu.engine.surface

import android.view.View.MeasureSpec
import android.view.WindowManager
import com.reco1l.framework.android.getSystemService
import com.reco1l.framework.animation.Ease
import com.reco1l.framework.animation.FloatAnimator
import com.reco1l.framework.IObservable
import com.reco1l.framework.forEachObserver
import game.rimu.IWithContext
import game.rimu.MainContext
import game.rimu.constants.RimuSetting.UI_SCALE_FACTOR
import game.rimu.management.Setting
import game.rimu.ui.IScalable
import org.andengine.engine.options.resolutionpolicy.IResolutionPolicy
import org.andengine.engine.options.resolutionpolicy.IResolutionPolicy.Callback

class EngineSurface(override val ctx: MainContext) :
    IObservable<IScalable>,
    IResolutionPolicy,
    IWithContext
{

    override val observers = mutableListOf<IScalable>()


    private val display = ctx.getSystemService<WindowManager>().defaultDisplay!!


    /**
     * The final scale factor, this is a composition of [ratio] times [factor].
     */
    val scale
        get() = ratio * factor


    /**
     * The scale factor set by user.
     */
    var factor by Setting<Float>(UI_SCALE_FACTOR)

    /**
     * The app window width.
     */
    var width = display.width
        private set

    /**
     * The app window height.
     */
    var height = display.height
        private set

    /**
     * The scaling ratio, see [RatioFunction.LEGACY].
     */
    var ratio: Float = 1f
        private set


    private val scaleAnimator = FloatAnimator().apply {

        duration = 300
        interpolator = Ease.DECELERATE

        addUpdateListener { updateObservers(it.animatedValue as Float) }
    }


    init
    {
        ctx.settings.bindObserver(UI_SCALE_FACTOR) {

            val oldScale = scale
            factor = it as Float

            scaleAnimator.setFloatValues(oldScale, scale)
            scaleAnimator.start()
        }

        ctx.initializationTree!!.add { updateObservers(scale) }
    }


    private fun updateObservers(scale: Float) = forEachObserver { it.onApplyScale(scale) }


    override fun onMeasure(callback: Callback, rawWidth: Int, rawHeight: Int)
    {

        fun Int.size() = MeasureSpec.getSize(this)

        ratio = RatioFunction.LEGACY(rawWidth.size(), rawHeight.size())

        // Comparing desired ratio with real screen ratio so we can apply properly the ratio to
        // the measured dimensions.
        when (width / height < ratio)
        {
            true -> height = (width / ratio).toInt()

            else -> width = (height * ratio).toInt()
        }

        callback.onResolutionChanged(width, height)
        ctx.engine.camera.onResolutionChanged(width, height)

        updateObservers(scale)
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
        val LEGACY = { width: Int, height: Int -> 1280f / (1280f * height / width) }
    }
}