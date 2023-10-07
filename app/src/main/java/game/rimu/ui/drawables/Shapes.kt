package game.rimu.ui.drawables

import android.graphics.drawable.shapes.Shape
import androidx.annotation.CallSuper
import com.reco1l.framework.graphics.Anchor
import com.reco1l.framework.lang.ifNotNull
import game.rimu.ui.IScalableWithDimensions
import com.reco1l.framework.graphics.RoundShape as FrameworkRoundShape
import game.rimu.ui.ScalableDimensions


// Base

open class ShapeDimensions<T : Shape> : ScalableDimensions<T>()
{
    @CallSuper
    override fun onApplyScale(target: T, scale: Float)
    {
        target.resize(
            if (width >= 0) width * scale else target.width,
            if (height >= 0) height * scale else target.height
        )
    }
}



// RoundShape

class RoundShapeDimensions : ShapeDimensions<RoundShape>()
{

    var radius: Float? = null

    var topLeftRadius: Float? = null

    var topRightRadius: Float? = null

    var bottomLeftRadius: Float? = null

    var bottomRightRadius: Float? = null


    override fun onApplyScale(target: RoundShape, scale: Float)
    {
        super.onApplyScale(target, scale)

        radius.ifNotNull {
            target.setRadius(radius = it)
            return
        }

        topLeftRadius.ifNotNull { target.setRadius(Anchor.TOP_LEFT, it) }
        topRightRadius.ifNotNull { target.setRadius(Anchor.TOP_RIGHT, it) }
        bottomLeftRadius.ifNotNull { target.setRadius(Anchor.BOTTOM_LEFT, it) }
        bottomRightRadius.ifNotNull { target.setRadius(Anchor.BOTTOM_RIGHT, it) }
    }
}

class RoundShape(block: (RoundShape.() -> Unit)? = null) :
    FrameworkRoundShape(),
    IScalableWithDimensions<RoundShape, RoundShapeDimensions>
{

    override val dimensions = RoundShapeDimensions()

    init
    {
        block?.invoke(this)
    }

    fun toDrawable() = ShapeDrawable(this)
}