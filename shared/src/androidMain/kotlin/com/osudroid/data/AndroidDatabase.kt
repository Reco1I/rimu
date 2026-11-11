package com.osudroid.data

import android.content.Context
import androidx.room.Room


/**
 * Opens the database on Android.
 */
object AndroidDatabase {

    @JvmStatic
    fun load(context: Context) {
        // Be careful when changing the database name, it may cause data loss.
        DatabaseManager.database = Room.databaseBuilder<OsuDatabase>(context, DatabaseManager.databasePath)
            .buildOsuDatabase()
    }

    /**
     * Loads an empty database kept in memory instead of the database file, so the real one is never
     * touched. Used while the game runs without the main activity, e.g. in the UI test browser.
     */
    @JvmStatic
    fun loadInMemory(context: Context) {
        DatabaseManager.database = Room.inMemoryDatabaseBuilder<OsuDatabase>(context).buildOsuDatabase()
    }

}
