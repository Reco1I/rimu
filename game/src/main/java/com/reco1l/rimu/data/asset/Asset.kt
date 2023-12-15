package com.reco1l.rimu.data.asset

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.reco1l.rimu.data.asset.Asset.Companion.SUPPORTED_FORMATS

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
    val variant: Int = 0,

    /**
     * The asset filetype aka extension.
     */
    val type: String

) : HashableAsset()
{


    /**
     * Compare along key and variant.
     */
    fun equals(key: String, variant: Int = 0) = this.key.equals(key, true) && this.variant == variant


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
        val SUPPORTED_FORMATS = AssetType.entries.flatMap { it.formats.toList() }.toTypedArray()

    }
}

enum class AssetType(vararg val formats: String)
{

    BEATMAP("osu"),

    SOUND("wav", "mp3", "ogg"),

    VIDEO("3gp", "mp4", "mkv", "webm", "avi", "flv"),

    IMAGE("png", "jpg", "jpeg"),

    FONT("ttf", "otf"),

    CONFIGURATION("ini");


    /**
     * Check if the [extension] is supported by this asset type.
     */
    operator fun contains(extension: String) = extension in formats

}

@Dao interface IAssetDAO
{

    /**
     * Insert a new [Asset] in the database.
     * [OnConflictStrategy.ABORT]
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(asset: Asset): Long

    /**
     * Delete am asset from the table.
     */
    @Delete
    fun delete(asset: Asset)

    /**
     * Get all assets from the parent [BeatmapSet.key] or [Skin.key].
     */
    @Query("SELECT * FROM Asset WHERE parent = :parentKey")
    fun getFromParent(parentKey: String): List<Asset>
}