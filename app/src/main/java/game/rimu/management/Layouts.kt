package game.rimu.management

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import com.reco1l.framework.android.views.attachTo
import com.reco1l.framework.android.views.removeSelf
import com.reco1l.framework.lang.createInstance
import com.reco1l.framework.lang.instanceMapOf
import com.reco1l.framework.lang.safeIn
import game.rimu.MainContext
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

    // Storing created layouts into a map to perform auto-show/hide events.
    private val layouts = instanceMapOf<ModelLayout>()

    // Storing layers to perform scene change events.
    private val layers = LAYERS.associateWith {

        // Creating an instance for every layer and attaching it.
        it.createInstance(ctx) attachTo this
    }


    init
    {
        ctx.initializationTree!!.add(0) { onActivityCreate() }
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


    /**
     * Set scene layouts.
     */
    fun onSceneChange(scene: BaseScene) = mainThread {

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
    fun show(layout: ModelLayout): Boolean
    {
        if (layout.shouldRemainInMemory)
        {
            if (layouts[layout::class] != layout)
                throw IllegalArgumentException("There's already loaded an instance of this unique layout.")

            layouts[layout::class] = layout
        }

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

        return layout.isAttached
    }

    fun hide(layout: ModelLayout): Boolean
    {
        layout.removeSelf()

        if (!layout.shouldRemainInMemory && layouts[layout::class] == layout)
            layouts.remove(layout::class)

        return !layout.isAttached
    }


    // Management

    operator fun <T : BaseLayer> get(clazz: KClass<T>): T
    {
        @Suppress("UNCHECKED_CAST")
        return layers[clazz] as T
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <T : ModelLayout> get(clazz: KClass<T>) = layouts[clazz] as? T ?: let {

        val instance = clazz.createInstance(ctx)

        if (instance.shouldRemainInMemory)
            layouts[clazz] = instance

        instance
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

