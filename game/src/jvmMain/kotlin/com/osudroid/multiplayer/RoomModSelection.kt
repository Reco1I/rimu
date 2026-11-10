package com.osudroid.multiplayer

import com.osudroid.mods.ModDoubleTime
import com.osudroid.mods.ModNightCore
import com.osudroid.mods.ModScoreV2
import com.osudroid.mods.ModSelection
import com.osudroid.multiplayer.api.data.RoomMods
import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.settings.Settings

/**
 * Unselects the mods the player may change. In a room, the mods only the host can change stay, and
 * so does ScoreV2, which the ScoreV2 win condition uses.
 */
fun ModSelection.clearPlayerMods() {
    val room = Multiplayer.room ?: return clear()
    val isHost = Multiplayer.isRoomHost

    clear { mod ->
        (!isHost && room.gameplaySettings.isFreeMod && !mod.isValidForMultiplayerAsFreeMod) || mod is ModScoreV2
    }
}

/**
 * Selects the mods of a room. With free mods, the player keeps the mods they may choose.
 */
fun ModSelection.setRoomMods(roomMods: RoomMods, isFreeMod: Boolean) {
    if (isFreeMod) {
        for (mod in mods.values) {
            if (!mod.isValidForMultiplayerAsFreeMod && mod !in roomMods) {
                remove(mod)
            }
        }
    } else {
        clearPlayerMods()
    }

    for (mod in roomMods.values) {
        if (mod.isValidForMultiplayer && (!isFreeMod || !mod.isValidForMultiplayerAsFreeMod)) {
            add(mod)
        }
    }

    if (!Multiplayer.isRoomHost) {
        val doubleTime = mods.ofType<ModDoubleTime>()
        val nightCore = mods.ofType<ModNightCore>()

        if (Settings[OsuSettings.UseNightcoreOnMultiplayer] && doubleTime != null) {
            remove(doubleTime)
            add(ModNightCore())
        } else if (!Settings[OsuSettings.UseNightcoreOnMultiplayer] && nightCore != null) {
            remove(nightCore)
            add(ModDoubleTime())
        }
    }

    if (ModScoreV2::class in roomMods) {
        add(ModScoreV2())
    } else {
        remove(ModScoreV2::class)
    }
}
