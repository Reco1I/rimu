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

    private val textureAtlases = mutableListOf<DynamicTextureAtlas>()
    private val textureStores = mutableMapOf<TextureStore, MutableMap<String, DynamicTextureRegion>>(
        TextureStore.Default to mutableMapOf(),
        TextureStore.Custom to mutableMapOf()
    )


    //region Textures

    @Synchronized
    fun loadTexture(store: TextureStore, key: String, filePath: String, isAsset: Boolean = false): DynamicTextureRegion? {

        //Log.i("UIResourceManager", "Loading texture: $filePath into store '$store' with key '$key' from ${if (isAsset) "assets" else "external sources"}")

        val store = getTextureStore(store)

        val source: IBitmapTextureAtlasSource

        if (isAsset) {
            try {
                context.assets.open(filePath).close()
            } catch (e: Exception) {
                Log.e("UIResourceManager", "Texture asset not found: $filePath")
                return null
            }

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

        if (store.containsKey(key)) {
            val oldRegion = store[key]

            if (oldRegion != null) {
                Log.w("UIResourceManager", "Texture with key '$key' already exists in texture store. Replacing...")
                findAtlasForTextureRegion(oldRegion)?.removeSource(oldRegion.source)
            }
        }

        val region = addSourceToAtlas(source)
        store[key] = region
        return region
    }

    fun unloadTexture(store: TextureStore, key: String) {
        val store = getTextureStore(store)
        val region = store.remove(key) ?: return

        findAtlasForTextureRegion(region)?.removeSource(region.source)
    }

    fun unloadTexture(store: TextureStore, region: TextureRegion) {
        val store = getTextureStore(store)

        val entry = store.entries.firstOrNull { it.value == region }
        if (entry == null) {
            Log.w("UIResourceManager", "Texture region not found in store '$store'. Cannot unload.")
            return
        }

        val (key, region) = entry

        store.remove(key)
        findAtlasForTextureRegion(region)?.removeSource(region.source)
    }

    fun unloadAllTextures(store: TextureStore) {
        val store = getTextureStore(store)
        for (region in store.values) {
            findAtlasForTextureRegion(region)?.removeSource(region.source)
        }
        store.clear()
    }


    fun containsTexture(store: TextureStore, key: String): Boolean {
        return getTextureStore(store).containsKey(key)
    }

    fun getTexture(store: TextureStore, key: String): DynamicTextureRegion? {
        return getTextureStore(store)[key]
    }

    fun getTextureKeys(store: TextureStore): Set<String> {
        return getTextureStore(store).keys
    }


    private fun findAtlasForTextureRegion(region: TextureRegion): DynamicTextureAtlas? {
        return textureAtlases.firstOrNull { it == region.texture }
    }

    private fun getTextureStore(store: TextureStore): MutableMap<String, DynamicTextureRegion> {
        return textureStores[store] ?: throw IllegalArgumentException("Texture store '$store' does not exist.")
    }

    private fun addSourceToAtlas(source: IBitmapTextureAtlasSource): DynamicTextureRegion {
        val atlas = textureAtlases.firstOrNull { it.addSource(source) } ?: run {

            Log.w("UIResourceManager", "No existing atlas has space for the new texture. Creating a new atlas...")

            val newAtlas = DynamicTextureAtlas()

            if (newAtlas.addSource(source)) {
                textureAtlases.add(newAtlas)
            } else {
                Log.e("UIResourceManager", "Failed to add texture source to new atlas")
            }

            newAtlas
        }

        return DynamicTextureRegion(atlas, source)
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