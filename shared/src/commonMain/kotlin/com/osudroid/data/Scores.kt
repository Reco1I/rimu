@file:JvmName("Scores")

package com.osudroid.data

import com.osudroid.settings.GameFolders
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import com.osudroid.beatmaps.sections.BeatmapDifficulty
import com.osudroid.mods.ModDifficultyAdjust
import com.osudroid.scoring.ScoreMultiplierCalculator
import com.osudroid.utils.deserializeMods
import kotlin.math.roundToLong
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads


@Entity(
    indices = [
        Index(name = "beatmapIdx", value = ["beatmapMD5"]),
    ]
)
data class ScoreInfo @JvmOverloads constructor(

    /**
     * The score ID.
     */
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * The MD5 hash of the beatmap.
     */
    val beatmapMD5: String,

    /**
     * The player name.
     */
    val playerName: String,

    /**
     * The replay file path.
     */
    var replayFilename: String,

    /**
     * The mods used.
     */
    var mods: String,

    /**
     * The total score.
     */
    val score: Long,

    /**
     * The maximum combo.
     */
    val maxCombo: Int,

    /**
     * The mark.
     */
    val mark: String,

    /**
     * The number of 300k hits.
     */
    val hit300k: Int,

    /**
     * The number of 300 hits.
     */
    val hit300: Int,

    /**
     * The number of 100k hits.
     */
    val hit100k: Int,

    /**
     * The number of 100 hits.
     */
    val hit100: Int,

    /**
     * The number of 50 hits.
     */
    val hit50: Int,

    /**
     * The number of misses.
     */
    val misses: Int,

    /**
     * The score date.
     */
    val time: Long,

    /**
     * The amount of slider heads that were hit.
     */
    var sliderHeadHits: Int?,

    /**
     * The amount of slider ticks that were hit.
     */
    var sliderTickHits: Int?,

    /**
     * The amount of slider repeats that were hit.
     */
    var sliderRepeatHits: Int?,

    /**
     * The amount of slider ends that were hit.
     */
    var sliderEndHits: Int?,

    /**
     * Whether this score's [score] still holds [StatisticV2.totalScoreWithMultiplier] and needs on-the-fly conversion
     * to raw [StatisticV2.totalScore] once the beatmap is available.
     *
     * This flag is set during database migration when a score uses a mod that requires beatmap data (e.g.
     * [ModDifficultyAdjust]) and the beatmap was not present in the library at migration time. Call
     * [IScoreInfoDAO.migrateScores] whenever the beatmap becomes available to complete the conversion.
     */
    var needsScoreMigration: Boolean = false
) {

    /**
     * The replay file path.
     */
    val replayPath
        get() = "${GameFolders.scorePath}/$replayFilename"

    /**
     * The number of notes hit.
     */
    val notesHit
        get() = hit300 + hit100 + hit50 + misses

    /**
     * The accuracy.
     */
    val accuracy
        get() = if (notesHit == 0) 1f else (hit300 * 6f + hit100 * 2f + hit50) / (6f * notesHit)

    /**
     * Calculates the score of this [ScoreInfo] after applying score multipliers from [mods] and [BeatmapDifficulty].
     */
    fun calculateEffectiveScore(difficulty: BeatmapDifficulty): Long {
        // Pending-migration rows store total score with multipliers directly, so multiplying again would apply the
        // multiplier twice.
        if (needsScoreMigration) {
            return score
        }

        val modMap = deserializeMods(mods)

        return (score * ScoreMultiplierCalculator(difficulty).calculateFor(modMap.values)).roundToLong()
    }


    @JvmOverloads
    @Throws(IllegalArgumentException::class)
    fun toStatisticV2(difficulty: BeatmapDifficulty? = null) = StatisticV2().also {

        it.playerName = playerName
        it.beatmapMD5 = beatmapMD5
        it.replayFilename = replayFilename
        it.mod = deserializeMods(mods)

        // Pending-migration rows store total score with multiplier, so we use forced score to prevent mod multipliers
        // from multiplying again.
        if (needsScoreMigration) {
            it.setForcedScore(score)
        } else {
            it.totalScore = score
        }

        it.scoreMaxCombo = maxCombo
        it.mark = mark
        it.hit300k = hit300k
        it.hit300 = hit300
        it.hit100k = hit100k
        it.hit100 = hit100
        it.hit50 = hit50
        it.misses = misses
        it.time = time
        it.sliderHeadHits = sliderHeadHits ?: -1
        it.sliderTickHits = sliderTickHits ?: -1
        it.sliderRepeatHits = sliderRepeatHits ?: -1
        it.sliderEndHits = sliderEndHits ?: -1

        if (difficulty != null) {
            it.migrateLegacyMods(difficulty)
        }

        it.calculateModScoreMultiplier(difficulty)
    }


}

@Dao
interface IScoreInfoDAO {

    @Query("SELECT * FROM ScoreInfo WHERE beatmapMD5 = :beatmapMD5")
    suspend fun getBeatmapScores(beatmapMD5: String): List<ScoreInfo>

    @Query("SELECT * FROM ScoreInfo WHERE id = :id")
    suspend fun getScore(id: Int): ScoreInfo?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertScore(score: ScoreInfo): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertScores(scores: List<ScoreInfo>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateScore(score: ScoreInfo)

    @Query("DELETE FROM ScoreInfo WHERE id = :id")
    suspend fun deleteScore(id: Int): Int

    @Query("SELECT EXISTS(SELECT 1 FROM ScoreInfo WHERE id = :id)")
    suspend fun scoreExists(id: Long): Boolean

}

/**
 * Converts the statistic to a [ScoreInfo].
 */
fun StatisticV2.toScoreInfo() = ScoreInfo(
    beatmapMD5 = beatmapMD5,
    playerName = playerName,
    replayFilename = replayFilename,
    mods = mod.serializeMods(false),
    score = totalScore,
    maxCombo = lastScoreMaxCombo,
    mark = mark,
    hit300k = hit300k,
    hit300 = hit300,
    hit100k = hit100k,
    hit100 = hit100,
    hit50 = hit50,
    misses = misses,
    time = time,
    sliderHeadHits = if (sliderHeadHits >= 0) sliderHeadHits else null,
    sliderTickHits = if (sliderTickHits >= 0) sliderTickHits else null,
    sliderRepeatHits = if (sliderRepeatHits >= 0) sliderRepeatHits else null,
    sliderEndHits = if (sliderEndHits >= 0) sliderEndHits else null
)
