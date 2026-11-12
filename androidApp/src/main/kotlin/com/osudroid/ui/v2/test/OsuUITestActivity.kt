package com.osudroid.ui.v2.test

import com.osudroid.settings.OsuSettings
import com.osudroid.ui.v2.SkinFolders
import com.osudroid.OsuGameHost
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Platform
import com.reco1l.verktex.PlatformApplication
import ru.nsu.ccfit.zuev.osuplus.app.BuildConfig
import java.io.File
import com.osudroid.ui.v2.Skin
import com.reco1l.verktex.android.UITestActivity
import com.reco1l.verktex.testing.VerktexUITests
import kotlinx.coroutines.runBlocking

/**
 * The UI test browser of osu!droid. It runs in its own process, so the game itself is never started
 * and only what the components need is loaded here.
 *
 * Launch it from its launcher entry in debug builds, or with
 * `adb shell am start -n <package>/com.osudroid.ui.v2.test.OsuUITestActivity --es test "osu!/Hit circles"`.
 */
class OsuUITestActivity : UITestActivity(), OsuGameHost {

    override val platformApplication: PlatformApplication
        get() = super.platformApplication.copy(buildType = BuildConfig.BUILD_TYPE)


    override fun onEngineInitialized() {
        // The game isn't running, a task reaching what only it sets up mustn't end the browser.
        Platform.onUncaughtException = { Logger.e("OsuUITestActivity", "Uncaught exception in a task", it) }

        OsuSettings.load()
        Skin.load(SkinFolders.path)

        // Before any suite is created, the components under test reach the game through it.
        // Before the suites are created, which read the database it seeds.
        runBlocking { TestGameContext.install() }
    }

    override fun createSuites() = listOf(VerktexUITests, OsuUITests, OsuSceneUITests, OsuGameplayUITests)
}
