package com.osudroid.ui.v2

import com.osudroid.ui.skinning.StringSkinData
import com.reco1l.verktex.graphics.Texture
import com.reco1l.verktex.ui.text.TextureText

open class SpriteFont(private val texturePrefix: StringSkinData) : TextureText(mutableMapOf<Char, Texture.Region?>().also {

    fun addChar(char: Char, textureName: String) {
        it[char] = SkinTextures[texturePrefix, textureName]
    }

    for (i in 0..9) {
        addChar('0' + i, i.toString())
    }

    addChar('.', "comma")
    addChar('%', "percent")
    addChar('x', "x")
    addChar('d', "d")
    addChar('p', "p")

})
