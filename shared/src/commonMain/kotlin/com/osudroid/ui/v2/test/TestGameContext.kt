package com.osudroid.ui.v2.test

import com.osudroid.OsuGame
import com.osudroid.BeatmapSelection
import com.osudroid.ui.v2.SkinFolders
import com.osudroid.data.BeatmapInfo
import com.osudroid.data.DatabaseManager
import com.osudroid.multiplayer.Multiplayer
import com.reco1l.verktex.Platform
import com.reco1l.verktex.testing.UITestStage
import okio.Path.Companion.toPath
import ru.nsu.ccfit.zuev.osu.LibraryManager
import ru.nsu.ccfit.zuev.osu.online.OnlineManager

/**
 * Sets up what the game would, for the scenes and components shown in the UI test browser, which
 * runs without the main activity (see [com.osudroid.OsuGame.isHeadless]).
 *
 * Everything is made up or kept in memory: the database is seeded with [TestData], whose beatmaps
 * are written to the cache folder so they can be played, songs are played by a [FakeMusicService]
 * (silently, the songs' files are empty) and multiplayer by the mock socket of debug builds. Nothing
 * goes online and no file of the game is changed, except the preferences, which are shared with the
 * game.
 */
object TestGameContext {

    /**
     * The music service every component sees as the game's.
     */
    lateinit var music: FakeMusicService
        private set


    /**
     * Must run before any component is built, some of them keep the music service they first see. The
     * platform sets up the toasts before. The database is an empty one kept in memory, so the game's
     * own is never touched.
     */
    suspend fun install() {

        // The skins of the skin folder, for the settings.
        SkinFolders.load()

        DatabaseManager.openInMemory()
        seedDatabase()
        LibraryManager.loadLibrary()

        music = FakeMusicService()
        OsuGame.music = music

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

        music.stop()
        BeatmapSelection.beatmap = null
        music.isGaming = false
    }


    private suspend fun seedDatabase() {
        val beatmapSets = TestData.beatmapSets()
        val beatmaps = beatmapSets.flatMap { it.beatmaps }

        writeBeatmapFiles(beatmaps)

        DatabaseManager.beatmapInfoTable.insertAll(beatmaps)
        DatabaseManager.scoreInfoTable.insertScores(beatmaps.flatMap { TestData.scores(it) })
        TestData.modPresets().forEach { DatabaseManager.modPresetTable.insert(it) }
        DatabaseManager.beatmapCollectionsTable.insertCollection("Favourites")
    }

    // The files of the beatmaps, so they can be played: their .osu and an empty audio file.
    private fun writeBeatmapFiles(beatmaps: List<BeatmapInfo>) {
        val fileSystem = Platform.fileSystem

        for (beatmap in beatmaps) {
            val directory = beatmap.absoluteSetDirectory.toPath()
            fileSystem.createDirectories(directory)
            fileSystem.write(directory / beatmap.filename) { write(TestData.beatmapFile(beatmap)) }
            fileSystem.write(directory / TestData.AUDIO_FILENAME) {}
        }
    }
}

/**
 * Restores the shared state of the game when the test is closed.
 */
fun UITestStage.useGameContext() {
    onDispose { TestGameContext.reset() }
}
