package game.rimu.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * Denotes an skin entry.
 */
@Entity
data class Skin(

    /**
     * The skin name (specified in the skin.ini) or the containing folder name.
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
    val isInternal: Boolean = key.endsWith('/')
)
{

    companion object
    {

        /**
         * The rimu! default skin, located at `assets/default/`.
         */
        val DEFAULT = Skin("default/", "rimu! team")

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
    fun insertSkin(skin: Skin): Long

    /**
     * Delete an skin from the database.
     */
    @Delete
    fun deleteSkin(skin: Skin)

    /**
     * Query all skins registered in the database.
     */
    @Query("SELECT * FROM Skin")
    fun getSkins(): Array<Skin>

    /**
     * Get an skin by its key.
     */
    @Query("SELECT * FROM Skin WHERE `key` = :key")
    fun getSkin(key: String): Skin?
}