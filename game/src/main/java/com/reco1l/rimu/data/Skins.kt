package com.reco1l.rimu.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Denotes an skin entry.
 */
@Entity
data class Skin(

    /**
     * The skin name or the containing folder name.
     */
    @PrimaryKey
    val key: String,

    /**
     * The declared skin author.
     */
    val author: String?,

    /**
     * Internal skins use a pattern where the hash equals to its subdirectory in the Android `/assets`
     * folder.
     */
    val isInternal: Boolean = false
)
{

    companion object
    {

        /**
         * The key of the default/main skin.
         */
        const val BASE = "skins/default"

    }
}

@Dao
interface ISkinDAO
{

    /**
     * Insert an skin into the database.
     *
     * @see OnConflictStrategy.REPLACE
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(skin: Skin): Long

    /**
     * Delete an skin from the database.
     */
    @Delete
    fun delete(skin: Skin)

    /**
     * Query all skins registered in the database.
     */
    @Query("SELECT * FROM Skin")
    fun getFlow(): Flow<List<Skin>>

    /**
     * Get an skin by its key.
     */
    @Query("SELECT * FROM Skin WHERE `key` = :key")
    fun findByKey(key: String): Skin?
}