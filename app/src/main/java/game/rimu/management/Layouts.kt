package game.rimu.management

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import com.reco1l.framework.android.views.attachTo
import com.reco1l.framework.android.views.removeSelf
import com.reco1l.framework.kotlin.createInstance
import com.reco1l.framework.kotlin.safeIn
import game.rimu.MainContext
import game.rimu.management.skin.WorkingSkin
import game.rimu.ui.LayerBackground
import game.rimu.ui.LayerOverlay
import game.rimu.ui.LayerScene
import game.rimu.ui.BaseLayer
import game.rimu.ui.layouts.ModelLayout
import game.rimu.ui.scenes.BaseScene
import game.rimu.ui.views.ConstraintLayout
import kotlin.reflect.KClass


class LayoutManager(override val ctx: MainContext) : ConstraintLayout(ctx)
{

    override val dimensions = super.dimensions.apply {

        size(MATCH_PARENT)
    }


    private val loadedLayouts = mutableListOf<ModelLayout>()

    private val layers = LAYERS.associateWith {

        // Creating an instance for every layer and attaching it.
        it.createInstance(ctx) attachTo this
    }


    init
    {
        ctx.initializationTree!!.add {

            onActivityCreate()
            skins.bindObserver(observer = this@LayoutManager)
        }
    }


    // Events

    fun onActivityCreate()
    {
        mainThread {

            // If the activity was recreated this must be called first.
            if (parent != null)
                removeSelf()

            ctx.activity.setContentView(this@LayoutManager)
        }
    }

    fun onSceneChange(scene: BaseScene) = mainThread {

        // We copying the list to avoid concurrency errors because during the iteration the original
        // list can be modified.
        loadedLayouts.toList().forEach {

            if (scene::class safeIn it.parents)
                it.show()
            else
                it.hide()
        }
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
        layers[layout.layer]?.also {

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

    operator fun <T : BaseLayer> get(clazz: KClass<T>): T
    {
        @Suppress("UNCHECKED_CAST")
        return layers[clazz] as T
    }

    operator fun <T : ModelLayout> get(clazz: KClass<T>): T
    {
        @Suppress("UNCHECKED_CAST")
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
         * List of [BaseLayer] inheritors, unfortunately this can't be achieved with reflection.
         */
        val LAYERS = arrayOf(
            LayerBackground::class,
            LayerScene::class,
            LayerOverlay::class,
        )

    }
}

