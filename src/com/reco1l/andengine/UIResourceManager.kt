@file:Suppress("ConstPropertyName")

package com.reco1l.andengine

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import com.reco1l.andengine.component.UIComponent
import org.anddev.andengine.opengl.font.Font
import org.anddev.andengine.opengl.texture.TextureOptions
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas
import org.anddev.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource
import org.anddev.andengine.opengl.texture.atlas.buildable.BuildableTextureAtlas
import org.anddev.andengine.opengl.texture.atlas.buildable.builder.BlackPawnTextureBuilder
import org.anddev.andengine.opengl.texture.region.TextureRegion
import org.anddev.andengine.opengl.util.GLHelper
import java.lang.ref.WeakReference

// Use the device's maximum supported texture size, capped at 4096 to avoid excessive memory usage.
private val FONT_TEXTURE_SIZE get() = GLHelper.GlMaxTextureWidth.coerceAtMost(4096).coerceAtLeast(1024)


data class TextureAtlasBuilderState(
    val builder: BuildableTextureAtlas<IBitmapTextureAtlasSource, BitmapTextureAtlas>,
    val subscribers: MutableList<WeakReference<UIComponent>>
)

class UIResourceManager(private val context: Context) {

    private val fonts = mutableMapOf<String, Font>()
    private val fontSubscribers = mutableMapOf<Font, MutableList<WeakReference<UIComponent>>>()

    private val textureBuilders = mutableMapOf<String, BlackPawnTextureBuilder<IBitmapTextureAtlasSource, BitmapTextureAtlas>>()
    private val textureRegions = mutableMapOf<String, TextureRegion>()
    private val textureRegionSubscribers = mutableMapOf<TextureRegion, MutableList<WeakReference<UIComponent>>>()

    //region Textures

    fun loadTexture() {

    }


    //endregion

    //region Fonts

    fun getOrStoreFont(size: Float, family: String): Font {

        val fontIdentifier = "${family}-${size}"

        val fetchedFont = fonts[fontIdentifier]
        if (fetchedFont != null) {
            return fetchedFont
        }

        //Log.i("UIResourceManager", "Loading font: $fontIdentifier with texture size ${GLHelper.GlMaxTextureWidth / 2}x${GLHelper.GlMaxTextureWidth / 2}")

        val texture = BitmapTextureAtlas(FONT_TEXTURE_SIZE, FONT_TEXTURE_SIZE, TextureOptions.BILINEAR_PREMULTIPLYALPHA)
        val typeface = Typeface.createFromAsset(context.assets, "fonts/${family}")
        val font = Font(texture, typeface, size, true, Color.WHITE)

        UIEngine.current.apply {
            textureManager.loadTexture(texture)
            fontManager.loadFont(font)

            fonts[fontIdentifier] = font
        }

        return font
    }

    fun subscribeToFont(font: Font, component: UIComponent) {
        val subscribers = fontSubscribers.getOrPut(font) { mutableListOf() }
        if (subscribers.none { it.get() === component }) {
            subscribers.add(WeakReference(component))
        }
    }

    fun unsubscribeFromFont(font: Font, component: UIComponent) {

        val subscribers = fontSubscribers[font] ?: return
        subscribers.removeAll { it.get() === component || it.get() == null }

        if (subscribers.isEmpty()) {
            val fontKey = fonts.entries.find { it.value == font }?.key
            //Log.i("UIResourceManager", "Unloading font: $fontKey")

            fonts.remove(fontKey)
            fontSubscribers.remove(font)

            UIEngine.current.apply {
                fontManager.unloadFont(font)
                textureManager.unloadTexture(font.texture)
            }
        }
    }

    //endregion

}