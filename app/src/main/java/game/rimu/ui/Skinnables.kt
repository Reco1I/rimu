package game.rimu.ui

import android.graphics.Color.TRANSPARENT
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.core.view.forEach
import com.reco1l.framework.android.views.setForegroundColor
import com.reco1l.framework.lang.isLazyInit
import com.reco1l.framework.lang.isLazyInitialized
import game.rimu.android.IWithContext
import game.rimu.android.RimuContext
import game.rimu.management.skin.WorkingSkin
import org.andengine.entity.IEntity

/**
 * Indicates that a View or Entity is skinnable.
 *
 * For [ViewGroup] or [IEntity] types the method [onApplySkin] will be called recursively between children.
 */
interface ISkinnable
{

    fun onApplySkin(skin: WorkingSkin)
    {
        if (this is View)
        {
            (background as? ISkinnable)?.onApplySkin(skin)
            (foreground as? ISkinnable)?.onApplySkin(skin)

            if (this is ViewGroup)
                forEach { (it as? ISkinnable)?.onApplySkin(skin) }

            return
        }

        if (this is IEntity)
            callOnChildren { (it as? ISkinnable)?.onApplySkin(skin) }
    }

    /**
     * Calls [onApplySkin] with the context skin, if there's a [RimuContext] implementation you
     * don't need to pass the context.
     */
    fun invalidateSkin(ctx: RimuContext? = (this as? IWithContext)?.ctx)
    {
        ctx?.also { onApplySkin(it.skins.current) }
    }
}


abstract class SkinningRules<T>
{
    abstract fun onApplySkin(target: T, skin: WorkingSkin)
}

interface ISkinnableWithRules<T, D : SkinningRules<T>> : ISkinnable
{

    val skinningRules: D

    @Suppress("UNCHECKED_CAST")
    override fun onApplySkin(skin: WorkingSkin)
    {
        // Preventing unnecessary initialization.
        if (!::skinningRules.isLazyInit || ::skinningRules.isLazyInitialized)
            skinningRules.onApplySkin(this as T, skin)

        super.onApplySkin(skin)
    }
}

inline fun <T, D : SkinningRules<T>> ISkinnableWithRules<T, D>.skinningRules(
    block: D.() -> Unit
) = skinningRules.apply(block)



// Views

/**
 * Defines the rules that the view should follow when the skin is changed.
 */
open class ViewSkinningRules<T : View> : SkinningRules<T>()
{

    /**
     * Define the drawable that should be set as background.
     */
    var background: Pair<String, Int>? = null

    /**
     * Define the background color that should be set.
     *
     * Note: This overrides the value set in [background] property.
     */
    var backgroundColor: Pair<String, Float>? = null

    /**
     * Define the drawable that should be set as foreground.
     */
    var foreground: Pair<String, Int>? = null

    /**
     * Define the foreground color that should be set.
     *
     * Note: This overrides the value set in [foreground] property.
     */
    var foregroundColor: Pair<String, Float>? = null


    @CallSuper
    override fun onApplySkin(target: T, skin: WorkingSkin)
    {

        backgroundColor?.also { (key, factor) ->

            target.setBackgroundColor(skin.data.colours.map[key]?.factorInt(factor) ?: TRANSPARENT)

        } ?: background?.also { (key, variant) ->

            target.background = skin.ctx.resources[key, variant]
        }


        foregroundColor?.also { (key, factor) ->

            target.setForegroundColor(skin.data.colours.map[key]?.factorInt(factor) ?: TRANSPARENT)

        } ?: foreground?.also { (key, variant) ->

            target.background = skin.ctx.resources[key, variant]
        }
    }
}