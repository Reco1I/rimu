package com.osudroid.ui.v2.skin.legacy

import com.osudroid.ui.v2.SkinTextures
import com.osudroid.ui.v2.skin.GameplaySkin
import com.osudroid.ui.v2.skin.SkinComponent
import com.osudroid.ui.v2.skinTexture
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.ui.Component
import com.reco1l.verktex.ui.Sprite
import com.reco1l.verktex.ui.choreographAt

/**
 * The pieces of a texture skin (osu!stable's skins), from the custom skin's and the beatmap skin's textures. It
 * [provides] a component when one of those has the component's textures, and creates the others from the default
 * skin's textures.
 */
object LegacySkin : GameplaySkin {

    override fun provides(component: SkinComponent<*>): Boolean = when (component) {
        SkinComponent.HitCircle -> isSkinned("hitcircle") || isSkinned("hitcircleoverlay")
        SkinComponent.SliderHeadCircle -> isSkinned("sliderstartcircle") || provides(SkinComponent.HitCircle)
        SkinComponent.SliderTailCircle -> isSkinned("sliderendcircle") || provides(SkinComponent.HitCircle)
        SkinComponent.ApproachCircle -> isSkinned("approachcircle")
        // Traceable always shows the default skin's.
        SkinComponent.TraceableApproachCircle -> false
        SkinComponent.ReverseArrow -> isSkinned("reversearrow")
        SkinComponent.SliderTick -> isSkinned("sliderscorepoint")
        SkinComponent.SliderBall -> isAnimationSkinned("sliderb")
        SkinComponent.FollowCircle -> isAnimationSkinned("sliderfollowcircle")
        // The body is drawn with the skin's settings, which any skin of its own has.
        SkinComponent.SliderBody -> !SkinTextures.isDefaultSkin()
        SkinComponent.Spinner -> SPINNER_TEXTURES.any { isSkinned(it) }
        SkinComponent.HitLighting -> isSkinned("lighting")
        is SkinComponent.Judgement -> isAnimationSkinned(component.textureName)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> create(component: SkinComponent<T>): T? = when (component) {
        SkinComponent.HitCircle -> LegacyMainCirclePiece("hitcircle", "hitcircleoverlay", hasNumber = true)
        SkinComponent.SliderHeadCircle -> LegacyMainCirclePiece(
            "sliderstartcircle", "sliderstartcircleoverlay", hasNumber = true,
            endCircleTexture = "sliderendcircle", endOverlayTexture = "sliderendcircleoverlay"
        )
        SkinComponent.SliderTailCircle -> LegacyMainCirclePiece("sliderendcircle", "sliderendcircleoverlay", hasNumber = false)
        SkinComponent.ApproachCircle -> sprite("approachcircle")
        SkinComponent.TraceableApproachCircle -> sprite("defaultapproachcircle")
        SkinComponent.ReverseArrow -> sprite("reversearrow")
        SkinComponent.SliderTick -> sprite("sliderscorepoint")
        SkinComponent.SliderBall -> LegacySliderBallPiece()
        SkinComponent.FollowCircle -> LegacyFollowCirclePiece()
        SkinComponent.SliderBody -> LegacySliderBodyStyle
        SkinComponent.Spinner -> if (SkinTextures.isNewStyleSpinner()) LegacyModernSpinnerPiece() else LegacyClassicSpinnerPiece()
        SkinComponent.HitLighting -> if (SkinTextures["lighting"] != null) HitLightingPiece() else null
        is SkinComponent.Judgement -> LegacyJudgementPiece(component.textureName)
    } as T?

    /**
     * Bursts [piece] from [time] on its clock, at the object's [scale]: it grows and fades out, as osu!lazer's
     * `LegacyMainCirclePiece` does. The circles of texture skins and their reverse arrows are hit this way.
     *
     * @return The time it ends at.
     */
    fun burst(piece: Component, scale: Float, time: Float): Float {
        // Reference: https://github.com/ppy/osu/blob/c5893f245ce7a89d1900dbb620390823702481fe/osu.Game.Rulesets.Osu/Skinning/Legacy/LegacyMainCirclePiece.cs#L152-L174
        val fadeDuration = 0.24f
        var endTime = time

        piece.choreographAt(time) { sequence ->
            sequence.scaleTo(scale * 1.4f, fadeDuration, Easing.OutQuad).fadeOut(fadeDuration)
            endTime = sequence.endTime
        }
        return endTime
    }

    // The textures of either style of spinner.
    private val SPINNER_TEXTURES = listOf(
        "spinner-background", "spinner-circle", "spinner-metre",
        "spinner-top", "spinner-bottom", "spinner-middle", "spinner-glow",
    )

    private fun sprite(texture: String) = Sprite().apply {
        origin = Anchor.Center
        this.texture = skinTexture(texture)
    }

    private fun isSkinned(name: String) = SkinTextures.isSkinned(name)

    // An animation's frames are `name-0`… or `name0`…
    private fun isAnimationSkinned(name: String) =
        isSkinned(name) || isSkinned("$name-0") || isSkinned("${name}0")
}
