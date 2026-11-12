package com.osudroid.data

import androidx.room.Room
import androidx.room.RoomDatabase
import com.osudroid.android.AndroidGame

internal actual fun osuDatabaseBuilder(path: String?): RoomDatabase.Builder<OsuDatabase> {
    // The application's context, the database outlives the activity.
    val context = AndroidGame.context.applicationContext

    return if (path == null) {
        Room.inMemoryDatabaseBuilder(context) { OsuDatabaseConstructor.initialize() }
    } else {
        Room.databaseBuilder(context, path) { OsuDatabaseConstructor.initialize() }
    }
}
