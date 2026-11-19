package com.osudroid.ui.v2.skin.legacy

import com.osudroid.ui.skinning.StringSkinData
import com.osudroid.ui.v2.SpriteFont
import com.osudroid.ui.v2.skin.NumberText
import com.reco1l.verktex.graphics.BlendMode
import com.reco1l.verktex.ui.text.TextAlign

/**
 * A number in a texture skin's font, the textures starting with [prefix] (`score-0`, `score-comma`…).
 */
class LegacyNumberText(prefix: StringSkinData) : NumberText() {

    private val font = SpriteFont(prefix)

    init {
        +font
    }

    override var text: String
        get() = font.text.toString()
        set(value) {
            font.text = value
        }

    override var spacing: Float
        get() = font.spacing
        set(value) {
            font.spacing = value
        }

    override var measureText: String?
        get() = font.measureText
        set(value) {
            font.measureText = value
        }

    override var textAlign: TextAlign
        get() = font.textAlign
        set(value) {
            font.textAlign = value
        }

    override var isAdditive: Boolean
        get() = font.blendMode == BlendMode.Additive
        set(value) {
            font.blendMode = if (value) BlendMode.Additive else BlendMode.Alpha
        }

    override fun useFixedDigitWidth() = font.useFixedDigitWidth()
}
