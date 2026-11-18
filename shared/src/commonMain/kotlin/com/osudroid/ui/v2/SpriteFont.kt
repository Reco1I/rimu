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

}) {

    /**
     * Gives every digit the width of the '5', centred in it, so numbers don't shift as they change, as osu!lazer's
     * legacy sprite texts do. Other characters keep their own width, or the one [others] gives them.
     */
    fun useFixedDigitWidth(others: Map<Char, Float> = emptyMap()) {
        val digitWidth = characters['5']?.displayWidth ?: 0f

        fixedCharWidths = buildMap {
            for (c in '0'..'9') {
                put(c, digitWidth)
            }
            putAll(others)
        }
    }
}
