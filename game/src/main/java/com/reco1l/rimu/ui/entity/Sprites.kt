package com.reco1l.rimu.ui.entity

import com.reco1l.rimu.graphics.WrappingTexture
import com.reco1l.rimu.graphics.toTextureRegion
import com.reco1l.rimu.IWithContext
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.management.skin.WorkingSkin
import com.reco1l.rimu.ui.ISkinnableWithRules
import com.reco1l.rimu.ui.SkinningRules
import org.andengine.entity.Entity
import org.andengine.entity.sprite.Sprite as AndEngineSprite


data class SpriteSkinnableRules<T : Sprite>(

    var texture: String? = null,

    var textureVariant: Int = 0,

    var textureMutate: Boolean = false

) : SkinningRules<T>()
{

    override fun onApplySkin(target: T, skin: WorkingSkin)
    {
        texture?.also {
            target.setTexture(target.ctx.resources[it, textureVariant], textureMutate)
        }
    }
}


fun IWithContext.Sprite(
    parent: Entity? = this as? Entity,
    init: Sprite.() -> Unit
) = Sprite(ctx).apply {
    parent?.attachChild(this)
    init()
}

open class Sprite(override val ctx: MainContext) :
    AndEngineSprite(ctx.engine.vertexBufferObjectManager),
    ISkinnableWithRules<Sprite, SpriteSkinnableRules<Sprite>>,
    IWithContext
{

    override val skinningRules by lazy { SpriteSkinnableRules<Sprite>() }


    /**
     * Set a texture region from a wrapping texture.
     *
     * @param mutate Determines if the texture region should be mutated or not, if `false` the
     * [shared][WrappingTexture.sharedTextureRegion] instance will be take.
     */
    fun setTexture(texture: WrappingTexture?, mutate: Boolean = false)
    {
        textureRegion = when (mutate)
        {
            true -> texture?.toTextureRegion()
            else -> texture?.sharedTextureRegion
        }
    }
}





