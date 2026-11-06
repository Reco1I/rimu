package com.osudroid.data

import com.osudroid.settings.GameFolders
import com.osudroid.OsuPlatform
import kotlin.jvm.JvmStatic

// Ported from rimu! project

/**
 * The current schema version of the database.
 */
const val DATABASE_VERSION = 5

/**
 * The tables of the database, which each platform builds (Room's `DroidDatabase` on Android).
 */
interface GameDatabase {

    fun getBeatmapInfoTable(): IBeatmapInfoDAO

    fun getBeatmapOptionsTable(): IBeatmapOptionsDAO

    fun getBeatmapCollectionsTable(): IBeatmapCollectionsDAO

    fun getScoreInfoTable(): IScoreInfoDAO

    fun getBlockAreaTable(): IBlockAreaDAO

    fun getModPresetTable(): IModPresetDAO
}

/**
 * The osu!droid database manager.
 */
object DatabaseManager {

    /**
     * Get the beatmaps table DAO.
     */
    @JvmStatic
    val beatmapInfoTable
        get() = database.getBeatmapInfoTable()

    /**
     * Get the beatmap options table DAO.
     */
    @JvmStatic
    val beatmapOptionsTable
        get() = database.getBeatmapOptionsTable()

    /**
     * Get the beatmap collections table DAO.
     */
    @JvmStatic
    val beatmapCollectionsTable
        get() = database.getBeatmapCollectionsTable()

    /**
     * Get the score table DAO.
     */
    @JvmStatic
    val scoreInfoTable
        get() = database.getScoreInfoTable()

    /**
     * Get the block area table DAO.
     */
    @JvmStatic
    val blockAreaTable
        get() = database.getBlockAreaTable()

    /**
     * Get the mod preset table DAO.
     */
    @JvmStatic
    val modPresetTable
        get() = database.getModPresetTable()

    /**
     * The path to the database file.
     */
    @JvmStatic
    val databasePath: String
        get() = "${GameFolders.corePath}databases/room-${OsuPlatform.implementation.buildType}.db"

    /**
     * The database, set by the platform once it's open.
     */
    @JvmStatic
    lateinit var database: GameDatabase

    /**
     * `true` if the database was created by a newer, incompatible version of the app and had to be reset.
     *
     * When this happens, the previous database file is kept as a backup alongside the new one.
     */
    @JvmStatic
    var wasResetDueToDowngrade = false

}
