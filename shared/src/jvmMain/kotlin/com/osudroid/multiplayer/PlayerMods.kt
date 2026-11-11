package com.osudroid.multiplayer

import com.osudroid.mods.ModDoubleTime
import com.osudroid.mods.ModNightCore
import com.osudroid.mods.ModScoreV2
import com.osudroid.multiplayer.api.data.RoomMods
import com.osudroid.settings.OsuSettings
import com.osudroid.utils.ModHashMap
import com.reco1l.verktex.settings.Settings

/**
 * Removes the mods the player may change. In a room, the mods only the host can change stay, and so
 * does ScoreV2, which the ScoreV2 win condition uses.
 */
fun ModHashMap.clearPlayerMods() {
    val room = Multiplayer.room ?: return clear()
    val isHost = Multiplayer.isRoomHost

    for (mod in values) {
        val isKept = (!isHost && room.gameplaySettings.isFreeMod && !mod.isValidForMultiplayerAsFreeMod) ||
            mod is ModScoreV2

        if (!isKept) {
            remove(mod::class)
        }
    }
}

/**
 * Sets these mods, the player's in a room, to the room's. With free mods, the player keeps the mods
 * they may choose.
 */
fun ModHashMap.setRoomMods(roomMods: RoomMods, isFreeMod: Boolean) {
    if (isFreeMod) {
        for (mod in values) {
            if (!mod.isValidForMultiplayerAsFreeMod && mod !in roomMods) {
                remove(mod::class)
            }
        }
    } else {
        clearPlayerMods()
    }

    for (mod in roomMods.values) {
        if (mod.isValidForMultiplayer && (!isFreeMod || !mod.isValidForMultiplayerAsFreeMod) && mod !in this) {
            put(mod)
        }
    }

    if (!Multiplayer.isRoomHost) {
        val useNightCore = Settings[OsuSettings.UseNightcoreOnMultiplayer]

        if (useNightCore && ModDoubleTime::class in this) {
            remove(ModDoubleTime::class)
            put(ModNightCore())
        } else if (!useNightCore && ModNightCore::class in this) {
            remove(ModNightCore::class)
            put(ModDoubleTime())
        }
    }

    if (ModScoreV2::class in roomMods) {
        if (ModScoreV2::class !in this) {
            put(ModScoreV2())
        }
    } else {
        remove(ModScoreV2::class)
    }
}
