package com.osudroid.android

import com.reco1l.verktex.Platform
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.messaging.FirebaseMessaging
import com.osudroid.OsuGameHost
import com.osudroid.UpdateManager
import com.osudroid.beatmaplisting.BeatmapDownloader
import com.osudroid.beatmaplisting.BeatmapMirror
import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.android.AndroidGameHost
import com.reco1l.verktex.settings.Settings
import java.io.File
import ru.nsu.ccfit.zuev.osu.AndroidSecurityUtils

/**
 * The game's host on Android: the activity running the engine, with the features of the game only
 * Android has. `MainActivity` extends it with the game's startup and its library and skin imports.
 */
abstract class AndroidOsuGameHost : AndroidGameHost(), OsuGameHost {

    /**
     * The version code of the application.
     */
    abstract val versionCode: Long

    /**
     * Ends the process, without going through the game's exit.
     */
    abstract fun forcedExit()

    override val buildType: String
        get() = AppBuild.buildType

    // The default preferences of the application, where the game kept its settings before.
    override fun legacyPreferences(): Map<String, Any?> =
        getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE).all

    override fun setAnnouncementsSubscribed(subscribed: Boolean) {
        if (subscribed) {
            FirebaseMessaging.getInstance().subscribeToTopic("announcements")
        } else {
            FirebaseMessaging.getInstance().unsubscribeFromTopic("announcements")
        }
    }

    override fun logAnalyticsEvent(name: String, parameters: Map<String, String>) {
        val bundle = android.os.Bundle()
        parameters.forEach { (key, value) -> bundle.putString(key, value) }
        FirebaseAnalytics.getInstance(this).logEvent(name, bundle)
    }

    override fun checkSkinTexture(path: String, optional: Boolean) {
        if (optional) {
            com.osudroid.ui.skinning.ensureOptionalTexture(File(path))
        } else {
            com.osudroid.ui.skinning.ensureTexture(File(path))
        }
    }

    override val canDownloadBeatmapSets: Boolean
        get() = true

    override fun downloadBeatmapSet(setId: Long, filename: String) {
        val url = BeatmapMirror.selected.download.request(setId, !Settings[OsuSettings.PreferNoVideoDownloads]).toString()
        BeatmapDownloader.download(url, filename)
    }

    override val canImportReplays: Boolean
        get() = Platform.canPickFiles

    override fun pickAndImportReplays() {
        com.osudroid.ui.v2.settings.pickAndImportReplays()
    }


    override fun verifyFileIntegrity() = AndroidSecurityUtils.verifyFileIntegrity(this)

    override val canCheckForUpdates: Boolean
        get() = true

    override fun checkForUpdates(silently: Boolean) = UpdateManager.checkNewUpdates(silently)

    override fun openChangelog() {
        val changelogFile = File(cacheDir, "changelog.html")

        assets.open("app/changelog.html").use { input ->
            changelogFile.outputStream().use { input.copyTo(it) }
        }

        val changelogUri = FileProvider.getUriForFile(this, AppBuild.applicationId + ".fileProvider", changelogFile)

        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(changelogUri, "text/html")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(intent)
    }
}
