package com.osudroid.ui.v2.multi

import com.osudroid.resources.R.string
import com.osudroid.multiplayer.api.*
import com.osudroid.multiplayer.api.data.*
import com.osudroid.ui.v2.LoaderScene
import com.osudroid.ui.v2.setText
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.data.ClipMode
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.pct
import com.reco1l.verktex.ui.UIButton
import com.reco1l.verktex.ui.UITextButton
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.UIScrollableContainer
import com.reco1l.verktex.ui.dialog.UIDialog
import com.reco1l.verktex.ui.form.*
import com.osudroid.utils.async
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.helper.*
import ru.nsu.ccfit.zuev.osu.online.*

class RoomCreateDialog(lobbyScene: LobbyScene) : UIDialog<UIScrollableContainer>(
    UIScrollableContainer().apply {
    scrollAxes = Axis.Y
    width = Dimension.FillAvailable
    clipping = ClipMode.Bounds
    padding = Padding(8f.dip, 0f.dip)
    maxHeight = 0.55f.pct
}) {
    init {

        val form = FormContainer().apply {
            width = Dimension.FillAvailable
            orientation = UILinearContainer.Orientation.Vertical
            padding = Padding(0f.dip, 16f.dip)

            onSubmit = { data ->
                async {
                    LoaderScene().show()

                    val name = data["name"]?.toString() ?: StringTable.format(string.multiplayer_lobby_create_room_name_default, OnlineManager.getInstance().username)
                    val password = data["password"]?.toString().orEmpty().takeUnless(String::isBlank)
                    val capacity = (data["capacity"] as Number).toInt()

                    val beatmap = GlobalManager.getInstance().selectedBeatmap?.let {

                        RoomBeatmap(
                            md5 = it.md5,
                            title = it.title,
                            artist = it.artist,
                            creator = it.creator,
                            version = it.version
                        )
                    }

                    var signStr = "${OnlineManager.getInstance().userId}_${name}_${capacity}"
                    if (password != null) {
                        signStr += "_${password}"
                    }
                    signStr += "_${RoomAPI.API_VERSION}_${OnlineManager.getInstance().sessionId}"

                    try {

                        val roomId = LobbyAPI.createRoom(
                            name = name,
                            beatmap = beatmap,
                            hostUID = OnlineManager.getInstance().userId,
                            sessionId = OnlineManager.getInstance().sessionId,
                            sign = SecurityUtils.signRequest(signStr),
                            password = password,
                            maxPlayers = capacity
                        )

                        RoomAPI.connectToRoom(
                            roomId = roomId,
                            userId = OnlineManager.getInstance().userId,
                            gameSessionId = OnlineManager.getInstance().sessionId,
                            roomPassword = password
                        )

                    } catch (e: Exception) {
                        // Removes the loader scene, leaving the lobby.
                        Verktex.withTreeLock { Verktex.popScene() }
                        ToastLogger.showText("Failed to create a room: ${e.message}", true)
                        e.printStackTrace()
                    }

                }
            }

            +FormInput(StringTable.format(string.multiplayer_lobby_create_room_name_default, OnlineManager.getInstance().username)).apply {
                key = "name"
                width = Dimension.FillAvailable
                label = StringTable.get(string.multiplayer_lobby_room_name)
            }

            +FormInput().apply {
                key = "password"
                width = Dimension.FillAvailable
                label = StringTable.get(string.multiplayer_lobby_room_password)
            }

            +FormSlider(8f).apply {
                key = "capacity"
                width = Dimension.FillAvailable
                label = StringTable.get(string.multiplayer_lobby_room_capacity)
                control.max = 16f
                control.min = 2f
                control.step = 1f
                valueFormatter = { it.toInt().toString() }
            }

        }

        innerContent += form

        title = StringTable.get(string.multiplayer_lobby_create_room)

        addButton(UITextButton().apply {
            setText(string.multiplayer_lobby_create_room_accept)
            variant = UIButton.Variant.Solid
            onActionUp = {
                form.submit()
            }
        })

        addButton(UITextButton().apply {
            setText(string.multiplayer_lobby_create_room_cancel)
            onActionUp = { hide() }
        })
    }
}
