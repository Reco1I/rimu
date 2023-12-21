package com.reco1l.rimu.ui.entity

import com.reco1l.rimu.IWithContext
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.ui.ISkinnable
import com.reco1l.toolkt.kotlin.createInstance
import org.andengine.entity.IEntity
import kotlin.math.max
import kotlin.math.min
import org.andengine.entity.Entity as AndEngineEntity


/**
 * Creates a entity from this context.
 *
 * @param parent The parent from where this new entity will be attached. If `null` the entity will not
 * be attached to anything.
 * @param block The block that will be called when the entity is created and after attachment if [parent]
 * was specified.
 */
inline fun <reified T> IWithContext.entity(

    parent: IEntity? = this as? IEntity,

    block: T.() -> Unit

): T where T : IEntity, T : IWithContext
{
    val entity = T::class.createInstance(ctx)
    parent?.attachChild(entity)
    entity.block()
    return entity
}


open class Entity(override val ctx: MainContext) :
    AndEngineEntity(),
    IWithContext,
    ISkinnable
{

    fun onMeasureSize()
    {
        if (childCount == 0)
        {
            mWidth = 0f
            mHeight = 0f
            return
        }

        var minX = 0f
        var maxX = 0f
        var minY = 0f
        var maxY = 0f

        mChildren.onEach {
            minX = min(minX, it.x)
            maxX = max(maxX, it.x + it.width)

            minY = min(minY, it.y)
            maxY = max(maxY, it.y + it.height)
        }

        mWidth = maxX - minX
        mHeight = maxY - minY
    }


    override fun attachChild(entity: IEntity)
    {
        super.attachChild(entity)
        onMeasureSize()
    }

    override fun detachChildren()
    {
        super.detachChildren()
        onMeasureSize()
    }
}