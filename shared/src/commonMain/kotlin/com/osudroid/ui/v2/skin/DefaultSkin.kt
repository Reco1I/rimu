package com.osudroid.ui.v2.skin

/**
 * The default skin, drawn in code as osu!lazer's Argon skin, see `docs/plans/default-skin.md`. It provides the
 * components it draws, which texture skins missing their textures fall back to. The others still come from the
 * default skin's textures.
 */
object DefaultSkin : GameplaySkin {

    /**
     * How many times larger than their size the texts of the pieces are rasterised, as the pieces are scaled up on
     * screen by the playfield.
     */
    const val TEXT_RESOLUTION = 2f

    override fun provides(component: SkinComponent<*>) = when (component) {
        SkinComponent.HitCircle,
        SkinComponent.SliderHeadCircle,
        SkinComponent.SliderTailCircle,
        SkinComponent.ApproachCircle,
        SkinComponent.TraceableApproachCircle,
        SkinComponent.ReverseArrow,
        SkinComponent.SliderTick,
        SkinComponent.SliderBall,
        SkinComponent.FollowCircle,
        SkinComponent.SliderBody,
        SkinComponent.Spinner,
        is SkinComponent.Judgement -> true
        SkinComponent.HitLighting -> false
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> create(component: SkinComponent<T>): T? = when (component) {
        SkinComponent.HitCircle, SkinComponent.SliderHeadCircle -> DefaultMainCirclePiece(hasNumber = true)
        SkinComponent.SliderTailCircle -> DefaultMainCirclePiece(hasNumber = false)
        SkinComponent.ApproachCircle, SkinComponent.TraceableApproachCircle -> DefaultApproachCircle()
        SkinComponent.ReverseArrow -> DefaultReverseArrow()
        SkinComponent.SliderTick -> DefaultSliderTick()
        SkinComponent.SliderBall -> DefaultSliderBallPiece()
        SkinComponent.FollowCircle -> DefaultFollowCirclePiece()
        SkinComponent.SliderBody -> DefaultSliderBodyStyle
        SkinComponent.Spinner -> DefaultSpinnerPiece()
        is SkinComponent.Judgement -> DefaultJudgementPiece(component.textureName)
        SkinComponent.HitLighting -> null
    } as T?
}

/**
 * The sizes of the textures the pieces of the default skin stand for, at 1x (an `@2x` texture's halved): those of
 * the default textures, osu!stable's sizes, which the objects scale their pieces from. A piece of the default skin
 * has the size of the texture it replaces, so it lines up with the textures of a skin that has only some of them,
 * and the objects place every skin's pieces alike. In texture pixels.
 */
object SkinDimensions {

    // Hit circles and sliders, scaled by the object's scale as their textures are.
    val HIT_CIRCLE = PieceSize(128f, 128f)
    val APPROACH_CIRCLE = PieceSize(128f, 128f)
    val REVERSE_ARROW = PieceSize(128f, 128f)
    val SLIDER_BALL = PieceSize(118f, 118f)
    val FOLLOW_CIRCLE = PieceSize(256f, 256f)
    val SLIDER_TICK = PieceSize(16f, 16f)
    val HIT_LIGHTING = PieceSize(184f, 184f)

    // Judgements, centred on the object.
    val JUDGEMENT_GREAT = PieceSize(103f, 60f)
    val JUDGEMENT_OK = PieceSize(97f, 57f)
    val JUDGEMENT_MEH = PieceSize(71f, 57f)
    val JUDGEMENT_MISS = PieceSize(65f, 65f)

    // The combo number's digits, in the hit circle font.
    const val COMBO_DIGIT_HEIGHT = 52f

    // Spinners, scaled by the spinner as its textures are.
    val SPINNER_BACKGROUND = PieceSize(757f, 512f)
    val SPINNER_METRE = PieceSize(757f, 512f)
    val SPINNER_CIRCLE = PieceSize(666f, 666f)
    val SPINNER_APPROACH_CIRCLE = PieceSize(379f, 384f)
    val SPINNER_TOP = PieceSize(667f, 667f)
    val SPINNER_BOTTOM = PieceSize(667f, 667f)
    val SPINNER_MIDDLE = PieceSize(689f, 689f)
    val SPINNER_GLOW = PieceSize(852f, 852f)
    val SPINNER_CLEAR = PieceSize(319f, 122f)
    val SPINNER_SPIN = PieceSize(343f, 119f)

    // Follow points and the cursor.
    val FOLLOW_POINT = PieceSize(16f, 22f)
    val CURSOR = PieceSize(76f, 76f)
    val CURSOR_TRAIL = PieceSize(11f, 11f)

    // The HUD: the score font's digits and the health bar.
    const val SCORE_DIGIT_HEIGHT = 46f
    val HEALTH_BAR_BACKGROUND = PieceSize(695f, 44f)
    val HEALTH_BAR_FILL = PieceSize(645f, 10f)
    val HEALTH_BAR_MARKER = PieceSize(24f, 24f)
}

/**
 * The size of a skin's piece, see [SkinDimensions].
 */
data class PieceSize(val width: Float, val height: Float)
