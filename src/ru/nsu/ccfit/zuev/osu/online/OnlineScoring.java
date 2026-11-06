package ru.nsu.ccfit.zuev.osu.online;

import com.osudroid.android.AndroidGame;
import android.content.Intent;
import android.net.Uri;

import com.google.android.material.snackbar.Snackbar;
import com.osudroid.data.BeatmapInfo;
import com.osudroid.utils.Execution;
import com.osudroid.ui.SendingPanel;

import ru.nsu.ccfit.zuev.osu.helper.Debug;

import java.util.concurrent.CancellationException;

import kotlinx.coroutines.Job;
import kotlinx.coroutines.JobKt;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;

public class OnlineScoring {
    private static final int attemptCount = 5;
    private static OnlineScoring instance = null;
    private final Boolean onlineMutex = Boolean.FALSE;
    private boolean avatarLoaded = false;
    // Created when first shown, it needs the window of the main activity.
    private Snackbar snackbar;

    private Job loginJob, profileAssetsJob;

    public static OnlineScoring getInstance() {
        if (instance == null)
            instance = new OnlineScoring();
        return instance;
    }

    public void login() {
        if (!OnlineManager.getInstance().isStayOnline())
            return;
        avatarLoaded = false;

        if (loginJob != null) {
            loginJob.cancel(new CancellationException("Login cancelled"));
        }

        loginJob = Execution.async((scope) -> {
            synchronized (onlineMutex) {
                boolean success = false;

                //Trying to send request
                for (int i = 0; i < 3; i++) {

                    try {
                        JobKt.ensureActive(scope.getCoroutineContext());
                        success = OnlineManager.getInstance().logIn();
                    } catch (OnlineManager.OnlineManagerException e) {
                        Debug.e("Login error: " + e.getMessage());
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e1) {
                            break;
                        }
                        continue;
                    }
                    break;
                }
                if (success) {
                    OnlineManager.getInstance().setStayOnline(true);
                    loadProfileAssets(true);
                } else {
                    OnlineManager.getInstance().setStayOnline(false);

                    if (OnlineManager.getInstance().getFailMessage().equals("Cannot connect to server")) {
                        var mainActivity = AndroidGame.getMainActivity();
                        if (mainActivity == null) {
                            return;
                        }

                        Execution.mainThread(() -> {
                            if (snackbar == null) {
                                snackbar = Snackbar.make(mainActivity.getWindow().getDecorView(), "", 10000);
                            }
                            snackbar.dismiss();
                            snackbar.setText("Cannot connect to server. Please check the following article for troubleshooting.");

                            snackbar.setAction("Check", (v) -> {
                                var intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://neroyuki.github.io/osudroid-guide/help/login_fail"));

                                AndroidGame.getMainActivity().startActivity(intent);
                            });

                            snackbar.show();
                        });
                    }
                }
            }
        });
    }

    public void sendRecord(final BeatmapInfo beatmap, final StatisticV2 record, final SendingPanel panel, final String replayPath) {
        if (!OnlineManager.getInstance().isStayOnline())
            return;

        Debug.i("Sending score");

        final String recordData = record.compile();

        Execution.async(() -> {
            boolean success = false;
            synchronized (onlineMutex) {
                for (int i = 0; i < attemptCount; i++) {
                    if (!record.isScoreValid()) {
                        Debug.e("Detected illegal actions.");
                        break;
                    }

                    try {
                        success = OnlineManager.getInstance().sendRecord(beatmap, recordData, replayPath);
                    } catch (OnlineManager.OnlineManagerException e) {
                        Debug.e("Login error: " + e.getMessage());
                        success = false;
                    }

                    if (OnlineManager.getInstance().getFailMessage().length() > 0) {
                        ToastLogger.showText(OnlineManager.getInstance().getFailMessage(), true);
                        if (OnlineManager.getInstance().getFailMessage().equals("Invalid record data"))
                            i = attemptCount;
                    } else if (success) {
                        OnlineManager mgr = OnlineManager.getInstance();
                        panel.show(mgr.getMapRank(), mgr.getRank(), mgr.getScore(), mgr.getAccuracy(), mgr.getPP());
                        break;
                    }

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ignored) {
                    }
                }

                if (!success) {
                    panel.setFail();
                }
            }
        });
    }

    public void loadProfileAssets(final boolean both) {
        if (!OnlineManager.getInstance().isStayOnline()) return;
        final String avatarUrl = OnlineManager.getInstance().getAvatarURL();
        final String profileBannerUrl = OnlineManager.getInstance().getProfileBannerURL();
        if ((avatarUrl == null || avatarUrl.isEmpty())
                && (profileBannerUrl == null || profileBannerUrl.isEmpty()))
            return;

        if (profileAssetsJob != null) {
            profileAssetsJob.cancel(new CancellationException("Profile asset loading cancelled"));
        }

        profileAssetsJob = Execution.async((scope) -> {
            synchronized (onlineMutex) {
                avatarLoaded = avatarUrl != null
                        && !avatarUrl.isEmpty()
                        && OnlineManager.getInstance().loadAvatarToTextureManager();
                if (profileBannerUrl != null && !profileBannerUrl.isEmpty()) {
                    OnlineManager.getInstance().loadProfileBannerToTextureManager();
                }
                JobKt.ensureActive(scope.getCoroutineContext());

            }
        });
    }

    public boolean isAvatarLoaded() {
        return avatarLoaded;
    }
}
