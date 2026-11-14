package com.osudroid.ui.v2.hud.elements

import com.osudroid.ui.v2.SkinTextures
import com.osudroid.ui.v2.hud.HUDElement
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.px
import com.reco1l.verktex.graphics.BlendMode
import com.reco1l.verktex.graphics.GraphicsTexture
import com.reco1l.verktex.math.ColorInterpolation
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.math.FloatInterpolation
import com.reco1l.verktex.ui.UIAnimatedSprite
import com.reco1l.verktex.ui.UISprite
import com.reco1l.verktex.ui.clearTransforms
import com.reco1l.verktex.ui.fadeOut
import com.reco1l.verktex.ui.scaleTo
import com.reco1l.verktex.ui.container.UIClipContainer
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.scoring.*
import ru.nsu.ccfit.zuev.skins.*

class HUDHealthBar : HUDElement() {

    private val fill: UIAnimatedSprite
    private val fillClip: UIClipContainer

    private val marker: UISprite
    private val explode: UISprite

    private val markerNormalTexture: GraphicsTexture.Region?
    private val markerDangerTexture: GraphicsTexture.Region?
    private val markerSuperDangerTexture: GraphicsTexture.Region?

    private val isNewStyle: Boolean


    private val fillX: Float
    private val fillY: Float
    private val fillWidth: Float
    private val fillHeight: Float

    private var lastHP = 0f
    private var clearedWidth = 0f


    init {

        val backgroundTexture = SkinTextures["scorebar-bg"]
        val markerTexture = SkinTextures["scorebar-marker"]

        // the marker lookup to decide which display style must be performed on the source of the bg, which is the most common element.
        isNewStyle = backgroundTexture != null && markerTexture != null

        // background implementation is the same for both versions.
        +UISprite().apply { textureRegion = backgroundTexture }

        val fillFrames = SkinTextures.frames("scorebar-colour").ifEmpty { listOfNotNull(SkinTextures["scorebar-colour"]) }

        // Preserve the first frame size.
        fillWidth = fillFrames.firstOrNull()?.width ?: 0f
        fillHeight = fillFrames.firstOrNull()?.height ?: 0f

        fill = UIAnimatedSprite(fillFrames, OsuSkin.get().animationFramerate)
        fill.width = fillWidth.px
        fill.height = fillHeight.px

        // Shows the part of the fill up to the health, from the left.
        fillClip = UIClipContainer()
        fillClip.clipAnchor = Anchor.TopLeft
        fillClip.width = fillWidth.px
        fillClip.height = fillHeight.px
        fillClip += fill
        +fillClip

        marker = UISprite()
        marker.origin = Anchor.Center
        marker.scaleOrigin = Anchor.Center
        marker.blendMode = BlendMode.Additive
        +marker

        explode = UISprite()
        explode.origin = Anchor.Center
        explode.scaleOrigin = Anchor.Center
        explode.blendMode = BlendMode.Additive
        explode.alpha = 0f
        +explode

        if (isNewStyle) {
            fillX = 7.5f * 1.6f
            fillY = 7.8f * 1.6f

            marker.textureRegion = markerTexture
            explode.textureRegion = markerTexture

            markerNormalTexture = null
            markerDangerTexture = null
            markerSuperDangerTexture = null
        } else {
            fillX = 3f * 1.6f
            fillY = 10f * 1.6f

            markerNormalTexture = SkinTextures["scorebar-ki"]
            markerDangerTexture = SkinTextures["scorebar-kidanger"]
            markerSuperDangerTexture = SkinTextures["scorebar-kidanger2"]

            marker.textureRegion = markerNormalTexture
            explode.textureRegion = markerNormalTexture
        }

        fillClip.x = fillX.px
        fillClip.y = fillY.px
    }


    override fun onGameplayUpdate(game: GameScene, secondsElapsed: Float) {
        val hp = game.stat.hp

        clearedWidth = FloatInterpolation.floatAt(secondsElapsed.coerceIn(0f, 0.2f), clearedWidth, (1f - hp) * fillWidth, 0f, 0.2f, Easing.OutQuint)
        fillClip.clipFactorX = if (fillWidth > 0f) 1f - clearedWidth / fillWidth else 1f

        marker.x = (fillX + fillWidth - clearedWidth).px
        marker.y = (fillY + (if (isNewStyle) fillHeight / 2 else 0f)).px

        explode.x = marker.x
        explode.y = marker.y

        if (hp > lastHP) {
            bulge()
        }

        lastHP = hp

        if (isNewStyle) {

            val color = getFillColor(hp)

            fill.color = color
            marker.color = color
            marker.blendMode = if (hp < EPIC_CUTOFF) BlendMode.Alpha else BlendMode.Additive

        } else {

            marker.textureRegion = when {
                hp < 0.2f -> markerSuperDangerTexture
                hp < EPIC_CUTOFF -> markerDangerTexture
                else -> markerNormalTexture
            }

        }
    }

    override fun onNoteHit(statistics: StatisticV2) {
        flash(statistics.hp)
    }

    override fun onBreakStateChange(isBreak: Boolean) {
        isVisible = !isBreak
    }


    private fun flash(hp: Float) {

        val isEpic = hp >= EPIC_CUTOFF

        bulge()

        explode.clearTransforms()
        explode.blendMode = if (isEpic) BlendMode.Additive else BlendMode.Alpha
        explode.alpha = 1f
        explode.scale = 1f

        explode.scaleTo(if (isEpic) 2f else 1.6f, 0.12f)
        explode.fadeOut(0.12f)
    }

    private fun bulge() {
        marker.clearTransforms()
        marker.scale = 1.4f
        marker.scaleTo(1f, 0.2f, Easing.OutQuad)
    }

    private fun getFillColor(hp: Float) = when {

        hp < 0.2f -> ColorInterpolation.colorAt(0.2f - hp, Color4.Black, Color4(0xFFEF4444), 0f, 0.2f)
        hp < EPIC_CUTOFF -> ColorInterpolation.colorAt(0.5f - hp, Color4.White, Color4.Black, 0f, 0.5f)

        else -> Color4.White
    }

    companion object {

        const val EPIC_CUTOFF = 0.5f

    }

}