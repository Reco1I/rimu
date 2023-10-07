package game.rimu.ui.drawables

import android.graphics.drawable.shapes.Shape
import game.rimu.ui.DrawableDimensions
import game.rimu.ui.DrawableSkinningRules
import game.rimu.ui.IScalableWithDimensions
import game.rimu.ui.ISkinnableWithRules
import android.graphics.drawable.ShapeDrawable as AndroidShapeDrawable


// ShapeDrawable

class ShapeDrawable(shape: Shape? = null, block: (ShapeDrawable.() -> Unit)? = null) :
    AndroidShapeDrawable(shape),
    IScalableWithDimensions<ShapeDrawable, DrawableDimensions<ShapeDrawable>>,
    ISkinnableWithRules<ShapeDrawable, DrawableSkinningRules<ShapeDrawable>>
{

    override val dimensions by lazy { DrawableDimensions<ShapeDrawable>() }

    override val skinRules by lazy { DrawableSkinningRules<ShapeDrawable>() }


    init
    {
        block?.invoke(this)
    }
}