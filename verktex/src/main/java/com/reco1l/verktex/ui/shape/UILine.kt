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
        val lineWidth = lineWidth.resolve(this, Axis.X)

        val x1 = fromPoint.x - lineWidth / 2
        val y1 = fromPoint.y - lineWidth / 2
        val x2 = toPoint.x + lineWidth / 2
        val y2 = toPoint.y + lineWidth / 2

        renderer.drawRect(x1, y1, x2, y2)
    }
}
