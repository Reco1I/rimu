package game.rimu.management.resources

import com.reco1l.framework.lang.between
import com.reco1l.framework.lang.getClassName
import com.reco1l.framework.android.logI
import com.reco1l.framework.data.subDirectory
import com.reco1l.framework.data.subFile
import game.rimu.android.IWithContext
import game.rimu.android.RimuContext
import game.rimu.constants.RimuSetting.UI_USE_BEATMAP_SKIN
import game.rimu.data.asset.AssetBundle
import game.rimu.management.Setting
import game.rimu.management.resources.ResourceProvider.BEATMAP
import game.rimu.management.resources.ResourceProvider.DEFAULT
import java.io.File

class ResourceManager(override val ctx: RimuContext) : IWithContext
{

    /**
     * The directory where all the resource files will be located.
     */
    val directory = File(ctx.obbDir, RESOURCES_PATH).apply { mkdirs() }

    /**
     * Bindable for `Use beatmap skin` setting.
     */
    var useBeatmapSkin by Setting<Boolean>(UI_USE_BEATMAP_SKIN)


    // List of valid resource filename patterns. This equals to a whitelist where its used to decide
    // if a file should be imported or not into the resource database.
    private val allowedFilenames = ctx.assets.list("default/")!!.associate { filename ->

        val name = filename.substringBeforeLast('.')

        // Extracting the variant pattern in case it have, if it does we replacing the number
        // identifier (#) with the regex equivalent.
        val pattern = name.between('[', ']')
            ?.replace("#", "(\\d+)")

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

        "Defined allowed asset: $key - $regex".logI(getClassName())

        // Storing the Regex only if it has a variant pattern:
        key to regex
    }

    private val filenamesKeySet = allowedFilenames.keys


    /**
     * Check if a file is in use by the game, this prevents unused files being stored in the resource
     * directory.
     * @return A pair of the filename without the variant and the variant number.
     */
    fun resolveAsset(filename: String): Pair<String, Int>?
    {
        // Removing extension and variant pattern indicator.
        val name = filename.substringBeforeLast('.').substringBefore('[')

        for (key in filenamesKeySet)
        {
            val regex = allowedFilenames[key]

            // If the key equals exactly the filename means the filename doesn't have variants.
            if (key == name && regex == null)
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
            // Creating sub directory in case it doesn't exist already.
            .subDirectory("${hash[0]}/${hash[0]}${hash[1]}", true)
            // Creating destination file.
            .subFile(hash)

        // If the destination file already exits we abort renaming it.
        if (!destination.createNewFile())
            return destination.exists()

        return file.renameTo(destination)
    }


    // Getters

    /**
     * Returns a resource from the loaded resources as the inferred type.
     *
     * @see AssetBundle.SUPPORTED_TYPES
     */
    inline operator fun <reified T : Any> get(
        key: String?,
        variant: Int = 0,
        fallbackToDefault: Boolean = true
    ): T?
    {
        if (key == null)
            return null

        for (source in ResourceProvider.entries)
        {
            if (source == BEATMAP && !useBeatmapSkin)
                continue

            if (source == DEFAULT && !fallbackToDefault)
                break

            return source[ctx]?.get(key, variant) ?: continue
        }
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
        for (source in ResourceProvider.entries)
        {
            if (source == BEATMAP && !useBeatmapSkin)
                continue

            if (source == DEFAULT && !fallbackToDefault)
                break

            return source[ctx]?.get(key) ?: continue
        }
        return null
    }


    companion object
    {
        /**
         * The resource directory sub-path.
         */
        const val RESOURCES_PATH = "resources/"
    }
}


enum class ResourceProvider(private val bundleProvider: RimuContext.() -> AssetBundle?)
{
    BEATMAP({ beatmaps.current?.assets }),

    SKIN({ skins.current.assets }),

    DEFAULT({ skins.default.assets });


    operator fun get(ctx: RimuContext) = bundleProvider(ctx)

}