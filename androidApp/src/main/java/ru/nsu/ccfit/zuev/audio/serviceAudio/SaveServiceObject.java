package ru.nsu.ccfit.zuev.audio.serviceAudio;

import android.app.Application;

import com.osudroid.android.AppBuild;

import ru.nsu.ccfit.zuev.osu.AppException;
import ru.nsu.ccfit.zuev.osuplus.app.BuildConfig;

public class SaveServiceObject extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppBuild.initialize(BuildConfig.APPLICATION_ID, BuildConfig.BUILD_TYPE, BuildConfig.DEBUG);
        Thread.setDefaultUncaughtExceptionHandler(AppException.getAppExceptionHandler());
    }
}
