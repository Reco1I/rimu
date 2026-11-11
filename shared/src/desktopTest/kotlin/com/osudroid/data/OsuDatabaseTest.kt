package com.osudroid.data

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.reco1l.verktex.database.Databases
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toOkioPath
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OsuDatabaseTest {

    private lateinit var directory: File

    private val file
        get() = File(directory, "databases/osu.db")

    @Before
    fun setUp() {
        directory = Files.createTempDirectory("osu-database").toFile()
    }

    @After
    fun tearDown() {
        directory.deleteRecursively()
    }

    private fun open() = Databases.open(OsuDatabaseConstructor, file.toOkioPath()) {
        fallbackToDestructiveMigration(dropAllTables = true)
    }

    private fun preset() = ModPreset(name = "Preset", serializedMods = "[]")

    @Test
    fun `a database file keeps its rows once reopened`() = runBlocking {
        open().apply { getModPresetTable().insert(preset()) }.close()
        assertTrue(file.exists())

        val database = open()
        assertEquals(listOf("Preset"), database.getModPresetTable().getAll().map { it.name })
        database.close()
    }

    @Test
    fun `a database of another version is recreated`() = runBlocking {
        open().apply { getModPresetTable().insert(preset()) }.close()

        BundledSQLiteDriver().open(file.path).apply { execSQL("PRAGMA user_version = ${DATABASE_VERSION - 1}") }.close()

        val database = open()
        assertTrue(database.getModPresetTable().getAll().isEmpty())
        database.close()
    }

    @Test
    fun `a database in memory starts empty and is never written`() = runBlocking {
        val database = Databases.openInMemory(OsuDatabaseConstructor)
        database.getModPresetTable().insert(preset())
        assertEquals(1, database.getModPresetTable().getAll().size)
        database.close()

        val another = Databases.openInMemory(OsuDatabaseConstructor)
        assertTrue(another.getModPresetTable().getAll().isEmpty())
        another.close()
        assertTrue(directory.listFiles().isNullOrEmpty())
    }
}
