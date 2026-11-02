package com.reco1l.verktex.ui.shape

import com.reco1l.verktex.data.px
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.ui.PaintStyle
import com.reco1l.verktex.ui.UICanvas
import com.reco1l.verktex.ui.container.UIContainer

inline fun UIContainer.box(builder: UIBox.() -> Unit) = UIBox().apply(builder).also { +it }

open class UIBox : UICanvas() {

    /**
     * Whether to draw the box filled or only its outline. Filled boxes take their color from
     * [backgroundColor], outlines from [color] and [lineWidth].
     */
    var paintStyle = PaintStyle.Fill

    /**
     * The width of the outline, only used with [PaintStyle.Outline].
     */
    var lineWidth = 1f.px


    override fun onDraw(renderer: GraphicsRenderer) {
        when (paintStyle) {
            PaintStyle.Fill -> renderer.drawRect(0f, 0f, bounds.width, bounds.height, radius = radius.toPixels())
            PaintStyle.Outline -> renderer.drawRect(
                x = 0f,
                y = 0f,
                width = bounds.width,
                height = bounds.height,
                borderWidth = lineWidth.toPixels(),
                borderColor = contentColor,
                radius = radius.toPixels()
            )
        }
    }
}
