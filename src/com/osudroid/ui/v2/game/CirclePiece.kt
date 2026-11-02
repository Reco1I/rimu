package com.osudroid.ui.v2.game

import com.osudroid.ui.v2.SkinTextures

import com.osudroid.ui.v2.SpriteFont
import com.reco1l.verktex.ui.UISprite
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.framework.*
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Anchor
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
        it.textureRegion = SkinTextures[circleTexture]

        +it
    }

    private val overlay = UISprite().also {

        it.origin = Anchor.Center
        it.anchor = Anchor.Center
        it.textureRegion = SkinTextures[overlayTexture]

        +it
    }


    fun setCircleColor(red: Float, green: Float, blue: Float) {
        circle.color = Color4(red, green, blue)
    }

    fun setCircleColor(color: Color4) {
        circle.color = color
    }

    fun setCircleTextureRegion(circleTexture: String) {
        circle.textureRegion = SkinTextures[circleTexture]
    }

    fun setOverlayTextureRegion(overlayTexture: String) {
        overlay.textureRegion = SkinTextures[overlayTexture]
    }
}

class NumberedCirclePiece(circleTexture: String, overlayTexture: String) : CirclePiece(circleTexture, overlayTexture) {


    private val number = SpriteFont(OsuSkin.get().hitCirclePrefix).also {

        it.origin = Anchor.Center
        it.anchor = Anchor.Center
        it.spacing = -OsuSkin.get().hitCircleOverlap

        +it
    }


    fun setNumberText(value: Int) {
        number.text = value.toString()
    }

    fun setNumberScale(value: Float) {
        number.textureScaleX = value
        number.textureScaleY = value
    }

    fun showNumber() {
        number.alpha = 1f
    }

    fun hideNumber() {
        number.alpha = 0f
    }

}