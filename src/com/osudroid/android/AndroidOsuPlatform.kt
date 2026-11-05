package com.osudroid.android

import android.app.Activity
import android.os.Environment
import android.util.DisplayMetrics
import androidx.preference.PreferenceManager
import com.google.firebase.messaging.FirebaseMessaging
import com.osudroid.OsuPlatform
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
}
