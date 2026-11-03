package com.osudroid.ui.v2.multi

import com.reco1l.verktex.ui.borderWidth
import com.reco1l.verktex.ui.borderColor
import com.edlplan.ui.fragment.WebViewFragment
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.multiplayer.api.RoomAPI
import com.osudroid.multiplayer.api.data.*
import com.osudroid.multiplayer.api.data.PlayerStatus.*
import com.osudroid.multiplayer.api.data.RoomTeam.Blue
import com.osudroid.multiplayer.api.data.RoomTeam.Red
import com.osudroid.resources.R
import com.osudroid.ui.OsuColors
import com.osudroid.ui.v2.*
import com.osudroid.utils.async
import com.osudroid.utils.updateThread
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.data.pct
import com.reco1l.verktex.data.px
import com.reco1l.verktex.texture.Textures
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.UIButton
import com.reco1l.verktex.ui.UIComponent
import com.reco1l.verktex.ui.UIIcon
import com.reco1l.verktex.ui.UISprite
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.container
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.control.UIDropdown
import com.reco1l.verktex.ui.dialog.UIConfirmDialog
import com.reco1l.verktex.ui.text.UIText
import com.reco1l.verktex.ui.text.text
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osu.online.OnlineManager

class RoomPlayerCard : UILinearContainer() {
    private val teamColorBar: UIButton
    private val playerButton: RoomPlayerButton

    init {
        width = Dimension.FillAvailable
        orientation = Orientation.Horizontal
        spacing = 16f.dip

        teamColorBar = UIButton().apply {
            style += Style {
                width = 0.025f.pct
                height = Dimension.FillAvailable
                radius = 32f.dip
            }
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

            teamColorBar.backgroundColor = when (player.team) {
                Blue -> Color4(0xFF60A5FA) * 0.8f
                Red -> Color4(0xFFF87171) * 0.8f
                null -> Verktex.theme.palette.accent * 0.6f
            }

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
        UIDropdown(teamColorBar).apply dropdown@{
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

    private class RoomPlayerButton : UIButton() {

        private lateinit var nameText: UIText
        private lateinit var iconContainer: UILinearContainer
        private lateinit var rankText: UIText

        private lateinit var innerContainer: UILinearContainer
        private var modDisplay: UIComponent? = null

        private val avatarSprite: UISprite

        var avatarJob: Job? = null
            private set

        private var lastPlayerId = -1L
        private val defaultAvatar = SkinTextures["emptyavatar"]

        private val hostIcon = UIIcon(FAIcon.Crown).apply {
            width = 24f.px
            height = 24f.px
            style = Style { theme -> color = theme.palette.accent }
        }

        private val mutedIcon = UIIcon(FAIcon.MicrophoneSlash).apply {
            width = 24f.px
            height = 24f.px
            color = OsuColors.redLight
        }

        private val missingBeatmapIcon = UISprite().apply {
            width = 24f.px
            height = 24f.px
            textureRegion = SkinTextures["missing"]
        }

        init {
            width = fill()

            style += Style { theme ->
                height = Dimension.WrapContent
                color = theme.palette.accent
                alpha = if (isEnabled) 1f else 0.5f
                borderWidth = 2f.px
                padding = Padding(8f.dip)
                backgroundColor = theme.palette.accent * 0.1f
            }

            avatarSprite = UISprite().apply {
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                width = 50f.px
                height = 50f.px
                radius = 8f.px
                scaleType = UISprite.ScaleType.Crop
                textureRegion = defaultAvatar
            }
            +avatarSprite

            container {
                width = fill()

                innerContainer = linearContainer {
                    x = 58f.px
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                    orientation = UILinearContainer.Orientation.Vertical

                    linearContainer {
                        orientation = UILinearContainer.Orientation.Horizontal
                        spacing = 8f.dip

                        nameText = text {
                            style = Style { theme -> color = theme.palette.accent }
                        }

                        iconContainer = linearContainer {
                            orientation = UILinearContainer.Orientation.Horizontal
                            spacing = 6f.px
                        }
                    }

                    rankText = text {
                        anchor = Anchor.CenterRight
                        origin = Anchor.CenterRight
                        style = Style { theme ->
                            color = theme.palette.accent * 0.8f
                            fontSize = theme.typography.smallBody.size
                        }
                    }
                }
            }
        }

        override fun onDetached() {
            super.onDetached()

            avatarJob?.cancel()
        }

        fun updateState(room: Room, player: RoomPlayer) {
            borderColor = when (player.status) {
                Playing -> Verktex.theme.palette.accent
                Ready -> Color4(0xFFBBF7D0)
                NotReady, MissingBeatmap -> Color4(0xFFFECACA)
            }

            if (lastPlayerId != player.id) {
                loadAvatar(player.id)
            }

            lastPlayerId = player.id
            nameText.text = player.name
            rankText.text = "#${player.rank}"

            updatePlayerIcons(room, player)

            missingBeatmapIcon.isVisible = player.status == MissingBeatmap

            if (Config.isPreferModAcronymInMultiplayer()) {
                if (modDisplay !is UIText) {
                    modDisplay?.removeSelf()

                    modDisplay = UIText().apply {
                        minHeight = 24f.px // Force to take space even if no mods are enabled
                        style = Style { theme ->
                            fontSize = theme.typography.smallCaption.size
                            color = theme.palette.accent * 0.8f
                        }
                    }

                    innerContainer += modDisplay!!
                }

                (modDisplay as UIText).text = player.mods.toDisplayModString()
            } else {
                if (modDisplay !is ModsIndicator) {
                    modDisplay?.removeSelf()

                    modDisplay = ModsIndicator().apply {
                        minHeight = 24f.px // Force to take space even if no mods are enabled
                        style = Style {
                            iconSize = 24f.px
                            spacing = (-5f).px
                        }
                    }

                    innerContainer += modDisplay!!
                }

                (modDisplay as ModsIndicator).mods = if (room.gameplaySettings.isFreeMod) player.mods.values else null
            }

            onActionLongPress = {
                UIDropdown(this@RoomPlayerButton).apply dropdown@{
                    maxWidth = 384f.dip

                    addButton {
                        setText(R.string.multiplayer_room_player_menu_view_profile)
                        onActionUp = {
                            WebViewFragment()
                                .setURL(WebViewFragment.PROFILE_URL + player.id)
                                .show()
                            this@dropdown.hide()
                        }
                    }

                    if (player.id != Multiplayer.player!!.id) {
                        addButton {
                            setText(if (player.isMuted) R.string.multiplayer_room_player_menu_unmute else R.string.multiplayer_room_player_menu_mute)
                            onActionUp = {
                                player.isMuted = !player.isMuted
                                updatePlayerIcons(room, player)
                                this@dropdown.hide()
                            }
                        }

                        if (Multiplayer.isRoomHost) {
                            addButton {
                                setText(R.string.multiplayer_room_player_menu_transfer_host)
                                onActionUp = {
                                    UIConfirmDialog().apply {
                                        title = StringTable.format(R.string.multiplayer_room_player_transfer_dialog_title, player.name)
                                        text = StringTable.get(R.string.multiplayer_room_player_transfer_dialog_message)
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
                                setText(R.string.multiplayer_room_player_menu_kick)
                                tintChildren = true
                                style += Style {
                                    color = Color4("#FFBFBF")
                                }
                                onActionUp = {
                                    UIConfirmDialog().apply {
                                        title = StringTable.format(R.string.multiplayer_room_player_kick_dialog_title, player.name)
                                        text = StringTable.get(R.string.multiplayer_room_player_kick_dialog_message)
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

            val loadedTexture = Textures[CachedTextures.keyOf(avatarUrl)]

            if (loadedTexture != null) {
                avatarSprite.textureRegion = loadedTexture
            } else {
                avatarSprite.textureRegion = defaultAvatar

                avatarJob = async {
                    ensureActive()

                    if (OnlineManager.getInstance().loadAvatarToTextureManager(avatarUrl)) {
                        ensureActive()

                        updateThread {
                            if (lastPlayerId == userId) {
                                avatarSprite.textureRegion = CachedTextures[avatarUrl] ?: defaultAvatar
                            }
                        }
                    } else {
                        updateThread {
                            if (lastPlayerId == userId) {
                                avatarSprite.textureRegion = defaultAvatar
                            }
                        }
                    }
                }
            }
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
}
