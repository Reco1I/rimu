package ru.nsu.ccfit.zuev.osu.menu

import com.osudroid.multiplayer.Multiplayer
import com.osudroid.multiplayer.api.data.WinCondition
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.text.NumberFormat
import java.util.Formatter
import java.util.Locale

open class ScoreBoardItem() : Cloneable {

    @JvmField
    var userName: String? = null
    @JvmField
    var playScore: Long = 0
    @JvmField
    var scoreId = 0

    /**
     * The rank position in the leaderboard.
     */
    @JvmField
    var rank = -1

    /**
     * In single player this is always 'maxCombo', in multiplayer if the room win condition isn't Max Combo it'll be
     * current combo.
     */
    @JvmField
    var maxCombo = 0

    /**
     * Only shown in multiplayer when room win condition is Accuracy
     */
    @JvmField
    var accuracy = -1f

    /**
     * Indicates that the player is alive (HP hasn't reached 0, or it recovered)
     */
    @JvmField
    var isAlive = true


    constructor(userName: String?, playScore: Long, maxCombo: Int, accuracy: Float, isAlive: Boolean) : this() {
        this.userName = userName
        this.playScore = playScore
        this.maxCombo = maxCombo
        this.accuracy = accuracy
        this.isAlive = isAlive
    }


    fun set(rankPos: Int, name: String?, combo: Int, score: Long, id: Int) {
        rank = rankPos
        userName = name
        maxCombo = combo
        playScore = score
        scoreId = id
    }


    fun get(): String {
        var text = userName + "\n" + NUMBER_FORMAT.format(playScore) + "\n"

        if (Multiplayer.isConnected && Multiplayer.room!!.winCondition == WinCondition.Accuracy) {
            accSb.setLength(0)
            text += DECIMAL_FORMAT.format("%2.2f%%", accuracy * 100f)
        } else
            text += NUMBER_FORMAT.format(maxCombo) + "x"

        return text
    }

    /**
     * Converts the item into a JsonObject, it is used specifically for Multiplayer.
     */
    fun toJson(): JsonObject {
        val map = LinkedHashMap<String, JsonElement>()
        map["accuracy"] = JsonPrimitive(accuracy)
        map["score"] = JsonPrimitive(playScore)
        map["combo"] = JsonPrimitive(maxCombo)
        map["isAlive"] = JsonPrimitive(isAlive)
        return JsonObject(map)
    }


    override fun equals(other: Any?): Boolean {
        if (other === this)
            return true

        if (other !is ScoreBoardItem)
            return false

        return other.userName == userName
            && other.playScore == playScore
            && other.maxCombo == maxCombo
            && other.accuracy == accuracy
            && other.isAlive == isAlive
    }

    override fun hashCode(): Int {
        var result = userName?.hashCode() ?: 0
        result = 31 * result + playScore.hashCode()
        result = 31 * result + maxCombo
        result = 31 * result + accuracy.hashCode()
        result = 31 * result + isAlive.hashCode()
        return result
    }

    public override fun clone(): ScoreBoardItem {
        return super.clone() as ScoreBoardItem
    }


    private companion object {
        val NUMBER_FORMAT: NumberFormat = NumberFormat.getNumberInstance(Locale.US)

        val accSb = StringBuilder()
        val DECIMAL_FORMAT = Formatter(accSb, Locale.ENGLISH)
    }
}
