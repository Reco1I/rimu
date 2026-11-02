package com.osudroid.ui.v2

import com.osudroid.ui.skinning.StringSkinData
import com.reco1l.verktex.graphics.GraphicsTexture
import com.reco1l.verktex.ui.text.UITextureText

open class SpriteFont(private val texturePrefix: StringSkinData) : UITextureText(mutableMapOf<Char, GraphicsTexture.Region?>().also {

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
