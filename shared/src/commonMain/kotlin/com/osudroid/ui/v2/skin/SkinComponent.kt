package com.osudroid.ui.v2.skin

import com.reco1l.verktex.ui.Component

/**
 * What a skin draws, as osu!lazer's `OsuSkinComponents`: a hit object asks [SkinPieces] for a piece of one of these,
 * typed by [T], the contract the piece fulfils.
 */
sealed class SkinComponent<T : Any> {

    /**
     * The circle of a hit circle, with its number.
     */
    data object HitCircle : SkinComponent<MainCirclePiece>()

    /**
     * The circle at the head of a slider, with its number.
     */
    data object SliderHeadCircle : SkinComponent<MainCirclePiece>()

    /**
     * The circle at the end of a slider, without a number.
     */
    data object SliderTailCircle : SkinComponent<MainCirclePiece>()

    /**
     * The approach circle of a hit circle or slider head, tinted by the object with the combo colour.
     */
    data object ApproachCircle : SkinComponent<Component>()

    /**
     * The approach circle shown with Traceable, always the default skin's.
     */
    data object TraceableApproachCircle : SkinComponent<Component>()

    /**
     * The arrow on a slider's end that is repeated.
     */
    data object ReverseArrow : SkinComponent<Component>()

    /**
     * The look of a slider tick, which the tick itself animates.
     */
    data object SliderTick : SkinComponent<Component>()

    /**
     * The ball following a slider's path.
     */
    data object SliderBall : SkinComponent<SliderBallPiece>()

    /**
     * The circle around the ball while it's tracked.
     */
    data object FollowCircle : SkinComponent<FollowCirclePiece>()

    /**
     * How a slider's body is drawn.
     */
    data object SliderBody : SkinComponent<SliderBodyStyle>()

    /**
     * The look of a spinner.
     */
    data object Spinner : SkinComponent<SpinnerPiece>()

    /**
     * A point between two objects.
     */
    data object FollowPoint : SkinComponent<FollowPointPiece>()

    /**
     * A cursor.
     */
    data object Cursor : SkinComponent<CursorPiece>()

    /**
     * The trail a cursor leaves.
     */
    data object CursorTrail : SkinComponent<CursorTrailStyle>()

    /**
     * The numbers of the HUD but the combo: the score, the accuracy, the pp and the statistics.
     */
    data object ScoreFont : SkinComponent<NumberText>()

    /**
     * The combo of the HUD.
     */
    data object ComboFont : SkinComponent<NumberText>()

    /**
     * The health bar of the HUD.
     */
    data object HealthBar : SkinComponent<HealthBarPiece>()

    /**
     * The light behind a judgement. Only texture skins have it.
     */
    data object HitLighting : SkinComponent<HitLightingPiece>()

    /**
     * The judgement shown with the texture [textureName] (`hit300`, `hit0`, `sliderpoint30`…).
     */
    data class Judgement(val textureName: String) : SkinComponent<JudgementPiece>()
}
