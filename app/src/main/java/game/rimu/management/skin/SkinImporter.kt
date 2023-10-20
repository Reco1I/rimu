package game.rimu.management.skin

import com.reco1l.framework.data.md5
import com.reco1l.framework.lang.orCatch
import com.reco1l.skindecoder.SkinDecoder
import com.reco1l.skindecoder.data.SkinData
import game.rimu.android.RimuContext
import game.rimu.data.asset.HashableAsset
import game.rimu.data.asset.Asset
import game.rimu.data.Skin
import game.rimu.management.resources.BaseImporter
import game.rimu.management.resources.ImportTask
import okio.IOException
import java.io.File

/**
 * Responsible of manage all [SkinImportTask] into a queue.
 */
class SkinImporter(ctx: RimuContext) : BaseImporter(ctx)
{

    override fun onCreateTask(folder: File) = SkinImportTask(ctx, folder)

}

/**
 * The assigned [ImportTask] for the beatmap folder that is about to be imported.
 */
class SkinImportTask internal constructor(ctx: RimuContext, root: File) : ImportTask(ctx, root)
{

    override val requiresManagementFiletypes = arrayOf("ini")

    override var parentKey: String? = null
        get()
        {
            // If the parent is null at this point we're using the folder name.
            if (field == null)
                field = root.nameWithoutExtension

            return field
        }


    private val decoder = SkinDecoder()

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

    override fun onComputeFile(file: File, dependencies: MutableList<String>): HashableAsset?
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
        is Asset -> { { ctx.database.assetTable.insertAsset(asset) > 0 }.orCatch { false } }

        // This shouldn't never happen.
        else -> false
    }

    override fun onFinish()
    {
        super.onFinish()

        // At this point the parent key shouldn't be null.
        Skin(parentKey!!, data.general.author).also {

            // Inserting Skin entity into the database.
            ctx.database.skinTable.insertSkin(it)
        }
    }
}


class EmptySkinException : IOException()