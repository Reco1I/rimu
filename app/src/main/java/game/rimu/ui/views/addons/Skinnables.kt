package game.rimu.ui.views.addons

import android.view.ViewGroup
import androidx.core.view.forEach
import game.rimu.android.IWithContext
import game.rimu.management.skin.WorkingSkin
import org.andengine.entity.IEntity

/**
 * Indicates that a View or Entity is skinable.
 * If the View/Entity is attached into a ViewGroup/Entity, the method [onApplySkin]
 * will be automatically called when the skin is changed.
 */
interface ISkinnable
{

    fun onApplySkin(skin: WorkingSkin)
    {
        when (this)
        {
            is ViewGroup -> forEach { (it as? ISkinnable)?.onApplySkin(skin) }
            is IEntity -> callOnChildren { (it as? ISkinnable)?.onApplySkin(skin) }
        }
    }

    fun IWithContext.onApplySkin() = onApplySkin(ctx.skins.current)
}