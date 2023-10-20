package com.reco1l.framework.android

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase


inline fun <reified T : Any> Context.getSystemService(): T = getSystemService(T::class.java)

inline fun <reified T : RoomDatabase> Room.databaseBuilder(context: Context, name: String?): RoomDatabase.Builder<T>
{
    return databaseBuilder(context, T::class.java, name)
}