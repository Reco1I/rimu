package com.osudroid.game.loading

import com.osudroid.data.BeatmapInfo
import com.osudroid.mods.ModAutoplay
import com.osudroid.utils.ModHashMap
import ru.nsu.ccfit.zuev.osu.menu.ScoreBoardItem

/**
 * What to play: a beatmap with mods, played or watched.
 */
data class PlayRequest(
    val beatmap: BeatmapInfo,

    /**
     * The mods of the play. Loading it removes the ones that aren't relevant to it.
     */
    val mods: ModHashMap,

    /**
     * The replay to watch: a file, or the `https://` address of an online replay, which is downloaded first.
     * Null to play the beatmap.
     */
    val replay: String? = null,

    /**
     * Whether the play is the HUD editor's, played by autoplay in a loop while the HUD is edited.
     */
    val isHUDEditor: Boolean = false,

    /**
     * The scores of the leaderboard shown during gameplay.
     */
    val leaderboardItems: () -> List<ScoreBoardItem> = { emptyList() },

    /**
     * Whether [leaderboardItems] are online scores.
     */
    val isLeaderboardOnline: Boolean = false
) {

    companion object {

        /**
         * The play of the HUD editor on [beatmap].
         */
        fun hudEditor(beatmap: BeatmapInfo) = PlayRequest(
            beatmap = beatmap,
            mods = ModHashMap().apply { put(ModAutoplay::class) },
            isHUDEditor = true
        )
    }
}
