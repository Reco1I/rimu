package com.osudroid.ui.v2.beatmaplisting

import com.osudroid.beatmaplisting.BeatmapDownloader
import com.osudroid.beatmaplisting.BeatmapMirror
import com.osudroid.beatmaplisting.BeatmapSetModel
import com.osudroid.settings.GameFolders
import com.osudroid.settings.OsuSettings
import com.osudroid.ui.v2.CachedTextures
import com.osudroid.utils.async
import com.osudroid.utils.updateThread
import com.reco1l.verktex.Platform
import com.reco1l.verktex.graphics.Texture
import com.reco1l.verktex.settings.Settings
import com.reco1l.verktex.texture.Textures
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import okio.Path.Companion.toPath
import ru.nsu.ccfit.zuev.osu.helper.MD5Calculator
import ru.nsu.ccfit.zuev.osu.online.OnlineFileOperator

/**
 * What the cards and the details of the listing show besides their set, and what they do. It is read and
 * changed on the update thread, and [onChange] tells the listing to refresh what shows it.
 */
open class BeatmapListingState {

    /**
     * The set whose preview plays or is being loaded.
     */
    var playingSetId: Long? = null
        set(value) {
            if (field != value) {
                field = value
                onChange?.invoke()
            }
        }

    /**
     * The download in progress.
     */
    var download: BeatmapDownloader.Download? = null
        set(value) {
            if (field != value) {
                field = value
                onChange?.invoke()
            }
        }

    /**
     * The sets that are in the library.
     */
    var downloadedSetIds: Set<Long> = emptySet()
        set(value) {
            if (field != value) {
                field = value
                onChange?.invoke()
            }
        }

    /**
     * The set the details are shown of.
     */
    var detailsSetId: Long? = null

    /**
     * Called when something the cards show changed.
     */
    var onChange: (() -> Unit)? = null


    /**
     * Whether the set can be downloaded without its video from the selected mirror.
     */
    open fun canDownloadWithoutVideo(set: BeatmapSetModel) = set.hasVideo && BeatmapMirror.selected.supportsNoVideoDownloads

    /**
     * Plays or stops the preview of the set.
     */
    open fun togglePreview(set: BeatmapSetModel) = Unit

    /**
     * Downloads the set and imports it.
     */
    open fun download(set: BeatmapSetModel, withVideo: Boolean) = Unit

    /**
     * Shows the details of the set.
     */
    open fun showDetails(set: BeatmapSetModel) = Unit

    /**
     * Whether a card of the listing shows the set.
     */
    open fun isShownInListing(setId: Long) = false


    /**
     * The download of the set, null if it isn't being downloaded.
     */
    fun downloadOf(set: BeatmapSetModel) = download?.takeIf { it.beatmapSetId == set.id }
}


/**
 * The title of the set, romanized when the setting asks for it.
 */
val BeatmapSetModel.displayTitle: String
    get() = if (Settings[OsuSettings.ForceRomanized] || titleUnicode.isBlank()) title else titleUnicode

/**
 * The artist of the set, romanized when the setting asks for it.
 */
val BeatmapSetModel.displayArtist: String
    get() = if (Settings[OsuSettings.ForceRomanized] || artistUnicode.isBlank()) artist else artistUnicode

/**
 * The name the archive of the set is saved as.
 */
fun BeatmapSetModel.archiveName(withVideo: Boolean) = "$id $artist - $title" + if (withVideo) "" else " [no video]"


/**
 * Loads the covers of the sets, downloaded once to the cache folder and loaded as [CachedTextures].
 */
object BeatmapCovers {

    /**
     * The cover at [url] if it's loaded.
     */
    fun loaded(url: String): Texture.Region? = CachedTextures.store[CachedTextures.keyOf(url)]

    /**
     * Downloads the cover at [url] if it isn't cached and loads it, giving it to [onLoaded] on the update
     * thread, or null if it couldn't be loaded. Cancelling the job before skips it.
     */
    fun load(url: String, onLoaded: (Texture.Region?) -> Unit): Job = async {
        val file = GameFolders.cachePath.toPath() / MD5Calculator.getStringMD5(url)

        if (!Platform.fileSystem.exists(file)) {
            OnlineFileOperator.downloadFile(url, file.toString())
        }
        ensureActive()

        val job = coroutineContext[Job]

        updateThread {
            if (job?.isCancelled != true) {
                onLoaded(if (Platform.fileSystem.exists(file)) CachedTextures[url] else null)
            }
        }
    }

    /**
     * Unloads the cover at [url]. Nothing may show it anymore.
     */
    fun unload(url: String) {
        if (loaded(url) != null) {
            CachedTextures.store.unload(CachedTextures.keyOf(url))
        }
    }
}
