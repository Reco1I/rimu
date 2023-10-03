package game.rimu.data.asset

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import game.rimu.data.asset.Asset.Companion.SUPPORTED_FORMATS

/**
 * An asset denotes a supported file in rimu! (see [SUPPORTED_FORMATS]).
 *
 * All beatmaps or skin sounds, textures and configuration files are parsed as an [Asset] to be saved
 * as entity in the database. All files are stored by its hash name in the resources directory and the
 * game uses the assets table from the database to identify them.
 */
@Entity(
    primaryKeys = [
        "hash",
        "parent"
    ]
)
data class Asset(

    /**
     * The file hash equals to the real filename too.
     *
     * Note: This doesn't include the extension.
     */
    override val hash: String,

    /**
     * The parent key.
     */
    val parent: String,

    /**
     * The asset file name excluding the extension.
     */
    val key: String,

    /**
     * Some assets can have the same name for the same, this identifies them.
     * In animatable textures this equals to the frame.
     */
    val variant: Int = 0

) : HashableAsset()
{


    fun equals(key: String, variant: Int = 0) = this.key == key && this.variant == variant


    companion object
    {
        /**
         * Array of supported game mode file format.
         */
        val MODE_FORMATS = arrayOf("osu")

        /**
         * Array of supported sound file format
         */
        val SOUND_FORMATS = arrayOf("wav", "mp3", "ogg")

        /**
         * Array of supported video file format
         */
        val VIDEO_FORMATS = arrayOf("3gp", "mp4", "mkv", "webm", "avi", "flv")

        /**
         * Array of supported image file format.
         */
        val IMAGE_FORMATS = arrayOf("png", "jpg", "jpeg")

        /**
         * Array of supported fonts file format.
         */
        val FONT_FORMATS = arrayOf("ttf", "otf")

        /**
         * List of currently asset supported formats by rimu!.
         */
        val SUPPORTED_FORMATS =
            MODE_FORMATS + SOUND_FORMATS + VIDEO_FORMATS + IMAGE_FORMATS + FONT_FORMATS

    }
}

@Dao interface IAssetDAO
{

    /**
     * Insert a new [Asset] in the database.
     * [OnConflictStrategy.ABORT]
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertAsset(asset: Asset): Long

    /**
     * Delete am asset from the table.
     */
    @Delete
    fun deleteAsset(asset: Asset)

    /**
     * Get all assets from the parent [BeatmapSet.key] or [Skin.key].
     */
    @Query("SELECT * FROM Asset WHERE parent = :key")
    fun getAssetsFrom(key: String): List<Asset>

    /**
     * Find an specific asset from its MD5 hash.
     */
    @Query("SELECT * FROM Asset WHERE `key` = :name")
    fun findAssetByName(name: String): Asset
}