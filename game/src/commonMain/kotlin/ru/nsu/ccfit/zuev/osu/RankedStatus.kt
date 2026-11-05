package ru.nsu.ccfit.zuev.osu

import com.osudroid.resources.Res
import com.osudroid.resources.StringResource
import kotlin.jvm.JvmStatic


enum class RankedStatus(
    /**
     * The string resource of the status name, recommended to use this rather than name().
     */
    val stringId: StringResource
) {

    ranked(Res.string.ranked_status_ranked),
    approved(Res.string.ranked_status_approved),
    qualified(Res.string.ranked_status_qualified),
    loved(Res.string.ranked_status_loved),
    pending(Res.string.ranked_status_pending),
    workInProgress(Res.string.ranked_status_wip),
    graveyard(Res.string.ranked_status_graveyard);

    val value: Int
        get() = when (this) {
            ranked -> 1
            approved -> 2
            qualified -> 3
            loved -> 4
            pending -> 0
            workInProgress -> -1
            graveyard -> -2
        }


    companion object {
        @JvmStatic
        fun valueOf(value: Int): RankedStatus {
            return when (value) {
                1 -> ranked
                2 -> approved
                3 -> qualified
                4 -> loved
                0 -> pending
                -1 -> workInProgress
                -2 -> graveyard
                else -> throw IllegalArgumentException("Invalid value: $value")
            }
        }
    }
}
