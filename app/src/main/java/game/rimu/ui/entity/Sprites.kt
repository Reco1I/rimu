package game.rimu.ui.entity

import com.reco1l.framework.kotlin.isLazyInitialized
import com.reco1l.framework.support.WrappingTexture
import com.reco1l.framework.support.toTextureRegion
import game.rimu.IWithContext
import game.rimu.MainContext
import game.rimu.management.skin.WorkingSkin
import game.rimu.ui.ISkinnableWithRules
import game.rimu.ui.SkinningRules
import org.andengine.entity.Entity
import org.andengine.opengl.texture.region.ITextureRegion
import org.andengine.entity.sprite.Sprite as AndEngineSprite


data class SpriteSkinnableRules<T : Sprite>(

    var texture: String? = null,

    var textureVariant: Int = 0,

    var textureMutate: Boolean = false

) : SkinningRules<T>()


fun IWithContext.Sprite(
    parent: Entity? = this as? Entity,
    init: Sprite.() -> Unit
) = Sprite(ctx).apply {
    parent?.attachChild(this)
    init()
}

open class Sprite(override val ctx: MainContext) :
    AndEngineSprite(0f, 0f, null),
    ISkinnableWithRules<Sprite>,
    IWithContext
{

    override val rules by lazy {
        ignoreRules = false
        SpriteSkinnableRules<Sprite>()
    }

    /**
     * Set the texture region from a [WrappingTexture], by default it will take the shared
     * texture region instance of the wrapping texture.
     */
    var texture: WrappingTexture?
        get() = textureRegion?.texture as? WrappingTexture
        set(value)
        {
            textureRegion = value?.sharedTextureRegion
            invalidateTexture = true
        }


    private var ignoreRules = true

    private var invalidateTexture = false


    /**
     * Update the texture, should be called if the texture key/variant defined in the skinning rules
     * has been changed.
     */
    fun invalidateTexture()
    {
        invalidateTexture = true
    }


    override fun onManagedUpdate(sElapsed: Float)
    {
        // Ignoring invalidation from skinning rules if it wasn't used.
        if (invalidateTexture && !ignoreRules)
        {
            val texture: WrappingTexture? = rules.texture?.let { ctx.resources[it, rules.textureVariant] }

            textureRegion = when (rules.textureMutate)
            {
                true -> texture?.toTextureRegion()
                false -> texture?.sharedTextureRegion
            }

            invalidateTexture = false
        }
        super.onManagedUpdate(sElapsed)
    }


    override fun onAttached() = invalidateTexture()

    override fun onApplySkin(skin: WorkingSkin) = invalidateTexture()
}





