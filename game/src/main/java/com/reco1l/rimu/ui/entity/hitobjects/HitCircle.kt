package com.reco1l.rimu.ui.entity.hitobjects

import com.reco1l.rimu.MainContext
import com.reco1l.rimu.management.skin.WorkingSkin
import com.reco1l.rimu.ui.entity.Sprite
import com.reco1l.rimu.ui.entity.TextureText


open class HitCircleEntity(ctx: MainContext) : HitObjectEntity(ctx)
{

    val bodySprite = Sprite { skinningRules.texture = "hitcircle" }

    val overlaySprite = Sprite { skinningRules.texture = "hitcircleoverlay" }

    val numberSprite = TextureText {

        skinningRules.textureProvider = {

            // Prepending the skin prefix for the font.
            "${ctx.skins.current?.run { data.fonts.hitCirclePrefix } ?: "default"}-$it"
        }

        text = "1"
    }

    val approachCircleSprite = Sprite { skinningRules.texture = "approachcircle" }


    override fun onApplySkin(skin: WorkingSkin)
    {
        super.onApplySkin(skin)

        // If true the number sprite will be placed as index 1 which is below the hit circle overlay otherwise 2 which
        // is the last index.
        val above = skin.data.general.hitCircleOverlayAboveNumber

        numberSprite.zIndex = if (above) 1 else 2
        overlaySprite.zIndex = if (above) 2 else 1
    }
}