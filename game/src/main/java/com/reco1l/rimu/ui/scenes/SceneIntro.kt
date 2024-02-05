package com.reco1l.rimu.ui.scenes

import com.badlogic.gdx.graphics.Texture
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.constants.BuildSettings
import com.reco1l.rimu.graphics.setTexture
import com.reco1l.rimu.management.beatmap.WorkingBeatmap
import com.reco1l.rimu.ui.entity.Image
import com.reco1l.rimu.ui.entity.game.Playfield
import com.reco1l.rimu.ui.entity.Sprite

class SceneIntro(ctx: MainContext) : BaseScene(ctx)
{

    val background = Image(ctx).also {

        stage.addActor(it)
    }

    val playfield = Playfield(ctx)


    init
    {
        ctx.beatmaps.bindObserver(observer = this)

        background.setTexture(ctx.resources["menu-background", 0])

        attachChild(playfield)
    }

    override fun onMusicChange(beatmap: WorkingBeatmap?)
    {
        var texture: Texture? = ctx.resources["menu-background", 0]

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

        background.setPosition(ctx.engine.surfaceWidth / 2f, ctx.engine.surfaceHeight / 2f)
        playfield.setPosition(ctx.engine.surfaceWidth / 2f, ctx.engine.surfaceHeight / 2f)
        playfield.setScale(ctx.engine.surfaceHeight / Playfield.PLAYFIELD_HEIGHT * 0.8f)
    }
}