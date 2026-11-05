package ru.nsu.ccfit.zuev.osu;

import com.osudroid.ui.v2.settings.SettingsSectionsKt;
import android.Manifest;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.provider.OpenableColumns;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.StatFs;
import android.util.DisplayMetrics;
import com.reco1l.verktex.Logger;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.PermissionChecker;
import androidx.preference.PreferenceManager;

import com.edlplan.ui.ActivityOverlay;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.osudroid.BuildSettings;
import com.osudroid.beatmaps.BeatmapCache;
import com.osudroid.data.DatabaseManager;
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

import com.osudroid.ui.v2.modmenu.ModMenu;
import com.osudroid.ui.v2.settings.SettingsMenu;
import com.reco1l.osu.ui.MessageDialog;
import com.osudroid.difficulty.BeatmapDifficultyCalculator;
import com.reco1l.verktex.Verktex;
import com.reco1l.verktex.android.AndroidGameHost;

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
import ru.nsu.ccfit.zuev.audio.serviceAudio.SaveServiceObject;
import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService;
import ru.nsu.ccfit.zuev.osu.helper.FileUtils;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osu.helper.Debug;
import com.osudroid.ui.v2.SplashScene;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;
import com.osudroid.debug.QuickPlay;

public class MainActivity extends AndroidGameHost {

    public static String versionName;
    public static SongService songService;
    public ServiceConnection connection;
    private String fileToAdd = null;
    private Uri contentUriToAdd = null;
    private SaveServiceObject saveServiceObject;
    private FirebaseAnalytics analytics;
    private FirebaseCrashlytics crashlytics;
    private boolean willReplay = false;
    private static boolean activityVisible = true;
    private static final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> logFlushFuture;
    private Display display;
    private DisplayManager.DisplayListener displayListener;
    private float currentRefreshRate = 60;
    private float maxRefreshRate = 60;
    private int maxRefreshRateModeId = 0;
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

        Config.loadConfig(this);
        StringTable.setContext(this);
        ToastLogger.init(this);
        initialGameDirectory();
        Multiplayer.initLog();
        //Debug.setDebugLevel(Debug.DebugLevel.NONE);
        OnlineManager.getInstance().init();
        crashlytics.setUserId(Config.getOnlineDeviceID());

        final DisplayMetrics dm = new DisplayMetrics();
        display = getWindowManager().getDefaultDisplay();
        display.getMetrics(dm);
        currentRefreshRate = display.getRefreshRate();

        displayListener = new DisplayManager.DisplayListener() {
            @Override
            public void onDisplayAdded(int displayId) {}

            @Override
            public void onDisplayRemoved(int displayId) {}

            @Override
            public void onDisplayChanged(int displayId) {
                if (displayId == display.getDisplayId()) {
                    currentRefreshRate = display.getRefreshRate();
                }
            }
        };

        ((DisplayManager) getSystemService(DISPLAY_SERVICE)).registerDisplayListener(displayListener, null);

        for (var mode : display.getSupportedModes()) {
            if (mode.getRefreshRate() > maxRefreshRate) {
                maxRefreshRate = mode.getRefreshRate();
                maxRefreshRateModeId = mode.getModeId();
            }
        }

        Verktex.pushScene(SplashScene.INSTANCE);

        Execution.async(() -> {
            GlobalManager.getInstance().init();

            // The "Show FPS" setting shows the engine's performance overlay.
            Settings.INSTANCE.set(VerktexSettings.ShowPerformanceOverlay, Config.isShowFPS());

            // The resolution scale setting is stored as a percentage.
            Settings.INSTANCE.set(VerktexSettings.ResolutionScale, Config.getInt(SettingsSectionsKt.RESOLUTION_SCALE_KEY, 100) / 100f);

            analytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, null);
            GlobalManager.getInstance().setLoadingProgress(50);
            checkNewSkins();
            Config.loadSkins();
            DifficultyCalculationManager.checkForOutdatedStarRatings();
            loadBeatmapLibrary();

            SplashScene.INSTANCE.playWelcomeAnimation();

            Execution.delayed(2500, () -> {

                UpdateManager.onActivityStart();
                DatabaseManager.notifyIfResetDueToDowngrade();
                GlobalManager.getInstance().setInfo("");
                GlobalManager.getInstance().setLoadingProgress(100);
                Verktex.pushScene(MainScene.INSTANCE);
                SongSelect.INSTANCE.loadBeatmaps();

                initPreferences();
                availableInternalMemory();
                applyRefreshRateSetting(Config.isForceMaxRefreshRate());

                AccessibilityDetector.register(MainActivity.this);

                logFlushFuture = scheduledExecutor.scheduleAtFixedRate(Multiplayer::flushLog, 0, 5, TimeUnit.SECONDS);

                // TEMPORARY: debug shortcut straight into gameplay, see QuickPlay.
                if (QuickPlay.startIfRequested(getIntent())) {
                    return;
                }

                if (roomInviteLink != null) {
                    final Uri inviteLink = roomInviteLink;
                    roomInviteLink = null;
                    Multiplayer.connectFromLink(inviteLink);
                } else if (willReplay) {
                    MainScene.INSTANCE.watchReplay(fileToAdd);
                    fileToAdd = null;
                    willReplay = false;
                }
            });
        });
    }

    @Override
    public void onEngineStarted() {}

    private void initialGameDirectory() {
        File dir = new File(Config.getBeatmapPath());
        // Creating Osu directory if it doesn't exist
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Config.setBeatmapPath(Config.getCorePath() + "Songs/");
                dir = new File(Config.getBeatmapPath());
                if (!(dir.exists() || dir.mkdirs())) {
                    ToastLogger.showText(StringTable.format(
                                    R.string.message_error_createdir, dir.getPath()),
                            true);
                } else {
                    final SharedPreferences prefs = PreferenceManager
                            .getDefaultSharedPreferences(this);
                    final SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("directory", dir.getPath());
                    editor.commit();
                }

            }
            final File nomedia = new File(dir.getParentFile(), ".nomedia");
            try {
                nomedia.createNewFile();
            } catch (final IOException e) {
                Logger.e("MainActivity", "LibraryManager: " + e.getMessage(), e);
            }
        }

        final File skinDir = new File(Config.getCorePath() + "/Skin");
        // Creating Osu/Skin directory if it doesn't exist
        if (!skinDir.exists()) {
            skinDir.mkdirs();
        }
    }

    private void initPreferences() {
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);

        if (!prefs.getBoolean("qualitySet", false)) {
            final SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("qualitySet", true);
            final DisplayMetrics dm = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(dm);

            if (dm.densityDpi > DisplayMetrics.DENSITY_MEDIUM) {
                editor.putBoolean("lowtextures", false);
            } else {
                editor.putBoolean("lowtextures", false);
            }
            editor.commit();
        }

        if (!prefs.getBoolean("onlineSet", false)) {

            Editor editor = prefs.edit();
            editor.putBoolean("onlineSet", true);
            editor.commit();
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
        String toastMessage = String.format(StringTable.get(R.string.message_low_storage_space), df.format(availableMemory / minMem));
        if (availableMemory < 0.5 * minMem) { //I set 512MiB as a minimum
            Execution.mainThread(() -> Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show());
        }
        Logger.i("MainActivity", "Free Space: " + df.format(availableMemory / minMem));
    }

    public void loadBeatmapLibrary() {
        GlobalManager.getInstance().setInfo("Checking for new maps...");
        if (contentUriToAdd != null) {
            fileToAdd = copyContentUriToCache(contentUriToAdd);
            contentUriToAdd = null;
        }
        final File mainDir = new File(Config.getCorePath());
        final HashSet<String> forceImportedBeatmaps = new HashSet<>();
        if (fileToAdd != null) {
            File file = new File(fileToAdd);
            if (file.getName().toLowerCase().endsWith(".osz")) {
                ToastLogger.showText(
                        StringTable.get(R.string.library_importing),
                        false);

                FileUtils.extractBeatmapset(fileToAdd, Config.getBeatmapPath());
                forceImportedBeatmaps.add(file.getName().substring(0, file.getName().length() - 4)
                        .replaceAll("(?i) \\[no video]$", ""));
                // LibraryManager.INSTANCE.sort();
                fileToAdd = null;
            } else if (file.getName().toLowerCase().endsWith(".osk")) {
                ToastLogger.showText(StringTable.get(R.string.library_skin_importing), false);
                FileUtils.extractZip(fileToAdd, Config.getSkinTopPath());
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

            File beatmapDir = new File(Config.getBeatmapPath());
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
            if (Config.isSCAN_DOWNLOAD()
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
                // final boolean deleteOsz = Config.isDELETE_OSZ();
                // Config.setDELETE_OSZ(true);
                ToastLogger.showText(StringTable.format(
                        R.string.library_importing_several,
                        beatmaps.size()), false);
                for (final String beatmap : beatmaps) {
                    FileUtils.extractBeatmapset(beatmap, Config.getBeatmapPath());
                }
                // Config.setDELETE_OSZ(deleteOsz);

                // LibraryManager.INSTANCE.sort();
            }
        }

        LibraryManager.scanDirectory(forceImportedBeatmaps);
        LibraryManager.loadLibrary();
    }

    public void checkNewSkins() {
        GlobalManager.getInstance().setInfo("Checking new skins...");

        final ArrayList<String> skins = new ArrayList<>();

        // Scanning skin directory
        final File skinDir = new File(Config.getSkinTopPath());

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

        if (Config.isSCAN_DOWNLOAD()
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
            ToastLogger.showText(StringTable.format(
                    R.string.library_skin_importing_several,
                    skins.size()), false);

            for (final String skin : skins) {
                if (FileUtils.extractZip(skin, Config.getSkinTopPath())) {
                    String folderName = skin.substring(0, skin.length() - 4);
                    // We have imported the skin!
                    ToastLogger.showText(
                            StringTable.format(R.string.library_imported, folderName),
                            true);
                    Config.addSkin(folderName.substring(folderName.lastIndexOf("/") + 1), skin);
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

    public static boolean isActivityVisible() {
        return activityVisible;
    }

    @Override
    protected void onCreate(Bundle pSavedInstanceState) {
        // Some components may already start using this class when onCreate is called. An example
        // is when the game is restoring after being killed by system due to low system memory.
        GlobalManager.getInstance().setMainActivity(this);

        // The engine picks its graphics backend when the host is created, Config isn't loaded yet.
        Settings.INSTANCE.set(VerktexSettings.GraphicsBackend, PreferenceManager.getDefaultSharedPreferences(this)
            .getString(SettingsSectionsKt.GRAPHICS_BACKEND_KEY, GraphicsBackend.Auto.name()));

        super.onCreate(pSavedInstanceState);

        try {
            versionName = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_ACTIVITIES).versionName;
        } catch (Exception ignored) {
        }

        if (BuildConfig.DEBUG) {
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
        onBeginBindService();
    }

    public void onBeginBindService() {
        if (connection == null && songService == null) {
            connection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    songService = ((SongService.ReturnBindObject) service).getObject();
                    saveServiceObject = (SaveServiceObject) getApplication();
                    saveServiceObject.setSongService(songService);
                    GlobalManager.getInstance().setSongService(songService);
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {

                }

            };

            bindService(new Intent(MainActivity.this, SongService.class), connection, BIND_AUTO_CREATE);
        }
        GlobalManager.getInstance().setSongService(songService);
        GlobalManager.getInstance().setSaveServiceObject(saveServiceObject);
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
                ToastLogger.showText(StringTable.get(R.string.import_failed_open_file), false);
                return null;
            }

            sink.writeAll(Okio.source(in));
            return tempFile.getAbsolutePath();
        } catch (IOException | SecurityException e) {
            Logger.e("MainActivity", "MainActivity.copyContentUriToCache: " + e.getMessage(), e);
            ToastLogger.showText(StringTable.get(R.string.import_failed_open_file), false);
            return null;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        if (parseIntent(intent)) {
            var globalManager = GlobalManager.getInstance();

            if (roomInviteLink != null) {
                Execution.updateThread(() -> {
                    var gameScene = globalManager.getGameScene();

                    // Ensure gameplay is cleaned up before joining the lobby to prevent any potential issues with
                    // lingering game state.
                    if (gameScene != null && Verktex.getScene() == gameScene.getScene()) {
                        gameScene.quit();
                    }

                    Multiplayer.connectFromLink(roomInviteLink);
                    roomInviteLink = null;
                });

                return;
            }

            Execution.async(() -> {
                loadBeatmapLibrary();

                if (willReplay && fileToAdd != null) {
                    final String replayFile = fileToAdd;

                    Execution.updateThread(() -> {
                        MainScene.INSTANCE.watchReplay(replayFile);
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
        activityVisible = true;

        logFlushFuture = scheduledExecutor.scheduleAtFixedRate(Multiplayer::flushLog, 0, 5, TimeUnit.SECONDS);

        if (isInMultiWindowMode()) {
            showMultiModeWindowAlert();
            return;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        activityVisible = false;

        if (logFlushFuture != null && !logFlushFuture.isCancelled()) {
            logFlushFuture.cancel(false);
        }

        Multiplayer.flushLog();


            /*if (Multiplayer.isMultiplayer) {
                ToastLogger.showText("You've left the match.", true);
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

        activityVisible = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Multiplayer.flushLog();
        AccessibilityDetector.unregister(this);

        var listener = displayListener;

        if (listener != null) {
            ((DisplayManager) getSystemService(DISPLAY_SERVICE)).unregisterDisplayListener(listener);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (!hasFocus) {
            if (Multiplayer.isConnected()
                    && (Verktex.getScene() == Multiplayer.roomScene || Verktex.getScene() == SongSelect.INSTANCE)) {
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

        var gameScene = GlobalManager.getInstance().getGameScene();

        if (gameScene != null && currentScene == gameScene.getScene() &&
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

        var scoringScene = GlobalManager.getInstance().getScoring();

        if (scoringScene != null && currentScene == scoringScene.getScene()) {
            scoringScene.back();
            return true;
        }

        if (currentScene instanceof SettingsMenu settingsMenu) {
            settingsMenu.back();
            return true;
        }

        if (ModMenu.INSTANCE.isShown()) {
            ModMenu.INSTANCE.back();
            return true;
        }

        if (currentScene == SongSelect.INSTANCE) {
            SongSelect.INSTANCE.back();
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
                runOnUiThread(Multiplayer.roomScene.getLeaveDialog()::show);
                return true;
            }
        }

        MainScene.INSTANCE.showExitDialog();
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
            var gameScene = GlobalManager.getInstance().getGameScene();

            if (Verktex.getScene() == gameScene.getScene()) {
                gameScene.quit();
            }

            if (Verktex.getScene() != MainScene.INSTANCE) {
                Verktex.pushScene(MainScene.INSTANCE);
            }

            MainScene.INSTANCE.exit();
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

    public float getRefreshRate() {
        return currentRefreshRate;
    }

    public void applyRefreshRateSetting(boolean enable) {
        runOnUiThread(() -> {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.preferredDisplayModeId = enable ? maxRefreshRateModeId : 0;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                lp.preferMinimalPostProcessing = enable && display.isMinimalPostProcessingSupported();
            }

            getWindow().setAttributes(lp);
        });
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
                .setTitle(getString(R.string.multi_mode_window_alert_title))
                .setMessage(getString(R.string.multi_mode_window_alert_message))
                .setAllowDismiss(false)
                .addButton(getString(R.string.accessibility_detector_exit), (d) -> {
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
