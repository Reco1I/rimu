package com.reco1l.rimu.management.beatmap

import com.reco1l.framework.kotlin.addIfNotNull
import com.reco1l.framework.data.extensionLowercase
import com.reco1l.framework.data.isExtension
import com.reco1l.framework.data.md5
import com.reco1l.framework.kotlin.orCatch
import com.rian.osu.beatmap.BeatmapData
import com.rian.osu.beatmap.parser.BeatmapDecoder
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.R.string.detail_beatmap_import_failed
import com.reco1l.rimu.R.string.detail_beatmap_import_success
import com.reco1l.rimu.R.string.detail_beatmap_importing
import com.reco1l.rimu.R.string.header_beatmap_importer
import com.reco1l.rimu.data.asset.HashableAsset
import com.reco1l.rimu.data.Beatmap
import com.reco1l.rimu.data.asset.AssetType
import com.reco1l.rimu.management.resources.BaseImporter
import com.reco1l.rimu.management.resources.ImportTask
import com.reco1l.rimu.ui.layouts.ProcessNotification
import okio.IOException
import java.io.File
import java.lang.Exception

/**
 * Responsible of manage all [BeatmapImportTask] into a queue.
 */
class BeatmapImporter(ctx: MainContext) : BaseImporter(ctx)
{

    override fun onCreateTask(folder: File) = BeatmapImportTask(ctx, folder)


    override fun onTaskStart(task: ImportTask)
    {
        task.notification.show(ctx)
    }

    override fun onTaskEnd(task: ImportTask, exception: Exception?)
    {
        val name = task.root.nameWithoutExtension

        task.notification.message = when (exception)
        {
            // Success
            null -> ctx.getString(detail_beatmap_import_success, name)

            // Fail
            else -> ctx.getString(detail_beatmap_import_failed, name,
                "${exception.javaClass.simpleName} - ${exception.message}")
        }
        task.notification.showIndicator = false
        task.notification.update(ctx)
    }
}

/**
 * The assigned [ImportTask] for the beatmap folder that is about to be imported.
 */
class BeatmapImportTask internal constructor(ctx: MainContext, root: File) : ImportTask(ctx, root)
{

    override val notification = ProcessNotification(
        header = ctx.getString(header_beatmap_importer),
        message = ctx.getString(detail_beatmap_importing, root.nameWithoutExtension),
        icon = "icon-notification"
    )


    override var parentKey: String? = null
        get()
        {
            // If the parent is null at this point we're using the folder MD5.
            if (field == null)
                field = root.md5

            return field
        }


    private val decoder = BeatmapDecoder()


    override fun onFilterFiles() = { file: File ->

        when
        {
            // Filtering mode files first
            file.extensionLowercase in AssetType.BEATMAP -> 0

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


    override fun isManagementRequired(fileExtension: String) = fileExtension in AssetType.BEATMAP

    override fun onManageFile(file: File, dependencies: MutableList<String>): HashableAsset?
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
        is Beatmap ->
        {
            { ctx.database.beatmapTable.insert(asset) > 0 }.orCatch { false }
        }

        else -> super.onInsertAsset(file, asset)
    }
}


// Exceptions

class EmptyBeatmapException : IOException()