package ru.nsu.ccfit.zuev.osu.helper;

import android.util.Log;

/**
 * Logging shortcuts for the legacy code.
 */
public final class Debug {
    private static final String TAG = "osu!droid";

    private Debug() {
    }

    public static void i(String message) {
        Log.i(TAG, message);
    }

    public static void e(String message) {
        Log.e(TAG, message);
    }

    public static void e(Throwable throwable) {
        Log.e(TAG, throwable.getMessage(), throwable);
    }

    public static void e(String message, Throwable throwable) {
        Log.e(TAG, message, throwable);
    }
}
