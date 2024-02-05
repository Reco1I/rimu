package com.reco1l.rimu.ui

import android.view.View
import android.view.ViewGroup
import androidx.core.view.forEach
import com.badlogic.gdx.scenes.scene2d.Group
import com.reco1l.toolkt.kotlin.isLazyInitialized
import com.reco1l.rimu.IWithContext
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.management.skin.WorkingSkin

/**
 * Indicates that a View or Entity is skinnable.
 *
 * For [ViewGroup] or [IEntity] types the method [onApplySkin] will be called recursively between children.
 */
interface ISkinnable
{

    fun onApplySkin(skin: WorkingSkin)
    {
        when (this)
        {
            is View -> {

                (background as? ISkinnable)?.onApplySkin(skin)
                (foreground as? ISkinnable)?.onApplySkin(skin)

                if (this is ViewGroup)
                    forEach { (it as? ISkinnable)?.onApplySkin(skin) }

                return
            }

            is Group -> children.forEach { (it as? ISkinnable)?.onApplySkin(skin) }
        }
    }

    /**
     * Calls [onApplySkin] with the context skin, if there's a [MainContext] implementation you
     * don't need to pass the context.
     */
    fun invalidateSkin(ctx: MainContext = (this as IWithContext).ctx)
    {
        // Preventing from race condition when skin isn't loaded yet.
        ctx.skins.current?.also { onApplySkin(it) }
    }
}

/**
 * Indicates that this View or Entity is skinnable with custom rules.
 * @see SkinningRules
 */
interface ISkinnableWithRules<T : Any, D : SkinningRules<T>> : ISkinnable
{

    /**
     * The view skinning rules, every rule will be applied once [onApplySkin] is called.
     */
    val skinningRules: D


    @Suppress("UNCHECKED_CAST")
    override fun onApplySkin(skin: WorkingSkin)
    {
        // Preventing unnecessary initialization.
        if (::skinningRules.isLazyInitialized)
            skinningRules.onApplySkin(this as T, skin)

        super.onApplySkin(skin)
    }


    /**
     * Change skinning rules parameters, in order to apply the new values you should call [invalidateSkin].
     */
    fun setSkinning(block: D.() -> Unit) = skinningRules.apply(block)
}

/**
 * The skinning rules to be used, defines how the View or Entity has to behave skin changes.
 * @see ISkinnableWithRules
 */
abstract class SkinningRules<T>
{
    abstract fun onApplySkin(target: T, skin: WorkingSkin)
}