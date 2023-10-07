package game.rimu.management

import android.view.ViewGroup
import androidx.core.view.contains
import com.reco1l.framework.android.views.attachTo
import com.reco1l.framework.lang.instanceMapOf
import com.reco1l.framework.lang.createInstance
import game.rimu.android.RimuContext
import game.rimu.ui.LayerBackground
import game.rimu.ui.LayerOverlay
import game.rimu.ui.LayerScene
import game.rimu.ui.LayoutLayer
import game.rimu.ui.layouts.AttachableLayout
import game.rimu.ui.scenes.RimuScene
import game.rimu.ui.views.ConstraintLayout
import kotlin.reflect.KClass


class LayoutManager(override val ctx: RimuContext) : ConstraintLayout(ctx)
{

    // Storing created layouts into a map to perform auto-show/hide events.
    private val layouts = instanceMapOf<AttachableLayout>()

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

        val layer = layers[LayerScene::class]
            ?: throw NullPointerException("Unable to find layer in the map")

        // When the scene is changed all other layouts in the SCENE layer should be removed.
        layer.removeAllViews()
        layer.addView(scene.layout)

        // Adding external layouts
        layouts.values.forEach {

            // At this point the parents array shouldn't be null
            if (scene::class in it.parents!!)
            {
                if (!it.isAttachedToWindow)
                    it.show()
            }
            else if (it.isAttachedToWindow)
                it.hide()
        }
    }


    // Management

    fun <T : AttachableLayout> load(clazz: KClass<T>): T
    {
        return clazz.createInstance(ctx).apply {

            if (parents.isNullOrEmpty())
                throw IllegalArgumentException("Layout $clazz isn't intended to remain loaded, consider instantiate it without this method.")

            layouts[clazz] = this
        }
    }


    // Attachment

    /**
     * Add a layout to the defined layer.
     */
    fun show(layout: AttachableLayout): Boolean
    {
        // If the layout has parents defined means we need to restore it to later automatically show.
        if (!layout.parents.isNullOrEmpty())
            layouts[layout::class] = layout

        // Finding layer, usually this shouldn't throw NPE.
        val desiredLayer = layers[layout.layer]
            ?: throw NullPointerException("Unable to find layer in the map")

        // If the layout is already attached to another layer we change it.
        if (!desiredLayer.contains(layout) && layout.parent != null)
        {
            // Finding the current parent
            val currentLayer = layout.parent as? ViewGroup
                ?: throw NullPointerException("Parent isn't a ViewGroup type")

            currentLayer.removeView(layout)
        }

        // Adding only if it hasn't been added yet
        if (layout.parent == null)
            desiredLayer.addView(layout)

        return layout.isAttachedToWindow
    }


    // Getters

    @Suppress("UNCHECKED_CAST")
    operator fun <T : LayoutLayer> get(clazz: KClass<T>) = layers[clazz] as T

    @Suppress("UNCHECKED_CAST")
    operator fun <T : AttachableLayout> get(clazz: KClass<T>) = layouts[clazz] as T


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

