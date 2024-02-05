package com.reco1l.rimu.ui.entity

import com.badlogic.gdx.scenes.scene2d.Actor
import com.reco1l.rimu.IWithContext
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.ui.ISkinnable
import kotlin.math.max
import kotlin.math.min
import com.badlogic.gdx.scenes.scene2d.Group as GdxGroup


fun Group.Group(block: Group.() -> Unit) = Group(ctx).also {
    addActor(it)
    it.block()
}

open class Group(override val ctx: MainContext) :
    GdxGroup(),
    IWithContext,
    IAttachable,
    ISkinnable
{

    /**
     * Automatically measure size when a child actor is added or removed.
     * This will measure the size of the group based on the positions and size of the children actors.
     *
     * @see onMeasureSize
     */
    var measureSizeWhenChildrenChanged = true


    override fun childrenChanged()
    {
        if (measureSizeWhenChildrenChanged)
            onMeasureSize()
    }


    /**
     * Measure the size of the group based on the positions and size of the children actors.
     *
     * @see measureSizeWhenChildrenChanged
     */
    fun onMeasureSize()
    {
        if (!hasChildren())
        {
            setSize(0f, 0f)
            return
        }

        var minX = 0f; var maxX = 0f
        var minY = 0f; var maxY = 0f

        children.onEach {
            minX = min(minX, it.x)
            maxX = max(maxX, it.x + it.width)

            minY = min(minY, it.y)
            maxY = max(maxY, it.y + it.height)
        }

        setSize(maxX - minX, maxY - minY)
    }


    override fun addActor(actor: Actor)
    {
        if (actor.parent == this)
            return

        super.addActor(actor)

        if (actor.parent == this && actor is IAttachable)
            actor.onAttached()
    }

    override fun removeActor(actor: Actor, unfocus: Boolean): Boolean
    {
        if (super.removeActor(actor, unfocus)) {

            if (actor is IAttachable)
                actor.onDetached()

            return true
        }
        return false
    }
}