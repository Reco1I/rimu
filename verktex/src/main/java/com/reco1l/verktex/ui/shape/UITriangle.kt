package com.reco1l.verktex.ui.shape

import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.ui.UICanvas
import com.reco1l.verktex.ui.container.UIContainer

inline fun UIContainer.triangle(builder: UITriangle.() -> Unit) = UITriangle().apply(builder).also { +it }

open class UITriangle : UICanvas() {

    /**
     * The position of the triangle's tip along the base, where 0.0 is the left corner and 1.0
     * is the right corner.
     */
    var middle: Float = 0.5f
        set(value) { field = value.coerceIn(0f, 1f) }


    override fun onDraw(renderer: GraphicsRenderer) {
        renderer.drawTriangle(
            x = 0f,
            y = 0f,
            width = widthPx,
            height = heightPx,
            middle = middle
        )
    }
}


