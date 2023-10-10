package game.rimu.management

import androidx.core.view.contains
import com.reco1l.framework.android.views.attachTo
import com.reco1l.framework.android.views.removeSelf
import com.reco1l.framework.lang.createInstance
import com.reco1l.framework.lang.instanceMapOf
import com.reco1l.framework.lang.safeIn
import game.rimu.android.RimuContext
import game.rimu.ui.LayerBackground
import game.rimu.ui.LayerOverlay
import game.rimu.ui.LayerScene
import game.rimu.ui.LayoutLayer
import game.rimu.ui.layouts.RimuLayout
import game.rimu.ui.scenes.RimuScene
import game.rimu.ui.views.ConstraintLayout
import kotlin.reflect.KClass


class LayoutManager(override val ctx: RimuContext) : ConstraintLayout(ctx)
{

    // Storing created layouts into a map to perform auto-show/hide events.
    private val layouts = instanceMapOf<RimuLayout>()

    // Storing layers to perform scene change events.
    private val layers = LAYERS.associateWith {

        // Creating an instance for every layer and attaching it.
        it.createInstance(ctx) attachTo this
    }


    init
    {
        ctx.initializationTree!!.add(0) {

            // Setting as content view
            activity.setContentView(this@LayoutManager)
        }
    }


    // Events

    /**
     * Set scene layouts.
     */
    fun onSceneChange(scene: RimuScene) = mainThread {

        // Removing layouts that doesn't correspond to new scene
        layers.values.forEach { it.onSceneChange(scene) }

        // Showing already loaded layouts that correspond to the new scene
        layouts.values.forEach {

            if (scene::class safeIn it.parents && !it.isAttachedToWindow)
                it.show()
        }
    }


    // Attachment

    /**
     * Add a layout to the defined layer.
     */
    fun show(layout: RimuLayout): Boolean
    {
        if (layout.shouldRemainInMemory)
        {
            if (layouts[layout::class] != layout)
                throw IllegalArgumentException("There's already loaded an instance of this unique layout.")

            layouts[layout::class] = layout
        }

        // The layer must be declared and initialized, otherwise this will throw an NPE which should
        // never happen.
        layers[layout.layer]!!.also {

            // Removing from previous layer if it was changed, removeSelf() will do nothing if parent
            // is null so we don't have to check nullability here.
            if (layout.parent != it)
                layout.removeSelf()

            if (layout.parent == null)
                it.addView(layout)
        }

        return layout.isAttachedToWindow
    }

    fun hide(layout: RimuLayout)
    {
        layout.removeSelf()

        if (!layout.shouldRemainInMemory && layouts[layout::class] == layout)
            layouts.remove(layout::class)
    }


    // Management

    operator fun <T : LayoutLayer> get(clazz: KClass<T>): T
    {
        @Suppress("UNCHECKED_CAST")
        return layers[clazz] as T
    }

    operator fun <T : RimuLayout> get(clazz: KClass<T>) = layouts[clazz] ?: let {

        val instance = clazz.createInstance(ctx)

        if (instance.shouldRemainInMemory)
            layouts[clazz] = instance

        instance
    }


    companion object
    {

        /**
         * List of [LayoutLayer] inheritors, unfortunately this can't be achieved with reflection.
         */
        val LAYERS = arrayOf(
            LayerBackground::class,
            LayerScene::class,
            LayerOverlay::class,
        )

    }
}

