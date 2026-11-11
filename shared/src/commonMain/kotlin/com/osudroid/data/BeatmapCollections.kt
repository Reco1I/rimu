package com.osudroid.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity
class BeatmapSetCollection(

    @PrimaryKey
    val name: String

)

@Suppress("ClassName")
@Entity(primaryKeys = ["collectionName", "setDirectory"])
data class BeatmapSetCollection_BeatmapSetInfo(

    val collectionName: String,

    val setDirectory: String

)

@Dao
interface IBeatmapCollectionsDAO {

    @Query("SELECT name FROM BeatmapSetCollection")
    suspend fun getCollections(): List<String>

    @Query("INSERT OR IGNORE INTO BeatmapSetCollection (name) VALUES (:name)")
    suspend fun insertCollection(name: String): Long

    @Query("DELETE FROM BeatmapSetCollection WHERE name = :name")
    suspend fun deleteCollection(name: String)

    @Query("DELETE FROM BeatmapSetCollection_BeatmapSetInfo WHERE collectionName = :collectionName")
    suspend fun clearCollection(collectionName: String)

    @Query("SELECT EXISTS(SELECT name FROM BeatmapSetCollection WHERE name = :name)")
    suspend fun collectionExists(name: String): Boolean


    @Query("SELECT setDirectory FROM BeatmapSetCollection_BeatmapSetInfo WHERE collectionName = :collectionName")
    suspend fun getBeatmaps(collectionName: String): List<String>?

    @Query("INSERT OR IGNORE INTO BeatmapSetCollection_BeatmapSetInfo (collectionName, setDirectory) VALUES (:collectionName, :setDirectory)")
    suspend fun addBeatmap(collectionName: String, setDirectory: String): Long

    @Query("DELETE FROM BeatmapSetCollection_BeatmapSetInfo WHERE collectionName = :collectionName AND setDirectory = :setDirectory")
    suspend fun removeBeatmap(collectionName: String, setDirectory: String)

    @Query("SELECT EXISTS(SELECT setDirectory FROM BeatmapSetCollection_BeatmapSetInfo WHERE collectionName = :collectionName AND setDirectory = :setDirectory)")
    suspend fun inCollection(collectionName: String, setDirectory: String): Boolean
}