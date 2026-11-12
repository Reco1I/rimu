package com.osudroid.android

import android.Manifest
import android.content.ContentResolver
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.OpenableColumns
import android.view.KeyEvent
import android.view.View
import androidx.core.content.PermissionChecker
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import com.osudroid.OsuGame
import com.osudroid.OsuGameHost
import com.osudroid.beatmaps.BeatmapCache
import com.osudroid.beatmaps.LibraryImport
import com.osudroid.debug.QuickPlay
import com.osudroid.difficulty.BeatmapDifficultyCalculator
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.multiplayer.api.LobbyAPI
import com.osudroid.resources.Res
import com.osudroid.settings.OsuSettings
import com.osudroid.ui.v2.mainmenu.MainScene
import com.osudroid.ui.v2.songselect.SongSelect
import com.osudroid.utils.AccessibilityDetector
import com.osudroid.utils.async
import com.osudroid.utils.runSafe
import com.osudroid.utils.updateThread
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Platform
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.android.AndroidGameHost
import com.reco1l.verktex.localization.formatString
import com.reco1l.verktex.ui.toast.Toasts
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import okio.buffer
import okio.sink
import okio.source
import ru.nsu.ccfit.zuev.osu.AndroidSecurityUtils
import ru.nsu.ccfit.zuev.osu.game.GameScene

/**
 * The game's host on Android: the activity running the engine, which starts the game
 * ([OsuGame.start]), with the features of the game only Android has and what it was opened with (a
 * beatmap set, a skin or a replay, a room invite). `MainActivity`, in `:androidApp`, only launches it,
 * with the screen that asks for access to the storage, [openPermissionScreen].
 */
abstract class AndroidOsuGameHost : AndroidGameHost(), OsuGameHost {

    // What the game was opened with: a file, a file of another application, or a room invite.
    private var fileToAdd: String? = null
    private var contentUriToAdd: Uri? = null
    private var roomInviteLink: Uri? = null

    // Whether fileToAdd is a replay, played once the library is loaded.
    private var willReplay = false

    private var logFlushJob: Job? = null


    /**
     * Opens the screen that asks for access to the storage and finishes this activity, which the
     * screen starts again once access is granted.
     */
    protected abstract fun openPermissionScreen()


    /**
     * Leaves the game right away, through the main menu's exit when it's shown.
     */
    fun forcedExit() = updateThread {
        GameScene.current?.quit()

        val mainScene = OsuGame.mainScene

        if (mainScene == null) {
            Platform.exit()
            return@updateThread
        }

        if (Verktex.scene !== mainScene) {
            Verktex.resetScenes(mainScene)
        }

        mainScene.exit()
    }


    //region Lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        // Some components may already use the activity when onCreate is called, e.g. when the game is
        // restored after the system killed it for memory.
        AndroidGame.mainActivity = this
        OsuGame.isHeadless = false

        super.onCreate(savedInstanceState)

        if (AppBuild.isDebug) {
            try {
                @Suppress("DEPRECATION")
                val folder = File(Environment.getExternalStorageDirectory(), "osu!droid/Log").apply { mkdirs() }
                val file = File(folder, "rawlog.txt").apply { createNewFile() }
                Runtime.getRuntime().exec("logcat -f ${file.absolutePath}")
            } catch (_: IOException) {
            }
        }
    }

    override fun onEngineInitialized() {
        if (!hasStorageAccess()) {
            // The permission screen starts the game again once access is granted.
            openPermissionScreen()
            return
        }

        OsuGame.start()
        FirebaseCrashlytics.getInstance().setUserId(OsuSettings.installId)
    }

    override fun onEngineStarted() = Unit

    override fun onGameStarted() {
        warnIfStorageIsLow()
        AccessibilityDetector.register(this)

        // TEMPORARY: debug shortcut straight into gameplay, see QuickPlay.
        val intent = intent
        val quickPlay = intent?.getStringExtra("quickplay")

        if (quickPlay != null) {
            // Once only, not again when the activity is recreated.
            intent.removeExtra("quickplay")

            if (QuickPlay.start(quickPlay, intent.getBooleanExtra("autoplay", true))) {
                return
            }
        }

        val inviteLink = roomInviteLink

        if (inviteLink != null) {
            roomInviteLink = null
            Multiplayer.connectFromLink(inviteLink.pathSegments)
        } else if (willReplay) {
            fileToAdd?.let { MainScene.watchReplay(it) }
            fileToAdd = null
            willReplay = false
        }
    }

    override fun onStart() {
        parseIntent(intent)
        super.onStart()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        if (!parseIntent(intent)) {
            return
        }

        val inviteLink = roomInviteLink

        if (inviteLink != null) {
            roomInviteLink = null

            updateThread {
                // Gameplay is left first, its state would linger in the lobby.
                GameScene.current?.quit()
                Multiplayer.connectFromLink(inviteLink.pathSegments)
            }
            return
        }

        async {
            loadBeatmapLibrary()

            val replayFile = fileToAdd

            if (willReplay && replayFile != null) {
                updateThread {
                    MainScene.watchReplay(replayFile)
                    fileToAdd = null
                    willReplay = false
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startFlushingLog()

        if (isInMultiWindowMode) {
            MultiWindowAlert.show(::finish)
        }
    }

    override fun onPause() {
        super.onPause()
        stopFlushingLog()
    }

    override fun onStop() {
        super.onStop()
        stopFlushingLog()
    }

    override fun onDestroy() {
        super.onDestroy()

        Multiplayer.flushLog()
        AccessibilityDetector.unregister(this)

        // A recreated activity may already have replaced it.
        if (AndroidGame.mainActivity === this) {
            AndroidGame.mainActivity = null
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (!hasFocus && Multiplayer.isConnected &&
            (Verktex.scene === Multiplayer.roomScene || Verktex.scene is SongSelect)) {
            async { runSafe { Multiplayer.roomScene?.invalidateStatus() } }
        }

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)

        if (isInMultiWindowMode) {
            MultiWindowAlert.show(::finish)
        } else {
            MultiWindowAlert.hide()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // Keys sent by an accessibility service that plays for the player are ignored.
        if (AccessibilityDetector.isIllegalServiceDetected) {
            return false
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)

        if (level >= TRIM_MEMORY_BACKGROUND) {
            Logger.i("AndroidOsuGameHost", "onTrimMemory: Clearing resources (level=$level)")

            async {
                BeatmapDifficultyCalculator.clearCache()
                BeatmapCache.clear()
            }
        }
    }

    //endregion


    //region Startup

    private fun hasStorageAccess() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        @Suppress("DEPRECATION")
        PermissionChecker.checkCallingOrSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PermissionChecker.PERMISSION_GRANTED
    }

    // Tells the player when less than 512 MiB are left, which isn't exact but tells low storage apart.
    private fun warnIfStorageIsLow() {
        val available = StatFs(Environment.getDataDirectory().path).availableBytes.toDouble()
        val gibibytes = formatString("%.2f", available / GIBIBYTE)

        if (available < 0.5 * GIBIBYTE) {
            Toasts.showLong(Res.string.message_low_storage_space.format(gibibytes))
        }
        Logger.i("AndroidOsuGameHost", "Free space: $gibibytes GiB")
    }

    // The multiplayer log is written every 5 seconds while the activity is shown, and when it isn't.
    private fun startFlushingLog() {
        logFlushJob?.cancel()
        logFlushJob = async {
            while (isActive) {
                Multiplayer.flushLog()
                delay(5000)
            }
        }
    }

    private fun stopFlushingLog() {
        logFlushJob?.cancel()
        logFlushJob = null
        Multiplayer.flushLog()
    }

    //endregion


    //region Opened files

    /**
     * Reads what [intent] opened the game with, consuming it.
     *
     * @return Whether it opened the game with something.
     */
    private fun parseIntent(intent: Intent?): Boolean {
        if (intent == null || intent.action != Intent.ACTION_VIEW) {
            return false
        }

        val data = intent.data ?: return false

        if (data.toString().startsWith(LobbyAPI.INVITE_HOST)) {
            roomInviteLink = data
        } else when (data.scheme) {
            ContentResolver.SCHEME_FILE -> fileToAdd = data.path
            ContentResolver.SCHEME_CONTENT -> contentUriToAdd = data
        }

        // Consumed, so that onStart and onResume don't read it again.
        intent.action = Intent.ACTION_MAIN
        return true
    }

    override suspend fun loadBeatmapLibrary() {
        contentUriToAdd?.let {
            fileToAdd = copyContentUriToCache(it)
            contentUriToAdd = null
        }

        // A file the game was opened with: an archive is imported instead of the ones in the folders,
        // a replay is played once the game has started.
        val openedFile = fileToAdd

        if (openedFile != null) {
            if (openedFile.lowercase().endsWith(".odr")) {
                willReplay = true
            } else {
                fileToAdd = null
            }
        }

        LibraryImport.loadLibrary(openedFile)
    }

    // Copies a file another application opened the game with to the cache, as it has no path.
    private fun copyContentUriToCache(uri: Uri): String? {
        var displayName: String? = null

        try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    displayName = cursor.getString(0)
                }
            }
        } catch (e: IllegalArgumentException) {
            Logger.e("AndroidOsuGameHost", "Cannot query the display name of $uri", e)
        } catch (e: SecurityException) {
            Logger.e("AndroidOsuGameHost", "Cannot query the display name of $uri", e)
        }

        val fileName = displayName?.takeIf { it.isNotEmpty() }?.let {
            // Without the folders a malicious provider might include.
            File(it).name
        } ?: run {
            val mimeType = try {
                contentResolver.getType(uri)
            } catch (e: SecurityException) {
                Logger.e("AndroidOsuGameHost", "Cannot query the MIME type of $uri", e)
                null
            }

            val extension = when (mimeType) {
                "application/x-osu-beatmap-archive" -> ".osz"
                "application/x-osu-skin-archive" -> ".osk"
                else -> ".zip"
            }
            "import_${System.currentTimeMillis()}$extension"
        }

        val file = File(cacheDir, fileName)

        try {
            if (!file.canonicalPath.startsWith(cacheDir.canonicalPath + File.separator)) {
                Logger.e("AndroidOsuGameHost", "Rejected an unsafe display name: $displayName")
                return null
            }
        } catch (e: IOException) {
            Logger.e("AndroidOsuGameHost", "Failed to resolve $file", e)
            return null
        }

        fun fail(e: Exception): String? {
            Logger.e("AndroidOsuGameHost", "Failed to copy $uri", e)
            Toasts.show(Res.string.import_failed_open_file)
            return null
        }

        return try {
            val input = contentResolver.openInputStream(uri)

            if (input == null) {
                Toasts.show(Res.string.import_failed_open_file)
                return null
            }

            input.source().use { source -> file.sink().buffer().use { it.writeAll(source) } }
            file.absolutePath
        } catch (e: IOException) {
            fail(e)
        } catch (e: SecurityException) {
            fail(e)
        }
    }

    //endregion


    //region Features

    override val buildType: String
        get() = AppBuild.buildType

    override fun checkNewSkins() = LibraryImport.importSkins()

    override fun setAnnouncementsSubscribed(subscribed: Boolean) {
        if (subscribed) {
            FirebaseMessaging.getInstance().subscribeToTopic("announcements")
        } else {
            FirebaseMessaging.getInstance().unsubscribeFromTopic("announcements")
        }
    }

    override fun logAnalyticsEvent(name: String, parameters: Map<String, String>) {
        val bundle = Bundle()
        parameters.forEach { (key, value) -> bundle.putString(key, value) }
        FirebaseAnalytics.getInstance(this).logEvent(name, bundle)
    }

    override fun verifyFileIntegrity() = AndroidSecurityUtils.verifyFileIntegrity(this)

    //endregion


    private companion object {
        const val GIBIBYTE = 1073741824.0
    }
}
