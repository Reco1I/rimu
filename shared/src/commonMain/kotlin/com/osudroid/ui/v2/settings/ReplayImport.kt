package com.osudroid.ui.v2.settings

import com.osudroid.replay.ReplayImporter
import com.osudroid.resources.Res
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Platform
import com.osudroid.notifications.Notifications
import okio.Path
import com.osudroid.utils.async

/**
 * Asks the user for replay files and imports them.
 */
fun pickAndImportReplays() {
    Platform.pickFiles("application/octet-stream") { files -> async { importReplays(files) } }
}

// Called in a coroutine with copies of the picked files, deleted once imported.
private suspend fun importReplays(files: List<Path>) {
    val progress = Notifications.startTask(Res.string.replay_import_title, Res.string.replay_import_result.format(0, files.size), isIndeterminate = false)
    var importedCount = 0

    for ((i, file) in files.withIndex()) {
        try {
            ReplayImporter.import(file)
            importedCount++
        } catch (e: Exception) {
            Logger.e("ReplayImport", "Failed to import the replay $file", e)
            Notifications.post(Res.string.replay_import_title, Res.string.replay_import_error.format("#${i + 1}", e.message ?: "Unknown error"), isError = true)
        } finally {
            Platform.fileSystem.delete(file, mustExist = false)
        }

        progress.text = Res.string.replay_import_result.format(importedCount, files.size)
        progress.progress = (i + 1f) / files.size
    }

    progress.complete(Res.string.replay_import_result.format(importedCount, files.size))
}
