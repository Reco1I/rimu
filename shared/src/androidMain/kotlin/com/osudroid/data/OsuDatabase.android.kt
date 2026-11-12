package com.osudroid.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.reco1l.verktex.Verktex

internal actual fun osuDatabaseBuilder(path: String?): RoomDatabase.Builder<OsuDatabase> {
    // The application's context: the host is an activity, which the database outlives.
    val context = (Verktex.host as Context).applicationContext

    return if (path == null) {
        Room.inMemoryDatabaseBuilder(context) { OsuDatabaseConstructor.initialize() }
    } else {
        Room.databaseBuilder(context, path) { OsuDatabaseConstructor.initialize() }
    }
}
