package com.osudroid.android

import android.app.Activity
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import androidx.preference.PreferenceManager
import com.google.firebase.messaging.FirebaseMessaging
import com.osudroid.OsuPlatform
import com.reco1l.verktex.Logger
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.MainActivity
import com.russhwolf.settings.SharedPreferencesSettings
import ru.nsu.ccfit.zuev.osuplus.BuildConfig

/**
 * The game's [OsuPlatform] on Android.
 */
class AndroidOsuPlatform(private val activity: Activity) : OsuPlatform.Implementation {

    override val buildType: String = BuildConfig.BUILD_TYPE

    override val isDebug: Boolean = BuildConfig.DEBUG

    // Committed right away as the preferences always were.
    override val preferences = SharedPreferencesSettings(PreferenceManager.getDefaultSharedPreferences(activity), commit = true)

    override val defaultCorePath: String
        get() = Environment.getExternalStorageDirectory().toString() + "/osu!droid/"

    override val cachePath: String
        get() = activity.cacheDir.path

    override val displayWidth: Int
        get() = realDisplayMetrics().widthPixels

    override val displayHeight: Int
        get() = realDisplayMetrics().heightPixels


    private fun realDisplayMetrics() = DisplayMetrics().also {
        @Suppress("DEPRECATION")
        activity.windowManager.defaultDisplay.getRealMetrics(it)
    }

    override fun setAnnouncementsSubscribed(subscribed: Boolean) {
        if (subscribed) {
            FirebaseMessaging.getInstance().subscribeToTopic("announcements")
        } else {
            FirebaseMessaging.getInstance().unsubscribeFromTopic("announcements")
        }
    }

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    override fun runOnMainThread(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
    }

    override fun handleUncaughtException(throwable: Throwable) {
        // Without the main activity (e.g. in the UI test browser) a task may reach something only the
        // game sets up, which must not take the whole process down.
        if (GlobalManager.getInstance().isHeadless) {
            Logger.e("Execution", "Uncaught exception in a task while headless", throwable)
        } else {
            Thread.currentThread().let { it.uncaughtExceptionHandler?.uncaughtException(it, throwable) ?: throw throwable }
        }
    }

    override val isInForeground: Boolean
        get() = MainActivity.isActivityVisible()

    override fun logAnalyticsEvent(name: String, parameters: Map<String, String>) {
        val bundle = android.os.Bundle()
        parameters.forEach { (key, value) -> bundle.putString(key, value) }
        com.google.firebase.analytics.FirebaseAnalytics.getInstance(activity).logEvent(name, bundle)
    }
}
