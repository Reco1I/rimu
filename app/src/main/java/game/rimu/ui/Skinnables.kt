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
import game.rimu.management.resources.AssetID
import game.rimu.management.resources.ColorID
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

interface ISkinnableWithRules<T> : ISkinnable
{

    val skinningRules: SkinningRules<T>

    @Suppress("UNCHECKED_CAST")
    override fun onApplySkin(skin: WorkingSkin)
    {
        // Preventing unnecessary initialization.
        if (!::skinningRules.isLazyInit || ::skinningRules.isLazyInitialized)
            skinningRules.onApplySkin(this as T, skin)

        super.onApplySkin(skin)
    }
}


// Views

/**
 * Defines the rules that the view should follow when the skin is changed.
 */
open class ViewSkinningRules<T : View> : SkinningRules<T>()
{

    /**
     * Define the drawable that should be set as background.
     */
    var background: AssetID? = null

    /**
     * Define the background color that should be set.
     *
     * Note: This overrides the value set in [background] property.
     */
    var backgroundColor: ColorID? = null

    /**
     * Define the drawable that should be set as foreground.
     */
    var foreground: AssetID? = null

    /**
     * Define the foreground color that should be set.
     *
     * Note: This overrides the value set in [foreground] property.
     */
    var foregroundColor: ColorID? = null


    @CallSuper
    override fun onApplySkin(target: T, skin: WorkingSkin)
    {
        backgroundColor?.also { target.setBackgroundColor(it[skin.ctx] ?: TRANSPARENT) }
            ?:
            background?.also { target.background = it[skin.ctx] }

        foregroundColor?.also { target.setForegroundColor(it[skin.ctx] ?: TRANSPARENT) }
            ?:
            foreground?.also { target.background = it[skin.ctx] }
    }
}