package com.osudroid.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert

@Entity
data class BeatmapOptions(

    /**
     * The beatmap set directory.
     */
    @PrimaryKey
    val setDirectory: String,

    /**
     * Whether the beatmap is marked as favorite.
     */
    var isFavorite: Boolean = false,

    /**
     * The beatmap offset.
     */
    var offset: Int = 0

)

@Dao interface IBeatmapOptionsDAO {

    @Query("SELECT * FROM BeatmapOptions WHERE setDirectory = :setDirectory")
    suspend fun getOptions(setDirectory: String): BeatmapOptions?

    @Query("SELECT setDirectory FROM BeatmapOptions WHERE isFavorite = 1")
    suspend fun getFavoriteDirectories(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(options: BeatmapOptions)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(options: List<BeatmapOptions>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(options: BeatmapOptions)

    @Upsert
    suspend fun upsert(options: BeatmapOptions)

    @Query("DELETE FROM BeatmapOptions")
    suspend fun deleteAll()

}

