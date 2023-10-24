package game.rimu.management.beatmap

import com.reco1l.framework.lang.addIfNotNull
import com.reco1l.framework.data.extensionLowercase
import com.reco1l.framework.data.isExtension
import com.reco1l.framework.data.md5
import com.reco1l.framework.lang.orCatch
import com.rian.osu.beatmap.BeatmapData
import com.rian.osu.beatmap.parser.BeatmapDecoder
import game.rimu.MainContext
import game.rimu.data.asset.HashableAsset
import game.rimu.data.asset.Asset
import game.rimu.data.Beatmap
import game.rimu.management.resources.BaseImporter
import game.rimu.management.resources.ImportTask
import okio.IOException
import java.io.File

/**
 * Responsible of manage all [BeatmapImportTask] into a queue.
 */
class BeatmapImporter(ctx: MainContext) : BaseImporter(ctx)
{

    override fun onCreateTask(folder: File) = BeatmapImportTask(ctx, folder)

}

/**
 * The assigned [ImportTask] for the beatmap folder that is about to be imported.
 */
class BeatmapImportTask internal constructor(ctx: MainContext, root: File) : ImportTask(ctx, root)
{

    override val requiresManagementFiletypes = Asset.MODE_FORMATS


    override var parentKey: String? = null
        get()
        {
            // If the parent is null at this point we're using the folder MD5.
            if (field == null)
                field = root.md5

            return field
        }


    // The beatmap decoder that'll used along with this task.
    private val decoder = BeatmapDecoder()


    override fun onFilterFiles() = { file: File ->

        when
        {
            // Filtering mode files first
            file.extensionLowercase in Asset.MODE_FORMATS -> 0

            // Directories after the above condition because of dependencies
            file.isDirectory -> 1

            // Everything else
            else -> 2
        }
    }


    private fun onManageDependencies(data: BeatmapData, dependencies: MutableList<String>)
    {
        data.general.apply {

            dependencies.add(audioFilename)
        }

        data.events.apply {

            dependencies.addIfNotNull(videoFilename)
            dependencies.addIfNotNull(backgroundFilename)
        }
    }


    override fun onComputeFile(file: File, dependencies: MutableList<String>): HashableAsset?
    {
        // osu!std mode
        if (file.isExtension("osu"))
        {
            val data = decoder.decode(file, false)

            data.metadata.apply {

                if (beatmapSetID > 0)
                    parentKey = beatmapSetID.toString()
            }

            // Adding beatmap resources to the dependencies list.
            onManageDependencies(data, dependencies)

            return Beatmap.from(data, parentKey!!)
        }

        return null
    }

    override fun onInsertAsset(file: File, asset: HashableAsset) = when (asset)
    {
        // Inserting in the beatmap database, in this case the returning Int from insertBeatmap()
        // will always be greater than 0 because its conflict strategy is REPLACE.
        is Beatmap -> { { ctx.database.beatmapTable.insert(asset) > 0 }.orCatch { false } }

        else -> super.onInsertAsset(file, asset)
    }
}


// Exceptions

class EmptyBeatmapException : IOException()