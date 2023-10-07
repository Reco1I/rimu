package game.rimu.ui

import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.core.view.forEach
import com.reco1l.framework.lang.ifNotNull
import com.reco1l.framework.lang.isLazyInit
import com.reco1l.framework.lang.isLazyInitialized
import com.reco1l.framework.graphics.Color4
import game.rimu.android.IWithContext
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

    fun IWithContext.invalidateSkin() = onApplySkin(ctx.skins.current)
}


abstract class SkinningRules<T>
{
    abstract fun onApplySkin(target: T, skin: WorkingSkin)
}

interface ISkinnableWithRules<T, D : SkinningRules<T>> : ISkinnable
{

    val skinRules: D

    @Suppress("UNCHECKED_CAST")
    override fun onApplySkin(skin: WorkingSkin)
    {
        // Preventing unnecessary initialization.
        if (!::skinRules.isLazyInit || ::skinRules.isLazyInitialized)
            skinRules.onApplySkin(this as T, skin)

        super.onApplySkin(skin)
    }
}

inline fun <T, D : SkinningRules<T>> ISkinnableWithRules<T, D>.skinRules(
    block: D.() -> Unit
) = skinRules.apply(block)



// Views

open class ViewSkinningRules<T : View> : SkinningRules<T>()
{

    var background: ((WorkingSkin) -> Drawable?)? = null

    var foreground: ((WorkingSkin) -> Drawable?)? = null


    @CallSuper
    override fun onApplySkin(target: T, skin: WorkingSkin)
    {
        background.ifNotNull { target.background = it(skin) }

        foreground.ifNotNull { target.foreground = it(skin) }
    }
}


// Drawable

open class DrawableSkinningRules<T : Drawable> : SkinningRules<T>()
{

    var color: ((WorkingSkin) -> Color4)? = null

    override fun onApplySkin(target: T, skin: WorkingSkin)
    {
        color?.invoke(skin).ifNotNull {

            target.setTint(it.toInt())
            target.alpha = it.alpha8bit
        }
    }

}