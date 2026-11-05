package com.osudroid.ui.v2.settings

import android.net.Uri
import com.reco1l.verktex.Logger
import androidx.activity.result.contract.ActivityResultContracts
import com.edlplan.ui.fragment.LoadingFragment
import com.google.android.material.snackbar.Snackbar
import com.osudroid.resources.R
import com.osudroid.utils.async
import com.osudroid.utils.mainThread
import com.osudroid.replay.ReplayImporter
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import java.io.File

/**
 * Asks the user for replay files and imports them.
 */
fun pickAndImportReplays() = mainThread {
    val activity = GlobalManager.getInstance().mainActivity

    // Registered here since scenes aren't tied to the activity lifecycle.
    val launcher = activity.activityResultRegistry.register(
        "importReplay",
        ActivityResultContracts.GetMultipleContents()
    ) { uris -> importReplays(uris) }

    launcher.launch("application/octet-stream")
}

private fun importReplays(uris: List<Uri>) {
    val loading = LoadingFragment()
    loading.show()

    async {
        val context = GlobalManager.getInstance().mainActivity
        val decorView = context.window.decorView
        var importedCount = 0
        var tempFile: File? = null

        for (uri in uris) {
            try {
                tempFile = File.createTempFile("importedReplay", null, context.externalCacheDir)

                context.contentResolver.openInputStream(uri)!!.use { input ->
                    tempFile!!.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                ReplayImporter.import(tempFile)
                importedCount++
            } catch (e: Exception) {
                Logger.e("ReplayImport", "Failed to import replay from $uri", e)

                mainThread {
                    Snackbar.make(
                        decorView,
                        StringTable.format(
                            R.string.replay_import_error,
                            uri.path ?: uri.toString(),
                            e.message ?: "Unknown error"
                        ),
                        3000
                    ).show()
                }
            } finally {
                tempFile?.delete()
                tempFile = null
            }
        }

        mainThread {
            loading.dismiss()

            Snackbar.make(
                decorView,
                StringTable.format(R.string.replay_import_result, importedCount, uris.size),
                5000
            ).show()
        }
    }
}
