package ru.nsu.ccfit.zuev.osu.game;

import com.osudroid.math.Vector2;

import androidx.annotation.Nullable;

import com.osudroid.ui.v2.SkinTextures;
import com.reco1l.verktex.data.Color4;
import com.reco1l.verktex.graphics.BlendFactor;
import com.reco1l.verktex.graphics.BlendMode;
import com.reco1l.verktex.math.Easing;
import com.reco1l.verktex.ui.UIAnimatedSprite;
import com.reco1l.verktex.ui.container.UIContainer;

import kotlin.random.Random;
import ru.nsu.ccfit.zuev.osu.Config;

/**
 * The effects shown when hit objects are judged: the judgement sprites (300, 100, miss...), the hit
 * lighting and the bursts of hit circles and slider ends.
 */
public class GameplayEffects {

    private UIContainer background;
    private UIContainer foreground;
    private float scale = 1;


    /**
     * Sets the containers the effects are added to: hit lighting goes into {@code background}, below the
     * hit objects, and everything else into {@code foreground}.
     */
    public void setLayers(UIContainer background, UIContainer foreground) {
        this.background = background;
        this.foreground = foreground;
    }

    /**
     * Sets the scale of the hit objects, effects are scaled the same.
     */
    public void setScale(float scale) {
        this.scale = scale;
    }


    /**
     * Shows the judgement sprite with the given texture name, e.g. {@code hit300} or {@code hit0}.
     */
    public void hit(final Vector2 pos, final String name, @Nullable Color4 color) {

        var effect = GameObjectPool.getInstance().getEffect(name);
        var isAnimated = effect.hit instanceof UIAnimatedSprite animatedHit && animatedHit.getFrames().size() > 1;

        // Reference https://github.com/ppy/osu/blob/ebf637bd3c33f1c886f6bfc81aa9ea2132c9e0d2/osu.Game/Skinning/LegacyJudgementPieceOld.cs

        float fadeInLength = 0.12f;
        float fadeOutLength = 0.6f;
        float fadeOutDelay = 0.5f;

        if (name.equals("hit0")) {
            var rotation = (float) Random.Default.nextDouble(8.6 * 2) - 8.6f;

            if (isAnimated) {
                // Legacy judgements don't play any transforms if they are an animation.
                effect.init(
                    foreground,
                    pos,
                    scale,
                    sequence -> sequence
                            .fadeIn(fadeInLength)
                            .then(fadeOutDelay)
                            .fadeOut(fadeOutLength)
                );
            } else {
                effect.init(
                    foreground,
                    pos,
                    scale * 1.6f,
                    sequence -> sequence
                            .fadeIn(fadeInLength)
                            .then(fadeOutDelay)
                            .fadeOut(fadeOutLength),
                    sequence -> sequence
                            .scaleTo(scale, 0.1f, Easing.InQuad)
                            .moveToY(-5)
                            .moveToY(80, fadeOutDelay + fadeOutLength, Easing.InQuad),
                    sequence -> sequence
                            .rotateTo(0)
                            .rotateTo(rotation, fadeInLength)
                            .then()
                            .rotateTo(rotation * 2, fadeOutDelay + fadeOutLength - fadeInLength, Easing.InQuad)
                );
            }

            return;
        }

        if (Config.isHitLighting() && !name.equals("sliderpoint10") && !name.equals("sliderpoint30") && SkinTextures.get("lighting") != null) {

            // Reference https://github.com/ppy/osu/blob/a7e110f6693beca6f6e6a20efb69a6913d58550e/osu.Game.Rulesets.Osu/Objects/Drawables/DrawableOsuJudgement.cs#L71-L88

            var light = GameObjectPool.getInstance().getEffect("lighting");
            light.setBlendMode(new BlendMode.Custom(BlendFactor.SrcAlpha, BlendFactor.DstAlpha, BlendFactor.SrcAlpha, BlendFactor.DstAlpha));
            light.setColor(color);
            light.init(
                background,
                pos,
                scale * 0.8f,
                sequence -> sequence.scaleTo(scale * 1.2f, 0.6f, Easing.OutQuad),
                sequence -> sequence.fadeIn(0.2f).then().fadeOut(1f)
            );
        }

        // Legacy judgements don't play any transforms if they are an animation.
        if (isAnimated) {
            effect.init(
                foreground,
                pos,
                scale,
                sequence -> sequence.fadeIn(fadeInLength).then(fadeOutDelay).fadeOut(fadeOutLength)
            );
        } else {
            effect.init(
                foreground,
                pos,
                scale * 0.6f,
                sequence -> sequence.fadeIn(fadeInLength).then(fadeOutDelay).fadeOut(fadeOutLength),
                sequence -> sequence
                        .scaleTo(scale * 1.1f, fadeInLength * 0.8f) // t = 0.8
                        .then(fadeInLength * 0.2f) // t = 1.0
                        .scaleTo(scale * 0.9f, fadeInLength * 0.2f) // t = 1.2
                        .then()

                        // stable dictates scale of 0.9->1 over time 1.0 to 1.4, but we are already at 1.2.
                        // so we need to force the current value to be correct at 1.2 (0.95) then complete the
                        // second half of the transform.
                        .scaleTo(scale * 0.95f).scaleTo(scale, fadeInLength * 0.2f) // t = 1.4
            );
        }
    }

    /**
     * The burst of a hit circle.
     */
    public void burst(final Vector2 pos, final Color4 color) {
        burst(pos, color, "hitcircle", "hitcircleoverlay");
    }

    /**
     * The burst of the head of a slider.
     */
    public void burstSliderStart(final Vector2 pos, final Color4 color) {
        burst(pos, color, "sliderstartcircle", "sliderstartcircleoverlay");
    }

    /**
     * The burst of the end of a slider.
     */
    public void burstSliderEnd(final Vector2 pos, final Color4 color) {
        burst(pos, color, "sliderendcircle", "sliderendcircleoverlay");
    }

    /**
     * The burst of a reverse arrow of a slider.
     */
    public void burstSliderReverse(final Vector2 pos, float ang, final Color4 color) {
        if (!areBurstsShown()) {
            return;
        }

        final GameEffect burst = GameObjectPool.getInstance().getEffect("reversearrow");
        burst.hit.setRotationZ(ang);
        applyBurst(burst, pos);
    }


    private void burst(final Vector2 pos, final Color4 color, String circleTexture, String overlayTexture) {
        if (!areBurstsShown()) {
            return;
        }

        final GameEffect circle = GameObjectPool.getInstance().getEffect(circleTexture);
        applyBurst(circle, pos);
        circle.setColor(color);

        final GameEffect overlay = GameObjectPool.getInstance().getEffect(overlayTexture);
        applyBurst(overlay, pos);
    }

    private boolean areBurstsShown() {
        return Config.isBurstEffects() &&
                (GameHelper.getHidden() == null || GameHelper.getHidden().isOnlyFadeApproachCircles()) &&
                !GameHelper.isTraceable();
    }

    private void applyBurst(GameEffect effect, Vector2 pos) {

        // Reference: https://github.com/ppy/osu/blob/c5893f245ce7a89d1900dbb620390823702481fe/osu.Game.Rulesets.Osu/Skinning/Legacy/LegacyMainCirclePiece.cs#L152-L174

        var fadeDuration = 0.24f;

        effect.init(foreground, pos, scale,
                sequence -> sequence
                        .scaleTo(scale * 1.4f, fadeDuration, Easing.OutQuad)
                        .fadeOut(fadeDuration)
        );
    }
}
