package com.reco1l.rimu.ui.entity

import com.badlogic.gdx.scenes.scene2d.Actor
import com.reco1l.rimu.IWithContext
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.graphics.setTexture
import com.reco1l.rimu.management.skin.WorkingSkin
import com.reco1l.rimu.ui.ISkinnableWithRules
import com.reco1l.rimu.ui.SkinningRules


class TextureTextSkinningRules<T : TextureText> : SkinningRules<T>()
{

    /**
     * Prefix of the texture to be used.
     */
    var texturePrefix: String = ""

    override fun onApplySkin(target: T, skin: WorkingSkin) = target.invalidateText()
}


fun IWithContext.TextureText(
    parent: Group? = this as? Group,
    init: TextureText.() -> Unit
) = TextureText(ctx).apply {
    parent?.addActor(this)
    init()
}

open class TextureText(ctx: MainContext) :
    Group(ctx),
    ISkinnableWithRules<TextureText, TextureTextSkinningRules<TextureText>>
{


    override val skinningRules = TextureTextSkinningRules<TextureText>()

    /**
     * Text to be displayed.
     */
    var text: String = ""
        set(value)
        {
            if (field != value)
                invalidateText()

            field = value
        }

    /**
     * Overlap between each character.
     */
    var overlap = 0f
        set(value)
        {
            if (field != value)
                invalidateText()

            field = value
        }

    /**
     * Scale of each character.
     */
    var charScale: Float = 1f
        set(value)
        {
            if (field != value)
                invalidateText()

            field = value
        }


    private fun onAllocateSprites(length: Int) = when
    {
        children.size < length ->
        {
            for (i in children.size until length)
                addActor(Image(ctx))
        }

        children.size > length ->
        {
            // Temporal array to avoid concurrency.
            val toDetach = mutableListOf<Actor>()

            for (i in length until children.size)
                toDetach.add(children[i])

            toDetach.forEach { it.remove() }
        }

        else -> Unit
    }


    fun invalidateText()
    {
        val text = text

        if (children.size != text.length)
            onAllocateSprites(text.length)

        if (children.size == 0)
            return

        // Accounting last right bound from the previous sprite so we can place each one at the right
        // side of the previous.
        var lastRight = 0f

        children.forEachIndexed { index, sprite -> sprite as Image

            // Assigning the character according to the index.
            val key = skinningRules.texturePrefix + text[index]

            sprite.setTexture(ctx.resources[key, 0])
            sprite.setPosition(lastRight, 0f)
            sprite.setScale(charScale)

            lastRight += sprite.width * scaleX - overlap
        }

        onMeasureSize()
    }
}