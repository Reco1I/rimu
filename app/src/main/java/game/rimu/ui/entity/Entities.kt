package game.rimu.ui.entity

import game.rimu.IWithContext
import game.rimu.MainContext
import game.rimu.ui.ISkinnable
import game.rimu.ui.ISkinnableWithRules
import game.rimu.ui.SkinningRules
import org.andengine.entity.IEntity
import kotlin.math.max
import kotlin.math.min
import org.andengine.entity.Entity as AndEngineEntity


fun IWithContext.Entity(
    parent: AndEngineEntity? = this as? AndEngineEntity,
    init: Entity.() -> Unit
) = Entity(ctx).apply {
    parent?.attachChild(this)
    init()
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


    override fun attachChild(pEntity: IEntity?, index: Int)
    {
        super.attachChild(pEntity, index)
        onMeasureSize()
    }

    override fun detachChildren()
    {
        super.detachChildren()
        onMeasureSize()
    }
}