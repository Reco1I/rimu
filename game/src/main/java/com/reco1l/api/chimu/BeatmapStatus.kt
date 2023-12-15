package com.reco1l.api.chimu

import androidx.annotation.IntDef

/**
 * Annotation integer definition for beatmap status, based on [Chimu](https://github.com/Chimu-moe/node-chimu-api/blob/master/src/lib.ts).
 */
@IntDef(value = [
    BeatmapStatus.GRAVEYARD,
    BeatmapStatus.WIP,
    BeatmapStatus.PENDING,
    BeatmapStatus.RANKED,
    BeatmapStatus.APPROVED,
    BeatmapStatus.QUALIFIED,
    BeatmapStatus.LOVED
])
annotation class BeatmapStatus
{
    companion object
    {
        const val GRAVEYARD = -2
        const val WIP = -1
        const val PENDING = 0
        const val RANKED = 1
        const val APPROVED = 2
        const val QUALIFIED = 3
        const val LOVED = 4
    }
}
