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
open class UIIcon(iconCode: Int = FAIcon.Question) : UICanvas() {

    /**
     * The code point of the icon to display.
     */
    var iconCode = iconCode
        set(value) {
            if (field == value) return
            field = value
            invalidateMeasure()
        }

    /**
     * The font family to use for this text.
     */
    var iconFamily: FontFamily = BuiltIn.Fonts.FontAwesomeIconSolid
        set(value) {
            if (field == value) return
            field = value
            fontSettingsChanged = true
            invalidateMeasure()
        }

    /**
     * The font size to use for this text.
     */
    var iconSize = Verktex.theme.typography.body.size
        set(value) {
            if (field == value) return
            field = value
            fontSettingsChanged = true
            invalidateMeasure()
        }


    private var font = iconFamily[iconSize]
    private var fontSettingsChanged = true


    override fun onThemeChanged() {
        // The font scale changes the resolved font even if the settings stay the same.
        fontSettingsChanged = true
        super.onThemeChanged()
    }


    init {
        width = wrap()
        height = wrap()
    }


    override fun onMeasure(
        parentConstraints: LayoutConstraints,
        contentConstraints: LayoutConstraints
    ): Vec2 {
        val font = iconFamily[iconSize]
        val measuredWidth = font[iconCode].advance
        val measuredHeight = font.metrics.lineHeight
        val measuredSize = Vec2(measuredWidth, measuredHeight)
        return parentConstraints.constrain(measuredSize.expand(padding.toVec4()))
    }

    override fun onDraw(renderer: GraphicsRenderer) {

        if (fontSettingsChanged) {
            fontSettingsChanged = false

            val newFont = iconFamily[iconSize]

            font = newFont
        }

        val font = font

        // Centered inside the content area, so rotations around the center keep the glyph in place.
        val contentWidth = bounds.width - paddingLeft.toPixels() - paddingRight.toPixels()
        val contentHeight = bounds.height - paddingTop.toPixels() - paddingBottom.toPixels()

        renderer.drawText(
            font = font,
            text = String(Character.toChars(iconCode)),
            x = paddingLeft.toPixels() + (contentWidth - font[iconCode].advance) / 2f,
            y = paddingTop.toPixels() + (contentHeight - font.metrics.lineHeight) / 2f,
            color = contentColor
        )
    }


}
