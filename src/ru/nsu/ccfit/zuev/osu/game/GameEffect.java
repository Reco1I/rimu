package ru.nsu.ccfit.zuev.osu.game;

import com.osudroid.math.Vector2;


import com.osudroid.utils.Execution;
import com.reco1l.verktex.data.Anchor;
import com.reco1l.verktex.ui.container.UIContainer;
import com.reco1l.verktex.ui.ChoreographyKt;
import com.reco1l.verktex.ui.TransformsKt;
import com.osudroid.ui.v2.SkinTextures;
import com.reco1l.verktex.graphics.BlendMode;
import com.reco1l.verktex.ui.ChoreographyBlock;
import com.reco1l.verktex.ui.UIAnimatedSprite;
import com.reco1l.verktex.ui.UISprite;
import com.reco1l.verktex.data.Color4;

import java.util.Arrays;
import java.util.HashSet;

import javax.annotation.Nullable;

import ru.nsu.ccfit.zuev.skins.OsuSkin;

public class GameEffect extends GameObject {
    private static final HashSet<String> animationEffects = new HashSet<>(Arrays.asList(
            "hit0", "hit50", "hit100", "hit100k", "hit300", "hit300k", "hit300g"
    ));

    UISprite hit;
    String texname;

    public GameEffect(final String texname) {
        this.texname = texname;

        if (isAnimationEffect(texname) && (SkinTextures.get(texname + "-0") != null)) {
            var hit = new UIAnimatedSprite(SkinTextures.frames(texname), OsuSkin.get().getAnimationFramerate());
            hit.setLoop(false);
            this.hit = hit;
        } else {
            hit = new UISprite();
            hit.setTextureRegion(SkinTextures.get(texname));
        }
    }

    private static boolean isAnimationEffect(String textureName) {
        return animationEffects.contains(textureName);
    }

    public void setColor(@Nullable Color4 color) {
        if (color == null) {
            hit.setColor(1f, 1f, 1f);
        } else {
            hit.setColor(color);
        }
    }

    public final void init(final UIContainer scene, final Vector2 pos, final float scale,
                           final ChoreographyBlock... sequenceConsumers) {
        if (hit instanceof UIAnimatedSprite animatedHit) {
            animatedHit.stop();
            animatedHit.play();
        }
        hit.setPosition(pos.x, pos.y);
        hit.setOrigin(Anchor.Center);
        hit.setScale(scale);
        hit.setAlpha(1);
        hit.removeSelf();
        hit.setBlendMode(BlendMode.Alpha.INSTANCE);
        scene.plusAssign(hit);

        hit.clearAnimations();

        for (int i = 0; i < sequenceConsumers.length; ++i) {
            ChoreographyKt.choreographNow(hit, sequenceConsumers[i]);
        }

        // Fired once every sequence has finished.
        ChoreographyKt.choreographAt(hit, ChoreographyKt.choreography(hit).getEndTime(), sequence -> sequence.after(e -> Execution.updateThread(() -> {
            hit.removeSelf();
            hit.clearAnimations();
            GameObjectPool.getInstance().putEffect(GameEffect.this);
        })));
    }

    public void setBlendMode(BlendMode blendMode) {
        hit.setBlendMode(blendMode);
    }

    public String getTexname() {
        return texname;
    }

    @Override
    public void update(final float dt) {
    }

}
