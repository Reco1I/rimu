package com.osudroid.ui.v2.settings

import com.osudroid.replay.ReplayImporter
import com.osudroid.resources.Res
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Platform
import com.reco1l.verktex.ui.toast.Toasts
import okio.Path

/**
 * Asks the user for replay files or replay packs and imports them.
 */
fun pickAndImportReplays() {
    Platform.pickFiles("application/octet-stream", ::importReplays)
}

// Called on a background thread with copies of the picked files, deleted once imported.
private fun importReplays(files: List<Path>) {
    val progress = Toasts.showPersistent(Res.string.replay_import_result.format(0, files.size), isIndeterminate = true)
    var importedCount = 0

    for ((i, file) in files.withIndex()) {
        try {
            ReplayImporter.import(file.toString())
            importedCount++
        } catch (e: Exception) {
            Logger.e("ReplayImport", "Failed to import the replay $file", e)
            Toasts.show(Res.string.replay_import_error.format("#${i + 1}", e.message ?: "Unknown error"))
        } finally {
            Platform.fileSystem.delete(file, mustExist = false)
        }

        progress.text = Res.string.replay_import_result.format(importedCount, files.size)
    }

    progress.dismiss()
    Toasts.showLong(Res.string.replay_import_result.format(importedCount, files.size))
}
