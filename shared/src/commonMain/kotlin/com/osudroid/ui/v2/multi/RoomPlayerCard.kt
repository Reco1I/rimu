package com.osudroid.ui.v2.multi

import com.reco1l.verktex.texture.reference
import com.reco1l.verktex.texture.TextureReference
import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.data.Background
import com.reco1l.verktex.data.ScaleType
import com.reco1l.verktex.data.CornerRadius
import com.reco1l.verktex.settings.Settings
import com.reco1l.verktex.ui.borderWidth
import com.reco1l.verktex.ui.borderColor
import com.reco1l.verktex.Platform
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.multiplayer.api.RoomAPI
import com.osudroid.multiplayer.api.data.*
import com.osudroid.multiplayer.api.data.PlayerStatus.*
import com.osudroid.multiplayer.api.data.RoomTeam.Blue
import com.osudroid.multiplayer.api.data.RoomTeam.Red
import com.osudroid.resources.Res
import com.osudroid.ui.OsuColors
import com.osudroid.ui.v2.*
import com.osudroid.utils.async
import com.osudroid.utils.updateThread
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.Shadow
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.data.pct
import com.reco1l.verktex.data.px
import com.reco1l.verktex.texture.Textures
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.Button
import com.reco1l.verktex.ui.Component
import com.reco1l.verktex.ui.Icon
import com.reco1l.verktex.ui.Sprite
import com.reco1l.verktex.ui.container.LinearContainer
import com.reco1l.verktex.ui.container.container
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.control.Dropdown
import com.reco1l.verktex.ui.dialog.ConfirmDialog
import com.reco1l.verktex.ui.text.Text
import com.reco1l.verktex.ui.text.text
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import com.reco1l.verktex.ui.themed

class RoomPlayerCard : LinearContainer() {
    private val teamColorBar: Button
    private val playerButton: RoomPlayerButton

    init {
        width = Dimension.FillAvailable
        orientation = Orientation.Horizontal
        spacing = 16f.dip

        teamColorBar = Button().apply {
            width = 0.025f.pct
            height = Dimension.FillAvailable
            radius = CornerRadius(32f.dip)
        }

        playerButton = RoomPlayerButton()
        +playerButton
    }

    fun updateState(room: Room, player: RoomPlayer) {
        playerButton.updateState(room, player)

        if (room.isTeamVersus) {
            if (teamColorBar.parent == null) {
                add(0, teamColorBar)
            }

            teamColorBar.background = Background.Color(when (player.team) {
                Blue -> Color4(0xFF60A5FA).shade(0.8f)
                Red -> Color4(0xFFF87171).shade(0.8f)
                null -> Verktex.theme.palette.accent.shade(0.6f)
            })

            if (player.id == OnlineManager.getInstance().userId) {
                teamColorBar.onActionUp = { showTeamDropdown() }
            } else {
                teamColorBar.onActionUp = null
            }
        } else {
            teamColorBar.removeSelf()
        }
    }

    fun cancelJobs() {
        playerButton.avatarJob?.cancel()
    }

    private fun showTeamDropdown() {
        Dropdown(teamColorBar).apply dropdown@{
            width = 100f.px

            addButton {
                text = "Red"
                onActionUp = {
                    RoomAPI.setPlayerTeam(Red)
                    this@dropdown.hide()
                }
            }

            addButton {
                text = "Blue"
                onActionUp = {
                    RoomAPI.setPlayerTeam(Blue)
                    this@dropdown.hide()
                }
            }
        }.show()
    }

    private class RoomPlayerButton : Button() {

        private lateinit var nameText: Text
        private lateinit var iconContainer: LinearContainer
        private lateinit var rankText: Text

        private lateinit var innerContainer: LinearContainer
        private var modDisplay: Component? = null

        private val avatarSprite = Sprite().apply {
            width = fill()
            height = fill()
            radius = themed { CornerRadius(radius.lg) }
            scaleType = ScaleType.Crop
            isVisible = false
        }

        var avatarJob: Job? = null
            private set

        private var lastPlayerId = -1L

        private val hostIcon = statusIcon(FAIcon.Crown).apply {
            color = themed { palette.accent }
        }

        private val mutedIcon = statusIcon(FAIcon.MicrophoneSlash).apply {
            color = OsuColors.redLight
        }

        private val missingBeatmapIcon = statusIcon(FAIcon.FileCircleXmark).apply {
            color = OsuColors.redLight
        }

        init {
            width = fill()
            height = Dimension.WrapContent
            padding = Padding(10f.dip)
            radius = themed { CornerRadius(radius.xl) }
            background = Background.Color(themed { palette.accent.shade(0.1f) })
            borderWidth = 2f.px

            // A background label, behind the rest.
            rankText = text {
                anchor = Anchor.BottomRight
                origin = Anchor.BottomRight
                fontSettings = themed { typography.h1 }
                fontSize = 36f.dip
                color = themed { palette.accent }
                alpha = 0.2f
            }

            container {
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                width = AVATAR_SIZE
                height = AVATAR_SIZE
                radius = themed { CornerRadius(radius.lg) }
                background = Background.Color(themed { palette.accent.shade(0.2f) })

                // Shown until the avatar loads, and for the players without one.
                +Icon(FAIcon.User).apply {
                    anchor = Anchor.Center
                    origin = Anchor.Center
                    iconSize = 24f.dip
                    color = themed { palette.accent.shade(0.6f) }
                }

                +avatarSprite
            }

            innerContainer = linearContainer {
                x = (AVATAR_SIZE.toPixels() + 12f.dip.toPixels()).px
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                orientation = LinearContainer.Orientation.Vertical
                spacing = 2f.dip

                linearContainer {
                    orientation = LinearContainer.Orientation.Horizontal
                    spacing = 8f.dip

                    nameText = text {
                        anchor = Anchor.CenterLeft
                        origin = Anchor.CenterLeft
                        color = themed { palette.accent }
                        fontSettings = themed { typography.h3 }
                    }

                    iconContainer = linearContainer {
                        anchor = Anchor.CenterLeft
                        origin = Anchor.CenterLeft
                        orientation = LinearContainer.Orientation.Horizontal
                        spacing = 6f.dip
                    }
                }
            }
        }

        override fun onDetached() {
            super.onDetached()

            avatarJob?.cancel()
        }

        fun updateState(room: Room, player: RoomPlayer) {
            val statusColor = when (player.status) {
                Playing -> resolvedTheme.palette.accent
                Ready -> READY_COLOR
                NotReady, MissingBeatmap -> NOT_READY_COLOR
            }
            borderColor = statusColor
            shadow = Shadow(statusColor.alpha(0.4f), blur = GLOW_RADIUS)

            if (lastPlayerId != player.id) {
                loadAvatar(player.id)
            }

            lastPlayerId = player.id
            nameText.text = player.name
            rankText.text = "#${player.rank}"

            updatePlayerIcons(room, player)

            missingBeatmapIcon.isVisible = player.status == MissingBeatmap

            if (Settings[OsuSettings.PreferModAcronymInMultiplayer]) {
                if (modDisplay !is Text) {
                    modDisplay?.removeSelf()

                    modDisplay = Text().apply {
                        minHeight = 24f.px // Force to take space even if no mods are enabled
                        fontSize = themed { typography.smallCaption.size }
                        color = themed { palette.accent.shade(0.8f) }
                    }

                    innerContainer += modDisplay!!
                }

                (modDisplay as Text).text = player.mods.toDisplayModString()
            } else {
                if (modDisplay !is ModsIndicator) {
                    modDisplay?.removeSelf()

                    modDisplay = ModsIndicator().apply {
                        minHeight = 24f.px // Force to take space even if no mods are enabled
                        iconSize = 24f.px
                        spacing = (-5f).px
                    }

                    innerContainer += modDisplay!!
                }

                (modDisplay as ModsIndicator).mods = if (room.gameplaySettings.isFreeMod) player.mods.values else null
            }

            onActionLongPress = {
                Dropdown(this@RoomPlayerButton).apply dropdown@{
                    maxWidth = 384f.dip

                    addButton {
                        text = Res.string.multiplayer_room_player_menu_view_profile
                        onActionUp = {
                            Platform.openWebPage(OnlineManager.profileEndpoint + player.id, OnlineManager.profileUserAgent)
                            this@dropdown.hide()
                        }
                    }

                    if (player.id != Multiplayer.player!!.id) {
                        addButton {
                            text = if (player.isMuted) Res.string.multiplayer_room_player_menu_unmute else Res.string.multiplayer_room_player_menu_mute
                            onActionUp = {
                                player.isMuted = !player.isMuted
                                updatePlayerIcons(room, player)
                                this@dropdown.hide()
                            }
                        }

                        if (Multiplayer.isRoomHost) {
                            addButton {
                                text = Res.string.multiplayer_room_player_menu_transfer_host
                                onActionUp = {
                                    ConfirmDialog().apply {
                                        title = Res.string.multiplayer_room_player_transfer_dialog_title.format(player.name)
                                        text = Res.string.multiplayer_room_player_transfer_dialog_message
                                        onConfirm = {
                                            if (Multiplayer.isConnected) {
                                                RoomAPI.setRoomHost(player.id)
                                            }
                                        }
                                    }.show()
                                    this@dropdown.hide()
                                }
                            }

                            addButton {
                                text = Res.string.multiplayer_room_player_menu_kick
                                color = Color4("#FFBFBF")
                                onActionUp = {
                                    ConfirmDialog().apply {
                                        title = Res.string.multiplayer_room_player_kick_dialog_title.format(player.name)
                                        text = Res.string.multiplayer_room_player_kick_dialog_message
                                        onConfirm = {
                                            if (Multiplayer.isConnected) {
                                                RoomAPI.kickPlayer(player.id)
                                            }
                                        }
                                    }.show()
                                    this@dropdown.hide()
                                }
                            }
                        }
                    }

                }.show()
            }
        }

        private fun loadAvatar(userId: Long) {
            val avatarUrl = OnlineManager.getAvatarURL(userId)

            avatarJob?.cancel()
            avatarJob = null

            val loadedTexture = CachedTextures.store[CachedTextures.keyOf(avatarUrl)]

            if (loadedTexture != null) {
                showAvatar(loadedTexture.reference)
            } else {
                showAvatar(null)

                avatarJob = async {
                    ensureActive()

                    if (OnlineManager.getInstance().loadAvatarToTextureManager(avatarUrl)) {
                        ensureActive()

                        updateThread {
                            if (lastPlayerId == userId) {
                                showAvatar(CachedTextures[avatarUrl]?.reference)
                            }
                        }
                    } else {
                        updateThread {
                            if (lastPlayerId == userId) {
                                showAvatar(null)
                            }
                        }
                    }
                }
            }
        }

        // The placeholder behind it shows while it's hidden.
        private fun showAvatar(texture: TextureReference?) {
            avatarSprite.texture = texture
            avatarSprite.isVisible = texture != null
        }

        private fun statusIcon(code: Int) = Icon(code).apply {
            anchor = Anchor.CenterLeft
            origin = Anchor.CenterLeft
            iconSize = themed { typography.h3.size }
        }

        private fun updatePlayerIcons(room: Room, player: RoomPlayer) {
            iconContainer.clearChildren()

            iconContainer.apply {
                if (player.id == room.host) {
                    +hostIcon
                }

                if (player.isMuted) {
                    +mutedIcon
                }

                if (player.status == MissingBeatmap) {
                    +missingBeatmapIcon
                }
            }
        }
    }

    companion object {

        /**
         * How far the glow of a card reaches past it.
         */
        val GLOW_RADIUS = 8f.dip

        private val AVATAR_SIZE = 50f.dip

        private val READY_COLOR = Color4(0xFF86EFAC)
        private val NOT_READY_COLOR = Color4(0xFFFCA5A5)
    }
}
