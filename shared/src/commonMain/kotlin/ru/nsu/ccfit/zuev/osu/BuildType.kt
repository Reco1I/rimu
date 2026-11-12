package ru.nsu.ccfit.zuev.osu

import com.reco1l.verktex.Platform
import kotlin.jvm.JvmStatic

object BuildType {
    @JvmStatic
    fun hasOnlineAccess(): Boolean {
        return Platform.application.buildType.matches(Regex("(release|pre_release)"))
    }
}
