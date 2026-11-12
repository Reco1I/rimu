package com.osudroid.android

import android.content.ContentResolver
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.OpenableColumns
import android.view.KeyEvent
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
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
import com.osudroid.utils.AccessibilityDetector
import com.osudroid.utils.async
import com.osudroid.utils.updateThread
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Platform
import com.reco1l.verktex.PlatformApplication
import com.reco1l.verktex.android.AndroidGameHost
import com.reco1l.verktex.localization.formatString
import com.reco1l.verktex.ui.toast.Toasts
import java.io.File
import java.io.IOException
import okio.buffer
import okio.sink
import okio.source
import ru.nsu.ccfit.zuev.osu.AndroidSecurityUtils
import ru.nsu.ccfit.zuev.osu.game.GameScene

/**
 * The game's host on Android: the activity running the engine, which starts the game
 * ([OsuGame.start]), with the features of the game only Android has and what it was opened with (a
 * beatmap set, a skin or a replay, a room invite). `MainActivity`, in `:androidApp`, only launches it,
 * once the storage is accessible.
 */
abstract class AndroidOsuGameHost : AndroidGameHost(), OsuGameHost {

    // What the game was opened with and hasn't opened yet: a file (of this or another application)
    // or a room invite, opened once the game has started. Guarded by openedLock, as the game starts
    // on another thread than the activity's.
    private val openedLock = Any()
    private var openedFile: Uri? = null
    private var roomInviteLink: Uri? = null
    private var isGameStarted = false


    //region Lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        OsuGame.isHeadless = false

        super.onCreate(savedInstanceState)

        if (Platform.application.isDebug) {
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

        synchronized(openedLock) { isGameStarted = true }
        openWhatTheGameWasOpenedWith()
    }

    override fun onStart() {
        parseIntent(intent)
        super.onStart()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // Before the game has started, it's opened once it has.
        if (parseIntent(intent)) {
            openWhatTheGameWasOpenedWith()
        }
    }

    override fun onResume() {
        super.onResume()

        if (isInMultiWindowMode) {
            MultiWindowAlert.show(::finish)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AccessibilityDetector.unregister(this)
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

    // Tells the player when less than 512 MiB are left, which isn't exact but tells low storage apart.
    private fun warnIfStorageIsLow() {
        val available = StatFs(Environment.getDataDirectory().path).availableBytes.toDouble()
        val gibibytes = formatString("%.2f", available / GIBIBYTE)

        if (available < 0.5 * GIBIBYTE) {
            Toasts.showLong(Res.string.message_low_storage_space.format(gibibytes))
        }
        Logger.i("AndroidOsuGameHost", "Free space: $gibibytes GiB")
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

        synchronized(openedLock) {
            if (data.toString().startsWith(LobbyAPI.INVITE_HOST)) {
                roomInviteLink = data
            } else if (data.scheme == ContentResolver.SCHEME_FILE || data.scheme == ContentResolver.SCHEME_CONTENT) {
                openedFile = data
            }
        }

        // Consumed, so that onStart and onResume don't read it again.
        intent.action = Intent.ACTION_MAIN
        return true
    }

    // Opens the room invite or the file the game was opened with, once the game has started: a replay
    // is played, an archive (a beatmap set or a skin) imported.
    private fun openWhatTheGameWasOpenedWith() {
        val (inviteLink, file) = synchronized(openedLock) {
            if (!isGameStarted) {
                return
            }
            (roomInviteLink to openedFile).also {
                roomInviteLink = null
                openedFile = null
            }
        }

        if (inviteLink != null) {
            updateThread {
                // Gameplay is left first, its state would linger in the lobby.
                GameScene.current?.quit()
                Multiplayer.connectFromLink(inviteLink.pathSegments)
            }
            return
        }

        file ?: return

        async {
            val path = if (file.scheme == ContentResolver.SCHEME_FILE) file.path else copyContentUriToCache(file)

            when {
                path == null -> Unit
                path.lowercase().endsWith(".odr") -> updateThread { MainScene.watchReplay(path) }
                else -> LibraryImport.loadLibrary(path)
            }
        }
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

    /**
     * The type of the application's build (its `BuildConfig.BUILD_TYPE`), which this module, a
     * library, can't read.
     */
    protected abstract val buildType: String

    override val platformApplication: PlatformApplication
        get() = super.platformApplication.copy(buildType = buildType)

    override fun verifyFileIntegrity() = AndroidSecurityUtils.verifyFileIntegrity(this)

    //endregion


    private companion object {
        const val GIBIBYTE = 1073741824.0
    }
}
