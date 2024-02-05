package com.reco1l.rimu.ui.entity.hitobjects

import com.badlogic.gdx.scenes.scene2d.Actor
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.management.skin.WorkingSkin
import com.reco1l.rimu.ui.entity.Group
import com.reco1l.rimu.ui.entity.Image
import com.reco1l.rimu.ui.entity.TextureText
import com.reco1l.rimu.ui.entity.animateSequential
import com.reco1l.rimu.ui.entity.actor
import com.reco1l.rimu.ui.entity.toAlpha
import com.reco1l.rimu.ui.entity.toDelay
import com.rian.osu.beatmap.hitobject.HitCircle
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.hitobject.endTime
import com.rian.osu.beatmap.hitobject.sliderobject.SliderHead
import com.rian.osu.beatmap.hitobject.sliderobject.SliderTail
import kotlin.math.min


// Base

sealed class BaseHitCircleEntity<T : HitObject>(ctx: MainContext, protected val data: T) : Group(ctx)
{

    abstract val body: Actor

    abstract val bodyOverlay: Actor

    abstract val comboNumber: Actor?

    init
    {
        val duration = (min(1.0, data.startTime / 0.45) * 0.4).toFloat()

        toAlpha(0f)
        animateSequential(
            toAlpha(1f, duration),
            toDelay((data.endTime - data.startTime).toFloat())
        )
    }


    override fun onApplySkin(skin: WorkingSkin)
    {
        (comboNumber as? TextureText)?.setSkinning {

            texturePrefix = "${ctx.skins.current!!.data.fonts.hitCirclePrefix}-"
        }

        super.onApplySkin(skin)

        body.setSize(HitObject.OBJECT_RADIUS, HitObject.OBJECT_RADIUS)
        bodyOverlay.setSize(HitObject.OBJECT_RADIUS, HitObject.OBJECT_RADIUS)

        // If true the number sprite will be placed as index 1 which is below the hit circle overlay otherwise 2 which
        // is the last index.
        val above = skin.data.general.hitCircleOverlayAboveNumber

        comboNumber?.zIndex = if (above) 1 else 2
        bodyOverlay.zIndex = if (above) 2 else 1
    }

}


// HitCircle

class HitCircleEntity(ctx: MainContext, data: HitCircle) : BaseHitCircleEntity<HitCircle>(ctx, data)
{

    override val body = actor<Image> {

        setSkinning { texture = "hitcircle" }
    }

    override val bodyOverlay = actor<Image> {

        setSkinning { texture = "hitcircleoverlay" }
    }

    override val comboNumber = actor<TextureText> {

        text = data.comboIndex.toString()
        charScale = 0.8f
    }

}


// Slider

class SliderHeadEntity(ctx: MainContext, data: SliderHead) : BaseHitCircleEntity<SliderHead>(ctx, data)
{

    override val body = actor<Image> {

        setSkinning { texture = "sliderstartcircle" }
    }

    override val bodyOverlay = actor<Image> {

        setSkinning { texture = "sliderstartcircleoverlay" }
    }

    override val comboNumber = actor<TextureText> {

        text = data.comboIndex.toString()
        charScale = 0.8f
    }

}

class SliderTailEntity(ctx: MainContext, data: SliderTail) : BaseHitCircleEntity<SliderTail>(ctx, data)
{

    override val body = actor<Image> {

        setSkinning { texture = "sliderendcircle" }
    }

    override val bodyOverlay = actor<Image> {

        setSkinning { texture = "sliderendcircleoverlay" }
    }

    override val comboNumber = null

}

