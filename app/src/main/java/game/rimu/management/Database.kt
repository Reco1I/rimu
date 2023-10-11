package game.rimu.management

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import game.rimu.android.IWithContext
import game.rimu.android.RimuContext
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
class DatabaseManager(override val ctx: RimuContext, database: RimuDatabase = RimuDatabase(ctx)) :

    IBeatmapDAO by database.getBeatmapTable(),
    IAssetDAO by database.getAssetTable(),
    ISkinDAO by database.getSkinTable(),
    IWithContext


/**
 * We create the database in the initialization so we can implement DAOs and delegate them.
 */
private fun RimuDatabase(context: RimuContext) = Room
    .databaseBuilder(
        context = context,
        klass = RimuDatabase::class.java,
        name = RimuDatabase::class.simpleName
    )
    .build()

/**
 * The rimu! database object class, this should be unique per instance.
 */
@Database(
    version = 5,
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