package game.rimu.data.asset

import game.rimu.android.IWithContext
import game.rimu.android.RimuContext
import java.io.File

/**
 * A hashable asset indicates a physically asset which is identified by its [hash].
 *
 * Note: Not all hashable assets have their [hash] as it unique primary key.
 */
abstract class HashableAsset
{

    abstract val hash: String

    /**
     * Returns the corresponding qualified path (without root directory) determined by its [hash].
     * The qualified path is a composition from the hash characters:
     *
     * * The 1st section of the path equals to the first character of the [hash]
     * * The 2nd section equals to the first two digits of the [hash]
     * * Finally the last section which is the filename equals to the [hash]
     *
     * Format: `{first hash character}/{first two hash characters}/{full hash}`
     *
     * In order to get the absolute file path you need prepend the current resources directory to the
     * [qualified path][qualifiedPath].
     */
    val qualifiedPath
        get() = "${hash[0]}/${hash[0]}${hash[1]}/$hash"


    /**
     * Returns a [File] instance of this asset.
     *
     * Warning: Don't use this for internal assets, it'll return a non-existent file. Internal assets
     * can only be get via an InputStream.
     */
    fun toFile(app: RimuContext) = File(app.resources.directory, qualifiedPath)
}