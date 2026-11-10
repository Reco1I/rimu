package com.osudroid.ui.v2.test

import com.osudroid.OsuGame
import com.osudroid.BeatmapSelection
import com.osudroid.debug.MockRoom
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.multiplayer.api.RoomAPI
import com.osudroid.multiplayer.api.data.PlayerStatus
import com.osudroid.mods.ModDoubleTime
import com.osudroid.mods.ModHidden
import com.osudroid.ui.v2.GameLoaderScene
import com.osudroid.ui.v2.hud.GameplayHUD
import com.osudroid.ui.v2.mainmenu.MainScene
import com.osudroid.utils.ModHashMap
import com.osudroid.ui.v2.mainmenu.MusicPlayer
import com.osudroid.ui.v2.modmenu.ModMenu
import com.osudroid.ui.v2.multi.LobbyScene
import com.osudroid.ui.v2.multi.RoomChat
import com.osudroid.ui.v2.settings.SettingsMenu
import com.osudroid.ui.v2.songselect.SongSelect
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.testing.UITestStage
import com.reco1l.verktex.testing.UITestSuite
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.UIButton
import com.reco1l.verktex.ui.container.UILinearContainer
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
 * the main menu) and going back from it are only logged. Overlays it opens, like the mod menu, are
 * shown. Settings changed here are the game's.
 */
object OsuSceneUITests : UITestSuite("Scenes") {

    init {
        test("Main menu") {
            useGameContext()
            scene(MainScene)
        }

        test("Music panel") {
            useGameContext()

            // Played as the main menu does, which isn't shown here.
            onEachFrame { RythimManager.onUpdate(it.elapsedFrameTime) }
            if (BeatmapSelection.beatmap == null) {
                OsuGame.music.next()
            }

            lateinit var trigger: UIButton
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
            SongSelect.loadBeatmaps()
            scene(SongSelect)
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

        test("Lobby") {
            useGameContext()
            Multiplayer.isMultiplayer = true
            scene(LobbyScene())
        }

        test("Room") {
            useGameContext()
            Multiplayer.isMultiplayer = true

            // The mock socket of debug builds answers as the server would, it ends up showing the
            // room scene, the first scene pushed into the stage.
            val online = OnlineManager.getInstance()
            RoomAPI.connectToRoom(1, online.userId, online.sessionId ?: "")

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
    }


    // The loader waits for the game scene, which never loads here: "Finish loading" marks it ready, as
    // the end of loading the beatmap does.
    private fun UITestStage.gameLoader(isRestart: Boolean, epilepsyWarning: Boolean) {
        useGameContext()

        val beatmap = firstBeatmap()?.copy()?.also { it.epilepsyWarning = epilepsyWarning }
        if (beatmap == null) {
            log("No beatmaps in the library")
            return
        }

        val gameScene = TestGameContext.gameScene
        gameScene.isReadyToStart = false
        // Faded in by the loader once loading is done.
        gameScene.hud = gameScene.hud ?: GameplayHUD()
        onDispose { gameScene.isReadyToStart = false }

        val mods = ModHashMap().apply {
            put(ModHidden::class)
            put(ModDoubleTime::class)
        }

        scene(GameLoaderScene(gameScene, beatmap, mods, isRestart))

        textButton {
            anchor = Anchor.BottomCenter
            origin = Anchor.BottomCenter
            y = (-16f).dip
            text = "Finish loading"
            onActionUp = {
                gameScene.isReadyToStart = true
                log("Game scene ready")
            }
        }
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
        orientation = UILinearContainer.Orientation.Horizontal
        // Next to the name of the room, where they cover the least of it.
        anchor = Anchor.TopCenter
        origin = Anchor.TopCenter
        y = 4f.dip
        spacing = 4f.dip

        var nextPlayerId = 100L

        fun event(name: String, action: () -> Unit) = textButton {
            text = name
            variant = UIButton.Variant.Surface
            density = UIButton.Density.Small
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
