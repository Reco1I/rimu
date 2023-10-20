package game.rimu.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.reco1l.api.chimu.BeatmapStatus
import com.rian.osu.beatmap.BeatmapData
import game.rimu.data.asset.HashableAsset
import kotlinx.coroutines.flow.Flow

@Entity
data class Beatmap(

    /**
     * The beatmap MD5.
     */
    @PrimaryKey
    override val hash: String,

    /**
     * The beatmap ID.
     */
    val id: Long?,

    /**
     * The audio filename.
     */
    val audio: String,


    // Online

    /**
     * The beatmap ranked status.
     */
    @BeatmapStatus
    val status: Int?,


    // Parent set

    /**
     * This indicates the parent set key (ID or MD5).
     */
    val parent: String,


    // Metadata

    /**
     * The title serialized with unicode.
     */
    val title: String,

    /**
     * The artist serialized with unicode.
     */
    val artist: String,

    /**
     * The beatmap creator.
     */
    val creator: String,

    /**
     * The beatmap version.
     */
    val version: String,

    /**
     * The date when the beatmap has been imported
     */
    val dateImported: Long,


    // Difficulty

    /**
     * The cached approach rate.
     */
    val approachRate: Float? = null,

    /**
     * The cached overall difficulty.
     */
    val overallDifficulty: Float? = null,

    /**
     * The cached circle size.
     */
    val circleSize: Float? = null,

    /**
     * The cached HP drain rate
     */
    val hpDrainRate: Float? = null,

    /**
     * The cached star rating.
     */
    val starRating: Float? = null

): HashableAsset()
{
    companion object
    {
        /**
         * Parse a new Beatmap from a [BeatmapData] instance.
         */
        fun from(data: BeatmapData, parentHash: String) = Beatmap(

            hash = data.md5,
            id = data.metadata.beatmapID.toLong(),
            audio = data.general.audioFilename,

            // Online
            status = null,

            // Parent set
            parent = data.metadata.beatmapSetID.takeUnless { it < 0 }?.toString() ?: parentHash,

            // Metadata
            title = data.metadata.title,
            artist = data.metadata.artist,
            creator = data.metadata.creator,
            version = data.metadata.version,
            dateImported = System.currentTimeMillis(),

            // Difficulty
            approachRate = data.difficulty.ar,
            overallDifficulty = data.difficulty.od,
            circleSize = data.difficulty.cs,
            hpDrainRate = data.difficulty.hp
        )
    }
}


@Dao interface IBeatmapDAO
{

    /**
     * Insert a new beatmap to the table.
     *
     * [OnConflictStrategy.REPLACE]
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(beatmap: Beatmap): Long

    /**
     * Delete a beatmap from the table.
     */
    @Delete
    fun delete(beatmap: Beatmap)

    /**
     * Get all beatmaps from the parent [BeatmapSet.key].
     */
    @Query("SELECT * FROM Beatmap WHERE parent = :parentHash")
    fun getFromParent(parentHash: String): Array<Beatmap>

    /**
     * Find an specific beatmap from its MD5 hash.
     */
    @Query("SELECT * FROM Beatmap WHERE hash = :hash")
    fun findByHash(hash: String): Beatmap?

    /**
     * Find an specific beatmap from its ID.
     */
    @Query("SELECT * FROM Beatmap WHERE id = :id")
    fun findByID(id: Long): Beatmap?

    /**
     * Get a flow that listens to database changes for the beatmap table.
     * It collects a list of [BeatmapSet] that each one wraps its corresponding beatmaps.
     */
    @Transaction
    @Query("SELECT DISTINCT parent FROM Beatmap")
    fun getParentSetFlow() : Flow<List<BeatmapSet>>
}



/**
 * Defines a beatmap set, they're virtually created by the database using DISTINCT operation.
 */
data class BeatmapSet(

    /**
     * This can equal to the set ID or its MD5.
     */
    @ColumnInfo(name = "parent")
    val key: String,

    /**The list of beatmaps*/
    @Relation(
        parentColumn = "parent",
        entityColumn = "parent"
    )
    val beatmaps: List<Beatmap>

)

