package ru.nsu.ccfit.zuev.osu

import com.osudroid.OsuPlatform

object BuildType {
    @JvmStatic
    fun hasOnlineAccess(): Boolean {
        return OsuPlatform.implementation.buildType.matches(Regex("(release|pre_release)"))
    }
}
