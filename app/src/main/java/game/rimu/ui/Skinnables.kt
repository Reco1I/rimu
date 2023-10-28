package game.rimu.ui

import android.view.View
import android.view.ViewGroup
import androidx.core.view.forEach
import com.reco1l.framework.kotlin.isLazyInit
import com.reco1l.framework.kotlin.isLazyInitialized
import game.rimu.IWithContext
import game.rimu.MainContext
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
     * Calls [onApplySkin] with the context skin, if there's a [MainContext] implementation you
     * don't need to pass the context.
     */
    fun invalidateSkin(ctx: MainContext = (this as IWithContext).ctx)
    {
        // Preventing from race condition when skin isn't loaded yet.
        if (ctx.skins.isInitialized)
            onApplySkin(ctx.skins.current)
    }
}


open class SkinningRules<T>
{
    open fun onApplySkin(target: T, skin: WorkingSkin) = Unit
}

interface ISkinnableWithRules<T> : ISkinnable
{

    val rules: SkinningRules<T>

    @Suppress("UNCHECKED_CAST")
    override fun onApplySkin(skin: WorkingSkin)
    {
        // Preventing unnecessary initialization.
        if (!::rules.isLazyInit || ::rules.isLazyInitialized)
            rules.onApplySkin(this as T, skin)

        super.onApplySkin(skin)
    }
}