package game.rimu.management.resources

import com.reco1l.framework.data.extensionLowercase
import com.reco1l.framework.data.forEachRecursive
import com.reco1l.framework.data.md5
import com.reco1l.framework.data.subDirectory
import com.reco1l.framework.lang.orCatch
import game.rimu.android.IWithContext
import game.rimu.android.RimuContext
import game.rimu.data.asset.Asset
import game.rimu.data.asset.HashableAsset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.lingala.zip4j.ZipFile
import java.io.File

abstract class BaseImporter(override val ctx: RimuContext) : IWithContext
{

    /**
     * The coroutine scope for the import process.
     */
    protected val importScope = CoroutineScope(Dispatchers.IO)

    /**
     * Here a new instance of your custom [ImportTask] should be initialized.
     */
    protected abstract fun onCreateTask(folder: File): ImportTask

    /**
     * Import from a folder.
     */
    fun import(folder: File) = onCreateTask(folder).apply {

        // Starting the task in the import coroutine.
        importScope.launch { start() }
    }

}

abstract class ImportTask(override val ctx: RimuContext, protected var root: File) : IWithContext
{

    /**
     * This should contain a list of file types that should be specially handled.
     */
    protected abstract val requiresManagementFiletypes: Array<String>

    /**
     * This should contain the parent identifier that assets will uses as [parent key][Asset.parent].
     */
    protected abstract var parentKey: String?


    private var wrappingZip: ZipFile? = null

    private var isValidTask = true

    private var isCancelled = false


    internal fun start()
    {
        if (!isValidTask)
            return

        isValidTask = false

        // If it's not a directory it can be a ZIP file, in order to operate with it we need to
        // extract it first.
        if (root.isFile)
        {
            wrappingZip = ZipFile(root)

            // The temporal folder will be placed in the cache directory and we'll deleted once the
            // import task ends.
            root = ctx.cacheDir.subDirectory(root.nameWithoutExtension, true)

            wrappingZip!!.extractAll(root.path)
        }

        // Storing files and assets in a map before inserting the asset into the database and before
        // moving the file into the internal filesystem, this will prevent files and assets being
        // added after premature cancellation in the process.
        val pending = mutableMapOf<File, HashableAsset>()

        // Listing dependency file paths that should be imported along with the beatmap/skin.
        val dependencies = mutableListOf<String>()

        // Iterating recursively in case the folder contains sub-folders.
        root.forEachRecursive(*Asset.SUPPORTED_FORMATS, selector = onFilterFiles()) { file ->

            if (isCancelled)
                return@forEachRecursive

            if (file.extensionLowercase in requiresManagementFiletypes)
            {
                // Computing the file, if this returns an HashableAsset we're adding it to the pending
                // files list to later import it.
                onComputeFile(file, dependencies)?.let { pending[file] = it }
                return@forEachRecursive
            }

            // Getting the relative filepath from root, this is used to check if the file path corresponds
            // to a dependency.
            val relativePath = file.toRelativeString(root)

            // Resolving resource key and variant number, if the file path corresponds to a dependency
            // its filename without extension is set as key and its variant will always be 0
            val pair = if (relativePath in dependencies)
                file.nameWithoutExtension to 0
            else
                ctx.resources.resolveAsset(file.name) ?: return@forEachRecursive

            // Adding to the pending files list.
            pending[file] = Asset(file.md5, parentKey!!, pair.first, pair.second)
        }

        if (isCancelled)
            return

        // Adding all registered assets.
        pending.forEach { (file, asset) ->

            // Moving it to the resource directory, we abort inserting the asset into the database
            // if for whatever reason it fails in the process.
            if (ctx.resources.storeResource(file, asset.hash))
                onInsertAsset(file, asset)
        }

        onFinish()
    }

    internal fun cancel()
    {
        isCancelled = true
    }

    /**
     * Called when the folder and its sub-folders are about to being iterated, this should filter
     * and order files by their extensions.
     *
     * This should be used to prioritize some files from others.
     */
    internal abstract fun onFilterFiles(): (File) -> Int

    /**
     * Called when a file is being computed/iterated from the files listing.
     *
     * This should return its [HashableAsset] entity to be inserted into the database along with the
     * file being moved to the resource directory, if `null` is returned, the file will be skipped.
     */
    internal abstract fun onComputeFile(
        file: File,
        dependencies: MutableList<String>
    ): HashableAsset?

    /**
     * Called when an asset is about to be moved to the resource directory and its hashable should be
     * inserted into the database.
     *
     * This should return `true` in case the file should be moved or `false` to skip the operation.
     */
    internal open fun onInsertAsset(file: File, asset: HashableAsset) = when (asset)
    {
        // Inserting in the asset database, if it returns -1 means the asset already exists on
        // the database.
        is Asset -> { { ctx.database.insertAsset(asset) > 0 }.orCatch { false } }

        // This shouldn't never happen.
        else -> false
    }

    protected open fun onFinish()
    {
        // Cleaning original file.
        wrappingZip?.file?.delete()

        // Clearing temporal folder.
        root.delete()
    }

}