package com.osudroid.ui.v2.multi

import com.reco1l.verktex.ui.toast.Toasts
import com.reco1l.verktex.ui.text.spans
import com.osudroid.resources.Res.string
import com.osudroid.multiplayer.*
import com.osudroid.multiplayer.api.*
import com.osudroid.multiplayer.api.data.*
import com.osudroid.ui.v2.*
import com.osudroid.utils.async
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.data.px
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.UIBadge
import com.reco1l.verktex.ui.UIButton
import com.reco1l.verktex.ui.UITextButton
import com.reco1l.verktex.ui.badge
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.dialog.UIDialog
import com.reco1l.verktex.ui.form.*
import com.reco1l.verktex.ui.labeledBadge
import com.reco1l.verktex.ui.text.text
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.helper.*
import ru.nsu.ccfit.zuev.osu.online.*
import com.reco1l.verktex.ui.themed

class RoomButton(val lobbyScene: LobbyScene, val room: Room) : UIButton() {

    init {
        width = fill()
        height = Dimension.WrapContent
        backgroundColor = themed { (palette.accent.shade(0.1f)).alpha(0.8f) }
        padding = Padding(12f.dip)

        linearContainer {
            orientation = UILinearContainer.Orientation.Vertical
            width = fill()
            spacing = 4f.dip

            linearContainer {
                width = fill()
                spacing = 8f.dip

                text {
                    width = fill()
                    color = themed { palette.accent }
                    spans {
                        icon(if (room.isLocked) FAIcon.Lock else FAIcon.LockOpen)
                        space(8f.dip)
                        +room.name
                    }
                }

                text {
                    scale = 0.85f
                    fontSize = themed { typography.smallBody.size }
                    color = themed { palette.accent }
                    text = when (room.status) {
                        RoomStatus.ChangingBeatmap -> string.multiplayer_room_status_changing_beatmap
                        RoomStatus.Playing -> string.multiplayer_room_status_playing
                        else -> string.multiplayer_room_status_idle
                    }
                }
            }

            text {
                text = room.playerNames.takeUnless { it.isEmpty() }
                    ?: if (room.playerCount > 0) "${room.playerCount} player${if (room.playerCount != 1) "s" else ""}"
                       else string.multiplayer_room_no_players
                fontSize = themed { typography.smallCaption.size }
                color = themed { palette.accent.alpha(0.95f) }
            }

            linearContainer {
                spacing = 8f.dip

                badge {
                    density = UIBadge.Density.Small
                    text = when (room.teamMode) {
                            TeamMode.HeadToHead -> string.multiplayer_room_head_to_head
                            TeamMode.TeamVS -> string.multiplayer_room_team_versus
                        }
                }

                badge {
                    density = UIBadge.Density.Small
                    text = when (room.winCondition) {
                            WinCondition.ScoreV1 -> string.multiplayer_room_score_v1
                            WinCondition.ScoreV2 -> string.multiplayer_room_score_v2
                            WinCondition.Accuracy -> string.multiplayer_room_highest_accuracy
                            WinCondition.MaxCombo -> string.multiplayer_room_maximum_combo
                        }
                }

                labeledBadge {
                    density = UIBadge.Density.Small
                    label = string.multiplayer_room_players
                    value = "${room.playerCount}/${room.maxPlayers}"
                }

                if (room.gameplaySettings.isFreeMod) {
                    badge {
                        density = UIBadge.Density.Small
                        variant = UIBadge.Variant.Solid
                        text = string.multiplayer_room_free_mods
                    }
                }
            }

            if (room.mods.isNotEmpty()) {
                +ModsIndicator().apply {
                    mods = room.mods.values
                    iconSize = 24f.px
                    spacing = (-5f).px
                }
            }
        }

        onActionUp = {
            if (room.isLocked) {

                val form: FormContainer

                object : UIDialog<FormContainer>(FormContainer().apply {
                    width = fill()
                    form = this

                    +FormInput().apply {
                        key = "password"
                        width = fill()
                        label = string.multiplayer_lobby_room_password
                    }

                    onSubmit = {
                        connectToRoom(it["password"]?.toString().orEmpty())
                    }
                }) {
                    init {
                        title = string.multiplayer_lobby_join_room

                        addButton(UITextButton().apply {
                            text = string.multiplayer_lobby_join_room_accept
                            variant = UIButton.Variant.Solid
                            onActionUp = { form.submit() }
                        })

                        addButton(UITextButton().apply {
                            text = string.multiplayer_lobby_join_room_cancel
                            onActionUp = { hide() }
                        })
                    }
                }.show()
            } else {
                connectToRoom()
            }
        }
    }


    fun connectToRoom(password: String? = null) {

        Multiplayer.log("Trying to connect socket...")
        LoaderScene().show()

        async {

            try {
                RoomAPI.connectToRoom(
                    roomId = room.id,
                    userId = OnlineManager.getInstance().userId,
                    gameSessionId = OnlineManager.getInstance().sessionId,
                    roomPassword = password
                )
            } catch (e: Exception) {
                Toasts.showLong("Failed to connect to the room: ${e.javaClass} - ${e.message}")
                Multiplayer.log(e)

                // Removes the loader scene, leaving the lobby.
                Verktex.withTreeLock { Verktex.popScene() }
            }
        }

    }

}