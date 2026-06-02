@file:Suppress("ConstPropertyName")

package com.reco1l.andengine

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import com.reco1l.andengine.component.UIComponent
import org.anddev.andengine.opengl.font.Font
import org.anddev.andengine.opengl.texture.TextureOptions
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas
import org.anddev.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource
import org.anddev.andengine.opengl.texture.region.TextureRegion
import org.anddev.andengine.opengl.util.GLHelper
import ru.nsu.ccfit.zuev.osu.helper.QualityAssetBitmapSource
import ru.nsu.ccfit.zuev.osu.helper.QualityFileBitmapSource
import java.io.File
import java.lang.ref.WeakReference

val MaxTextureSize
    get() = GLHelper.GlMaxTextureWidth
        .coerceAtMost(4096)
        .coerceAtLeast(1024)


enum class TextureStore {
    Default,
    Custom
}

class UIResourceManager(private val context: Context) {

    private val fonts = mutableMapOf<String, Font>()
    private val fontSubscribers = mutableMapOf<Font, MutableList<WeakReference<UIComponent>>>()

    private val textureAtlases = mutableListOf<TextureAtlasState>()
    private val textureStores = mutableMapOf<TextureStore, MutableMap<String, DynamicTextureRegion>>(
        TextureStore.Default to mutableMapOf(),
        TextureStore.Custom to mutableMapOf()
    )


    //region Textures

    fun loadTexture(store: TextureStore, key: String, filePath: String, isAsset: Boolean = false): DynamicTextureRegion? {

        val store = textureStores[store] ?: throw IllegalArgumentException("Texture store '$store' does not exist.")

        val alreadyExists = store.containsKey(key)
        if (alreadyExists) {
            Log.w("UIResourceManager", "Texture with key '$key' already exists. Replacing...")
        }

        val source: IBitmapTextureAtlasSource

        if (isAsset) {
            source = QualityAssetBitmapSource(context, filePath)
        } else {
            var file = File(filePath.substringAfterLast('.') + "@2x." + filePath.substringAfterLast('.'))
            var isHDTexture = true

            if (!file.exists()) {
                file = File(filePath)
                isHDTexture = false
            }

            if (!file.exists()) {
                Log.e("UIResourceManager", "Texture file not found: $filePath")
                return null
            }

            source = QualityFileBitmapSource(file, if (isHDTexture) 2 else 1)
        }

        val region = addSourceToAtlasAndGetRegion(source)
        if (alreadyExists) {
            val oldRegion = store[key]
            if (oldRegion != null) {
                val atlasState = textureAtlases.firstOrNull { it.atlas == oldRegion.texture }
                atlasState?.removeSource(oldRegion.source)
            }
        }

        store[key] = region
        return region
    }

    fun unloadTexture(store: TextureStore, key: String) {
        val store = textureStores[store] ?: throw IllegalArgumentException("Texture store '$store' does not exist.")
        val region = store.remove(key) ?: return
        val atlasState = textureAtlases.firstOrNull { it.atlas == region.texture } ?: return
        atlasState.removeSource(region.source)
    }

    fun unloadTexture(store: TextureStore, region: TextureRegion) {
        val store = textureStores[store] ?: throw IllegalArgumentException("Texture store '$store' does not exist.")
        val entry = store.entries.firstOrNull { it.value == region }

        if (entry == null) {
            Log.w("UIResourceManager", "Texture region not found in store '$store'. Cannot unload.")
            return
        }

        val region = entry.value
        val atlasState = textureAtlases.firstOrNull { it.atlas == region.texture } ?: return
        atlasState.removeSource(region.source)
    }

    fun unloadAllTextures(store: TextureStore) {
        val store = textureStores[store] ?: throw IllegalArgumentException("Texture store '$store' does not exist.")
        for (region in store.values) {
            val atlasState = textureAtlases.firstOrNull { it.atlas == region.texture } ?: continue
            atlasState.removeSource(region.source)
        }
        store.clear()
    }


    fun containsTexture(store: TextureStore, key: String): Boolean {
        val store = textureStores[store] ?: throw IllegalArgumentException("Texture store '$store' does not exist.")
        return store.containsKey(key)
    }

    fun getTexture(store: TextureStore, key: String): DynamicTextureRegion? {
        val store = textureStores[store] ?: throw IllegalArgumentException("Texture store '$store' does not exist.")
        return store[key]
    }

    fun getTextureKeys(store: TextureStore): Set<String> {
        val store = textureStores[store] ?: throw IllegalArgumentException("Texture store '$store' does not exist.")
        return store.keys
    }


    private fun addSourceToAtlasAndGetRegion(source: IBitmapTextureAtlasSource): DynamicTextureRegion {
        val atlasState = textureAtlases.firstOrNull { it.spaceAvailable && it.addSource(source) } ?: run {
            val newAtlas = TextureAtlasState()
            if (newAtlas.addSource(source)) {
                textureAtlases.add(newAtlas)
            } else {
                Log.e("UIResourceManager", "Failed to add texture source to new atlas")
            }
            newAtlas
        }
        return DynamicTextureRegion(atlasState, source)
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

        val texture = BitmapTextureAtlas(MaxTextureSize, MaxTextureSize, TextureOptions.BILINEAR_PREMULTIPLYALPHA)
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