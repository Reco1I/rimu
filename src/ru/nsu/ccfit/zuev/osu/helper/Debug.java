package ru.nsu.ccfit.zuev.osu.helper;

import com.reco1l.verktex.Logger;

/**
 * Logging shortcuts for the legacy code.
 */
public final class Debug {
    private static final String TAG = "osu!droid";

    private Debug() {
    }

    public static void i(String message) {
        Logger.i(TAG, message);
    }

    public static void e(String message) {
        Logger.e(TAG, message);
    }

    public static void e(Throwable throwable) {
        Logger.e(TAG, throwable.getMessage(), throwable);
    }

    public static void e(String message, Throwable throwable) {
        Logger.e(TAG, message, throwable);
    }
}
