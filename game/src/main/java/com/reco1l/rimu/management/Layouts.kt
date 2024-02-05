package com.reco1l.rimu.management

import android.animation.ValueAnimator
import android.app.Activity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import com.reco1l.toolkt.android.attachTo
import com.reco1l.toolkt.android.removeSelf
import com.reco1l.rimu.constants.Ease
import com.reco1l.toolkt.animation.animateTo
import com.reco1l.toolkt.kotlin.createInstance
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.constants.RimuSetting
import com.reco1l.rimu.mainThread
import com.reco1l.rimu.management.skin.WorkingSkin
import com.reco1l.rimu.ui.*
import com.reco1l.rimu.ui.layouts.ModelLayout
import com.reco1l.rimu.ui.scenes.BaseScene
import com.reco1l.rimu.ui.views.ConstraintLayout
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KClass


class LayoutManager(override val ctx: MainContext) : ConstraintLayout(ctx)
{

    override val dimensions = super.dimensions.apply {

        size(MATCH_PARENT)
    }

    /**
     * The UI scale.
     */
    val scale
        get() = scaleFactor * scaleRatio

    val loadedLayouts = mutableListOf<ModelLayout>()

    val loadedLayers = LAYERS.associateWith { it.createInstance(ctx) attachTo this }


    private var scaleAnimator: ValueAnimator? = null

    private var scaleRatio = 1f

    private var scaleFactor: Float = ctx.settings[RimuSetting.UI_SCALE]
        set(value)
        {
            field = value
            invalidateScale()
        }


    init
    {
        ctx.onPostInitialization {

            // Binding skin change observer to update properly all attached layouts.
            skins.bindObserver(observer = this@LayoutManager)

            // Binding scale factor observer to rescale properly all attached layouts.
            ctx.settings.bindObserver(RimuSetting.UI_SCALE) { value ->

                scaleAnimator?.cancel()
                scaleAnimator = ::scaleFactor.animateTo(value as Float, 300, ease = Ease.DECELERATE)
            }
        }
    }


    // Events

    fun onActivityCreate(activity: Activity, renderView: View)
    {
        mainThread {

            get<LayerBackground>().addView(renderView, 0)

            // If the activity was recreated this must be called first.
            if (parent != null)
                removeSelf()

            activity.setContentView(this@LayoutManager)
        }
    }

    fun onSceneChange(scene: BaseScene) = mainThread {

        // We're copying the list to avoid concurrency errors because during the iteration the original
        // list can be modified.
        loadedLayouts.toList().forEach {

            if (scene::class in it.parents)
                it.show()
            else
                it.hide()
        }
    }

    fun onSurfaceChange(width: Int, height: Int)
    {
        // Applying over a ratio multiplier based on 16:9 to make scale relative to screen aspect
        // ratio, this will make the scale to have the same factor in different screen sizes.
        scaleRatio = 1f // RATIO_FUNCTION(width, height)

        invalidateScale()
    }


    override fun onApplyScale(scale: Float)
    {
        // This may be called from a Coroutine scope so we have to move the operation to main thread.
        mainThread { super.onApplyScale(scale) }
    }

    override fun onApplySkin(skin: WorkingSkin)
    {
        // This may be called from a Coroutine scope so we have to move the operation to main thread.
        mainThread { super.onApplySkin(skin) }
    }


    // Attachment

    /**
     * Add a layout to the defined layer.
     *
     * @param overridePrevious If `true` and the layout specifies that is a [singleton][ModelLayout.isSingleton]
     * and there's already a previous instance of the same class it'll be override. If it's not a
     * singleton this will not take any effect.
     */
    fun show(layout: ModelLayout, overridePrevious: Boolean = false): Boolean
    {
        // Storing the layout in the map where all unique layouts are stored, if the previous value
        // isn't null and 'override' is false it'll throw an exception.
        if (layout.isSingleton)
        {
            loadedLayouts.filter { it::class == layout::class }.forEach {

                if (it == layout)
                    return@forEach

                if (!overridePrevious)
                    throw IllegalStateException("There's already loaded an instance of this layout.")
                else
                    it.hide()
            }
        }

        if ((layout.isSingleton || layout.shouldRemainInMemory) && layout !in loadedLayouts)
            loadedLayouts.add(layout)

        // The layer must be declared and initialized, otherwise this will throw an NPE which should
        // never happen.
        loadedLayers[layout.layer]?.also {

            // Removing from previous layer if it was changed, removeSelf() will do nothing if parent
            // is null so we don't have to check nullability here.
            if (layout.parent != it)
                layout.removeSelf()

            if (layout.parent == null)
                it.addView(layout)

        } ?: throw NullPointerException("The declared layer isn't loaded into the manager.")

        return layout.isAttachedToLayer
    }

    fun hide(layout: ModelLayout): Boolean
    {
        layout.removeSelf()

        if (!layout.shouldRemainInMemory && layout in loadedLayouts)
            loadedLayouts.remove(layout)

        return !layout.isAttachedToLayer
    }


    // Management

    inline operator fun <reified T : BaseLayer> get(clazz: KClass<T> = T::class): T
    {
        return loadedLayers[clazz] as T
    }

    inline operator fun <reified T : ModelLayout> get(clazz: KClass<T> = T::class): T
    {
        return loadedLayouts.find { it.isSingleton && it::class == clazz } as? T ?: let {

            val instance = clazz.createInstance(ctx)

            if (instance.shouldRemainInMemory)
                loadedLayouts.add(instance)

            instance
        }
    }


    companion object
    {

        /**
         * List of layer classes to be created at manager initialization.
         */
        private val LAYERS = arrayOf(LayerBackground::class, LayerScene::class, LayerOverlay::class)

        /**
         * Based on the osu!droid scale ratio function.
         * [osu!droid code snippet](https://github.com/osudroid/osu-droid/blob/522716f870701f4b3728bfb912e18dd264f8fa0c/src/ru/nsu/ccfit/zuev/osu/Config.java#L269-L272)
         */
        private val RATIO_FUNCTION = { width: Int, height: Int ->

            1280f / (1280f * (min(height, width) / max(width, height)))
        }

    }

}

