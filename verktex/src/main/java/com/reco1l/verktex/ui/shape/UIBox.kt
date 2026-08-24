package com.reco1l.verktex.ui.shape

import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.ui.UICanvas
import com.reco1l.verktex.ui.container.UIContainer

inline fun UIContainer.box(builder: UIBox.() -> Unit) = UIBox().apply(builder).also { +it }

open class UIBox : UICanvas() {
    override fun onDraw(renderer: GraphicsRenderer) {
        renderer.drawRect(0f, 0f, widthPx, heightPx, radius = radiusPx)
    }
}