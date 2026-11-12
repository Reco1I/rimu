package com.osudroid.data

import androidx.room.Room
import androidx.room.RoomDatabase

internal actual fun osuDatabaseBuilder(path: String?): RoomDatabase.Builder<OsuDatabase> =
    if (path == null) {
        Room.inMemoryDatabaseBuilder { OsuDatabaseConstructor.initialize() }
    } else {
        Room.databaseBuilder(path) { OsuDatabaseConstructor.initialize() }
    }
