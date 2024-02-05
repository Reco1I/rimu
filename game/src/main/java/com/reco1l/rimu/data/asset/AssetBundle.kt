package com.reco1l.rimu.data.asset

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.Typeface
import android.util.Log
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.caverock.androidsvg.SVG
import com.reco1l.basskt.stream.AssetSampleStream
import com.reco1l.basskt.stream.BaseStream
import com.reco1l.basskt.stream.SampleStream
import com.reco1l.toolkt.kotlin.orCatch
import com.reco1l.rimu.IWithContext
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.data.Skin
import com.reco1l.rimu.graphics.toBitmap
import org.jetbrains.kotlin.library.impl.buffer
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

sealed class AssetBundle(override val ctx: MainContext) : IWithContext
{

    /**
     * List of assets wrapped in the bundle.
     */
    abstract val list: List<Asset>

    /**
     * Array map of loaded assets grouped by its classifier.
     *
     * Since this is an array (for performance purposes) instead of using keys, we're using the
     * indexes from [SUPPORTED_TYPES] as keys.
     */
    val loadedMap by lazy { Array(SUPPORTED_TYPES.size) { mutableMapOf<Asset, Any?>() } }

    /**
     * Called by [get] only once if the asset wasn't tried to load yet.
     */
    abstract fun <T : Any> onLoadAsset(
        expectedType: KClass<T>,
        key: String,
        variant: Int = 0,
        type: String
    ): T?


    fun onRelease()
    {
        // Unloading loaded textures from engine.
        //getMapOf<WrappingTexture>().forEach { (it.value as WrappingTexture).unload() }
    }


    /**
     * Returns the real asset path according to its variant.
     */
    open fun getAssetPath(key: String, variant: Int = 0): String?
    {
        val asset = list.find { it.equals(key, variant) } ?: return null

        val path = "${ctx.resources.directory.path}/${asset.qualifiedPath}"
        Log.v(javaClass.simpleName, "Queried path for asset with key \"$key\": \"$path\"")

        return path
    }

    /**
     * Returns an [InputStream] of the asset.
     */
    open fun getInputStream(name: String, variant: Int = 0): InputStream?
    {
        val path = getAssetPath(name, variant) ?: return null

        return File(path).inputStream()
    }


    inline fun <reified T : Any> getMapOf(): MutableMap<Asset, Any?>
    {
        val mapIndex = SUPPORTED_TYPES.indexOfFirst(T::class::isSubclassOf)

        if (mapIndex == -1)
            throw UnsupportedOperationException("${T::class} is not a supported type.")

        // Finding the corresponding map for the declared classifier.
        return loadedMap[mapIndex]
    }


    /**
     * Get an asset converted to the inferred type.
     */
    inline operator fun <reified T : Any> get(key: String, variant: Int = 0): T?
    {
        val map = getMapOf<T>()

        // Searching the key and variant into the listed assets, if the find function returns null,
        // means the bundle source doesn't have such file.
        val asset = list.find { it.equals(key, variant) } ?: run {
            return null
        }

        return map[asset] as? T ?: run {

            // Trying to load sound only if it wasn't tried yet. If the key has a null mapping means that
            // it was already tried to load unsuccessfully.
            if (asset in map)
            {
                Log.e(javaClass.simpleName, "Asset \"$key\" was already tried to load unsucessfully.")
                return null
            }

            // We storing it in the map no matter if it's still null, the key will have a null mapping
            // to avoid trying to load the asset again in the future.
            { onLoadAsset(T::class, key, variant, asset.type) }.orCatch {

                Log.e(javaClass.simpleName, "Failed to load asset $key::$variant of type ${T::class}", it)
                null

            }.also { map[asset] = it }
        }
    }

    /**
     * Get a list of assets and its variants converted to the inferred type.
     */
    inline operator fun <reified T : Any> get(key: String): List<T>?
    {
        return list
            .filter { key == it.key }
            .sortedBy { it.variant }
            .mapNotNull { get(it.key, it.variant) as? T }
            .takeUnless { it.isEmpty() }
    }


    companion object
    {

        /**
         * List of supported types.
         */
        val SUPPORTED_TYPES = arrayOf(
            Bitmap::class,
            Texture::class,
            Typeface::class,
            BitmapFont::class,
            BaseStream::class
        )

        /**
         * Returns the proper type of [AssetBundle].
         *
         * If the skin is internal it returns [InternalAssetsBundle] otherwise [ExternalAssetBundle].
         */
        fun from(ctx: MainContext, skin: Skin): AssetBundle
        {
            return if (skin.isInternal)
                InternalAssetsBundle(ctx, skin.key)
            else
                ExternalAssetBundle(ctx, skin.key)
        }
    }
}


/**
 * Created specifically to load default textures from Android [AssetManager].
 */
class InternalAssetsBundle(ctx: MainContext, val directory: String) : AssetBundle(ctx)
{

    override val list = ctx.assets.list(directory)!!.mapNotNull {

        val (key, variant) = ctx.resources.resolveAsset(it) ?: return@mapNotNull null

        Asset(
            hash = it,
            parent = directory,
            key = key,
            variant = variant,
            type = it.substringAfterLast('.', "")
        )
    }


    override fun getInputStream(name: String, variant: Int): InputStream?
    {
        return ctx.assets.open(getAssetPath(name) ?: return null)
    }

    /**
     * Warning: This doesn't return the absolute path of the file because of how [AssetManager] works.
     */
    override fun getAssetPath(key: String, variant: Int): String?
    {
        val asset = list.find { it.equals(key, variant) } ?: return null

        return "$directory/${asset.hash}"
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> onLoadAsset(
        expectedType: KClass<T>,
        key: String,
        variant: Int,
        type: String
    ) = when (expectedType)
    {
        // Audio formats
        SampleStream::class -> AssetSampleStream(ctx, getAssetPath(key)) as? T


        // Image formats
        Texture::class -> getInputStream(key, variant)?.use {

            val bytes = it.readBytes()
            val pixmap = Pixmap(bytes, 0, bytes.size)

            Texture(pixmap)
        } as? T

        Bitmap::class -> getInputStream(key, variant)?.use {

            if (type == "svg")
                SVG.getFromInputStream(it).toBitmap()
            else
                it.toBitmap()

        } as? T


        // Font format
        Typeface::class -> Typeface.createFromAsset(ctx.assets, getAssetPath(key)) as? T

        // Unknown type
        else -> throw UnsupportedOperationException("The expected type is not supported.")
    }
}


/**
 * An AssetBundle wraps all assets used by a Skin or a Beatmap into a mapping.
 */
class ExternalAssetBundle(ctx: MainContext, key: String) : AssetBundle(ctx)
{

    override val list = ctx.database.assetTable.getFromParent(key)


    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> onLoadAsset(
        expectedType: KClass<T>,
        key: String,
        variant: Int,
        type: String
    ): T?
    {
        Log.v(javaClass.simpleName, "Loading external asset: Key=\"$key\" Type=\"$type\"")

        return {
            when (expectedType)
            {
                // Audio formats
                SampleStream::class -> SampleStream(getAssetPath(key, variant)) as? T

                // Image formats
                Texture::class -> getInputStream(key, variant)?.use {

                    val bytes = it.readBytes().buffer
                    val pixmap = Pixmap(bytes)

                    Texture(pixmap)
                } as? T

                Bitmap::class -> getInputStream(key, variant)?.use {

                    if (type == "svg")
                        SVG.getFromInputStream(it).toBitmap()
                    else
                        it.toBitmap()

                } as? T

                // Font format
                Typeface::class -> Typeface.createFromFile(getAssetPath(key, variant)) as? T

                // Unknown type
                else -> null
            }
        }.orCatch {
            Log.e(javaClass.simpleName, "Error while loading asset: $key $variant $type", it)
            null
        }
    }

}