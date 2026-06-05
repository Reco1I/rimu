package com.reco1l.andengine.text

import com.reco1l.andengine.*
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.theme.FontSize
import com.reco1l.andengine.theme.IconVariant
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.opengl.font.*
import javax.microedition.khronos.opengles.*
import com.reco1l.andengine.theme.Size
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas

/**
 * A text entity that can be displayed on the screen.
 */
open class FontAwesomeIcon(
    /**
     * The text to be displayed
     */
    var icon: Int

) : UIBufferedComponent() {

    /**
     * The variant of the icon font.
     */
    var iconVariant = IconVariant.Solid

    /**
     * The size of the icon font.
     */
    var iconSize = FontSize.SM

    /**
     * Called when the font settings (font size or family) change.
     */
    var onFontSettingsChange: () -> Unit = {
        val oldFont = font

        if (oldFont != null) {
            UIEngine.current.resources.unsubscribeFromFont(oldFont, this)
        }

        val newFont = UIEngine.current.resources.getOrStoreFont(iconSize, iconVariant)
        font = newFont
        UIEngine.current.resources.subscribeToFont(newFont, this)

        invalidate(InvalidationFlag.Content)
    }


    private var font: Font? = null
        set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Content)
            }
        }

    private var fontSettingsChanged = true


    init {
        width = Size.Auto
        height = Size.Auto
    }


    override fun onContentChanged() {
        val text = icon.toChar()
        val font = font

        if (font == null) {
            contentWidth = 0f
            contentHeight = 0f
            return
        }

        contentWidth = font.getLetter(text).mAdvance.toFloat()
        contentHeight = font.lineHeight.toFloat()
    }

    override fun doDraw(gl: GL10, camera: Camera) {
        if (fontSettingsChanged) {
            fontSettingsChanged = false
            onFontSettingsChange()
        }

        super.doDraw(gl, camera)

        val font = font ?: return

        TextRenderer.renderCharacter(
            gl,
            character = icon.toChar().toString(),
            font = font,
            viewportX = paddingLeft,
            viewportY = paddingTop,
            viewportWidth = innerWidth,
            viewportHeight = innerHeight,
            alignment = Anchor.Center
        )
    }


    fun finalize() {
        val font = font ?: return
        UIEngine.current.resources.unsubscribeFromFont(font, this)
    }


}
