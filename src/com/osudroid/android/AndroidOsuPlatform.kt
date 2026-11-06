package com.osudroid.android

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import androidx.core.os.LocaleListCompat
import com.acivev.VibratorManager
import com.acivev.utils.DevicePerformanceUtil
import com.osudroid.UpdateManager
import com.osudroid.beatmaplisting.BeatmapDownloader
import com.osudroid.beatmaplisting.BeatmapListing
import com.osudroid.ui.v1.BlockAreaEditorFragment
import com.osudroid.ui.v1.LoadingBadgeFragment
import com.osudroid.utils.mainThread
import com.reco1l.verktex.android.isVulkanAvailable
import com.reco1l.verktex.android.resetVulkanCrashGuard
import ru.nsu.ccfit.zuev.osu.AndroidSecurityUtils
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.ConfigBackup
import java.io.File
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import androidx.preference.PreferenceManager
import com.google.firebase.messaging.FirebaseMessaging
import com.osudroid.OsuPlatform
import com.reco1l.verktex.Logger
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.MainActivity
import com.russhwolf.settings.SharedPreferencesSettings
import ru.nsu.ccfit.zuev.osuplus.BuildConfig

/**
 * The game's [OsuPlatform] on Android.
 */
class AndroidOsuPlatform(private val activity: Activity) : OsuPlatform.Implementation {

    override val buildType: String = BuildConfig.BUILD_TYPE

    override val versionName: String = BuildConfig.VERSION_NAME

    override val isDebug: Boolean = BuildConfig.DEBUG

    // Committed right away as the preferences always were.
    override val preferences = SharedPreferencesSettings(PreferenceManager.getDefaultSharedPreferences(activity), commit = true)

    override val defaultCorePath: String
        get() = Environment.getExternalStorageDirectory().toString() + "/osu!droid/"

    override val cachePath: String
        get() = activity.cacheDir.path

    override val displayWidth: Int
        get() = realDisplayMetrics().widthPixels

    override val displayHeight: Int
        get() = realDisplayMetrics().heightPixels


    private fun realDisplayMetrics() = DisplayMetrics().also {
        @Suppress("DEPRECATION")
        activity.windowManager.defaultDisplay.getRealMetrics(it)
    }

    override fun setAnnouncementsSubscribed(subscribed: Boolean) {
        if (subscribed) {
            FirebaseMessaging.getInstance().subscribeToTopic("announcements")
        } else {
            FirebaseMessaging.getInstance().unsubscribeFromTopic("announcements")
        }
    }

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    override fun runOnMainThread(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
    }

    override fun handleUncaughtException(throwable: Throwable) {
        // Without the main activity (e.g. in the UI test browser) a task may reach something only the
        // game sets up, which must not take the whole process down.
        if (GlobalManager.getInstance().isHeadless) {
            Logger.e("Execution", "Uncaught exception in a task while headless", throwable)
        } else {
            Thread.currentThread().let { it.uncaughtExceptionHandler?.uncaughtException(it, throwable) ?: throw throwable }
        }
    }

    override val isInForeground: Boolean
        get() = MainActivity.isActivityVisible()

    override fun logAnalyticsEvent(name: String, parameters: Map<String, String>) {
        val bundle = android.os.Bundle()
        parameters.forEach { (key, value) -> bundle.putString(key, value) }
        com.google.firebase.analytics.FirebaseAnalytics.getInstance(activity).logEvent(name, bundle)
    }


    /**
     * The game's activity, or null when it runs without it (e.g. in the UI test browser).
     */
    private val mainActivity
        get() = activity as? MainActivity

    override fun loadBeatmapLibrary() {
        mainActivity?.loadBeatmapLibrary()
    }

    override fun checkNewSkins() {
        mainActivity?.checkNewSkins()
    }

    override fun exitGame() {
        mainActivity?.finish()
    }

    override val hasBeatmapListing = true

    override fun showBeatmapListing() {
        mainThread { BeatmapListing().show() }
    }

    override fun downloadBeatmapSet(setId: Long, filename: String) {
        val url = BeatmapListing.mirror.download.request(setId, !Config.isPreferNoVideoDownloads()).toString()
        BeatmapDownloader.download(url, filename)
    }

    override val canImportReplays = true

    override fun pickAndImportReplays() {
        com.osudroid.ui.v2.settings.pickAndImportReplays()
    }

    override val hasBlockAreaEditor = true

    override fun showBlockAreaEditor() {
        mainThread { BlockAreaEditorFragment().show() }
    }

    override fun showProgressBadge(header: String, message: String): OsuPlatform.ProgressBadge {
        val badge = LoadingBadgeFragment().apply {
            this.header = header
            this.message = message
        }
        mainThread { badge.show() }

        return object : OsuPlatform.ProgressBadge {
            override var header by badge::header
            override var message by badge::message
            override var isIndeterminate by badge::isIndeterminate
            override var progress by badge::progress

            override fun dismiss() = badge.dismiss()
        }
    }

    override fun createVideo(path: String) = ExoPlayerVideo(path)

    override val vibrations: OsuPlatform.GameplayVibrations
        get() = VibratorManager

    override fun verifyFileIntegrity() = AndroidSecurityUtils.verifyFileIntegrity(activity)

    override val canCheckForUpdates = true

    override fun checkForUpdates(silently: Boolean) = UpdateManager.checkNewUpdates(silently)

    override val canBackUpPreferences = true

    override fun exportPreferences() = ConfigBackup.exportPreferences()

    override fun importPreferences() = ConfigBackup.importPreferences()

    override val canChooseLanguage = true

    override fun setLanguage(tag: String) {
        mainThread {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))

            // Headless there's no game to restart.
            val mainActivity = mainActivity ?: return@mainThread
            val intent = Intent(mainActivity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            mainActivity.startActivity(intent)
        }
    }

    override fun copyToClipboard(label: String, text: String) {
        activity.getSystemService<ClipboardManager>()?.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    override fun openChangelog() {
        val changelogFile = File(activity.cacheDir, "changelog.html")

        activity.assets.open("app/changelog.html").use { input ->
            changelogFile.outputStream().use { input.copyTo(it) }
        }

        val changelogUri = FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".fileProvider", changelogFile)

        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(changelogUri, "text/html")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        activity.startActivity(intent)
    }

    override val isLowEndDevice
        get() = DevicePerformanceUtil.isLowEndDevice(activity)

    // Obtaining supported refresh rates is only available on Android 12 and above.
    // See https://developer.android.com/reference/android/view/Display.Mode#getAlternativeRefreshRates().
    override val canForceMaxRefreshRate = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    override fun applyRefreshRateSetting(enabled: Boolean) {
        mainActivity?.applyRefreshRateSetting(enabled)
    }

    override val isVulkanAvailable
        get() = isVulkanAvailable(activity)

    override fun resetVulkanCrashGuard() = resetVulkanCrashGuard(activity)
}
