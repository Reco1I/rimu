package com.reco1l.rimu.ui.entity

import com.reco1l.rimu.IWithContext
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.management.skin.WorkingSkin
import com.reco1l.rimu.ui.ISkinnableWithRules
import com.reco1l.rimu.ui.SkinningRules
import org.andengine.entity.IEntity


class TextureTextSkinningRules<T : TextureText> : SkinningRules<T>()
{

    /**
     * Define how the texture keys should be resolved given the character.
     */
    var textureProvider: (Char) -> String = { it.toString() }

}


fun IWithContext.TextureText(
    parent: Entity? = this as? Entity,
    init: TextureText.() -> Unit
) = TextureText(ctx).apply {
    parent?.attachChild(this)
    init()
}

open class TextureText(ctx: MainContext) :
    Entity(ctx),
    ISkinnableWithRules<TextureText, TextureTextSkinningRules<TextureText>>
{


    override val rules = TextureTextSkinningRules<TextureText>()


    var text: String = ""
        set(value)
        {
            if (field != value)
                invalidate = true

            field = value
        }

    var overlap = 0f
        set(value)
        {
            if (field != value)
                invalidate = true

            field = value
        }


    private var invalidate = false


    private fun onAllocateSprites(length: Int) = when
    {
        childCount < length ->
        {
            for (i in childCount until length)
                attachChild(Sprite(ctx))
        }

        childCount > length ->
        {
            // Temporal array to avoid concurrency.
            val toDetach = mutableListOf<IEntity>()

            for (i in length until childCount)
                toDetach.add(mChildren[i])

            toDetach.forEach { it.detachSelf() }
        }

        else -> Unit
    }


    override fun onManagedUpdate(sElapsed: Float)
    {
        val text = text

        if (childCount != text.length)
            onAllocateSprites(text.length)

        super.onManagedUpdate(sElapsed)

        if (!invalidate || childCount == 0)
            return

        // Accounting last right bound from the previous sprite so we can place each one at the right
        // side of the previous.
        var lastRight = 0f

        mChildren.forEachIndexed { index, sprite -> sprite as Sprite

            // Assigning the character according to the index.
            val key = rules.textureProvider(text[index])

            sprite.setTexture(ctx.resources[key, 0])
            sprite.setPosition(lastRight, 0f)

            lastRight += sprite.width * sprite.scaleX - overlap
        }

        invalidate = false
    }

    override fun onApplySkin(skin: WorkingSkin)
    {
        // No need to call ISkinnableWithRules super type because these skinning rules doesn't
        // override onApplySkin.
        super<Entity>.onApplySkin(skin)

        this.invalidate = true
    }
}