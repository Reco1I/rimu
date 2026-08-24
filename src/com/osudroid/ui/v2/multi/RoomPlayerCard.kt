package com.osudroid.ui.v2.multi

import com.edlplan.ui.fragment.WebViewFragment
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.multiplayer.api.RoomAPI
import com.osudroid.multiplayer.api.data.*
import com.osudroid.multiplayer.api.data.PlayerStatus.*
import com.osudroid.multiplayer.api.data.RoomTeam.Blue
import com.osudroid.multiplayer.api.data.RoomTeam.Red
import com.osudroid.ui.OsuColors
import com.osudroid.ui.v2.*
import com.osudroid.utils.async
import com.osudroid.utils.updateThread
import com.reco1l.verktex.*
import com.reco1l.verktex.component.*
import com.reco1l.verktex.ui.container.*
import com.reco1l.verktex.shape.*
import com.reco1l.verktex.ui.UISprite
import com.reco1l.verktex.ui.sprite.ScaleType
import com.reco1l.verktex.ui.text.CompoundText
import com.reco1l.verktex.ui.UIIcon
import com.reco1l.verktex.ui.text.UIText
import com.reco1l.verktex.theme.Colors
import com.reco1l.verktex.theme.FontSize
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.ui.pct
import com.reco1l.verktex.theme.srem
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.*
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.control.UIDropdown
import com.reco1l.verktex.ui.dialog.UIConfirmDialog
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.data.Vec4
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.ui.Theme
import com.reco1l.verktex.ui.shape.UIBox
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.helper.MD5Calculator
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.osuplus.R

class RoomPlayerCard : UILinearContainer() {
    private val teamColorBar: UIButton
    private val playerButton: RoomPlayerButton

    init {
        width = Dimension.FillAvailable
        orientation = Orientation.Horizontal
        style = {
            spacing = 4f.srem
        }

        teamColorBar = UIButton().apply {
            style = {
                width = 0.025f.pct
                height = Dimension.FillAvailable
                radius = 2f.rem
            }
        }

        playerButton = RoomPlayerButton()
        +playerButton
    }

    fun updateState(room: Room, player: RoomPlayer) {
        playerButton.updateState(room, player)

        if (room.isTeamVersus) {
            if (!teamColorBar.hasParent()) {
                attachChild(teamColorBar, 0)
            }

            teamColorBar.backgroundColor = when (player.team) {
                Blue -> Colors.Blue400 * 0.8f
                Red -> Colors.Red400 * 0.8f
                null -> Theme.current.accentColor * 0.6f
            }

            if (player.id == OnlineManager.getInstance().userId) {
                teamColorBar.onActionUp = { showTeamDropdown() }
            } else {
                teamColorBar.onActionUp = null
            }
        } else {
            teamColorBar.detachSelf()
        }
    }

    fun cancelJobs() {
        playerButton.avatarJob?.cancel()
        playerButton.bannerJob?.cancel()
    }

    private fun showTeamDropdown() {
        UIDropdown(teamColorBar).apply dropdown@{
            width = 100f

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

        private lateinit var nameText: CompoundText
        private lateinit var rankText: UIText

        private lateinit var innerContainer: UILinearContainer
        private var modDisplay: UIComponent? = null

        private val bannerSprite: UIShapedSprite
        private val avatarSprite: UIShapedSprite

        var bannerJob: Job? = null
            private set

        var avatarJob: Job? = null
            private set

        private val defaultBackground = UIBox().apply {
            radius = 12f
            color = Theme.current.accentColor * 0.15f
            alpha = 0.5f
        }

        private var lastPlayerId = -1L
        private val defaultAvatar = ResourceManager.getInstance().getTexture("emptyavatar")

        private val hostIcon = UIIcon(FAIcon.Crown).apply {
            style = { color = it.accentColor }
            measuredSize = Vec2(24f)
        }

        private val mutedIcon = UIIcon(FAIcon.MicrophoneSlash).apply {
            style = { color = OsuColors.redLight }
            measuredSize = Vec2(24f)
        }

        private val missingBeatmapIcon = UISprite().apply {
            textureRegion = ResourceManager.getInstance().getTexture("missing")
            measuredSize = Vec2(24f)
        }

        init {
            width = Density.Full
            clipToBounds = false

            style = Style {
                color = it.accentColor
                alpha = if (isEnabled) 1f else 0.5f
                borderWidth = 2f
                padding = Vec4(2f.srem)
                backgroundColor = Theme.current.accentColor * 0.1f
            }

            bannerSprite = UIShapedSprite().apply {
                inheritAncestorsColor = false

                shape = object : UIBox() {
                    init {
                        radius = 12f
                        color = Color4.Transparent
                    }
                }

                scaleType = ScaleType.Crop
                setColor(0.25f, 0.25f, 0.25f)
            }

            avatarSprite = UIShapedSprite().apply {
                inheritAncestorsColor = false
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                measuredSize = Vec2(50f)

                shape = object : UIBox() {
                    init {
                        radius = 8f
                        color = Color4.Transparent
                    }
                }

                scaleType = ScaleType.Crop
                textureRegion = defaultAvatar
            }
            +avatarSprite

            container {
                width = Density.Full
                height = Density.Full

                innerContainer = linearContainer {
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                    orientation = Orientation.Vertical
                    inheritAncestorsColor = false

                    nameText = compoundText {
                        style = { color = it.accentColor }
                    }

                    rankText = text {
                        anchor = Anchor.CenterRight
                        origin = Anchor.CenterRight
                        style = {
                            color = it.accentColor * 0.8f
                            fontSize = FontSize.SM
                        }
                    }
                }
            }
        }

        override fun onDetached() {
            super.onDetached()

            avatarJob?.cancel()
            bannerJob?.cancel()
        }

        fun updateState(room: Room, player: RoomPlayer) {
            borderColor = when (player.status) {
                Playing -> Theme.current.accentColor
                Ready -> Colors.Green200
                NotReady, MissingBeatmap -> Colors.Red200
            }

            if (lastPlayerId != player.id) {
                loadAvatar(player.id)
                loadBanner(player.id)
            }

            lastPlayerId = player.id
            nameText.text = player.name
            rankText.text = "#${player.rank}"

            updatePlayerIcons(room, player)

            missingBeatmapIcon.isVisible = player.status == MissingBeatmap

            if (Config.isPreferModAcronymInMultiplayer()) {
                if (modDisplay !is UIText) {
                    modDisplay?.detachSelf()

                    modDisplay = UIText().apply {
                        minHeight = 24f // Force to take space even if no mods are enabled
                        style + {
                            fontSize = FontSize.XS
                            color = it.accentColor * 0.8f
                        }
                    }

                    innerContainer += modDisplay!!
                }

                (modDisplay as UIText).text = player.mods.toDisplayModString()
            } else {
                if (modDisplay !is ModsIndicator) {
                    modDisplay?.detachSelf()

                    modDisplay = ModsIndicator().apply {
                        minHeight = 24f // Force to take space even if no mods are enabled
                        iconSize = 24f
                    }

                    innerContainer += modDisplay!!
                }

                (modDisplay as ModsIndicator).mods = if (room.gameplaySettings.isFreeMod) player.mods.values else null
            }

            onActionLongPress = {
                UIDropdown(this@RoomPlayerButton).apply dropdown@{
                    style = {
                        maxWidth = 24f.rem
                    }

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
                                style = {
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
            val resourceManager = ResourceManager.getInstance()
            val avatarUrl = OnlineManager.getAvatarURL(userId)

            avatarJob?.cancel()
            avatarJob = null

            val avatarKey = MD5Calculator.getStringMD5(avatarUrl)
            val loadedTexture = resourceManager.getTextureIfLoaded(avatarKey)

            if (loadedTexture != null) {
                avatarSprite.textureRegion = loadedTexture
            } else {
                avatarSprite.textureRegion = defaultAvatar

                avatarJob = async {
                    ensureActive()

                    if (OnlineManager.getInstance().loadAvatarToTextureManager(avatarUrl)) {
                        ensureActive()

                        val texture = resourceManager.getTextureIfLoaded(avatarKey)

                        updateThread {
                            if (lastPlayerId == userId) {
                                avatarSprite.textureRegion = texture ?: defaultAvatar
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

        private fun loadBanner(userId: Long) {
            val resourceManager = ResourceManager.getInstance()
            val bannerUrl = OnlineManager.getProfileBannerURL(userId)

            bannerJob?.cancel()
            bannerJob = null

            val loadedTexture = resourceManager.getProfileBannerTextureIfLoaded(bannerUrl)

            if (loadedTexture != null) {
                bannerSprite.textureRegion = loadedTexture
                //background = bannerSprite
            } else {
                bannerSprite.textureRegion = null
                //background = defaultBackground

                bannerJob = async {
                    ensureActive()

                    if (OnlineManager.getInstance().loadProfileBannerToTextureManager(bannerUrl)) {
                        ensureActive()

                        val texture = resourceManager.getProfileBannerTextureIfLoaded(bannerUrl)

                        updateThread {
                            if (lastPlayerId == userId) {
                                bannerSprite.textureRegion = texture
                                if (texture != null) {
                                    //background = bannerSprite
                                }
                            }
                        }
                    } else {
                        updateThread {
                            if (lastPlayerId == userId) {
                                //background = defaultBackground
                            }
                        }
                    }
                }
            }
        }

        private fun updatePlayerIcons(room: Room, player: RoomPlayer) {
            hostIcon.detachSelf()
            mutedIcon.detachSelf()
            missingBeatmapIcon.detachSelf()

            nameText.trailingIcon = UILinearContainer().apply {
                orientation = Orientation.Horizontal
                spacing = 6f

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