package ru.nsu.ccfit.zuev.osu

import com.osudroid.OsuGameHost
import kotlin.jvm.JvmStatic

object BuildType {
    @JvmStatic
    fun hasOnlineAccess(): Boolean {
        return OsuGameHost.current.buildType.matches(Regex("(release|pre_release)"))
    }
}
