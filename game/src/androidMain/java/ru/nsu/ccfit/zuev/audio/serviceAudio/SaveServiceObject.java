package ru.nsu.ccfit.zuev.audio.serviceAudio;

import android.app.Application;

import com.osudroid.android.AppBuild;

import ru.nsu.ccfit.zuev.osu.AppException;

public class SaveServiceObject extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppBuild.initialize(this);
        Thread.setDefaultUncaughtExceptionHandler(AppException.getAppExceptionHandler());
    }
}
