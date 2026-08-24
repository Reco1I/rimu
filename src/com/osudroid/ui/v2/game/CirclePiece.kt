package com.osudroid.ui.v2.game

import com.reco1l.verktex.*
import com.reco1l.verktex.ui.container.*
import com.reco1l.verktex.ui.sprite.*
import com.osudroid.ui.v2.SpriteFont
import com.reco1l.verktex.ui.UISprite
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.framework.*
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.texture.Textures
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.skins.*

open class CirclePiece(

    circleTexture: String,
    overlayTexture: String

) : UIContainer() {


    init {
        origin = Anchor.Center
    }


    private val circle = UISprite().also {

        it.origin = Anchor.Center
        it.anchor = Anchor.Center
        it.textureRegion = Textures.getInstance().getTexture(circleTexture)

        attachChild(it)
    }

    private val overlay = UISprite().also {

        it.origin = Anchor.Center
        it.anchor = Anchor.Center
        it.textureRegion = Textures.getInstance().getTexture(overlayTexture)

        attachChild(it)
    }


    fun setCircleColor(red: Float, green: Float, blue: Float) {
        circle.setColor(red, green, blue)
    }

    fun setCircleColor(color: Color4) {
        circle.color = color
    }

    fun setCircleTextureRegion(circleTexture: String) {
        circle.textureRegion = Textures.getInstance().getTexture(circleTexture)
    }

    fun setOverlayTextureRegion(overlayTexture: String) {
        overlay.textureRegion = Textures.getInstance().getTexture(overlayTexture)
    }
}

class NumberedCirclePiece(circleTexture: String, overlayTexture: String) : CirclePiece(circleTexture, overlayTexture) {


    private val number = SpriteFont(OsuSkin.get().hitCirclePrefix).also {

        it.origin = Anchor.Center
        it.anchor = Anchor.Center
        it.spacing = -OsuSkin.get().hitCircleOverlap

        attachChild(it)
    }


    fun setNumberText(value: Int) {
        number.text = value.toString()
    }

    fun setNumberScale(value: Float) {
        number.setTextureScale(value)
    }

    fun showNumber() {
        number.alpha = 1f
    }

    fun hideNumber() {
        number.alpha = 0f
    }

}