package com.reco1l.rimu.graphics

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable


/**
 * Creates a new [TextureRegion] based on this texture.
 */
fun Texture.toTextureRegion() = TextureRegion(this)

/**
 * Creates a new [TextureRegionDrawable] from this region.
 */
fun TextureRegion.toDrawable() = TextureRegionDrawable(this)


fun Image.setTexture(texture: Texture?)
{
    if (texture == null)
    {
        drawable = null
        return
    }

    drawable = texture.toTextureRegion().toDrawable()
}