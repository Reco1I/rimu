package game.rimu.management

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.reco1l.framework.android.databaseBuilder
import game.rimu.IWithContext
import game.rimu.MainContext
import game.rimu.data.asset.Asset
import game.rimu.data.asset.IAssetDAO
import game.rimu.data.Beatmap
import game.rimu.data.IBeatmapDAO
import game.rimu.data.ISkinDAO
import game.rimu.data.Skin


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

            databaseImpl = Room.databaseBuilder<RimuDatabase>(ctx, DATABASE_NAME).build()
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
    version = 6,
    entities = [
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
}