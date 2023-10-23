package game.rimu.data.asset

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.Typeface
import com.caverock.androidsvg.SVG
import com.reco1l.basskt.stream.AssetSampleStream
import com.reco1l.basskt.stream.BaseStream
import com.reco1l.basskt.stream.SampleStream
import com.reco1l.framework.android.logE
import com.reco1l.framework.lang.orCatch
import com.reco1l.framework.graphics.toBitmap
import com.reco1l.framework.lang.klass
import game.rimu.android.IWithContext
import game.rimu.android.RimuContext
import game.rimu.data.Skin
import java.io.File
import java.io.InputStream
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

sealed class AssetBundle(override val ctx: RimuContext) : IWithContext
{

    /**
     * List of assets wrapped in the bundle.
     */
    abstract val list: List<Asset>

    /**
     * List of loaded assets.
     */
    val loadedAssets by lazy { mutableMapOf<Asset, Any?>() }


    /**
     * Called by [get] only once if the asset wasn't tried to load yet.
     */
    abstract fun <T : Any> onLoadAsset(expectedType: KClass<T>, name: String, variant: Int = 0, type: String): T?


    /**
     * Returns the real asset path according to its variant.
     */
    open fun getAssetPath(key: String, variant: Int = 0): String?
    {
        val asset = list.find { it.equals(key, variant) } ?: return null

        return "${ctx.resources.directory.path}/${asset.qualifiedPath}"
    }

    /**
     * Returns an [InputStream] of the asset.
     */
    open fun getInputStream(name: String, variant: Int = 0): InputStream?
    {
        val path = getAssetPath(name, variant) ?: return null

        return File(path).inputStream()
    }


    /**
     * Get an asset converted to the inferred type.
     */
    inline operator fun <reified T : Any> get(key: String, variant: Int = 0): T?
    {
        if (SUPPORTED_TYPES.none { T::class == it || T::class.isSubclassOf(it) })
            throw UnsupportedOperationException("${T::class} is not supported, see ${::SUPPORTED_TYPES.name}.")

        // Searching the key and variant into the listed assets, if the find function returns null,
        // means the bundle (skin or beatmap) doesn't have such file.
        val asset = list.find { it.equals(key, variant) } ?: return null

        var value = loadedAssets[asset]?.also {

            // Means the loaded asset isn't type of the required type, shouldn't happen if the
            // function is used properly.
            return if (it::class != T::class) null else it as? T
        }

        // Trying to load sound only if it wasn't tried yet. If the key has a null mapping means that
        // it was already tried to load unsuccessfully.
        if (asset !in loadedAssets)
        {
            value = onLoadAsset(T::class, key, variant, asset.type)

            // We storing it in the map no matter if it's still null, the key will have a null mapping
            // to avoid trying to load the asset again.
            loadedAssets[asset] = value
        }

        return value as? T
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
        val SUPPORTED_TYPES = arrayOf(Bitmap::class, Typeface::class, BaseStream::class)

        /**
         * Returns the proper type of [AssetBundle].
         *
         * If the skin is internal it returns [InternalAssetsBundle] otherwise [ExternalAssetBundle].
         */
        fun from(ctx: RimuContext, skin: Skin): AssetBundle
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
class InternalAssetsBundle(app: RimuContext, val directory: String) : AssetBundle(app)
{

    // Removing trailing slash isn't really necessary in newer APIs but apparently in Nougat and possibly
    // Oreo causes the method 'list()' returning an empty list.
    override val list = app.assets.list(directory.substringBeforeLast('/'))!!.map {

        val (key, variant) = app.resources.resolveAsset(it)!!

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

        return "$directory${asset.hash}"
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> onLoadAsset(expectedType: KClass<T>, name: String, variant: Int, type: String): T?
    {
        return {
            when (expectedType)
            {
                // Audio formats
                SampleStream::class -> AssetSampleStream(ctx, getAssetPath(name)) as? T

                // Image formats
                Bitmap::class -> getInputStream(name, variant)?.use {

                    if (type == "svg")
                        SVG.getFromInputStream(it).toBitmap()
                    else
                        it.toBitmap()

                } as? T

                // Font format
                Typeface::class -> Typeface.createFromAsset(ctx.assets, getAssetPath(name)) as? T

                // Unknown type
                else -> null
            }
        }.orCatch {
            klass logE ("Failed to load asset: \"$name\" with variant $variant of type \"$type\"" to it)
            null
        }
    }

}


/**
 * An AssetBundle wraps all assets used by a Skin or a Beatmap into a mapping.
 */
class ExternalAssetBundle(ctx: RimuContext, key: String) : AssetBundle(ctx)
{

    override val list = ctx.database.assetTable.getFromParent(key)


    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> onLoadAsset(expectedType: KClass<T>, name: String, variant: Int, type: String): T?
    {
        return {
            when (expectedType)
            {
                // Audio formats
                SampleStream::class -> SampleStream(getAssetPath(name, variant)) as? T

                // Image formats
                Bitmap::class -> getInputStream(name, variant)?.use {

                    if (type == "svg")
                        SVG.getFromInputStream(it).toBitmap()
                    else
                        it.toBitmap()

                } as? T

                // Font format
                Typeface::class -> Typeface.createFromFile(getAssetPath(name, variant)) as? T

                // Unknown type
                else -> null
            }
        }.orCatch {
            klass logE ("Error while loading asset: $name $variant $type" to it)
            null
        }
    }

}