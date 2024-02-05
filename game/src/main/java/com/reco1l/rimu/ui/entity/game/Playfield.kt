package com.reco1l.rimu.ui.entity.game

import com.reco1l.rimu.IWithContext
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.management.beatmap.HitObjectTimeline
import com.reco1l.rimu.management.beatmap.IBeatmapObserver
import com.reco1l.rimu.management.beatmap.WorkingBeatmap
import com.reco1l.rimu.management.time.IClockObserver
import com.reco1l.rimu.ui.ISkinnable
import com.reco1l.rimu.updateThread
import com.badlogic.gdx.scenes.scene2d.Group


class Playfield(override val ctx: MainContext) :
    Group(),
    IWithContext,
    IBeatmapObserver,
    IClockObserver,
    ISkinnable
{

    var hitObjectTimeline: HitObjectTimeline? = null


    init
    {
        setBounds(
            ctx.engine.surfaceWidth / 2f,
            ctx.engine.surfaceHeight / 2f,
            PLAYFIELD_WIDTH,
            PLAYFIELD_HEIGHT
        )
        setColor(0f, 0f, 0f, 0.4f)

        ctx.beatmaps.bindObserver(observer = this)
    }


    override fun onMusicChange(beatmap: WorkingBeatmap?)
    {
        beatmap?.clock?.bindObserver(observer = this)

        updateThread {

            hitObjectTimeline = null
            clearChildren()


            if (beatmap == null)
                return@updateThread

            hitObjectTimeline = HitObjectTimeline(ctx, beatmap.data, arrayOf(this))
        }
    }

    override fun onClockUpdate(sElapsedTime: Double, sDeltaTime: Float)
    {
        hitObjectTimeline?.onClockUpdate(sElapsedTime, sDeltaTime)

        super.act(sDeltaTime)
    }

    override fun act(delta: Float) = Unit


    companion object
    {

        const val PLAYFIELD_WIDTH = 512f

        const val PLAYFIELD_HEIGHT = 384f

    }
}