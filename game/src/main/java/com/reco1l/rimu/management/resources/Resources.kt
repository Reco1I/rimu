package com.reco1l.rimu.management.resources

import android.util.Log
import com.reco1l.toolkt.kotlin.between
import com.reco1l.toolkt.data.subDirectory
import com.reco1l.toolkt.data.subFile
import com.reco1l.rimu.IWithContext
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.constants.RimuSetting.UI_USE_BEATMAP_SKIN
import com.reco1l.rimu.data.Skin
import com.reco1l.rimu.data.asset.AssetBundle
import com.reco1l.rimu.data.asset.InternalAssetsBundle
import com.reco1l.rimu.management.Setting
import com.reco1l.rimu.management.resources.Source.BEATMAP
import com.reco1l.rimu.management.resources.Source.DEFAULT
import com.reco1l.rimu.management.resources.Source.SKIN
import java.io.File

class ResourceManager(override val ctx: MainContext) : IWithContext
{

    /**
     * The directory where all the resource files will be located.
     */
    val directory = ctx.getExternalFilesDir(null)!!.subDirectory("resources/")



    /**
     * Map of assets sources and its getters.
     */
    val sources = mapOf(

        BEATMAP to { ctx.beatmaps.current?.assets },
        SKIN to { ctx.skins.current?.assets },
        DEFAULT to { ctx.resources.defaultAssets }
    )


    /**
     * Bindable for `Use beatmap skin` setting.
     */
    var useBeatmapSkin by Setting<Boolean>(UI_USE_BEATMAP_SKIN)


    // List of valid resource filename patterns. This equals to a whitelist where its used to decide
    // if a file should be imported or not into the resource database.
    private val allowedFilenames = ctx.assets.list(Skin.BASE)!!.associate { filename ->

        val name = filename.substringBeforeLast('.')

        // Extracting the variant pattern in case it have, if it does we're replacing the number
        // identifier (#) with the regex equivalent.
        val pattern = name.between('[', ']')?.replace("#", "(\\d+)")

        // Extracting resource name, the name is always before the variant pattern.
        var key = name.substringBefore('[')

        // Handling assets in 'default\' skin, some resources can have a variant indicator and
        // a variant pattern indicator in it.
        if (pattern != null)
            key = key.replace("^(.*?)($pattern)?$".toRegex()) { it.groupValues[1] }

        // The regex will contain 3 capturing groups:
        // 1 - It'll be a literal between the start until the pattern indicator starts marked between
        //     the brackets [], in case it doesn't have the end will be the dot (where the file extension
        //     starts).
        // 2 - The second will take what's inside the brackets [] as a regex pattern, the '#' it's
        //     considered as the number indicator and it's replaced with a nested capturing group:
        //     '(\\d+)'
        // 3 - As specified above the number will have its own capturing group to differentiate it.
        val regex = pattern?.let { "^($key)($it)?$".toRegex() }

        // Storing the Regex only if it has a variant pattern:
        key to regex
    }


    /**
     * Asset bundle with defaults assets.
     */
    val defaultAssets by lazy { InternalAssetsBundle(ctx, Skin.BASE) }


    /**
     * Check if a file is in use by the game, this prevents unused files being stored in the resource
     * directory.
     * @return A pair of the filename without the variant and the variant number.
     */
    fun resolveAsset(filename: String): Pair<String, Int>?
    {
        // Extracting the key name.
        val name = filename
            .substringBeforeLast('.')
            .substringBefore('[')
            .substringBeforeLast('@')

        for ((key, regex) in allowedFilenames)
        {
            // If the key equals exactly the filename means the filename doesn't have variants.
            if (key.equals(name, true) && regex == null)
                return key to 0

            // If the key has regex means it can have variants, in that case we check if the
            // pattern is correct.
            val result = regex?.find(name) ?: continue

            // Group 1 equals the key, meanwhile the group 3 equals the variant number.
            return result.groupValues[1] to (result.groupValues[3].toIntOrNull() ?: 0)
        }

        // The filename wasn't found in the whitelist / isn't valid.
        return null
    }

    /**
     * Store a resource in the default resource directory (`/obb`).
     *
     * @return `true` if success otherwise `false`.
     */
    fun storeResource(file: File, hash: String): Boolean
    {
        val destination = directory
            .subDirectory("${hash[0]}/${hash[0]}${hash[1]}")
            .subFile(hash)

        if (destination.exists() && destination.length() == file.length())
            return true

        destination.createNewFile()
        file.copyTo(destination, true)

        return destination.exists()
    }


    init
    {
        File(directory, "Important.txt").apply {

            if (exists())
                return@apply

            createNewFile()

            writeText("""
                Please do not touch or modify any file or folder inside this directory, otherwise the game may not recognize them. 
                If you wanna delete all resources consider using the "Clear data" option in device settings.
            """.trimIndent())
        }
    }


    // Getters

    /**
     * Returns a resource from the loaded resources as the inferred type.
     *
     * @see AssetBundle.SUPPORTED_TYPES
     */
    inline operator fun <reified T : Any> get(
        key: String,
        variant: Int = 0,
        fallbackToDefault: Boolean = true
    ): T? {
        for ((source, bundle) in sources) {
            if (source == BEATMAP && !useBeatmapSkin)
                continue

            if (source == DEFAULT && !fallbackToDefault)
                break

            return bundle()?.get(key, variant) ?: continue
        }

        if (fallbackToDefault)
            Log.w(javaClass.simpleName, "Expected asset $key with variant $variant, wasn't found.")

        return null
    }

    /**
     * Returns a resource with its variants from the loaded resources as list of the inferred type.
     *
     * @see AssetBundle.SUPPORTED_TYPES
     */
    inline operator fun <reified T : Any> get(
        key: String,
        fallbackToDefault: Boolean = true
    ): List<T>?
    {
        for ((source, bundle) in sources)
        {
            if (source == BEATMAP && !useBeatmapSkin)
                continue

            if (source == DEFAULT && !fallbackToDefault)
                break

            return bundle()?.get(key) ?: continue
        }

        return null
    }

}

/**
 * Enumeration of sources where the assets can get from.
 */
enum class Source
{
    SKIN,
    BEATMAP,
    DEFAULT
}