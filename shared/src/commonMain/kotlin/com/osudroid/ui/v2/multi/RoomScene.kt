package com.osudroid.ui.v2.multi

import com.osudroid.beatmaplisting.BeatmapListing
import ru.nsu.ccfit.zuev.osu.game.GameScene
import com.osudroid.OsuGame
import com.osudroid.BeatmapSelection
import com.osudroid.beatmaplisting.BeatmapDownloader
import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.settings.Settings
import com.osudroid.utils.*
import kotlinx.serialization.json.JsonArray

import com.osudroid.ui.v2.SkinSounds
import com.osudroid.BuildSettings
import kotlin.concurrent.atomics.AtomicBoolean
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.multiplayer.api.IPlayerEventListener
import com.osudroid.multiplayer.api.IRoomEventListener
import com.osudroid.multiplayer.api.RoomAPI
import com.osudroid.multiplayer.api.data.PlayerStatus
import com.osudroid.multiplayer.api.data.Room
import com.osudroid.multiplayer.api.data.RoomBeatmap
import com.osudroid.multiplayer.api.data.RoomGameplaySettings
import com.osudroid.multiplayer.api.data.RoomMods
import com.osudroid.multiplayer.api.data.RoomPlayer
import com.osudroid.multiplayer.api.data.RoomTeam
import com.osudroid.multiplayer.api.data.TeamMode
import com.osudroid.multiplayer.api.data.WinCondition
import com.osudroid.mods.ModScoreV2
import com.osudroid.resources.Res
import com.osudroid.ui.v2.BeatmapInfoLayout
import com.osudroid.ui.v2.GameLoaderScene
import com.osudroid.ui.v2.LoaderScene
import com.osudroid.ui.v2.ModsIndicator
import com.osudroid.ui.v2.SkinTextures
import com.osudroid.multiplayer.clearPlayerMods
import com.osudroid.multiplayer.setRoomMods
import com.osudroid.ui.v2.modmenu.ModMenu
import com.osudroid.ui.v2.settings.SettingsMenu
import com.osudroid.ui.v2.songselect.SongSelect
import com.osudroid.utils.async
import com.osudroid.utils.mainThread
import com.osudroid.utils.runSafe
import com.osudroid.utils.updateThread
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.data.ClipMode
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.px
import com.reco1l.verktex.texture.Textures
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.UIBadge
import com.reco1l.verktex.ui.UIButton
import com.reco1l.verktex.ui.UILabeledBadge
import com.reco1l.verktex.ui.UIScene
import com.reco1l.verktex.ui.UISprite
import com.reco1l.verktex.ui.UITextButton
import com.reco1l.verktex.ui.badge
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.container
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.container.scrollableContainer
import com.reco1l.verktex.ui.dialog.UIMessageDialog
import com.reco1l.verktex.ui.labeledBadge
import com.reco1l.verktex.ui.shape.box
import com.reco1l.verktex.ui.sprite
import com.reco1l.verktex.ui.text.UIText
import com.reco1l.verktex.ui.text.faSolidIcon
import com.reco1l.verktex.ui.text.text
import com.reco1l.verktex.ui.textButton
import ru.nsu.ccfit.zuev.audio.Status
import ru.nsu.ccfit.zuev.osu.LibraryManager
import com.reco1l.verktex.ui.toast.Toasts
import com.reco1l.verktex.ui.themed
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.KeyboardEvent

class RoomScene(
    /**
     * The [Room] this [RoomScene] is showing. Updated in-place when the socket reconnects so that this [RoomScene] can
     * be reused and update its current state accordingly.
     */
    var room: Room
) : UIScene(), IRoomEventListener, IPlayerEventListener {

    // Back closes the mod menu or asks to leave the room, once no component consumed it.
    override fun onInputEvent(event: InputEvent): Boolean {
        if (super.onInputEvent(event)) {
            return true
        }

        if (event is KeyboardEvent && event.isBackPress) {
            onBackPressed()
            return true
        }
        return false
    }
    /**
     * Indicates that the host can change beatmap. **This must be `false` when waiting for a beatmap change request**.
     *
     * This is only used if [com.osudroid.multiplayer.Multiplayer.player] is the room host.
     */
    @JvmField
    val isWaitingForBeatmapChange = AtomicBoolean(false)

    /**
     * Indicates that the player can change its status. Its purpose is to await for server changes.
     */
    @JvmField
    val isWaitingForStatusChange = AtomicBoolean(false)

    /**
     * Indicates that the player can change its mods. Its purpose is to await for server changes.
     */
    @JvmField
    val isWaitingForModsChange = AtomicBoolean(false)

    /**
     * The room chat.
     */
    val chat: RoomChat

    /**
     * The mods the player selected in this room.
     */
    val mods = ModHashMap()

    private var modMenu: ModMenu? = null

    /**
     * The match started last, kept for its results.
     */
    var game: GameScene? = null
        private set

    /**
     * The leave dialog that will be shown when the player tries to leave the room.
     */
    val leaveDialog = UIMessageDialog().apply {

        title = "Leave room"
        text = "Are you sure?"

        addButton {
            text = "Yes"
            onActionUp = {
                hide()
                back()
            }
        }

        addButton {
            text = "No"
            onActionUp = {
                hide()
            }
        }

    }


    private lateinit var backgroundSprite: UISprite
    private lateinit var beatmapInfoLayout: BeatmapInfoLayout
    private lateinit var beatmapInfoAlert: UIText
    private lateinit var modsButton: UITextButton
    private lateinit var startButton: UITextButton
    private lateinit var statusButton: UITextButton
    private lateinit var nameText: UIText
    private lateinit var teamModeBadge: UIBadge
    private lateinit var freeModsBadge: UIBadge
    private lateinit var winConditionBadge: UIBadge
    private lateinit var playersBadge: UILabeledBadge
    private lateinit var modsIndicator: ModsIndicator
    private lateinit var playersContainer: UILinearContainer
    private lateinit var changeBeatmapButton: UITextButton
    private lateinit var downloadBeatmapButton: UITextButton


    private var currentPlayers = LongArray(0)
    private var allowAutomaticPlaybackRestart = true


    init {
        RoomAPI.playerEventListener = this
        RoomAPI.roomEventListener = this
        chat = RoomChat()

        background = UIContainer().apply {
            width = Dimension.FillAvailable
            height = Dimension.FillAvailable

            backgroundSprite = sprite {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                scaleType = UISprite.ScaleType.Crop
                textureRegion = SkinTextures["menu-background"]

                if (!Settings[OsuSettings.SafeBeatmapBackground]) {
                    textureRegion = Textures["::background"] ?: textureRegion
                }
            }

            box {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                alpha = 0.9f
                backgroundColor = Verktex.theme.palette.accent.shade(0.1f)
            }
        }

        container {
            width = Dimension.FillAvailable
            height = Dimension.FillAvailable
            padding = Padding(
            left = 80f.px,
            top = 8f.dip,
            right = 80f.px,
            bottom = (8f.dip.toPixels() + (Multiplayer.roomScene?.chat?.buttonHeight ?: 0f)).px
            )

            linearContainer {
                orientation = UILinearContainer.Orientation.Vertical
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable

                container {
                    width = Dimension.FillAvailable
                    padding = Padding(0f.dip, 10f.dip)

                    linearContainer {
                        orientation = UILinearContainer.Orientation.Vertical
                        spacing = 8f.dip

                        nameText = text {
                            text = room.name
                            color = themed { palette.accent }
                            fontSize = themed { typography.h3.size }
                        }

                        linearContainer {
                            spacing = 8f.dip

                            teamModeBadge = badge {
                                density = UIBadge.Density.Small
                            }

                            winConditionBadge = badge {
                                density = UIBadge.Density.Small
                            }

                            playersBadge = labeledBadge {
                                density = UIBadge.Density.Small
                                label = Res.string.multiplayer_room_players
                                value = "0/${room.maxPlayers}"
                            }

                            freeModsBadge = badge {
                                density = UIBadge.Density.Small
                                variant = UIBadge.Variant.Solid
                                text = Res.string.multiplayer_room_free_mods
                                isVisible = false
                            }

                            +ModsIndicator().apply {
                                modsIndicator = this
                            }
                        }
                    }

                    textButton {
                        leadingIcon = faSolidIcon(FAIcon.Gear)
                        anchor = Anchor.CenterRight
                        origin = Anchor.CenterRight
                        onActionUp = {
                            // Shown again when leaving the settings.
                            chat.hide()
                            Verktex.pushScene(SettingsMenu())
                        }
                        text = Res.string.multiplayer_room_settings
                    }
                }

                linearContainer {
                    width = Dimension.FillAvailable
                    height = Dimension.FillAvailable
                    padding = Padding(0f.dip, 8f.dip)
                    spacing = 24f.dip

                    fun UILinearContainer.Section(title: com.reco1l.verktex.localization.LocalizedString, block: UILinearContainer.() -> Unit) {
                        linearContainer {
                            orientation = UILinearContainer.Orientation.Vertical
                            width = Dimension.FillAvailable
                            height = Dimension.FillAvailable
                            spacing = 8f.dip

                            text {
                                text = title.toString().uppercase()
                                color = themed { palette.accent.shade(0.7f) }
                            }

                            block()
                        }
                    }

                    Section(Res.string.multiplayer_room_players) {

                        scrollableContainer {
                            width = Dimension.FillAvailable
                            height = Dimension.FillAvailable
                            scrollAxes = Axis.Y
                            clipping = ClipMode.Bounds

                            linearContainer {
                                orientation = UILinearContainer.Orientation.Vertical
                                width = Dimension.FillAvailable
                                padding = Padding(1f.px)
                                spacing = 4f.dip
                                playersContainer = this
                            }
                        }
                    }

                    Section(Res.string.multiplayer_room_beatmap) {

                        +BeatmapInfoLayout { mods }.apply {
                            padding = Padding(12f.dip)
                            radius = themed { radius.md }
                            backgroundColor = themed { (palette.accent.shade(0.1f)).alpha(0.5f) }
                            isVisible = false

                            beatmapInfoLayout = this
                        }

                        beatmapInfoAlert = text {
                            width = Dimension.FillAvailable
                            alignment = Anchor.Center
                            padding = Padding(12f.dip)
                            radius = themed { radius.md }
                            backgroundColor = themed { (palette.accent.shade(0.1f)).alpha(0.5f) }
                        }

                        linearContainer {
                            width = Dimension.FillAvailable
                            spacing = 8f.dip

                            changeBeatmapButton = textButton {
                                leadingIcon = faSolidIcon(FAIcon.ArrowRightArrowLeft)
                                alignment = Anchor.CenterLeft
                                text = Res.string.multiplayer_room_change_beatmap
                                onActionUp = {
                                    if (Multiplayer.isRoomHost) {
                                        if (LibraryManager.getLibrary().isEmpty()) {
                                            OsuGame.music.pause()
                                            BeatmapListing.show()
                                        }

                                        SongSelect().apply {
                                            loadBeatmaps()
                                            show()
                                        }

                                        // We notify all clients that the host is changing beatmap
                                        RoomAPI.changeBeatmap()
                                    }
                                }
                            }

                            downloadBeatmapButton = textButton {
                                leadingIcon = faSolidIcon(FAIcon.Download)
                                alignment = Anchor.CenterLeft
                                isVisible = false
                            }
                        }

                    }
                }

                container {
                    width = Dimension.FillAvailable
                    padding = Padding(0f.dip, 8f.dip)

                    linearContainer {
                        origin = Anchor.BottomLeft
                        anchor = Anchor.BottomLeft
                        spacing = 8f.dip

                        textButton {
                            leadingIcon = faSolidIcon(FAIcon.ArrowRightFromBracket)
                            text = Res.string.multiplayer_room_leave
                            color = Color4(0xFFFFBFBF)
                            backgroundColor = Color4(0xFF342121)
                            onActionUp = { leaveDialog.show() }
                        }

                        modsButton = textButton {
                            leadingIcon = faSolidIcon(FAIcon.Sliders)
                            text = Res.string.multiplayer_room_mods
                            onActionUp = { showModMenu() }
                        }
                    }

                    linearContainer {
                        orientation = UILinearContainer.Orientation.Vertical
                        origin = Anchor.CenterRight
                        anchor = Anchor.CenterRight
                        width = 160f.dip
                        spacing = 8f.dip

                        textButton {
                            width = Dimension.FillAvailable
                            variant = UIButton.Variant.Solid
                            text = Res.string.multiplayer_room_start_game
                            onActionUp = callback@{

                                if (isWaitingForStatusChange.load() || !Multiplayer.isRoomHost || Multiplayer.player?.status != PlayerStatus.Ready) {
                                    return@callback
                                }

                                if (room.beatmap == null) {
                                    Toasts.showLong(Res.string.multiplayer_room_must_select_beatmap)
                                    return@callback
                                }

                                if (!BuildSettings.MOCK_MULTIPLAYER) {
                                    if (room.teamMode == TeamMode.TeamVS) {
                                        val teams = room.teamMap

                                        if (teams.values.any { it.isEmpty() }) {
                                            Toasts.showLong(Res.string.multiplayer_room_at_least_one_player_per_team)
                                            return@callback
                                        }
                                    }

                                    val players = room.activePlayers.filter { it.status != PlayerStatus.MissingBeatmap }

                                    if (players.size <= 1) {
                                        Toasts.showLong(Res.string.multiplayer_room_at_least_two_players_beatmap)
                                        return@callback
                                    }
                                }

                                SkinSounds["menuhit"].play()
                                RoomAPI.notifyMatchPlay()
                            }
                            startButton = this
                        }

                        textButton {
                            width = Dimension.FillAvailable
                            text = Res.string.multiplayer_room_not_ready
                            onActionUp = callback@{

                                // Atomic guard: only the first tap wins even if two ACTION_UP events
                        // are delivered before the server reply clears the flag.
                        if (!isWaitingForStatusChange.compareAndSet(false, true)) {
                                    return@callback
                                }

                        SkinSounds["menuclick"].play()

                                // Guard: if back() nulled player between the isWaitingForStatusChange check
                        // and here, reset the flag and exit instead of crashing.
                        val player = Multiplayer.player ?: run {
                            isWaitingForStatusChange.store(false)
                            return@callback
                        }

                        when (player.status) {

                            PlayerStatus.NotReady -> {
                                if (room.beatmap == null) {
                                    Toasts.showLong(Res.string.multiplayer_room_cannot_ready_changing_beatmap)
                                    isWaitingForStatusChange.store(false)
                                    return@callback
                                }

                                if (room.teamMode == TeamMode.TeamVS && player.team == null) {
                                    Toasts.showLong(Res.string.multiplayer_room_cannot_ready_no_team)
                                    isWaitingForStatusChange.store(false)
                                    return@callback
                                }

                                        RoomAPI.setPlayerStatus(PlayerStatus.Ready)
                                    }

                                    PlayerStatus.Ready -> invalidateStatus()

                                    PlayerStatus.MissingBeatmap -> {
                                        Toasts.showLong(Res.string.multiplayer_room_cannot_ready_missing_beatmap)
                                        isWaitingForStatusChange.store(false)
                                    }

                                    else -> isWaitingForStatusChange.store(false) /*This case can never happen, the PLAYING status is set when a game starts*/
                                }
                            }
                            statusButton = this
                        }
                    }
                }
            }

        }
    }

    // Update events

    private fun updateBackground(path: String?) {
        val textureRegion = if (path != null) Textures.load("::background", path) else null

        if (textureRegion != null) {
            backgroundSprite.textureRegion = textureRegion
        } else {
            backgroundSprite.textureRegion = SkinTextures["menu-background"]
        }
    }

    private fun updateInformation() {

        updateButtons()

        playersBadge.value = "${room.activePlayers.size}/${room.maxPlayers}"

        winConditionBadge.text = when (room.winCondition) {
                WinCondition.ScoreV1 -> Res.string.multiplayer_room_score_v1
                WinCondition.ScoreV2 -> Res.string.multiplayer_room_score_v2
                WinCondition.Accuracy -> Res.string.multiplayer_room_highest_accuracy
                WinCondition.MaxCombo -> Res.string.multiplayer_room_maximum_combo
            }

        teamModeBadge.text = when (room.teamMode) {
                TeamMode.HeadToHead -> Res.string.multiplayer_room_head_to_head
                TeamMode.TeamVS -> Res.string.multiplayer_room_team_versus
            }

        freeModsBadge.isVisible = room.gameplaySettings.isFreeMod

        nameText.text = room.name
        modsIndicator.mods = room.mods.values
    }

    fun updatePlayerList() = updateThread {
        // Room state is mutated on the socket EventThread (and room itself is replaced on reconnection), so take a
        // single snapshot and derive everything from it. Reading room state multiple times here can observe different
        // player lists, desynchronizing the card count from the player count.
        val room = room
        val players = room.activePlayers

        val shouldReload = playersContainer.childCount != players.size
                || currentPlayers.size != players.size
                || players.indices.any { currentPlayers[it] != players[it].id }

        playersContainer.apply {

            if (shouldReload) {
                clearChildren()

                players.forEach {
                    +RoomPlayerCard().apply {
                        updateState(room, it)
                    }
                }
                currentPlayers = LongArray(players.size) { players[it].id }
            } else {
                players.forEachIndexed { index, player ->
                    val card = this[index] as RoomPlayerCard
                    card.updateState(room, player)
                }
            }
        }
    }

    private fun updateButtons() {
        // Guard: player is nulled by back() when the scene is tearing down; any
        // concurrent call (socket EventThread or update thread) must exit cleanly.
        val player = Multiplayer.player ?: return

        statusButton.apply {
            text = when (player.status) {
                    PlayerStatus.NotReady -> Res.string.multiplayer_room_ready
                    PlayerStatus.Ready -> Res.string.multiplayer_room_not_ready
                    else -> Res.string.multiplayer_room_unable_status
                }

            isEnabled = when (player.status) {
                PlayerStatus.Ready, PlayerStatus.NotReady -> true
                else -> false
            }

            isSelected = player.status == PlayerStatus.NotReady
        }

        val playersReady = room.activePlayers.filter { it.status == PlayerStatus.Ready }

        startButton.apply {
            isVisible = Multiplayer.isRoomHost && player.status == PlayerStatus.Ready

            // isVisible does only hide the button, but we also need to disable it.
            isEnabled = isVisible

            text = when (playersReady.size) {
                room.activePlayers.size -> Res.string.multiplayer_room_start_game
                else -> Res.string.multiplayer_room_force_start_game.format(playersReady.size, room.activePlayers.size)
            }
        }

        modsButton.isEnabled = Multiplayer.isRoomHost || room.gameplaySettings.isFreeMod

        changeBeatmapButton.apply {
            isEnabled = Multiplayer.isRoomHost
            isVisible = Multiplayer.isRoomHost
        }
    }


    private fun updateBeatmap(roomBeatmap: RoomBeatmap?) {

        if (roomBeatmap == null) {
            beatmapInfoLayout.isVisible = false
            beatmapInfoAlert.isVisible = true
            downloadBeatmapButton.apply {
                isVisible = false
                onActionUp = null
            }

            beatmapInfoAlert.text = if (Multiplayer.isRoomHost) {
                    Res.string.multiplayer_room_no_beatmap_selected
                } else {
                    Res.string.multiplayer_room_changing_beatmap
                }
            return
        }

        beatmapInfoLayout.isVisible = true
        beatmapInfoAlert.isVisible = false

        updateBeatmapInfo(roomBeatmap)

        downloadBeatmapButton.apply {
            val beatmapInfo = BeatmapSelection.beatmap

            if (beatmapInfo == null) {
                isVisible = true

                if (roomBeatmap.parentSetID == null) {
                    isEnabled = false
                    leadingIcon = faSolidIcon(FAIcon.Download)
                    text = Res.string.multiplayer_room_not_available_beatmap
                    return@apply
                }

                isEnabled = true
                leadingIcon = faSolidIcon(FAIcon.Download)
                text = Res.string.multiplayer_room_download_beatmap
                onActionUp = {
                    async {
                        try {
                            BeatmapDownloader.downloadBeatmapSet(
                                roomBeatmap.parentSetID!!,
                                "${roomBeatmap.parentSetID} ${roomBeatmap.artist} - ${roomBeatmap.title}"
                            )
                        } catch (e: Exception) {
                            Toasts.showLong(Res.string.multiplayer_room_unable_download.format(e.message))
                            e.printStackTrace()
                        }
                    }
                }

            } else {
                isVisible = false
                onActionUp = null
            }
        }
    }

    @JvmOverloads
    fun updateBeatmapInfo(roomBeatmap: RoomBeatmap? = room.beatmap) {
        val beatmapInfo = BeatmapSelection.beatmap

        if (beatmapInfo != null) {
            beatmapInfoLayout.setBeatmapInfo(beatmapInfo)
        } else {
            beatmapInfoLayout.setBeatmapInfo(roomBeatmap)
        }
    }


    // The player is no longer ready once the game's window loses the focus, in the room or in the song
    // select opened from it.
    override fun onGameWindowFocusChange(hasFocus: Boolean) {
        if (!hasFocus && Multiplayer.isConnected && (Verktex.scene === this || Verktex.scene is SongSelect)) {
            async { runSafe { invalidateStatus() } }
        }
    }


    // Actions

    fun invalidateStatus() {

        // Status shouldn't be changed during reconnection because it's done by server, this function can be called by
        // any of the updating functions.
        if (Multiplayer.isReconnecting) {
            return
        }

        // Guard: player is nulled by back(); if we race past the reconnecting check, exit cleanly.
        val player = Multiplayer.player ?: return

        isWaitingForStatusChange.store(true)

        var newStatus = PlayerStatus.NotReady

        if (room.beatmap != null && BeatmapSelection.beatmap == null) {
            newStatus = PlayerStatus.MissingBeatmap
        }

        if (player.status != newStatus) {
            RoomAPI.setPlayerStatus(newStatus)
        } else {
            isWaitingForStatusChange.store(false)
        }
    }

    // Navigation

    /**
     * Tears down all multiplayer state: cancels reconnection, nulls event listeners,
     * disconnects the socket, cancels pending jobs, and hides the chat.
     *
     * This is the common teardown path shared by [back] (which also navigates to the lobby)
     * and the kicked-during-game handler (which must NOT navigate since the game scene must
     * be allowed to finish).
     *
     * **Must be called on the update thread.**
     */
    fun teardownSession() {
        Multiplayer.cancelReconnection()
        beatmapInfoLayout.cancelCalculation()
        playersContainer.forEach { (it as RoomPlayerCard).cancelJobs() }

        // Null out event listeners before disconnect so any queued socket events that
        // arrive after teardown find no listener to call.
        RoomAPI.roomEventListener = null
        RoomAPI.playerEventListener = null

        runSafe { RoomAPI.disconnect() }

        Multiplayer.room = null
        Multiplayer.roomScene = null
        Multiplayer.player = null
        chat.hide()
    }

    /**
     * Closes the mod menu if it's shown, or asks to leave the room otherwise.
     */
    fun onBackPressed() {
        if (modMenu?.isShown == true) {
            closeModMenu()
        } else {
            leaveDialog.show()
        }
    }

    private fun showModMenu() {
        val menu = modMenu ?: ModMenu(mods).apply { onBack = { closeModMenu() } }

        modMenu = menu
        menu.show()
    }

    /**
     * Hides the mod menu if it's shown, and sends the mods to the server with [sendMods].
     */
    fun closeModMenu(updatePlayerMods: Boolean = true) {
        modMenu?.hide()
        sendMods(updatePlayerMods)
    }

    /**
     * Sends the selected mods to the server, unless playing: as the room's mods for the host, and as
     * the player's mods otherwise when [updatePlayerMods] is true.
     */
    fun sendMods(updatePlayerMods: Boolean = true) {
        if (!Multiplayer.isConnected) {
            return
        }

        if (GameScene.current != null) {
            return
        }

        chat.show()
        isWaitingForModsChange.store(true)

        // The room mods are the same as the host mods
        if (Multiplayer.isRoomHost) {
            RoomAPI.setRoomMods(mods.serializeMods())
        } else if (updatePlayerMods) {
            RoomAPI.setPlayerMods(mods.serializeMods())
        } else {
            isWaitingForModsChange.store(false)
        }
    }

    fun back() {
        teardownSession()

        Verktex.withTreeLock {
            // Leaves everything opened on top of the lobby, which is created again when it's not there.
            while (Verktex.scene !is LobbyScene && Verktex.popScene() != null) {
                continue
            }

            if (Verktex.scene !is LobbyScene) {
                Verktex.pushScene(LobbyScene())
            }
        }
    }

    fun show() {
        if (!Multiplayer.isConnected) {
            back()
            return
        }

        Verktex.withTreeLock {
            if (Verktex.scene is LoaderScene) {
                Verktex.popScene()
            }

            if (Verktex.scene !== this) {
                Verktex.pushScene(this)
            }
        }

        // It may not be shown, e.g. a scene tested on its own in the UI test browser can't open it.
        if (Verktex.scene !== this) {
            return
        }

        if (!isWaitingForBeatmapChange.load()) {
            // onRoomBeatmapChange sets engine.scene = this just above, so it will always
            // find the scene active and call invalidateStatus() internally.  Do NOT call
            // invalidateStatus() again afterwards — that would emit a redundant
            // setPlayerStatus to the server (SI-4).
            onRoomBeatmapChange(room.beatmap)
        }
        // When isWaitingForBeatmapChange == true a beatmap change is already in flight.
        // Emitting a status now would race the imminent onRoomBeatmapChange callback that
        // will call invalidateStatus() with the correct beatmap context.

        chat.show()
        allowAutomaticPlaybackRestart = true
    }

    override fun onUpdate(clock: FrameClock) {
        if (Settings[OsuSettings.PlayMusicPreview] && allowAutomaticPlaybackRestart) {
            val selectedBeatmap = BeatmapSelection.beatmap

            if (selectedBeatmap != null) {
                val music = OsuGame.music

                if (music.status == Status.STOPPED) {
                    music.preLoad(selectedBeatmap.audioPath)
                    music.play()
                }
            }
        }

        super.onUpdate(clock)
    }


    // Communication

    override fun onServerError(error: String) {
        Toasts.showLong(error)
    }

    override fun onRoomChatMessage(uid: Long?, message: String) {

        if (uid != null) {
            // Look up the sender. If they are not yet in playersMap the most likely cause is
            // that chatMessage arrived on the EventThread just before the playerJoined event
            // that was queued right behind it (EH-3). Rather than silently discarding the
            // message, build a temporary stub player so the text is displayed in the chat log.
            // The stub uses "#uid" as its name; once playerJoined fires, any subsequent
            // messages from that player will show their real username.
            val player = room.playersMap[uid] ?: run {
                Multiplayer.log("WARNING: chatMessage from unknown UID $uid — displaying with stub name")
                RoomPlayer(id = uid, name = "#$uid", rank = 0, status = PlayerStatus.NotReady, team = null, mods = RoomMods())
            }

            if (!player.isMuted) {
                chat.onRoomChatMessage(player, message)
            }

        } else {
            chat.onSystemChatMessage(message, "#459FFF")
        }
    }


    // Connection

    override fun onRoomConnect(newRoom: Room) {

        if (Multiplayer.room != newRoom) {
            chat.onSystemChatMessage(Res.string.multiplayer_room_welcome, "#459FFF")
        }

        mods.clearPlayerMods()
        mods.setRoomMods(newRoom.mods, newRoom.gameplaySettings.isFreeMod)

        isWaitingForModsChange.store(true)

        RoomAPI.setPlayerMods(mods.serializeMods())

        updateInformation()
        updatePlayerList()

        if (Multiplayer.isReconnecting) {
            onRoomBeatmapChange(room.beatmap)

            Multiplayer.onReconnectAttempt(true)

            val player = Multiplayer.player ?: return

            // If the server reports Playing, the match started while the player was disconnected.
            if (player.status == PlayerStatus.Playing && BeatmapSelection.beatmap != null) {
                onRoomMatchPlay()
            } else {
                // Always re-sync status after reconnection so that the isWaitingForStatusChange lock
                // (set in onRoomDisconnect) is cleared and the player can toggle ready again.
                invalidateStatus()
            }

            return
        }

        val beatmapInfo = LibraryManager.findBeatmapByMD5(newRoom.beatmap?.md5)
        BeatmapSelection.beatmap = beatmapInfo

        updateBackground(beatmapInfo?.backgroundPath)
        updateBeatmap(newRoom.beatmap)

        show()
    }

    override fun onRoomDisconnect(reason: String?, byUser: Boolean) {

        if (!byUser) {
            // Setting await locks to avoid player emitting events that will be ignored.
            isWaitingForBeatmapChange.store(true)
            isWaitingForStatusChange.store(true)
            isWaitingForModsChange.store(true)

            if (Multiplayer.isReconnecting) {
                // A reconnection-attempt socket dropped before receiving initialConnection.
                // Fail the current attempt so the loop retries immediately rather than staying
                // stuck in the isWaitingAttemptResponse polling branch until the 30s timeout.
                Multiplayer.onReconnectAttempt(false)
            } else {
                chat.onSystemChatMessage(Res.string.multiplayer_room_reconnecting, "#FFBFBF")
                Multiplayer.onReconnect()
            }

            return
        }

        updateThread { back() }
    }

    override fun onRoomConnectFail(error: String?) {
        Multiplayer.log("ERROR: Failed to connect -> $error")

        if (Multiplayer.isReconnecting) {
            Multiplayer.onReconnectAttempt(false)
            return
        }

        updateThread { back() }
    }


    // Room changes

    override fun onRoomNameChange(name: String) {
        room.name = name
        updateThread { updateInformation() }
    }

    override fun onRoomMaxPlayersChange(maxPlayers: Int) {
        // The server caps maxPlayers to the current amount of players in the lobby.
        // If the server sends back the current maxPlayers value, we don't need to update anything.
        if (maxPlayers == room.maxPlayers) {
            return
        }

        room.maxPlayers = maxPlayers
        room.resizePlayers(maxPlayers)

        updateThread { updateInformation() }
        updatePlayerList()
    }

    override fun onRoomBeatmapChange(beatmap: RoomBeatmap?) {

        room.beatmap = beatmap

        BeatmapSelection.beatmap = LibraryManager.findBeatmapByMD5(beatmap?.md5)

        if (Verktex.scene != this) {
            updateThread { updateBeatmapInfo() }
            isWaitingForBeatmapChange.store(false)
            return
        }

        // Notify to the host when other players can't download the beatmap.
        if (Multiplayer.isRoomHost && beatmap != null && beatmap.parentSetID == null) {
            mainThread { Toasts.show(Res.string.multiplayer_room_beatmap_unavailable) }
        }

        invalidateStatus()
        isWaitingForBeatmapChange.store(false)

        val selectedBeatmap = BeatmapSelection.beatmap
        updateThread {
            updateBackground(selectedBeatmap?.backgroundPath)
            updateBeatmap(beatmap)
        }

        if (selectedBeatmap == null) {
            OsuGame.music.stop()
            return
        }

        OsuGame.music.preLoad(selectedBeatmap.audioPath)
        OsuGame.music.play()
    }

    override fun onRoomHostChange(uid: Long) {

        room.host = uid

        val newHostName = room.playersMap[uid]?.name ?: "#$uid"
        chat.onSystemChatMessage(Res.string.multiplayer_room_new_host.format(newHostName), "#459FFF")

        updateThread {
            closeModMenu(false)
            updateButtons()
        }

        updatePlayerList()
    }


    // Mods

    override fun onRoomModsChange(mods: RoomMods) {

        if (!mods.equalsWithContext(room.mods, room.gameplaySettings.isFreeMod)) {
            invalidateStatus()
        }

        room.mods = mods

        isWaitingForModsChange.store(true)

        updateThread {
            // Select the new room mods first, so that the mods sent to the server aren't the
            // previous ones.
            this.mods.setRoomMods(mods, room.gameplaySettings.isFreeMod)
            modMenu?.updateModButtonVisibility()
            RoomAPI.setPlayerMods(this.mods.serializeMods())
            updateInformation()
        }
    }

    override fun onRoomGameplaySettingsChange(settings: RoomGameplaySettings) {
        val wasFreeMod = room.gameplaySettings.isFreeMod

        room.gameplaySettings = settings

        updatePlayerList()

        isWaitingForModsChange.store(true)

        updateThread {
            updateButtons()
            updateInformation()
            closeModMenu(false)
        }

        invalidateStatus()
    }

    /**This method is used purely to update UI in other clients*/
    override fun onPlayerModsChange(uid: Long, mods: RoomMods) {

        val target = room.playersMap[uid] ?: run {
            Multiplayer.log("WARNING: onPlayerModsChange — unknown uid $uid, player may have already left")
            return
        }
        target.mods = mods

        updatePlayerList()

        val player = Multiplayer.player ?: return
        if (uid == player.id) {
            isWaitingForModsChange.store(false)
            updateThread { updateBeatmapInfo() }
        }
    }

    override fun onRoomTeamModeChange(mode: TeamMode) {

        room.teamMode = mode

        updateThread { updateInformation() }
        updatePlayerList()

        isWaitingForStatusChange.store(true)
        invalidateStatus()
    }

    override fun onRoomWinConditionChange(winCondition: WinCondition) {

        room.winCondition = winCondition

        updateThread { updateInformation() }

        if (Multiplayer.isRoomHost) {
            isWaitingForModsChange.store(true)

            // If win condition is Score V2 we add the mod.
            val roomMods = room.mods.apply {

                if (winCondition == WinCondition.ScoreV2)
                    put(ModScoreV2::class)
                else
                    remove(ModScoreV2::class)
            }

            RoomAPI.setRoomMods(roomMods.serializeMods())
        }

        updatePlayerList()
        invalidateStatus()
    }


    // Match

    override fun onRoomMatchPlay() {

        val player = Multiplayer.player ?: return

        if (player.status == PlayerStatus.MissingBeatmap) {
            // This client does not have the beatmap and will never call startGame(), so
            // GameScene.onGameLoad() will never fire and notifyBeatmapLoaded() would never be
            // sent.  The server waits for every client's beatmapLoadComplete before sending
            // allPlayersBeatmapLoadComplete, so one missing-beatmap client would block the
            // match for everyone (EH-5).  Send the ACK immediately so the server can proceed
            // once all other players have loaded.
            Multiplayer.log("INFO: MissingBeatmap — sending immediate beatmapLoadComplete ACK (EH-5)")
            RoomAPI.notifyBeatmapLoaded()
        }

        updateThread {

            if (player.status != PlayerStatus.MissingBeatmap && GameScene.current == null) {

                if (BeatmapSelection.beatmap == null) {
                    Multiplayer.log("WARNING: Attempt to start match with null track.")
                    return@updateThread
                }

                allowAutomaticPlaybackRestart = false
                OsuGame.music.stop()
                game = OsuGame.createGameScene().apply { startGame(BeatmapSelection.beatmap, null, mods) }

            }
        }

        updatePlayerList()
    }

    override fun onRoomMatchStart() {
        updateThread {
            if (Verktex.scene is GameLoaderScene) {
                game?.isReadyToStart = true
            }
        }

        updatePlayerList()
    }

    override fun onRoomMatchSkip() {

        updateThread {
            GameScene.current?.skip()
        }
    }


    // Leaderboard

    override fun onRoomLiveLeaderboard(leaderboard: JsonArray) {
        Multiplayer.onLiveLeaderboard(leaderboard)
    }

    override fun onRoomFinalLeaderboard(leaderboard: JsonArray) {
        Multiplayer.onFinalLeaderboard(leaderboard)
    }

    // Player related events

    override fun onPlayerJoin(player: RoomPlayer) {

        // We send the message if the player wasn't in the room, sometimes this event can be called by a reconnection.
        if (room.addPlayer(player)) {
            chat.onSystemChatMessage(Res.string.multiplayer_room_player_joined.format(player.name, player.id), "#459FFF")
        }

        updateThread { updateInformation() }
        updatePlayerList()
    }

    override fun onPlayerLeft(uid: Long) {

        val player = room.removePlayer(uid)

        if (player != null) {
            chat.onSystemChatMessage(Res.string.multiplayer_room_player_left.format(player.name, player.id), "#459FFF")
        }

        updateThread { updateInformation() }
        updatePlayerList()
    }

    override fun onPlayerKick(uid: Long) {

        val player = Multiplayer.player ?: return

        if (uid == player.id) {

            Multiplayer.log("Kicked from room.")

            updateThread {
                if (GameScene.current != null) {
                    // Kicked while a game is in progress. We cannot navigate away from the
                    // game scene immediately — doing so would abruptly interrupt gameplay.
                    // Instead:
                    //   1. Show the toast so the player knows why they were kicked.
                    //   2. Tear down all multiplayer state (disconnect socket, null globals,
                    //      cancel coroutines, hide chat) so live-score events stop firing.
                    //   3. Clear isMultiplayer so the scoring scene and GameScene treat the
                    //      remainder of the game as a solo session — submitFinalScore() will
                    //      not be called, and ScoringScene.back() will return to SongMenu
                    //      rather than trying to re-enter the (now-disconnected) room.
                    Toasts.showLong(Res.string.multiplayer_room_kicked_gameplay)
                    teardownSession()
                    Multiplayer.isMultiplayer = false

                    return@updateThread
                }

                back()

                UIMessageDialog().apply dialog@{
                    title = Res.string.multiplayer_room_kicked_title
                    text = Res.string.multiplayer_room_kicked_message

                    addButton {
                        text = Res.string.multiplayer_room_kicked_close
                        onActionUp = { this@dialog.hide() }
                    }
                }.show()
            }
            return
        }

        val removedPlayer = room.removePlayer(uid)

        if (removedPlayer != null) {
            chat.onSystemChatMessage(Res.string.multiplayer_room_player_kicked.format(removedPlayer.name, removedPlayer.id), "#FFBFBF")
        }

        updateThread { updateInformation() }
        updatePlayerList()
    }

    override fun onPlayerStatusChange(uid: Long, status: PlayerStatus) {

        val target = room.playersMap[uid] ?: run {
            Multiplayer.log("WARNING: onPlayerStatusChange — unknown uid $uid, player may have already left")
            return
        }
        target.status = status

        val player = Multiplayer.player
        if (player != null && uid == player.id) {
            isWaitingForStatusChange.store(false)
        }

        updateThread { updateInformation() }
        updatePlayerList()
    }

    override fun onPlayerTeamChange(uid: Long, team: RoomTeam?) {

        val target = room.playersMap[uid] ?: run {
            Multiplayer.log("WARNING: onPlayerTeamChange — unknown uid $uid, player may have already left")
            return
        }
        target.team = team

        updatePlayerList()
        updateThread { updateInformation() }
    }

}