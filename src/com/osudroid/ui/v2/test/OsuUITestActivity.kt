package com.osudroid.ui.v2.test

import com.reco1l.framework.net.OkHttpClientImplementation
import com.osudroid.android.SocketIORoomSocket
import com.osudroid.multiplayer.api.RoomSocket
import com.reco1l.framework.net.Http
import com.osudroid.OsuPlatform
import com.osudroid.android.AndroidOsuPlatform
import com.osudroid.android.AndroidGame
import com.osudroid.data.AndroidDatabase
import com.google.firebase.FirebaseApp
import com.osudroid.ui.v2.Skin
import com.reco1l.verktex.android.UITestActivity
import com.reco1l.verktex.testing.VerktexUITests
import ru.nsu.ccfit.zuev.osu.Config

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
        Http.implementation = OkHttpClientImplementation()
        RoomSocket.factory = SocketIORoomSocket.factory
        Config.loadConfig()
        Skin.load(Config.getSkinPath())

        // The activity is the context of the game here, it can start other activities too.
        AndroidGame.context = this

        // Before any suite is created, the components under test reach the game through it.
        TestGameContext.install { AndroidDatabase.loadInMemory(this) }
    }

    override fun createSuites() = listOf(VerktexUITests, OsuUITests, OsuSceneUITests, OsuGameplayUITests)
}
