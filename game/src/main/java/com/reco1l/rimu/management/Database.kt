package com.reco1l.rimu.management

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.reco1l.rimu.IWithContext
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.data.asset.Asset
import com.reco1l.rimu.data.asset.IAssetDAO
import com.reco1l.rimu.data.Beatmap
import com.reco1l.rimu.data.Filename
import com.reco1l.rimu.data.IBeatmapDAO
import com.reco1l.rimu.data.IFilenameDAO
import com.reco1l.rimu.data.ISkinDAO
import com.reco1l.rimu.data.Skin


/**
 * The rimu! database manager.
 * It joins all entities DAOs in one class.
 *
 * @see IBeatmapDAO
 * @see IAssetDAO
 * @see ISkinDAO
 */
class DatabaseManager(override val ctx: MainContext) : IWithContext
{

    /**
     * Get beatmaps table.
     */
    val beatmapTable
        get() = databaseImpl.getBeatmapTable()

    /**
     * Get assets table.
     */
    val assetTable
        get() = databaseImpl.getAssetTable()

    /**
     * Get skins table.
     */
    val skinTable
        get() = databaseImpl.getSkinTable()


    private lateinit var databaseImpl: RimuDatabase


    init
    {
        ctx.initializationTree!!.add {

            databaseImpl = Room.databaseBuilder(ctx, RimuDatabase::class.java, DATABASE_NAME).build()
        }
    }


    companion object
    {
        /**
         * The internal database name, changing this will break existing databases.
         */
        const val DATABASE_NAME = "RimuDatabase"
    }
}


/**
 * The rimu! database object class, this should be unique per instance.
 */
@Database(
    version = 7,
    entities = [
        Filename::class,
        Beatmap::class,
        Asset::class,
        Skin::class
    ]
)
abstract class RimuDatabase : RoomDatabase()
{
    abstract fun getBeatmapTable(): IBeatmapDAO

    abstract fun getAssetTable(): IAssetDAO

    abstract fun getSkinTable(): ISkinDAO

    abstract fun getFilenameTable(): IFilenameDAO
}