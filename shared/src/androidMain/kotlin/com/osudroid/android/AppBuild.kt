package com.osudroid.android

/**
 * What the game knows about the application it was built into. This module is a library, with a
 * single variant and no `BuildConfig` of its own, so the application module passes its own when
 * the process starts, see `SaveServiceObject` in `:androidApp`.
 */
object AppBuild {

    /**
     * The id of the application, e.g. `ru.nsu.ccfit.zuev.osuplus.debug`.
     */
    @JvmStatic
    var applicationId = ""
        private set

    /**
     * The type of the build: `debug`, `release`, `pre_release` or `profile`.
     */
    @JvmStatic
    var buildType = "debug"
        private set

    /**
     * Whether it's a debug build.
     */
    @JvmStatic
    var isDebug = false
        private set


    /**
     * Sets what the application's `BuildConfig` says.
     */
    @JvmStatic
    fun initialize(applicationId: String, buildType: String, isDebug: Boolean) {
        this.applicationId = applicationId
        this.buildType = buildType
        this.isDebug = isDebug
    }
}
