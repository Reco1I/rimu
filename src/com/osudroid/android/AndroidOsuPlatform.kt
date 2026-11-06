package com.osudroid.android

import android.content.Context
import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.settings.Settings
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import com.acivev.VibratorManager
import com.acivev.utils.DevicePerformanceUtil
import com.osudroid.UpdateManager
import com.osudroid.beatmaplisting.BeatmapDownloader
import com.osudroid.beatmaplisting.BeatmapListing
import com.osudroid.ui.v1.BlockAreaEditorFragment
import com.osudroid.utils.mainThread
import com.reco1l.verktex.android.isVulkanAvailable
import com.reco1l.verktex.android.resetVulkanCrashGuard
import ru.nsu.ccfit.zuev.osu.AndroidSecurityUtils
import java.io.File
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import com.google.firebase.messaging.FirebaseMessaging
import com.osudroid.OsuPlatform
import com.reco1l.verktex.Logger
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.MainActivity
import ru.nsu.ccfit.zuev.osuplus.BuildConfig

/**
 * The game's [OsuPlatform] on Android.
 */
class AndroidOsuPlatform(private val activity: Activity) : OsuPlatform.Implementation {

    override val buildType: String = BuildConfig.BUILD_TYPE

    override val versionName: String = BuildConfig.VERSION_NAME

    override val isDebug: Boolean = BuildConfig.DEBUG

    // The default preferences of the application, where the game kept its settings before.
    override fun legacyPreferences(): Map<String, Any?> =
        activity.getSharedPreferences(activity.packageName + "_preferences", Context.MODE_PRIVATE).all

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

    override fun checkSkinTexture(path: String, optional: Boolean) {
        if (optional) {
            com.osudroid.ui.skinning.ensureOptionalTexture(File(path))
        } else {
            com.osudroid.ui.skinning.ensureTexture(File(path))
        }
    }

    override val hasBeatmapListing = true

    override fun showBeatmapListing() {
        mainThread { BeatmapListing().show() }
    }

    override fun downloadBeatmapSet(setId: Long, filename: String) {
        val url = BeatmapListing.mirror.download.request(setId, !Settings[OsuSettings.PreferNoVideoDownloads]).toString()
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

    override fun createVideo(path: String) = ExoPlayerVideo(path)

    override val vibrations: OsuPlatform.GameplayVibrations
        get() = VibratorManager

    override fun verifyFileIntegrity() = AndroidSecurityUtils.verifyFileIntegrity(activity)

    override val canCheckForUpdates = true

    override fun checkForUpdates(silently: Boolean) = UpdateManager.checkNewUpdates(silently)

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
