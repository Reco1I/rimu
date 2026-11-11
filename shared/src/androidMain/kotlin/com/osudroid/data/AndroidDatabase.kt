package com.osudroid.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


/**
 * Opens the database on Android, with Room.
 */
object AndroidDatabase {

    @JvmStatic
    fun load(context: Context) {
        // Be careful when changing the database name, it may cause data loss.
        DatabaseManager.database = Room.databaseBuilder(context, DroidDatabase::class.java, DatabaseManager.databasePath)
            // There are no migrations: a database of another version is recreated, as on desktop.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .allowMainThreadQueries()
            .build()
    }

    /**
     * Loads an empty database kept in memory instead of the database file, so the real one is never
     * touched. Used while the game runs without the main activity, e.g. in the UI test browser.
     */
    @JvmStatic
    fun loadInMemory(context: Context) {
        DatabaseManager.database = Room.inMemoryDatabaseBuilder(context, DroidDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

}

@Database(
    version = DATABASE_VERSION,
    entities = [
        BeatmapInfo::class,
        BeatmapOptions::class,
        ScoreInfo::class,
        BeatmapSetCollection::class,
        BeatmapSetCollection_BeatmapSetInfo::class,
        BlockArea::class,
        ModPreset::class
    ],
    exportSchema = false
)
abstract class DroidDatabase : RoomDatabase(), GameDatabase {

    abstract override fun getBeatmapInfoTable(): IBeatmapInfoDAO

    abstract override fun getBeatmapOptionsTable(): IBeatmapOptionsDAO

    abstract override fun getBeatmapCollectionsTable(): IBeatmapCollectionsDAO

    abstract override fun getScoreInfoTable(): IScoreInfoDAO

    abstract override fun getBlockAreaTable(): IBlockAreaDAO

    abstract override fun getModPresetTable(): IModPresetDAO
}
