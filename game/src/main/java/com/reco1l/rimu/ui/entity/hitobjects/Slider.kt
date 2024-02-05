package com.reco1l.rimu.ui.entity.hitobjects

import com.badlogic.gdx.graphics.Color
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.graphics.MeshActor
import com.reco1l.rimu.graphics.PathMesh
import com.reco1l.rimu.graphics.PathMeshDrawer
import com.reco1l.rimu.graphics.getSegments
import com.reco1l.rimu.ui.entity.Group
import com.reco1l.rimu.ui.entity.animateSequential
import com.reco1l.rimu.ui.entity.toAlpha
import com.reco1l.rimu.ui.entity.toDelay
import com.rian.osu.beatmap.hitobject.Slider
import kotlin.math.min


class SliderEntity(ctx: MainContext, val data: Slider) : Group(ctx)
{

    private val drawer = PathMeshDrawer(data.path.getSegments())


    var borderWidth = 6


    val body = SliderBody(ctx, data, drawer.apply { radius -= borderWidth }).apply {

        mesh.clearDepthOnStart = true
    }

    val border = SliderBody(ctx, data, drawer.apply {

        radius += borderWidth
        outerColor = Color.WHITE
    })


    val headCircle = SliderHeadEntity(ctx, data.head)

    val tailCircle = SliderTailEntity(ctx, data.tail)


    init
    {
        addActor(body)
        addActor(border)
        addActor(headCircle)


        val duration = (min(1.0, data.startTime / 0.45) * 0.4).toFloat()

        toAlpha(0f)
        animateSequential(
            toAlpha(1f, duration),
            toDelay((data.endTime - data.startTime).toFloat())
        )
    }

    override fun onAttached()
    {
        super.onAttached()

        headCircle.setPosition(data.head.position.x - x, data.head.position.y - y)
        tailCircle.setPosition(data.tail.position.x - x, data.tail.position.y - y)

        val duration = 0.4 * min(1.0, data.startTime / 0.45)

        /*toAlpha(0f)
        animateSequential(
            toAlpha(1f, (duration * 1000).toLong()),
            toDelay((data.endTime * 1000).toLong())
        )*/
    }
}


class SliderBody(ctx: MainContext, data: Slider, drawer: PathMeshDrawer) : MeshActor(PathMesh(drawer))
{

}
