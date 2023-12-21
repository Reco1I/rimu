package com.reco1l.rimu.ui.scenes

import com.reco1l.rimu.graphics.WrappingTexture
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.constants.BuildSettings
import com.reco1l.rimu.management.beatmap.IBeatmapObserver
import com.reco1l.rimu.management.beatmap.WorkingBeatmap
import com.reco1l.rimu.ui.entity.Sprite
import com.reco1l.rimu.ui.entity.hitobjects.HitCircleEntity

class SceneIntro(ctx: MainContext) : BaseScene(ctx)
{

    val hitCircleTest = HitCircleEntity(ctx)

    val background = Sprite {  }


    init
    {
        attachChild(hitCircleTest)
        background.setTexture(ctx.resources["menu-background", 0])

        ctx.beatmaps.bindObserver(observer = this)
    }

    override fun onMusicChange(beatmap: WorkingBeatmap?)
    {
        var texture: WrappingTexture? = ctx.resources["menu-background", 0]

        beatmap?.data?.events?.apply {

            if (!BuildSettings.SFW_MODE)
                texture = backgroundFilename?.let { beatmap.assets[it.substringBeforeLast('.'), 0] } ?: texture
        }

        background.setTexture(texture)
        background.setScale(ctx.engine.surfaceWidth / background.width)
    }

    override fun onMusicEnd()
    {
        ctx.beatmaps.next()
    }


    override fun onManagedUpdate(pSecondsElapsed: Float)
    {
        super.onManagedUpdate(pSecondsElapsed)

        hitCircleTest.setPosition(ctx.engine.surfaceWidth / 2f, ctx.engine.surfaceHeight / 2f)
        background.setPosition(ctx.engine.surfaceWidth / 2f, ctx.engine.surfaceHeight / 2f)
    }
}