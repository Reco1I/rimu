package game.rimu.ui

import android.view.View
import android.view.ViewGroup
import androidx.core.view.forEach
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