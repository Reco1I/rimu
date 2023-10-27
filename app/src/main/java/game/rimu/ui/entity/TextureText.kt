package game.rimu.ui.entity

import game.rimu.IWithContext
import game.rimu.MainContext
import game.rimu.management.skin.WorkingSkin
import game.rimu.ui.ISkinnableWithRules
import game.rimu.ui.SkinningRules
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
    ISkinnableWithRules<TextureText>
{


    override val rules = TextureTextSkinningRules<TextureText>()


    var text: String = ""
        set(value)
        {
            if (field != value)
                invalidateText = true

            field = value
        }

    var overlap = 0f
        set(value)
        {
            if (field != value)
                invalidatePositions = true

            field = value
        }

    var charScale = 1f
        set(value)
        {
            if (field != value)
                invalidatePositions = true

            field = value
        }


    private var invalidateText = false

    private var invalidatePositions = false


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

        if (childCount == 0)
            return

        // Updating text only if necessary, we're delegating texture change to the sprite's
        // onManagedUpdate method where the texture is changed.
        if (invalidateText && ctx.skins.isInitialized)
        {
            mChildren.forEachIndexed { index, sprite ->

                sprite as Sprite

                // Assigning the character according to the index.
                val key = rules.textureProvider(text[index])

                // Avoiding unnecessary invalidation in cases where the character is the same.
                if (key != sprite.rules.texture)
                {
                    sprite.rules.texture = key
                    sprite.invalidateTexture()
                }
            }
            invalidateText = false

            // New textures may differ size from previous one so we must update positions too.
            invalidatePositions = true
        }

        super.onManagedUpdate(sElapsed)

        if (!invalidatePositions)
            return

        // Accounting last right bound from the previous sprite so we can place each one at the right
        // side of the previous.
        var lastRight = 0f

        mChildren.forEachIndexed { index, sprite ->

            sprite as Sprite
            sprite.setScale(charScale)
            sprite.setPosition(lastRight, 0f)

            lastRight += sprite.width * sprite.scaleX - overlap
        }

        onMeasureSize()
        invalidatePositions = false
    }

    override fun onApplySkin(skin: WorkingSkin)
    {
        // No need to call ISkinnableWithRules super type because these skinning rules doesn't
        // override onApplySkin.
        super<Entity>.onApplySkin(skin)

        invalidateText = true
    }
}