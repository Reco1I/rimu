package com.osudroid.ui.v2.test

import com.osudroid.settings.OsuSettings
import com.osudroid.ui.v2.SkinFolders
import com.osudroid.OsuGameHost
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Platform
import ru.nsu.ccfit.zuev.osuplus.BuildConfig
import java.io.File
import com.osudroid.android.AndroidGame
import com.osudroid.data.AndroidDatabase
import com.google.firebase.FirebaseApp
import com.osudroid.ui.v2.Skin
import com.reco1l.verktex.android.UITestActivity
import com.reco1l.verktex.testing.VerktexUITests

/**
 * The UI test browser of osu!droid. It runs in its own process, so the game itself is never started
 * and only what the components need is loaded here.
 *
 * Launch it from its launcher entry in debug builds, or with
 * `adb shell am start -n <package>/com.osudroid.ui.v2.test.OsuUITestActivity --es test "osu!/Hit circles"`.
 */
class OsuUITestActivity : UITestActivity(), OsuGameHost {

    override val buildType: String = BuildConfig.BUILD_TYPE


    override fun checkSkinTexture(path: String, optional: Boolean) {
        if (optional) {
            com.osudroid.ui.skinning.ensureOptionalTexture(File(path))
        } else {
            com.osudroid.ui.skinning.ensureTexture(File(path))
        }
    }

    override fun onEngineInitialized() {
        // Firebase only initializes itself in the main process, the configuration subscribes to topics.
        FirebaseApp.initializeApp(this)

        // The game isn't running, a task reaching what only it sets up mustn't end the browser.
        Platform.onUncaughtException = { Logger.e("OsuUITestActivity", "Uncaught exception in a task", it) }

        OsuSettings.load()
        Skin.load(SkinFolders.path)

        // The activity is the context of the game here, it can start other activities too.
        AndroidGame.context = this

        // Before any suite is created, the components under test reach the game through it.
        TestGameContext.install { AndroidDatabase.loadInMemory(this) }
    }

    override fun createSuites() = listOf(VerktexUITests, OsuUITests, OsuSceneUITests, OsuGameplayUITests)
}
