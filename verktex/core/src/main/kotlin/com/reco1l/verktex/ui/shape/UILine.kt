package com.reco1l.verktex.ui.shape

import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.ui.UICanvas
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.data.px

inline fun UIContainer.line(builder: UILine.() -> Unit) = UILine().apply(builder).also { +it }

class UILine : UICanvas() {

    /**
     * The starting point of the line.
     */
    var fromPoint = Vec2.Zero

    /**
     * The ending point of the line.
     */
    var toPoint = Vec2.Zero

    /**
     * The width of the line.
     */
    var lineWidth = 1f.px


    override fun onDraw(renderer: GraphicsRenderer) {
        renderer.drawLine(fromPoint.x, fromPoint.y, toPoint.x, toPoint.y, lineWidth.toPixels(), contentColor)
    }
}
