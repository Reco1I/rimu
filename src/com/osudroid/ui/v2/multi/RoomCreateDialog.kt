package com.osudroid.ui.v2.multi

import com.osudroid.resources.R.string
import com.osudroid.multiplayer.api.*
import com.osudroid.multiplayer.api.data.*
import com.osudroid.ui.v2.LoaderScene
import com.reco1l.verktex.*
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.theme.srem
import com.reco1l.verktex.ui.vh
import com.reco1l.verktex.ui.*
import com.reco1l.verktex.ui.container.Orientation
import com.reco1l.verktex.ui.container.UIScrollableContainer
import com.reco1l.verktex.ui.dialog.UIDialog
import com.reco1l.verktex.ui.form.*
import com.reco1l.verktex.data.Vec4
import com.reco1l.toolkt.kotlin.*
import com.reco1l.verktex.data.Axis
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.helper.*
import ru.nsu.ccfit.zuev.osu.online.*

class RoomCreateDialog(lobbyScene: LobbyScene) : UIDialog<UIScrollableContainer>(
    UIScrollableContainer().apply {
    scrollAxes = Axis.Y
    width = Dimension.FillAvailable
    clipToBounds = true
    style = {
        height = 0.55f.vh
        padding = Vec4(2f.srem, 0f)
    }
}) {
    init {

        val form = FormContainer().apply {
            width = Dimension.FillAvailable
            orientation = Orientation.Vertical
            style = {
                padding = Vec4(0f, 4f.srem)
            }

            onSubmit = { data ->
                async {
                    LoaderScene().show()

                    val name = data.getString("name") ?: StringTable.format(string.multiplayer_lobby_create_room_name_default, OnlineManager.getInstance().username)
                    val password = data.optString("password").takeUnless(String::isBlank)
                    val capacity = data.getDouble("capacity").toInt()

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
                        Engine.current.scene = lobbyScene
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

        innerContent.attachChild(form)

        title = StringTable.get(string.multiplayer_lobby_create_room)

        addButton(UITextButton().apply {
            setText(string.multiplayer_lobby_create_room_accept)
            colorVariant = ColorVariant.Primary
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
