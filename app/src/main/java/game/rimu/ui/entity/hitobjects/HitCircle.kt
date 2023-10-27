package game.rimu.ui.entity.hitobjects

import game.rimu.MainContext
import game.rimu.management.skin.WorkingSkin
import game.rimu.ui.entity.Sprite
import game.rimu.ui.entity.TextureText


open class HitCircleEntity(ctx: MainContext) : HitObjectEntity(ctx)
{

    val bodySprite = Sprite { rules.texture = "hitcircle" }

    val overlaySprite = Sprite { rules.texture = "hitcircleoverlay" }

    val numberSprite = TextureText {

        // By default: "default-#"
        rules.textureProvider = { "${ctx.skins.current.data.fonts.hitCirclePrefix}-$it" }

        text = "1"
    }

    val approachCircleSprite = Sprite { rules.texture = "approachcircle" }


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