package com.osudroid.ui.v2.test

import com.osudroid.android.AndroidToasts
import android.app.Activity
import com.osudroid.MusicManager
import com.osudroid.data.AndroidDatabase
import com.osudroid.data.DatabaseManager
import com.osudroid.multiplayer.Multiplayer
import com.reco1l.verktex.testing.UITestStage
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.LibraryManager
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.osu.scoring.ScoringScene

/**
 * Sets up what the game would, for the scenes and components shown in the UI test browser, which
 * runs without the main activity (see [GlobalManager.isHeadless]).
 *
 * Everything is made up or kept in memory: the database is seeded with [TestData], songs are played
 * by a [FakeSongService], gameplay by a [FakeGameScene] and multiplayer by the mock socket of debug
 * builds. Nothing goes online and no file of the game is changed, except the preferences, which are
 * shared with the game.
 */
object TestGameContext {

    /**
     * The game scene every component sees as the game's.
     */
    lateinit var gameScene: FakeGameScene
        private set

    /**
     * The song service every component sees as the game's.
     */
    lateinit var songService: FakeSongService
        private set


    /**
     * Must run before any component is built, some of them keep the song service they first see.
     */
    fun install(activity: Activity) {
        val global = GlobalManager.getInstance()

        // The activity is the context of the game here, it can start other activities too.
        global.setContext(activity)
        ToastLogger.init(AndroidToasts(activity))

        // The skins of the skin folder, for the settings.
        Config.loadSkins()

        AndroidDatabase.loadInMemory(activity)
        seedDatabase()
        LibraryManager.loadLibrary()

        songService = FakeSongService()
        global.songService = songService

        gameScene = FakeGameScene()
        global.gameScene = gameScene
        global.scoring = ScoringScene(gameScene)
        gameScene.setScoringScene(global.scoring)

        // It's on until the account is loaded, which is never done here.
        OnlineManager.getInstance().isStayOnline = false
    }

    /**
     * Restores the state the scenes share once a test is closed. Registered by [useGameContext].
     */
    fun reset() {
        Multiplayer.roomScene?.teardownSession()
        Multiplayer.room = null
        Multiplayer.player = null
        Multiplayer.isMultiplayer = false

        MusicManager.stop()
        GlobalManager.getInstance().selectedBeatmap = null
        songService.isGaming = false

        gameScene.resetPlay()
        gameScene.onStartGame = {}
    }


    private fun seedDatabase() {
        val beatmapSets = TestData.beatmapSets()
        val beatmaps = beatmapSets.flatMap { it.beatmaps }

        DatabaseManager.beatmapInfoTable.insertAll(beatmaps)
        DatabaseManager.scoreInfoTable.insertScores(beatmaps.flatMap { TestData.scores(it) })
        TestData.modPresets().forEach { DatabaseManager.modPresetTable.insert(it) }
        DatabaseManager.beatmapCollectionsTable.insertCollection("Favourites")
    }
}

/**
 * Restores the shared state of the game when the test is closed, and reports gameplay that would
 * start to the log of the browser.
 */
fun UITestStage.useGameContext() {
    TestGameContext.gameScene.onStartGame = { log("Would play ${it?.artist} - ${it?.title} [${it?.version}]") }
    onDispose { TestGameContext.reset() }
}
