package com.osudroid.data

import com.osudroid.settings.GameFolders
import androidx.room.ColumnInfo
import androidx.room.Relation
import kotlin.jvm.JvmName

/**
 * Defines a beatmap set, they're virtually created by the database using DISTINCT operation. This means it doesn't have
 * a table.
 */
data class BeatmapSetInfo(

    /**
     * The ID.
     */
    @ColumnInfo(name = "setId")
    val id: Int?,

    /**
     * The directory name.
     */
    @ColumnInfo(name = "setDirectory")
    val directory: String,

    /**
     * The list of beatmaps.
     */
    @Relation(parentColumn = "setDirectory", entityColumn = "setDirectory")
    val beatmaps: List<BeatmapInfo>

) {

    /**
     * The beatmap set size.
     */
    val count
        get() = beatmaps.size

    /**
     * The absolute path.
     */
    val path
        get() = "${GameFolders.beatmapPath}${directory}"


    /**
     * Get a beatmap from its index.
     */
    @JvmName("getBeatmap")
    operator fun get(index: Int): BeatmapInfo {
        return beatmaps[index]
    }

}