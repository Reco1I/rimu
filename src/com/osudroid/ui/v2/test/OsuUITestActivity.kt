package com.osudroid.ui.v2.test

import com.osudroid.OsuPlatform
import com.osudroid.android.AndroidOsuPlatform
import com.google.firebase.FirebaseApp
import com.osudroid.ui.v2.Skin
import com.reco1l.verktex.android.UITestActivity
import com.reco1l.verktex.testing.VerktexUITests
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.helper.StringTable

/**
 * The UI test browser of osu!droid. It runs in its own process, so the game itself is never started
 * and only what the components need is loaded here.
 *
 * Launch it from its launcher entry in debug builds, or with
 * `adb shell am start -n <package>/com.osudroid.ui.v2.test.OsuUITestActivity --es test "osu!/Hit circles"`.
 */
class OsuUITestActivity : UITestActivity() {

    override fun onEngineInitialized() {
        // Firebase only initializes itself in the main process, the configuration subscribes to topics.
        FirebaseApp.initializeApp(this)

        OsuPlatform.implementation = AndroidOsuPlatform(this)
        Config.loadConfig()
        StringTable.setContext(this)
        Skin.load(Config.getSkinPath())

        // Before any suite is created, the components under test reach the game through it.
        TestGameContext.install(this)
    }

    override fun createSuites() = listOf(VerktexUITests, OsuUITests, OsuSceneUITests, OsuGameplayUITests)
}
