package com.reco1l.rimu.ui.entity

import com.reco1l.rimu.IWithContext
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.graphics.setTexture
import com.reco1l.rimu.management.skin.WorkingSkin
import com.reco1l.rimu.ui.ISkinnableWithRules
import com.reco1l.rimu.ui.SkinningRules
import com.badlogic.gdx.scenes.scene2d.ui.Image as GdxImage


data class SpriteSkinnableRules<T : Image>(

    var texture: String? = null,

    var textureVariant: Int = 0,

    var textureMutate: Boolean = false

) : SkinningRules<T>()
{
    override fun onApplySkin(target: T, skin: WorkingSkin)
    {
        texture?.also { target.setTexture(target.ctx.resources[it, textureVariant]) }
    }
}


fun IWithContext.Sprite(
    parent: Group? = this as? Group,
    init: Image.() -> Unit
) = Image(ctx).apply {
    parent?.addActor(this)
    init()
}

fun Group.Image(block: Image.() -> Unit) = Image(ctx).also {
    addActor(it)
    it.block()
}

open class Image(override val ctx: MainContext) :
    GdxImage(),
    ISkinnableWithRules<Image, SpriteSkinnableRules<Image>>,
    IWithContext
{

    override val skinningRules by lazy { SpriteSkinnableRules<Image>() }

}





