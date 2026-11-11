package com.osudroid.data

import androidx.room.*
import com.osudroid.utils.ModHashMap
import kotlin.jvm.JvmField

/**
 * Represents a mod preset.
 */
@Entity
data class ModPreset(
    @PrimaryKey(autoGenerate = true)
    @JvmField
    var id: Long = 0,

    /**
     * The name of this [ModPreset].
     */
    @JvmField
    var name: String,

    /**
     * The mods that are in this [ModPreset] in serialized form.
     */
    @JvmField
    var serializedMods: String
) {

    /**
     * The mods that are in this [ModPreset] in deserialized form.
     */
    @Ignore
    lateinit var mods: ModHashMap

}

@Dao
interface IModPresetDAO {
    @Query("SELECT * FROM ModPreset")
    suspend fun getAll(): List<ModPreset>

    @Query("SELECT * FROM ModPreset WHERE id = :id")
    suspend fun getById(id: Long): ModPreset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(modPreset: ModPreset): Long

    @Update
    suspend fun update(modPreset: ModPreset)

    @Delete
    suspend fun delete(modPreset: ModPreset)

    @Query("DELETE FROM ModPreset")
    suspend fun deleteAll()
}