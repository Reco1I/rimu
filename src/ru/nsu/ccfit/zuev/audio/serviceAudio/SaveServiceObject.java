package ru.nsu.ccfit.zuev.audio.serviceAudio;

import android.app.Application;

import ru.nsu.ccfit.zuev.osu.AppException;

public class SaveServiceObject extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(AppException.getAppExceptionHandler());
    }
}
