package com.reco1l.rimu.ui.entity

import com.badlogic.gdx.scenes.scene2d.Actor as GdxActor
import com.badlogic.gdx.scenes.scene2d.Group
import com.reco1l.rimu.IWithContext
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.ui.ISkinnable
import com.reco1l.toolkt.kotlin.createInstance


/**
 * Creates a entity from this context.
 *
 * @param parent The parent from where this new entity will be attached. If `null` the entity will not
 * be attached to anything.
 * @param block The block that will be called when the entity is created and after attachment if [parent]
 * was specified.
 */
inline fun <reified T> IWithContext.actor(

    parent: Group? = this as? Group,

    block: T.() -> Unit

): T where T : GdxActor, T : IWithContext
{
    val actor = T::class.createInstance(ctx)
    parent?.addActor(actor)
    actor.block()
    return actor
}


open class DummyActor(override val ctx: MainContext) :
    GdxActor(),
    IWithContext,
    ISkinnable


/**
 * Listener for attaching and detaching actors.
 */
interface IAttachable
{
    /**
     * Called when the actor is attached to a parent.
     */
    fun onAttached() = Unit

    /**
     * Called when the actor is detached from the parent.
     */
    fun onDetached() = Unit
}
