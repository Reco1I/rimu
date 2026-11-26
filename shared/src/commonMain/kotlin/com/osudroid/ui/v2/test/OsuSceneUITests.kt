package com.osudroid.ui.v2.test

import com.osudroid.OsuGame
import com.osudroid.BeatmapSelection
import com.osudroid.debug.MockRoom
import com.osudroid.game.Gameplay
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.multiplayer.api.RoomAPI
import com.osudroid.multiplayer.api.data.PlayerStatus
import com.osudroid.mods.ModDoubleTime
import com.osudroid.mods.ModHidden
import com.osudroid.mods.ModDifficultyAdjust
import com.osudroid.scoring.Statistic
import com.osudroid.ui.v2.results.RankText
import com.osudroid.ui.v2.results.ResultsLeaderboard
import com.osudroid.ui.v2.results.ResultsScene
import com.osudroid.ui.v2.results.ScoreSubmissionCard
import com.osudroid.utils.currentTimeMillis
import com.osudroid.ui.v2.GameLoaderScene
import com.osudroid.ui.v2.mainmenu.MainScene
import com.osudroid.utils.ModHashMap
import com.osudroid.ui.v2.mainmenu.MusicPlayer
import com.osudroid.ui.v2.modmenu.ModMenu
import com.osudroid.ui.v2.beatmaplisting.BeatmapListingScene
import com.osudroid.ui.v2.multi.LobbyScene
import kotlinx.coroutines.delay
import com.osudroid.ui.v2.multi.RoomChat
import com.osudroid.ui.v2.settings.SettingsMenu
import com.osudroid.ui.v2.songselect.SongSelect
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.testing.UITestStage
import com.reco1l.verktex.testing.UITestSuite
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.Button
import com.reco1l.verktex.ui.container.LinearContainer
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.textButton
import com.reco1l.verktex.ui.text.faSolidIcon
import com.osudroid.ui.v2.mainmenu.RythimManager
import ru.nsu.ccfit.zuev.osu.LibraryManager
import ru.nsu.ccfit.zuev.osu.online.OnlineManager

/**
 * Tests for the scenes of the game and the components that need the game around them, which runs
 * made up here, see [TestGameContext].
 *
 * Each scene is tested on its own inside the stage: the scenes it would open (e.g. song select from
 * the main menu) and going back from it are only logged, except in the tests that start plays (song
 * select and the room), which allow navigating so gameplay is shown: the made up beatmaps are copies
 * of a real one, played without sound. Overlays a scene opens, like the mod menu,
 * are shown. Settings changed here are the game's.
 */
object OsuSceneUITests : UITestSuite("Scenes") {

    init {
        test("Main menu") {
            useGameContext()
            scene(MainScene())
        }

        test("Music panel") {
            useGameContext()

            // Played as the main menu does, which isn't shown here.
            onEachFrame { RythimManager.onUpdate(it.elapsedFrameTime) }
            if (BeatmapSelection.beatmap == null) {
                OsuGame.music.next()
            }

            lateinit var trigger: Button
            trigger = textButton {
                anchor = Anchor.TopRight
                origin = Anchor.TopRight
                x = (-16f).dip
                y = 16f.dip
                text = "Music"
                leadingIcon = faSolidIcon(FAIcon.Music)
                onActionUp = { MusicPlayer(trigger).show() }
            }

            MusicPlayer(trigger).show()
        }

        test("Game loader") {
            gameLoader(isRestart = false, epilepsyWarning = false)
        }

        test("Game loader (restart)") {
            gameLoader(isRestart = true, epilepsyWarning = false)
        }

        test("Game loader (epilepsy warning)") {
            gameLoader(isRestart = false, epilepsyWarning = true)
        }

        test("Settings") {
            useGameContext()
            scene(SettingsMenu())
        }

        test("Song select") {
            useGameContext()
            // Confirming a beatmap plays it.
            allowsNavigation = true
            scene(SongSelect().apply { loadBeatmaps() })
        }

        test("Mod menu") {
            useGameContext()
            BeatmapSelection.beatmap = firstBeatmap()

            val modMenu = ModMenu(ModHashMap())
            showModMenuButton(modMenu)
            onDispose { modMenu.hide() }
            modMenu.show()
        }

        test("Mod menu (multiplayer)") {
            useGameContext()
            BeatmapSelection.beatmap = firstBeatmap()

            // A free mod room this player doesn't host, so only the mods of the player can change.
            val player = TestData.roomPlayer(OnlineManager.getInstance().userId)
            Multiplayer.isMultiplayer = true
            Multiplayer.player = player
            Multiplayer.room = MockRoom().apply {
                host = player.id + 1
                addPlayer(player)
                addPlayer(TestData.roomPlayer(player.id + 1))
            }

            val modMenu = ModMenu(ModHashMap())
            showModMenuButton(modMenu)
            onDispose { modMenu.hide() }
            modMenu.show()
        }

        test("Results") {
            results()
        }

        test("Results (ranks)") {
            linearContainer {
                anchor = Anchor.Center
                origin = Anchor.Center

                for (mark in listOf("XH", "X", "SH", "S", "A", "B", "C", "D")) {
                    +RankText(mark, 112f.dip)
                }
            }
        }

        test("Results (SS)") {
            results(isPerfect = true)
        }

        test("Results (replay)") {
            results(action = ResultsScene.Action.WatchReplay, isFreshPlay = false)
        }

        test("Results (submitting)") {
            results { it.showSubmission(ScoreSubmissionCard(1234, 98_000_000, 0.97f, 4321f)) }
        }

        test("Results (submitted)") {
            results {
                val card = ScoreSubmissionCard(1234, 98_000_000, 0.97f, 4321f)
                it.showSubmission(card)
                card.show(52, 1201, 98_845_210, 0.9712f, 4352f)
            }
        }

        test("Results (multiplayer)") {
            Multiplayer.isMultiplayer = true
            Multiplayer.room = MockRoom()

            results(action = ResultsScene.Action.None) { scene ->
                val stats = Array(8) { i ->
                    testStatistic(400, false, "Player ${i + 1}").apply {
                        setForcedScore(900_000L - i * 61_000L)
                        isAlive = i != 5
                    }
                }

                scene.showLeaderboard(ResultsLeaderboard(stats, stats[2]) { log("Selected ${it.playerName}") })
            }
        }

        test("Lobby") {
            useGameContext()
            Multiplayer.isMultiplayer = true
            scene(LobbyScene())
        }

        test("Room") {
            useGameContext()
            Multiplayer.isMultiplayer = true
            // Starting the match plays the beatmap.
            allowsNavigation = true

            // The mock socket of debug builds answers as the server would, it ends up showing the
            // room scene, the first scene pushed into the stage.
            val online = OnlineManager.getInstance()
            RoomAPI.connectToRoom(1, online.userId, online.sessionId)

            roomEvents()
        }

        test("Room chat") {
            useGameContext()

            val chat = RoomChat()
            chat.show()
            onDispose { chat.hide() }

            val players = List(3) { TestData.roomPlayer(it + 10L) }
            var elapsed = 0f
            var index = 0

            onEachFrame {
                elapsed += it.elapsedFrameTime
                if (elapsed < 1.5f) {
                    return@onEachFrame
                }
                elapsed = 0f

                when (index++ % 4) {
                    3 -> chat.onSystemChatMessage("${players[index % players.size].name} is ready", "#FFD966")
                    else -> chat.onRoomChatMessage(players[index % players.size], SAMPLE_MESSAGES[index % SAMPLE_MESSAGES.size])
                }
            }
        }

        test("Beatmap listing") {
            useGameContext()

            // Made up sets instead of a mirror's, 200 of them, filtered as a mirror would.
            val sets = TestData.listingSets(count = 200)

            scene(BeatmapListingScene { search, offset, limit ->
                delay(300)
                sets.filter { set ->
                    (search.query.isEmpty() || set.title.contains(search.query, ignoreCase = true)) &&
                        (search.status == null || set.status == search.status)
                }.drop(offset).take(limit)
            })

            log("200 made up sets in pages of 50, without covers")
        }

        test("Beatmap listing (mirror)") {
            useGameContext()
            scene(BeatmapListingScene())
            log("Searches the selected mirror, it needs a connection")
        }
    }


    // The loader inside the stage, over a game scene that isn't loading the beatmap, so the loader
    // stays on.
    private fun UITestStage.gameLoader(isRestart: Boolean, epilepsyWarning: Boolean) {
        useGameContext()

        val beatmap = firstBeatmap()?.copy()?.also { it.epilepsyWarning = epilepsyWarning }
        if (beatmap == null) {
            log("No beatmaps in the library")
            return
        }

        scene(GameLoaderScene(Gameplay(), beatmap, testMods(), isRestart))
    }

    // Results of a made up score on the first beatmap, built as the results build them, but without
    // saving or submitting the score.
    private fun UITestStage.results(
        action: ResultsScene.Action = ResultsScene.Action.Retry,
        isFreshPlay: Boolean = true,
        isPerfect: Boolean = false,
        block: (ResultsScene) -> Unit = {},
    ) {
        useGameContext()

        val beatmap = firstBeatmap()
        if (beatmap == null) {
            log("No beatmaps in the library")
            return
        }

        val stat = testStatistic(beatmap.totalHitObjectCount, isPerfect)
        val results = Gameplay().results
        results.beatmapInfo = beatmap
        results.currentStatistic = stat

        val scene = ResultsScene(
            results = results,
            stat = stat,
            beatmapInfo = beatmap,
            action = action,
            isFreshPlay = isFreshPlay,
            showStatistics = true,
            replayPath = null,
            mapMD5 = null,
            beatmapToReplay = null,
        )

        scene(scene)
        block(scene)
    }

    private fun testStatistic(notes: Int, isPerfect: Boolean, name: String = "Player") = Statistic().apply {
        playerName = name
        time = currentTimeMillis()

        if (isPerfect) {
            hit300 = notes
            hit300k = notes / 2
            mod = ModHashMap().apply { put(ModHidden::class) }
            setForcedScore(1_234_567L)
        } else {
            hit100 = notes / 10
            hit50 = notes / 30
            misses = notes / 50
            hit300 = notes - hit100 - hit50 - misses
            hit300k = hit300 / 3
            hit100k = hit100 / 4
            mod = testMods().apply { put(ModDifficultyAdjust::class, ModDifficultyAdjust(ar = 9.5f, od = 8f)) }
            setForcedScore(845_210L)

            // Hits a bit early or late, for an unstable rate.
            for (i in 0 until 200) {
                addHitOffset(((i * 7) % 21 - 9) * 0.0021)
            }
        }

        scoreMaxCombo = notes - misses * 20
    }

    private fun testMods() = ModHashMap().apply {
        put(ModHidden::class)
        put(ModDoubleTime::class)
    }

    private fun firstBeatmap() = LibraryManager.getLibrary().firstOrNull()?.beatmaps?.firstOrNull()

    // The mod menu covers the stage, the button opens it again after it's closed.
    private fun UITestStage.showModMenuButton(modMenu: ModMenu) = textButton {
        anchor = Anchor.Center
        origin = Anchor.Center
        text = "Mods"
        onActionUp = { modMenu.show() }
    }

    // Events the server would send to the room, sent straight to the room scene.
    private fun UITestStage.roomEvents() = linearContainer {
        orientation = LinearContainer.Orientation.Horizontal
        // Next to the name of the room, where they cover the least of it.
        anchor = Anchor.TopCenter
        origin = Anchor.TopCenter
        y = 4f.dip
        spacing = 4f.dip

        var nextPlayerId = 100L

        fun event(name: String, action: () -> Unit) = textButton {
            text = name
            variant = Button.Variant.Surface
            density = Button.Density.Small
            onActionUp = {
                val roomScene = Multiplayer.roomScene

                if (roomScene == null) {
                    log("Not in a room")
                } else {
                    action()
                    log(name)
                }
            }
        }

        event("Player joins") {
            Multiplayer.roomScene?.onPlayerJoin(TestData.roomPlayer(nextPlayerId++))
        }

        event("Player ready") {
            val room = Multiplayer.room ?: return@event
            val player = room.activePlayers.firstOrNull { it.id != Multiplayer.player?.id && it.status != PlayerStatus.Ready }
                ?: return@event
            Multiplayer.roomScene?.onPlayerStatusChange(player.id, PlayerStatus.Ready)
        }

        event("Other host") {
            val room = Multiplayer.room ?: return@event
            val other = room.activePlayers.firstOrNull { it.id != room.host } ?: return@event
            Multiplayer.roomScene?.onRoomHostChange(other.id)
        }

        event("Change beatmap") {
            val beatmap = LibraryManager.getLibrary().randomOrNull()?.beatmaps?.randomOrNull() ?: return@event
            Multiplayer.roomScene?.onRoomBeatmapChange(TestData.roomBeatmap(beatmap))
        }

        event("Chat") {
            val room = Multiplayer.room ?: return@event
            val player = room.activePlayers.randomOrNull() ?: return@event
            Multiplayer.roomScene?.onRoomChatMessage(player.id, SAMPLE_MESSAGES.random())
        }
    }


    private val SAMPLE_MESSAGES = listOf(
        "hi",
        "can we play something easier",
        "gl hf",
        "host pls start",
        "that slider was cursed",
    )
}
