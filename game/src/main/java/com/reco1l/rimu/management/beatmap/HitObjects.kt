package com.reco1l.rimu.management.beatmap

import com.badlogic.gdx.scenes.scene2d.Actor
import com.reco1l.rimu.IWithContext
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.constants.approachRateToSeconds
import com.reco1l.rimu.management.time.IClockObserver
import com.reco1l.rimu.ui.entity.game.Playfield
import com.reco1l.rimu.ui.entity.hitobjects.HitCircleEntity
import com.reco1l.rimu.ui.entity.hitobjects.SliderEntity
import com.reco1l.rimu.ui.layouts.DebugOverlay
import com.rian.osu.beatmap.BeatmapData
import com.rian.osu.beatmap.hitobject.HitCircle
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.beatmap.hitobject.endTime


class HitObjectTimeline(

    override val ctx: MainContext,

    val beatmap: BeatmapData,

    val playfields: Array<Playfield>,

    ): IWithContext, IClockObserver
{

    val hitObjects = beatmap.hitObjects!!.getObjects().apply {

        var previousNewComboIndex = 0

        forEachIndexed { i, hitObject ->

            hitObject.comboIndex = i - previousNewComboIndex + 10

            if (hitObject.isNewCombo)
                previousNewComboIndex = i
        }
    }

    private val activeEntities = playfields.associateWith {

        mutableMapOf<HitObject, Actor>()
    }

    val approachRate = approachRateToSeconds(beatmap.difficulty.ar)



    override fun onClockUpdate(sElapsedTime: Double, sDeltaTime: Float)
    {
        for (i in hitObjects.indices)
        {
            val hitObject = hitObjects[i]

            val sObjectStartTime = hitObject.startTime / 1000.0
            val sObjectEndTime = hitObject.endTime / 1000.0

            val inLifeTime = sElapsedTime > sObjectStartTime - approachRate && sElapsedTime < sObjectEndTime

            playfields.forEach { playfield ->

                if (inLifeTime && !hitObject.isActive)
                {
                    hitObject.isActive = true

                    val entity = createHitObjectEntity(hitObject) ?: return@forEach
                    entity.setPosition(hitObject.position.x, hitObject.position.y)

                    activeEntities[playfield]!![hitObject] = entity
                    playfield.addActor(entity)
                }
                else if (!inLifeTime && hitObject.isActive)
                {
                    hitObject.isActive = false

                    val toRemove = activeEntities[playfield]!!.remove(hitObject) ?: return@forEach
                    playfield.removeActor(toRemove)
                }
            }
        }

        ctx.layouts[DebugOverlay::class].setSection("HitObjects", """
            active_objects_count: ${activeEntities.size}
        """.trimIndent())
    }


    private fun createHitObjectEntity(data: HitObject) = when(data)
    {
        is HitCircle -> HitCircleEntity(ctx, data)

        is Slider -> SliderEntity(ctx, data)

        else -> null

    }
}