package ru.nsu.ccfit.zuev.osu.scoring

import com.osudroid.settings.OsuSettings
import com.osudroid.beatmaps.sections.BeatmapDifficulty
import com.osudroid.mods.IMigratableMod
import com.osudroid.mods.ModFlashlight
import com.osudroid.mods.ModHidden
import com.osudroid.scoring.ScoreMultiplierCalculator
import com.osudroid.utils.ModHashMap
import com.osudroid.utils.deserializeMods
import kotlin.jvm.JvmField
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sqrt
import kotlin.random.Random
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import ru.nsu.ccfit.zuev.osu.game.GameHelper

class StatisticV2() {
    var hit300 = 0
    var hit100 = 0
    var hit50 = 0
    var hit300k = 0
    var hit100k = 0
    var misses = 0
    private var scoreMaxComboValue = 0
    var sliderHeadHits = 0
    var sliderTickHits = 0
    var sliderRepeatHits = 0
    var sliderEndHits = 0
    var time = 0L
    private var currentCombo = 0
    private var scoreHash = 0
    var totalScore = 0L
    var hp = 1f
        private set
    var diffModifier = 1f
    var mod = ModHashMap()
    var playerName: String = OsuSettings.onlineUsername
    var replayFilename = ""
    private var forcedScore = -1L
    private var markValue: String? = null
    var beatmapNoteCount = 0
    var beatmapMaxCombo = 0
    private var bonusScore = 0
    private var positiveHitOffsetCount = 0
    private var positiveHitOffsetSum = 0.0
    private var negativeHitOffsetCount = 0
    private var negativeHitOffsetSum = 0.0
    var unstableRate = 0.0
        private set

    private var life = 1

    /**
     * The player's UID, used in multiplayer to match the entry to the local player.
     */
    @JvmField
    var uid = -1L

    /**
     * Indicates that the player is alive (HP hasn't reached 0, or it recovered), this is exclusively used for
     * multiplayer.
     */
    @JvmField
    var isAlive = true

    /**
     * Whether the player can fail.
     */
    @JvmField
    var canFail = true

    /**
     * The score multiplier from mods.
     */
    private var modScoreMultiplier = 1.0

    /**
     * The MD5 hash of the beatmap.
     */
    var beatmapMD5 = ""

    /**
     * The current performance points.
     */
    @get:JvmName("getPP")
    @set:JvmName("setPP")
    var pp = 0.0


    @JvmOverloads
    constructor(params: Array<String>, originalDifficulty: BeatmapDifficulty? = null) : this() {
        playerName = ""
        if (params.size < 6) return

        mod = deserializeMods(params[0])
        setForcedScore(params[1].toLong())
        scoreMaxComboValue = params[2].toInt()
        markValue = params[3]
        hit300k = params[4].toInt()
        hit300 = params[5].toInt()
        hit100k = params[6].toInt()
        hit100 = params[7].toInt()
        hit50 = params[8].toInt()
        misses = params[9].toInt()

        // params[10] is a flag that denotes whether a replay is available, which is not used here, so we skip it.
        if (params.size >= 12) {
            time = params[11].toLong()
        }

        sliderHeadHits = if (params.size >= 13) params[12].toInt() else -1
        sliderTickHits = if (params.size >= 14) params[13].toInt() else -1
        sliderRepeatHits = if (params.size >= 15) params[14].toInt() else -1
        sliderEndHits = if (params.size >= 16) params[15].toInt() else -1

        if (originalDifficulty != null) {
            migrateLegacyMods(originalDifficulty)
        }
    }

    fun changeHp(amount: Float) {
        hp += amount
        if (hp < 0) {
            hp = 0f
            life = maxOf(0, life - 1)

            if (canFail && life == 0) {
                isAlive = false
            }
        }
        if (hp > 1) {
            hp = 1f
            isAlive = true
        }
    }

    val totalScoreWithMultiplier: Long
        get() {
            if (forcedScore > 0)
                return forcedScore

            return (totalScore * modScoreMultiplier).roundToLong()
        }

    fun registerSpinnerHit() {
        addScore(100, false)
    }

    fun registerHit(score: Int, k: Boolean, g: Boolean) {
        registerHit(score, k, g, true)
    }

    fun registerHit(score: Int, k: Boolean, g: Boolean, incrementCombo: Boolean) {
        if (score == 1000) {
            addScore(score, false)
            return
        }
        if (score < 50 && score > 0) {
            changeHp(0.05f)
            addScore(score, false)
            currentCombo++
            return
        }
        if (score == 0 && k) {
            changeHp(-(5 + GameHelper.getHealthDrain()) / 100f)
            if (currentCombo > scoreMaxComboValue) {
                scoreMaxComboValue = currentCombo
            }
            currentCombo = 0
            return
        }

        when (score) {
            300 -> {
                changeHp(if (k) 0.10f else 0.05f)
                if (g) {
                    hit300k++
                }
                hit300++
                addScore(300, true)
                if (incrementCombo) {
                    currentCombo++
                }
            }
            100 -> {
                changeHp(if (k) 0.15f else 0.05f)
                if (k) {
                    hit100k++
                }
                hit100++
                addScore(100, true)
                if (incrementCombo) {
                    currentCombo++
                }
            }
            50 -> {
                changeHp(0.05f)
                hit50++
                addScore(50, true)
                if (incrementCombo) {
                    currentCombo++
                }
            }
            else -> {
                changeHp(-(5 + GameHelper.getHealthDrain()) / 100f)
                misses++
                if (currentCombo > scoreMaxComboValue) {
                    scoreMaxComboValue = currentCombo
                }
                currentCombo = 0
                // Still add score to update ScoreV2 value.
                addScore(0, false)
            }
        }
    }

    val accuracy: Float
        get() {
            val notesHit = notesHit

            if (notesHit == 0) {
                return 1f
            }

            return (hit300 * 6f + hit100 * 2 + hit50) / (6 * notesHit)
        }

    private fun addScore(amount: Int, combo: Boolean) {
        if (!isScoreValid) {
            scoreHash = random.nextInt(1313) or 3455
            return
        }
        //如果使用scorev2
        if (GameHelper.isScoreV2()) {
            if (amount == 1000) {
                bonusScore += amount
            }

            var currentMaxCombo = scoreMaxCombo
            // At this point, the combo increment in registerHit has not happened, but it is necessary for ScoreV2
            // calculation, so we do it locally here.
            if (combo && currentCombo == currentMaxCombo) {
                currentMaxCombo++
            }

            // In floats, as it always was.
            val comboPortion = (scoreV2ComboPortion * currentMaxCombo / beatmapMaxCombo).toDouble()
            val accuracyPortion = scoreV2AccPortion * accuracy.toDouble().pow(10) * notesHit / beatmapNoteCount

            totalScore = (scoreV2MaxScore * (comboPortion + accuracyPortion)).toLong() + bonusScore
        } else if (amount + amount * currentCombo * diffModifier / 25 > 0) {
            // It is possible for score addition to be a negative number due to
            // difficulty modifier, hence the prior check.
            //
            // In that case, just skip score addition to ensure score is always positive.

            //如果分数溢出或分数满了
            if (totalScore + (amount * currentCombo * diffModifier) / 25 + amount < 0 || totalScore == Long.MAX_VALUE) {
                totalScore = Long.MAX_VALUE
            } else {
                totalScore += amount
                if (combo) {
                    totalScore += ((amount * currentCombo * diffModifier) / 25).toLong()
                }
            }
        }
        scoreHash = getHigh16Bits(totalScore)
    }

    var mark: String
        get() {
            markValue?.let { return it }

            val isH = ModHidden::class in mod || ModFlashlight::class in mod

            val notesHit = notesHit

            if (hit100 == 0 && hit50 == 0 && misses == 0) {
                if (isH) {
                    return "XH"
                }
                return "X"
            }
            if (hit300 / notesHit.toFloat() > 0.9f && misses == 0
                    && hit50 / notesHit.toFloat() < 0.01f) {
                if (isH) {
                    return "SH"
                }
                return "S"
            }
            if (hit300 / notesHit.toFloat() > 0.8f && misses == 0
                    || hit300 / notesHit.toFloat() > 0.9f) {
                return "A"
            }
            if (hit300 / notesHit.toFloat() > 0.7f && misses == 0
                    || hit300 / notesHit.toFloat() > 0.8f) {
                return "B"
            }
            if (hit300 / notesHit.toFloat() > 0.6f) {
                return "C"
            }
            return "D"
        }
        set(mark) {
            markValue = mark
        }

    var scoreMaxCombo: Int
        get() {
            if (currentCombo > scoreMaxComboValue) {
                scoreMaxComboValue = currentCombo
            }

            return scoreMaxComboValue
        }
        set(maxCombo) {
            scoreMaxComboValue = maxCombo
        }

    /**
     * The max combo as it was last updated: unlike [scoreMaxCombo], it doesn't include the current combo
     * until it breaks or [scoreMaxCombo] is read.
     */
    val lastScoreMaxCombo
        get() = scoreMaxComboValue

    val notesHit: Int
        get() = hit300 + hit100 + hit50 + misses

    fun addSliderHeadHit() {
        sliderHeadHits++
    }

    fun addSliderTickHit() {
        sliderTickHits++
    }

    fun addSliderRepeatHit() {
        sliderRepeatHits++
    }

    fun addSliderEndHit() {
        sliderEndHits++
    }

    val isPerfect: Boolean
        get() = accuracy == 1f

    var combo: Int
        get() = currentCombo
        set(combo) {
            currentCombo = combo
        }

    fun setForcedScore(forcedScore: Long) {
        this.forcedScore = forcedScore
        totalScore = forcedScore
    }

    val isScoreValid: Boolean
        get() = getHigh16Bits(totalScore) == scoreHash

    fun compile(): String {
        val builder = StringBuilder()
        builder.append(mod.serializeMods(false))
        builder.append(' ')
        builder.append(totalScore)
        builder.append(' ')
        builder.append(scoreMaxCombo)
        builder.append(' ')
        builder.append(mark)
        builder.append(' ')
        builder.append(hit300k)
        builder.append(' ')
        builder.append(hit300)
        builder.append(' ')
        builder.append(hit100k)
        builder.append(' ')
        builder.append(hit100)
        builder.append(' ')
        builder.append(hit50)
        builder.append(' ')
        builder.append(misses)
        builder.append(' ')
        builder.append(sliderHeadHits)
        builder.append(' ')
        builder.append(sliderTickHits)
        builder.append(' ')
        builder.append(sliderRepeatHits)
        builder.append(' ')
        builder.append(sliderEndHits)
        builder.append(' ')
        builder.append(accuracy)
        builder.append(' ')
        builder.append(time)
        builder.append(' ')
        builder.append(if (isPerfect) 1 else 0)
        builder.append(' ')
        builder.append(playerName)
        return builder.toString()
    }

    fun addHitOffset(accuracy: Double) {
        val msAccuracy = accuracy * 1000

        // Update hit offset
        if (accuracy >= 0) {
            positiveHitOffsetSum += msAccuracy
            positiveHitOffsetCount++
        } else {
            negativeHitOffsetSum += msAccuracy
            negativeHitOffsetCount++
        }

        // Update unstable rate
        // Reference: https://math.stackexchange.com/questions/775391/can-i-calculate-the-new-standard-deviation-when-adding-a-value-without-knowing-t
        val hitOffsetCount = positiveHitOffsetCount + negativeHitOffsetCount
        val hitOffsetSum = positiveHitOffsetSum + negativeHitOffsetSum

        if (hitOffsetCount > 1) {
            unstableRate = 10 * sqrt(
                ((hitOffsetCount - 1) * (unstableRate / 10).pow(2) +
                    (msAccuracy - hitOffsetSum / hitOffsetCount) * (msAccuracy - (hitOffsetSum - msAccuracy) / (hitOffsetCount - 1))) / hitOffsetCount
            )
        }
    }

    val averageHitOffset: Double
        get() {
            val count = positiveHitOffsetCount + negativeHitOffsetCount
            return if (count > 0) (positiveHitOffsetSum + negativeHitOffsetSum) / count else 0.0
        }

    val negativeHitError: Double
        get() = if (negativeHitOffsetCount == 0) 0.0 else negativeHitOffsetSum / negativeHitOffsetCount

    val positiveHitError: Double
        get() = if (positiveHitOffsetCount == 0) 0.0 else positiveHitOffsetSum / positiveHitOffsetCount

    /**
     * Converts the statistic into a JSON object readable by the multiplayer server.
     */
    fun toJson(): JsonObject {
        return buildJsonObject {
            put("accuracy", accuracy)
            put("score", totalScoreWithMultiplier)
            put("username", playerName)
            put("mods", mod.serializeMods())
            put("maxCombo", scoreMaxComboValue)
            put("geki", hit300k)
            put("perfect", hit300)
            put("katu", hit100k)
            put("good", hit100)
            put("bad", hit50)
            put("miss", misses)
            put("isAlive", isAlive)
        }
    }

    fun calculateModScoreMultiplier(difficulty: BeatmapDifficulty?) {
        modScoreMultiplier = ScoreMultiplierCalculator(difficulty).calculateFor(mod.values)
    }

    fun migrateLegacyMods(originalDifficulty: BeatmapDifficulty) {
        for (m in mod.values) {
            if (m is IMigratableMod) {
                mod.remove(m)
                mod.put(m.migrate(originalDifficulty))
            }
        }
    }

    /**
     * Resets the statistics to their initial values.
     */
    fun reset() {
        hit300 = 0
        hit100 = 0
        hit50 = 0
        hit300k = 0
        hit100k = 0
        misses = 0
        sliderHeadHits = 0
        sliderTickHits = 0
        sliderRepeatHits = 0
        sliderEndHits = 0
        scoreMaxComboValue = 0
        currentCombo = 0
        totalScore = 0
        scoreHash = getHigh16Bits(0)
        hp = 1f
        life = 1
        isAlive = true
        markValue = null
        bonusScore = 0
        positiveHitOffsetCount = 0
        positiveHitOffsetSum = 0.0
        negativeHitOffsetCount = 0
        negativeHitOffsetSum = 0.0
        unstableRate = 0.0
        pp = 0.0
    }

    companion object {
        private val random = Random.Default
        private const val scoreV2MaxScore = 1000000
        private const val scoreV2AccPortion = 0.3f
        private const val scoreV2ComboPortion = 0.7f

        private fun getHigh16Bits(x: Long): Int {
            return ((x shr 12) % 3389).toInt()
        }
    }
}
