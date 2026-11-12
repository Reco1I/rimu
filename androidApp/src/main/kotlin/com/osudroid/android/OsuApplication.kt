package com.osudroid.android

import android.app.Application
import ru.nsu.ccfit.zuev.osuplus.app.BuildConfig

/**
 * The application. It runs once in every process before any of its components, so what they all
 * need is set up here: what `:shared` knows about the build ([AppBuild]).
 */
class OsuApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AppBuild.initialize(BuildConfig.APPLICATION_ID, BuildConfig.BUILD_TYPE, BuildConfig.DEBUG)
    }
}
