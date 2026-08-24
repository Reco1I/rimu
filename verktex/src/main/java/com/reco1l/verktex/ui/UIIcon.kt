package com.reco1l.verktex.ui

import com.reco1l.verktex.*
import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.LayoutConstraints
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.data.wrap
import com.reco1l.verktex.fonts.FontFamily
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.ui.container.UIContainer

fun UIContainer.icon(builder: UIIcon.() -> Unit) = UIIcon().apply(builder).also { +it }

/**
 * A UI component that displays an icon from a font family, such as Font Awesome.
 */
open class UIIcon(
    /**
     * The text to be displayed
     */
    var iconCode: Int = FAIcon.Question

) : UICanvas() {

    /**
     * The font family to use for this text.
     */
    var iconFamily: FontFamily = BuiltIn.Fonts.FontAwesomeIconSolid
        set(value) {
            if (field == value) return
            field = value
            fontSettingsChanged = true
        }

    /**
     * The font size to use for this text.
     */
    var iconSize = Verktex.theme.typography.body.size
        set(value) {
            if (field == value) return
            field = value
            fontSettingsChanged = true
        }


    private var font = iconFamily[iconSize]
    private var fontSettingsChanged = true


    init {
        width = wrap()
        height = wrap()
    }


    override fun onMeasure(
        parentConstraints: LayoutConstraints,
        contentConstraints: LayoutConstraints
    ): Vec2 {
        val measuredWidth = font[iconCode].advance
        val measuredHeight = font.metrics.lineHeight
        val measuredSize = Vec2(measuredWidth, measuredHeight)
        return parentConstraints.constrain(measuredSize.expand(padding.toVec4()))
    }

    override fun onDraw(renderer: GraphicsRenderer) {

        if (fontSettingsChanged) {
            fontSettingsChanged = false

            val newFont = iconFamily[iconSize]

            iconFamily.unsubscribe(font, this)
            iconFamily.subscribe(newFont, this)

            font = newFont
        }

        val font = font

        renderer.drawText(
            font = font,
            text = iconCode.toChar().toString(),
            x = 0f,
            y = 0f,
            color = color
        )
    }


    fun finalize() {
        iconFamily.unsubscribe(font, this)
    }


}
