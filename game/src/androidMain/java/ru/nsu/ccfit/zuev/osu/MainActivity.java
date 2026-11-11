package ru.nsu.ccfit.zuev.osu;

import com.osudroid.OsuGame;
import com.reco1l.verktex.Platform;
import com.osudroid.settings.GameFolders;
import com.osudroid.settings.OsuSettings;
import com.osudroid.ui.v2.SkinFolders;
import com.osudroid.resources.Res;
import com.reco1l.verktex.ui.toast.Toasts;
import com.osudroid.android.AndroidGame;
import com.osudroid.android.AndroidOsuGameHost;

import android.Manifest;
import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.provider.OpenableColumns;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import com.reco1l.verktex.Logger;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.PermissionChecker;

import com.edlplan.ui.ActivityOverlay;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.osudroid.BuildSettings;
import com.osudroid.beatmaps.BeatmapCache;
import com.osudroid.data.AndroidDatabase;
import com.osudroid.resources.R;
import com.reco1l.verktex.graphics.GraphicsBackend;
import com.reco1l.verktex.settings.Settings;
import com.reco1l.verktex.settings.VerktexSettings;
import com.osudroid.ui.v2.GameLoaderScene;
import com.osudroid.ui.v2.LoaderScene;
import com.osudroid.ui.v2.mainmenu.MainScene;
import com.osudroid.ui.v2.songselect.SongSelect;
import com.osudroid.utils.Execution;
import com.osudroid.multiplayer.api.LobbyAPI;
import com.osudroid.utils.AccessibilityDetector;
import com.osudroid.beatmaps.DifficultyCalculationManager;
import com.osudroid.multiplayer.Multiplayer;
import com.osudroid.UpdateManager;
import com.osudroid.ui.v2.multi.LobbyScene;

import com.osudroid.ui.v2.settings.SettingsMenu;
import com.reco1l.osu.ui.MessageDialog;
import com.osudroid.difficulty.BeatmapDifficultyCalculator;
import com.reco1l.verktex.Verktex;

import net.lingala.zip4j.ZipFile;

import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import okio.Okio;
import ru.nsu.ccfit.zuev.osu.helper.FileUtils;
import ru.nsu.ccfit.zuev.osu.helper.Debug;
import com.osudroid.ui.v2.SplashScene;
import ru.nsu.ccfit.zuev.osu.game.GameScene;
import ru.nsu.ccfit.zuev.osu.scoring.ScoringScene;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;
import com.osudroid.android.AppBuild;
import com.osudroid.debug.QuickPlay;

public class MainActivity extends AndroidOsuGameHost {

    public static String versionName;
    private String fileToAdd = null;
    private Uri contentUriToAdd = null;
    private FirebaseAnalytics analytics;
    private FirebaseCrashlytics crashlytics;
    private boolean willReplay = false;

    // The main menu, null until the game has started.
    private MainScene mainScene;
    private static final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> logFlushFuture;
    private MessageDialog multiWindowAlert;

    // Multiplayer
    private Uri roomInviteLink;

    @Override
    public void onEngineInitialized() {

        if (!checkPermissions()) {
            // The permission screen was opened, the game starts again once access is granted.
            return;
        }

        analytics = FirebaseAnalytics.getInstance(this);
        crashlytics = FirebaseCrashlytics.getInstance();

        // Detect the device's maximum supported OpenGL ES version and report it to Firebase
        // so we can see the GLES capability distribution across users.
        reportGlesVersion();

        OsuSettings.load();
        initialGameDirectory();
        Multiplayer.initLog();
        //Debug.setDebugLevel(Debug.DebugLevel.NONE);
        OnlineManager.getInstance().init();
        crashlytics.setUserId(OsuSettings.getInstallId());

        var splashScene = new SplashScene();
        Verktex.pushScene(splashScene);

        Execution.async(() -> {
            AndroidDatabase.load(this);
            OsuGame.init();

            // The "Show FPS" setting shows the engine's performance overlay.
            Settings.set(VerktexSettings.ShowPerformanceOverlay, Settings.get(OsuSettings.ShowFps));

            // The resolution scale setting is stored as a percentage.
            Settings.set(VerktexSettings.ResolutionScale, Settings.get(OsuSettings.ResolutionScale) / 100f);

            analytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, null);
            SplashScene.setProgress(50);
            checkNewSkins();
            SkinFolders.load();
            DifficultyCalculationManager.checkForOutdatedStarRatings();
            loadBeatmapLibrary();

            splashScene.playWelcomeAnimation();

            Execution.delayed(2500, () -> {

                UpdateManager.onActivityStart();
                AndroidDatabase.notifyIfResetDueToDowngrade();
                SplashScene.setInfo("");
                SplashScene.setProgress(100);

                // The splash can't be gone back to.
                mainScene = new MainScene();
                Verktex.resetScenes(mainScene);

                availableInternalMemory();
                Platform.INSTANCE.setForceMaxRefreshRate(Settings.get(OsuSettings.ForceMaxRefreshRate));

                AccessibilityDetector.register(MainActivity.this);

                logFlushFuture = scheduledExecutor.scheduleAtFixedRate(Multiplayer::flushLog, 0, 5, TimeUnit.SECONDS);

                // TEMPORARY: debug shortcut straight into gameplay, see QuickPlay.
                if (QuickPlay.startIfRequested(getIntent())) {
                    return;
                }

                if (roomInviteLink != null) {
                    final Uri inviteLink = roomInviteLink;
                    roomInviteLink = null;
                    Multiplayer.connectFromLink(inviteLink.getPathSegments());
                } else if (willReplay) {
                    MainScene.watchReplay(fileToAdd);
                    fileToAdd = null;
                    willReplay = false;
                }
            });
        });
    }

    @Override
    public void onEngineStarted() {}

    private void initialGameDirectory() {
        File dir = new File(GameFolders.getBeatmapPath());
        // Creating Osu directory if it doesn't exist
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                GameFolders.setBeatmapPath(GameFolders.getCorePath() + "Songs/");
                dir = new File(GameFolders.getBeatmapPath());
                if (!(dir.exists() || dir.mkdirs())) {
                    Toasts.showLong(Res.string.message_error_createdir.format(dir.getPath()));
                }

            }
            final File nomedia = new File(dir.getParentFile(), ".nomedia");
            try {
                nomedia.createNewFile();
            } catch (final IOException e) {
                Logger.e("MainActivity", "LibraryManager: " + e.getMessage(), e);
            }
        }

        final File skinDir = new File(GameFolders.getCorePath() + "/Skin");
        // Creating Osu/Skin directory if it doesn't exist
        if (!skinDir.exists()) {
            skinDir.mkdirs();
        }
    }

    /*
    Accuracy isn't the best, but it's sufficient enough
    to determine whether storage is low or not
     */
    private void availableInternalMemory() {
        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.HALF_EVEN);

        double availableMemory;
        double minMem = 1073741824D; //1 GiB = 1073741824 bytes
        File internal = Environment.getDataDirectory();
        StatFs stat = new StatFs(internal.getPath());
        availableMemory = (double) stat.getAvailableBytes();
        String toastMessage = Res.string.message_low_storage_space.format(df.format(availableMemory / minMem)).toString();
        if (availableMemory < 0.5 * minMem) { //I set 512MiB as a minimum
            Toasts.showLong(toastMessage);
        }
        Logger.i("MainActivity", "Free Space: " + df.format(availableMemory / minMem));
    }

    public void loadBeatmapLibrary() {
        SplashScene.setInfo("Checking for new maps...");
        if (contentUriToAdd != null) {
            fileToAdd = copyContentUriToCache(contentUriToAdd);
            contentUriToAdd = null;
        }
        final File mainDir = new File(GameFolders.getCorePath());
        final HashSet<String> forceImportedBeatmaps = new HashSet<>();
        if (fileToAdd != null) {
            File file = new File(fileToAdd);
            if (file.getName().toLowerCase().endsWith(".osz")) {
                Toasts.show(Res.string.library_importing);

                FileUtils.extractBeatmapset(fileToAdd, GameFolders.getBeatmapPath());
                forceImportedBeatmaps.add(file.getName().substring(0, file.getName().length() - 4)
                        .replaceAll("(?i) \\[no video]$", ""));
                // LibraryManager.INSTANCE.sort();
                fileToAdd = null;
            } else if (file.getName().toLowerCase().endsWith(".osk")) {
                Toasts.show(Res.string.library_skin_importing);
                FileUtils.extractZip(fileToAdd, SkinFolders.getTopPath());
                fileToAdd = null;
            } else if (file.getName().toLowerCase().endsWith(".odr")) {
                willReplay = true;
            } else {
                fileToAdd = null;
            }
        } else if (mainDir.exists() && mainDir.isDirectory()) {
            File[] filelist = FileUtils.listFiles(mainDir, ".osz");
            final ArrayList<String> beatmaps = new ArrayList<>();
            for (final File file : filelist) {
                try (var zip = new ZipFile(file)) {
                    if (zip.isValidZipFile()) {
                        beatmaps.add(file.getPath());
                    }
                } catch (IOException ignored) {
                }
            }

            File beatmapDir = new File(GameFolders.getBeatmapPath());
            if (beatmapDir.exists()
                    && beatmapDir.isDirectory()) {
                filelist = FileUtils.listFiles(beatmapDir, ".osz");
                for (final File file : filelist) {
                    try (var zip = new ZipFile(file)) {
                        if (zip.isValidZipFile()) {
                            beatmaps.add(file.getPath());
                            forceImportedBeatmaps.add(file.getName().substring(0, file.getName().length() - 4)
                                    .replaceAll("(?i) \\[no video]$", ""));
                        }
                    } catch (IOException ignored) {
                    }
                }
            }

            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (Settings.get(OsuSettings.ScanDownload)
                    && downloadDir.exists()
                    && downloadDir.isDirectory()) {
                filelist = FileUtils.listFiles(downloadDir, ".osz");
                for (final File file : filelist) {
                    try (var zip = new ZipFile(file)) {
                        if (zip.isValidZipFile()) {
                            beatmaps.add(file.getPath());
                            forceImportedBeatmaps.add(file.getName().substring(0, file.getName().length() - 4)
                                    .replaceAll("(?i) \\[no video]$", ""));
                        }
                    } catch (IOException ignored) {
                    }
                }
            }

            if (beatmaps.size() > 0) {
                // final boolean deleteOsz = Settings.get(OsuSettings.DeleteOsz);
                // Settings.set(OsuSettings.DeleteOsz, true);
                Toasts.show(Res.string.library_importing_several.format(beatmaps.size()));
                for (final String beatmap : beatmaps) {
                    FileUtils.extractBeatmapset(beatmap, GameFolders.getBeatmapPath());
                }
                // Settings.set(OsuSettings.DeleteOsz, deleteOsz);

                // LibraryManager.INSTANCE.sort();
            }
        }

        LibraryManager.scanDirectory(forceImportedBeatmaps);
        LibraryManager.loadLibrary();
    }

    public void checkNewSkins() {
        SplashScene.setInfo("Checking new skins...");

        final ArrayList<String> skins = new ArrayList<>();

        // Scanning skin directory
        final File skinDir = new File(SkinFolders.getTopPath());

        if (skinDir.exists() && skinDir.isDirectory()) {
            final File[] files = FileUtils.listFiles(skinDir, ".osk");

            for (final File file : files) {
                try (var zip = new ZipFile(file)) {
                    if (zip.isValidZipFile()) {
                        skins.add(file.getPath());
                    }
                } catch (IOException ignored) {
                }
            }
        }

        // Scanning download directory
        final File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        if (Settings.get(OsuSettings.ScanDownload)
                && downloadDir.exists()
                && downloadDir.isDirectory()) {
            final File[] files = FileUtils.listFiles(downloadDir, ".osk");

            for (final File file : files) {
                try (var zip = new ZipFile(file)) {
                    if (zip.isValidZipFile()) {
                        skins.add(file.getPath());
                    }
                } catch (IOException ignored) {
                }
            }
        }

        if (skins.size() > 0) {
            Toasts.show(Res.string.library_skin_importing_several.format(skins.size()));

            for (final String skin : skins) {
                if (FileUtils.extractZip(skin, SkinFolders.getTopPath())) {
                    String folderName = skin.substring(0, skin.length() - 4);
                    // We have imported the skin!
                    Toasts.showLong(Res.string.library_imported.format(folderName));
                    SkinFolders.add(folderName.substring(folderName.lastIndexOf("/") + 1), skin);
                }
            }
        }
    }

    /**
     * Detects the device's maximum supported OpenGL ES version via
     * {@link ActivityManager#getDeviceConfigurationInfo()} and reports it to Firebase Analytics
     * both as a persistent user property (for audience segmentation) and inside the APP_OPEN
     * event bundle (for per-session inspection).
     *
     * <p>The version string format is {@code "<major>.<minor>"}, e.g. {@code "3.2"} or
     * {@code "3.0"}.  Firebase user-property values are capped at 36 characters, so the short
     * string is well within limits.
     */
    private void reportGlesVersion() {
        try {
            ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            // Use var so we never need to name the inner class ActivityManager.DeviceConfigurationInfo.
            var configInfo = am.getDeviceConfigurationInfo();

            // reqGlEsVersion encodes the version as (major << 16 | minor).
            // Example: 0x00030002 → OpenGL ES 3.2
            int reqGlEsVersion = configInfo.reqGlEsVersion;
            int glesMajor = reqGlEsVersion >> 16;
            int glesMinor = reqGlEsVersion & 0xFFFF;
            String glesVersionStr = glesMajor + "." + glesMinor;

            // Persist as a user property so the Firebase console can segment audiences
            // by GLES version (e.g. "gles_version = 3.2" vs "3.0").
            analytics.setUserProperty("gles_version", glesVersionStr);

            // Also attach to Crashlytics so crash reports include the GLES capability.
            crashlytics.setCustomKey("gles_version", glesVersionStr);

            Logger.i("MainActivity", "Device max GLES version: " + glesVersionStr + " (raw=0x" + Integer.toHexString(reqGlEsVersion) + ")");
        } catch (Exception e) {
            Logger.e("MainActivity", "Failed to detect GLES version: " + e.getMessage(), e);
        }
    }

    public FirebaseAnalytics getAnalytics() {
        return analytics;
    }

    @Override
    protected void onCreate(Bundle pSavedInstanceState) {
        // Some components may already start using this class when onCreate is called. An example
        // is when the game is restoring after being killed by system due to low system memory.
        AndroidGame.setMainActivity(this);
        OsuGame.setHeadless(false);

        // The engine picks its graphics backend from its settings when the host is created. Until
        // the settings were imported from the preferences, on the first start with them, the backend
        // chosen before is taken from there.
        if (!new File(getFilesDir(), "settings.json").exists()) {
            Settings.set(VerktexSettings.GraphicsBackend, getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE)
                .getString(OsuSettings.LegacyGraphicsBackend.getName(), GraphicsBackend.Auto.name()));
        }

        super.onCreate(pSavedInstanceState);

        try {
            versionName = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_ACTIVITIES).versionName;
        } catch (Exception ignored) {
        }

        if (AppBuild.isDebug()) {
            //Toast.makeText(this,"this is debug version",Toast.LENGTH_LONG).show();
            try {
                File d = new File(Environment.getExternalStorageDirectory(), "osu!droid/Log");
                if (!d.exists()) d.mkdirs();
                File f = new File(d, "rawlog.txt");
                if (!f.exists()) f.createNewFile();
                Runtime.getRuntime().exec("logcat -f " + (f.getAbsolutePath()));
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    protected void onStart() {
        parseIntent(getIntent());
        super.onStart();
    }

    private String copyContentUriToCache(Uri uri) {
        String displayName = null;
        String fileName;

        try (var cursor = getContentResolver().query(uri, new String[]{ OpenableColumns.DISPLAY_NAME }, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                displayName = cursor.getString(0);
            }
        } catch (IllegalArgumentException | SecurityException e) {
            Logger.e("MainActivity", "MainActivity.copyContentUriToCache: cannot query display name: " + e.getMessage(), e);
        }

        if (displayName != null && !displayName.isEmpty()) {
            // Strip any directory components a malicious provider might include.
            fileName = new File(displayName).getName();
        } else {
            String mimeType = null;

            try {
                mimeType = getContentResolver().getType(uri);
            } catch (SecurityException e) {
                Logger.e("MainActivity", "MainActivity.copyContentUriToCache: cannot query MIME type: " + e.getMessage(), e);
            }

            String ext;

            if ("application/x-osu-beatmap-archive".equals(mimeType)) {
                ext = ".osz";
            } else if ("application/x-osu-skin-archive".equals(mimeType)) {
                ext = ".osk";
            } else {
                ext = ".zip";
            }

            fileName = "import_" + System.currentTimeMillis() + ext;
        }

        var tempFile = new File(getCacheDir(), fileName);

        try {
            if (!tempFile.getCanonicalPath().startsWith(getCacheDir().getCanonicalPath() + File.separator)) {
                Logger.e("MainActivity", "MainActivity.copyContentUriToCache: rejected unsafe display name: " + displayName);
                return null;
            }
        } catch (IOException e) {
            Logger.e("MainActivity", "MainActivity.copyContentUriToCache: " + e.getMessage(), e);
            return null;
        }

        try (var in = getContentResolver().openInputStream(uri);
             var sink = Okio.buffer(Okio.sink(tempFile))) {
            if (in == null) {
                Toasts.show(Res.string.import_failed_open_file);
                return null;
            }

            sink.writeAll(Okio.source(in));
            return tempFile.getAbsolutePath();
        } catch (IOException | SecurityException e) {
            Logger.e("MainActivity", "MainActivity.copyContentUriToCache: " + e.getMessage(), e);
            Toasts.show(Res.string.import_failed_open_file);
            return null;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        if (parseIntent(intent)) {
            if (roomInviteLink != null) {
                Execution.updateThread(() -> {
                    var gameScene = GameScene.getCurrent();

                    // Ensure gameplay is cleaned up before joining the lobby to prevent any potential issues with
                    // lingering game state.
                    if (gameScene != null) {
                        gameScene.quit();
                    }

                    Multiplayer.connectFromLink(roomInviteLink.getPathSegments());
                    roomInviteLink = null;
                });

                return;
            }

            Execution.async(() -> {
                loadBeatmapLibrary();

                if (willReplay && fileToAdd != null) {
                    final String replayFile = fileToAdd;

                    Execution.updateThread(() -> {
                        MainScene.watchReplay(replayFile);
                        fileToAdd = null;
                        willReplay = false;
                    });
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        logFlushFuture = scheduledExecutor.scheduleAtFixedRate(Multiplayer::flushLog, 0, 5, TimeUnit.SECONDS);

        if (isInMultiWindowMode()) {
            showMultiModeWindowAlert();
            return;
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (logFlushFuture != null && !logFlushFuture.isCancelled()) {
            logFlushFuture.cancel(false);
        }

        Multiplayer.flushLog();


            /*if (Multiplayer.isMultiplayer) {
                Toasts.showLong("You've left the match.");
                Execution.updateThread(gameScene::quit);
                Multiplayer.log("Player left the match.");
            } else {
                Execution.updateThread(gameScene::pause);
            }*/
    }

    @Override
    public void onStop() {
        super.onStop();

        if (logFlushFuture != null && !logFlushFuture.isCancelled()) {
            logFlushFuture.cancel(false);
        }

        Multiplayer.flushLog();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Multiplayer.flushLog();
        AccessibilityDetector.unregister(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (!hasFocus) {
            if (Multiplayer.isConnected()
                    && (Verktex.getScene() == Multiplayer.roomScene || Verktex.getScene() instanceof SongSelect)) {
                Execution.async(() -> Execution.runSafe(Multiplayer.roomScene::invalidateStatus));
            }
        }

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode, @NonNull Configuration newConfig) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig);

        if (isInMultiWindowMode) {
            showMultiModeWindowAlert();
        } else {
            hideMultiModeWindowAlert();
        }
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        if (AccessibilityDetector.isIllegalServiceDetected()) {
            return false;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected boolean onUnhandledKeyDown(final int keyCode, @NonNull final KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return false;
        }

        var currentScene = Verktex.getScene();

        if (keyCode == KeyEvent.KEYCODE_BACK && ActivityOverlay.onBackPress()) {
            return true;
        }

        var gameScene = GameScene.getCurrent();

        if (gameScene != null &&
                (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU)) {
            if (gameScene.isPaused()) {
                Execution.updateThread(gameScene::resume);
            } else {
                Execution.updateThread(gameScene::pause);
            }
            return true;
        }

        if (keyCode != KeyEvent.KEYCODE_BACK) {
            return false;
        }

        var scoringScene = ScoringScene.getCurrent();

        if (scoringScene != null) {
            scoringScene.back();
            return true;
        }

        if (currentScene instanceof SettingsMenu settingsMenu) {
            settingsMenu.back();
            return true;
        }

        if (currentScene instanceof SongSelect songSelect) {
            songSelect.back();
            return true;
        }

        if (currentScene instanceof GameLoaderScene loaderScene) {
            loaderScene.cancel();
            return true;
        }

        if (currentScene instanceof LoaderScene) {
            return true;
        }

        if (Multiplayer.isMultiplayer) {
            if (currentScene instanceof LobbyScene lobbyScene) {
                lobbyScene.back();
                return true;
            }

            if (currentScene == Multiplayer.roomScene) {
                runOnUiThread(Multiplayer.roomScene::onBackPressed);
                return true;
            }
        }

        if (mainScene != null) {
            mainScene.showExitDialog();
        }
        return true;
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        if (level >= TRIM_MEMORY_BACKGROUND) {
            Debug.i("onTrimMemory: Clearing resources (level=" + level + ")");

            Execution.async(() -> {
                BeatmapDifficultyCalculator.clearCache();
                BeatmapCache.clear();
            });
        }
    }

    public void forcedExit() {
        Execution.updateThread(() -> {
            var gameScene = GameScene.getCurrent();

            if (gameScene != null) {
                gameScene.quit();
            }

            if (mainScene == null) {
                Platform.INSTANCE.exit();
                return;
            }

            if (Verktex.getScene() != mainScene) {
                Verktex.resetScenes(mainScene);
            }

            mainScene.exit();
        });
    }

    public long getVersionCode() {
        long versionCode = 0;
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(
                    getPackageName(), 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                versionCode = packageInfo.getLongVersionCode();
            } else {
                versionCode = packageInfo.versionCode;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Debug.e("PackageManager: " + e.getMessage(), e);
        }
        return versionCode;
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                Environment.isExternalStorageManager()) {
            return true;
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R &&
                PermissionChecker.checkCallingOrSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PermissionChecker.PERMISSION_GRANTED) {
            return true;
        } else {
            Intent grantPermission = new Intent(this, PermissionActivity.class);
            startActivity(grantPermission);
            overridePendingTransition(ru.nsu.ccfit.zuev.osuplus.R.anim.fast_activity_swap, ru.nsu.ccfit.zuev.osuplus.R.anim.fast_activity_swap);
            finish();
            return false;
        }
    }

    private void showMultiModeWindowAlert() {
        if (multiWindowAlert == null) {
            multiWindowAlert = new MessageDialog()
                .setTitle(Res.string.multi_mode_window_alert_title.toString())
                .setMessage(Res.string.multi_mode_window_alert_message.toString())
                .setAllowDismiss(false)
                .addButton(Res.string.accessibility_detector_exit.toString(), (d) -> {
                    finish();
                    return null;
                });
        }

        multiWindowAlert.show();
    }

    private void hideMultiModeWindowAlert() {
        if (multiWindowAlert != null) {
            multiWindowAlert.dismiss();
        }
    }

    private boolean parseIntent(Intent intent) {
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri data = intent.getData();

            if (data != null) {
                if (data.toString().startsWith(LobbyAPI.INVITE_HOST)) {
                    roomInviteLink = data;
                } else {
                    String scheme = data.getScheme();
                    if (ContentResolver.SCHEME_FILE.equals(scheme)) {
                        fileToAdd = data.getPath();
                    } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
                        contentUriToAdd = data;
                    }
                }

                // Consume the action so it doesn't get repeatedly processed
                // on subsequent onStart() or onResume() triggers.
                intent.setAction(Intent.ACTION_MAIN);
                return true;
            }
        }

        return false;
    }
}
