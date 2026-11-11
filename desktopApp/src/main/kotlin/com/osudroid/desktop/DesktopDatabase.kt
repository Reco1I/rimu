package com.osudroid.desktop

import androidx.room.Room
import com.osudroid.data.DatabaseManager
import com.osudroid.data.OsuDatabase
import com.osudroid.data.buildOsuDatabase
import java.io.File

/**
 * Opens the game's database on desktop.
 */
object DesktopDatabase {

    /**
     * Opens the database file of the game's folder.
     */
    fun load() {
        val path = DatabaseManager.databasePath
        File(path).parentFile?.mkdirs()

        DatabaseManager.database = Room.databaseBuilder<OsuDatabase>(name = path).buildOsuDatabase()
    }

    /**
     * Opens an empty database kept in memory instead of the database file, so the real one is never
     * touched, e.g. in the UI test browser.
     */
    fun loadInMemory() {
        DatabaseManager.database = Room.inMemoryDatabaseBuilder<OsuDatabase>().buildOsuDatabase()
    }
}
