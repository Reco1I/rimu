package com.osudroid.desktop

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.osudroid.data.BeatmapInfo
import com.osudroid.data.BeatmapOptions
import com.osudroid.data.BeatmapSetCollection
import com.osudroid.data.BeatmapSetCollection_BeatmapSetInfo
import com.osudroid.data.BlockArea
import com.osudroid.data.DATABASE_VERSION
import com.osudroid.data.DatabaseManager
import com.osudroid.data.GameDatabase
import com.osudroid.data.ModPreset
import com.osudroid.data.ScoreInfo
import com.osudroid.desktop.data.SuspendBeatmapCollectionsDAO
import com.osudroid.desktop.data.SuspendBeatmapCollectionsDAOAdapter
import com.osudroid.desktop.data.SuspendBeatmapInfoDAO
import com.osudroid.desktop.data.SuspendBeatmapInfoDAOAdapter
import com.osudroid.desktop.data.SuspendBeatmapOptionsDAO
import com.osudroid.desktop.data.SuspendBeatmapOptionsDAOAdapter
import com.osudroid.desktop.data.SuspendBlockAreaDAO
import com.osudroid.desktop.data.SuspendBlockAreaDAOAdapter
import com.osudroid.desktop.data.SuspendModPresetDAO
import com.osudroid.desktop.data.SuspendModPresetDAOAdapter
import com.osudroid.desktop.data.SuspendScoreInfoDAO
import com.osudroid.desktop.data.SuspendScoreInfoDAOAdapter
import java.io.File

/**
 * The game's database on desktop, the same tables as on Android, with Room over the bundled SQLite.
 * Room only implements suspend DAOs here, which [load] adapts to the game's blocking ones, see the
 * generateSuspendDaos task.
 */
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
abstract class DesktopDatabase : RoomDatabase() {

    abstract fun beatmapInfoTable(): SuspendBeatmapInfoDAO

    abstract fun beatmapOptionsTable(): SuspendBeatmapOptionsDAO

    abstract fun beatmapCollectionsTable(): SuspendBeatmapCollectionsDAO

    abstract fun scoreInfoTable(): SuspendScoreInfoDAO

    abstract fun blockAreaTable(): SuspendBlockAreaDAO

    abstract fun modPresetTable(): SuspendModPresetDAO


    companion object {

        /**
         * Opens the database file of the game's folder.
         */
        fun load() {
            val path = DatabaseManager.databasePath
            File(path).parentFile?.mkdirs()

            use(
                Room.databaseBuilder<DesktopDatabase>(name = path)
                    .setDriver(BundledSQLiteDriver())
                    // Desktop databases have no users to keep data for yet, so they don't migrate: a
                    // database of another version is created again.
                    .fallbackToDestructiveMigration(true)
                    .build()
            )
        }

        /**
         * Opens an empty database kept in memory instead of the database file, so the real one is
         * never touched, e.g. in the UI test browser.
         */
        fun loadInMemory() {
            use(Room.inMemoryDatabaseBuilder<DesktopDatabase>().setDriver(BundledSQLiteDriver()).build())
        }

        private fun use(database: DesktopDatabase) {
            DatabaseManager.database = object : GameDatabase {
                private val beatmapInfo = SuspendBeatmapInfoDAOAdapter(database.beatmapInfoTable())
                private val beatmapOptions = SuspendBeatmapOptionsDAOAdapter(database.beatmapOptionsTable())
                private val beatmapCollections = SuspendBeatmapCollectionsDAOAdapter(database.beatmapCollectionsTable())
                private val scoreInfo = SuspendScoreInfoDAOAdapter(database.scoreInfoTable())
                private val blockArea = SuspendBlockAreaDAOAdapter(database.blockAreaTable())
                private val modPreset = SuspendModPresetDAOAdapter(database.modPresetTable())

                override fun getBeatmapInfoTable() = beatmapInfo
                override fun getBeatmapOptionsTable() = beatmapOptions
                override fun getBeatmapCollectionsTable() = beatmapCollections
                override fun getScoreInfoTable() = scoreInfo
                override fun getBlockAreaTable() = blockArea
                override fun getModPresetTable() = modPreset
            }
        }
    }
}
