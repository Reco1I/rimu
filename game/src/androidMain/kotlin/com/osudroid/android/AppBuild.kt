package com.osudroid.android

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

/**
 * What the game knows about the application it was built into, which the application module
 * decides: this module is a library, with a single variant and no `BuildConfig` of its own.
 *
 * The application's class, `SaveServiceObject`, initializes it when the process starts.
 */
object AppBuild {

    /**
     * The id of the application, e.g. `ru.nsu.ccfit.zuev.osuplus.debug`.
     */
    @JvmStatic
    var applicationId = ""
        private set

    /**
     * The type of the build: `debug`, `release`, `pre_release` or `profile`. The application's
     * manifest declares it as the [BUILD_TYPE_KEY] meta-data.
     */
    @JvmStatic
    var buildType = "debug"
        private set

    /**
     * Whether the application is debuggable, which only debug builds are.
     */
    @JvmStatic
    var isDebug = false
        private set


    @JvmStatic
    fun initialize(context: Context) {
        applicationId = context.packageName
        isDebug = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

        val metaData = try {
            context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA).metaData
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }

        buildType = metaData?.getString(BUILD_TYPE_KEY)
            ?: throw IllegalStateException("The application's manifest doesn't declare the $BUILD_TYPE_KEY meta-data")
    }


    const val BUILD_TYPE_KEY = "com.osudroid.buildType"
}
