package com.reco1l.rimu.management.skin

import com.reco1l.toolkt.data.md5
import com.reco1l.toolkt.kotlin.orCatch
import com.reco1l.skindecoder.SkinMapper
import com.reco1l.skindecoder.data.SkinData
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.R
import com.reco1l.rimu.data.asset.HashableAsset
import com.reco1l.rimu.data.asset.Asset
import com.reco1l.rimu.data.Skin
import com.reco1l.rimu.data.asset.AssetType
import com.reco1l.rimu.management.resources.BaseImporter
import com.reco1l.rimu.management.resources.ImportTask
import com.reco1l.rimu.ui.layouts.ProcessNotification
import okio.IOException
import java.io.File
import java.lang.Exception

/**
 * Responsible of manage all [SkinImportTask] into a queue.
 */
class SkinImporter(ctx: MainContext) : BaseImporter(ctx)
{

    override fun onCreateTask(folder: File) = SkinImportTask(ctx, folder)

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
            null -> ctx.getString(R.string.detail_skin_import_success, name)

            // Fail
            else -> ctx.getString(R.string.detail_skin_import_failed, name,
                "${exception.javaClass.simpleName} - ${exception.message}")
        }
        task.notification.showIndicator = false
        task.notification.update(ctx)
    }
}

/**
 * The assigned [ImportTask] for the beatmap folder that is about to be imported.
 */
class SkinImportTask internal constructor(ctx: MainContext, root: File) : ImportTask(ctx, root)
{

    override val notification = ProcessNotification(
        header = ctx.getString(R.string.header_skin_importer),
        message = ctx.getString(R.string.detail_skin_importing, root.nameWithoutExtension),
        icon = "icon-notification"
    )


    override var parentKey: String? = null
        get()
        {
            // If the parent is null at this point we're using the folder name.
            if (field == null)
                field = root.nameWithoutExtension

            return field
        }


    private val decoder = SkinMapper()

    private var data = SkinData()


    override fun onFilterFiles() = { file: File ->

        when
        {
            // Filtering mode files first
            file.name.equals("skin.ini", true) -> 0

            // Directories after the above condition because of dependencies
            file.isDirectory -> 1

            // Everything else
            else -> 2
        }
    }

    private fun onManageDependencies(data: SkinData, dependencies: MutableList<String>)
    {
        data.fonts.apply {

            fun addDependenciesFrom(prefix: String)
            {
                // Usage for combo, score and hit circle prefixes
                for (i in 0..9)
                    dependencies.add("$prefix-$i")

                // Usage for score and combo prefixes
                dependencies.add("$prefix-comma")
                dependencies.add("$prefix-dot")
                dependencies.add("$prefix-percent")
                dependencies.add("$prefix-x")
            }

            addDependenciesFrom(hitCirclePrefix)
            addDependenciesFrom(scorePrefix)
            addDependenciesFrom(comboPrefix)
        }
    }

    override fun isManagementRequired(fileExtension: String) = fileExtension in AssetType.CONFIGURATION

    override fun onManageFile(file: File, dependencies: MutableList<String>): HashableAsset?
    {
        // skin.ini
        if (file.name.equals("skin.ini", true))
        {
            data = decoder.decode(file).apply {

                if (general.name != null)
                    parentKey = general.name
            }

            onManageDependencies(data, dependencies)

            return Asset(file.md5, parentKey!!, file.nameWithoutExtension, 0, file.extension)
        }
        return null
    }

    override fun onInsertAsset(file: File, asset: HashableAsset) = when (asset)
    {
        // Inserting in the asset database, if it returns -1 means the asset already exists on
        // the database.
        is Asset -> { { ctx.database.assetTable.insert(asset) > 0 }.orCatch { false } }

        // This shouldn't never happen.
        else -> false
    }

    override fun onFinish()
    {
        super.onFinish()

        // At this point the parent key shouldn't be null.
        Skin(parentKey!!, data.general.author).also {

            // Inserting Skin entity into the database.
            ctx.database.skinTable.insert(it)
        }
    }
}


class EmptySkinException : IOException()