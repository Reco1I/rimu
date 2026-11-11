package ru.nsu.ccfit.zuev.osu

import com.osudroid.settings.GameResolution
import com.osudroid.math.Vector2
import kotlin.jvm.JvmStatic
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt
import ru.nsu.ccfit.zuev.osu.game.GameHelper

object Utils {
    private const val FSReservedChars = "|\\?*<\":>+[]/"
    private var accSign = 0

    @JvmStatic
    fun <T> oneObjectArray(obj: T, ary: Array<T>): Array<T> {
        ary.fill(obj)
        return ary
    }

    @JvmStatic
    fun setAccelerometerSign(sign: Int) {
        accSign = sign
    }

    @JvmStatic
    fun getAccselerometerSign(): Int {
        return accSign
    }

    @JvmStatic
    fun sqr(x: Float): Float {
        return (x * x)
    }

    @JvmStatic
    fun inter(v1: Vector2, v2: Vector2, percent: Float): Vector2 {
        return Vector2(v1.x * (1 - percent) + v2.x * percent, v1.y * (1 - percent) + v2.y * percent)
    }

    @JvmStatic
    fun trackToRealCoords(coords: Vector2): Vector2 {
        return trackToRealCoords(coords, false)
    }

    @JvmStatic
    fun trackToRealCoords(pos: Vector2, flipVertically: Boolean): Vector2 {
        pos.x *= Constants.MAP_ACTUAL_WIDTH / Constants.MAP_WIDTH.toFloat()
        pos.y *= Constants.MAP_ACTUAL_HEIGHT / Constants.MAP_HEIGHT.toFloat()

        pos.y += (GameResolution.height - toRes(Constants.MAP_ACTUAL_HEIGHT)) / 2f
        pos.x += (GameResolution.width - toRes(Constants.MAP_ACTUAL_WIDTH)) / 2f

        if (flipVertically) {
            flipRealCoordsVertically(pos)
        }
        return pos
    }

    @JvmStatic
    fun flipRealCoordsVertically(coords: Vector2) {
        coords.y -= GameResolution.height / 2f
        coords.y *= -1f
        coords.y += GameResolution.height / 2f
    }

    @JvmStatic
    fun changeTrackToRealCoords(coords: Vector2) {
        val pos = scaleToRealC(coords)
        pos.y += (GameResolution.height - toRes(Constants.MAP_ACTUAL_HEIGHT)) / 2f
        pos.x += (GameResolution.width - toRes(Constants.MAP_ACTUAL_WIDTH)) / 2f
        if (GameHelper.isHardRock()) {
            pos.y -= GameResolution.height.toFloat() / 2
            pos.y *= -1f
            pos.y += GameResolution.height.toFloat() / 2
        }
    }

    @JvmStatic
    fun realToTrackCoords(coords: Vector2): Vector2 {
        return realToTrackCoords(coords, GameResolution.width.toFloat(), GameResolution.height.toFloat(), false)
    }

    @JvmStatic
    fun realToTrackCoords(coords: Vector2, width: Float, height: Float, isOld: Boolean): Vector2 {
        val pos = Vector2(coords.x, coords.y)
        if (GameHelper.isHardRock()) {
            pos.y -= height / 2
            pos.y *= -1f
            pos.y += height / 2
        }
        pos.y -= (height - toRes((if (isOld) Constants.MAP_ACTUAL_HEIGHT_OLD else Constants.MAP_ACTUAL_HEIGHT))) / 2f
        pos.x -= (width - toRes((if (isOld) Constants.MAP_ACTUAL_WIDTH_OLD else Constants.MAP_ACTUAL_WIDTH))) / 2f
        return scaleToTrack(pos, isOld)
    }

    @JvmStatic
    fun flipY(y: Short): Short {
        val height = GameResolution.height / 2
        return (((y - height) * -1) + height).toShort()
    }

    @JvmStatic
    fun scaleToRealC(v: Vector2): Vector2 {
        v.x *= toRes(Constants.MAP_ACTUAL_WIDTH) / Constants.MAP_WIDTH.toFloat()
        v.y *= toRes(Constants.MAP_ACTUAL_HEIGHT) / Constants.MAP_HEIGHT.toFloat()
        return v
    }

    @JvmStatic
    fun scaleToTrack(v: Vector2, isOld: Boolean): Vector2 {
        val pos = Vector2(v.x, v.y)
        pos.x *= Constants.MAP_WIDTH / toRes((if (isOld) Constants.MAP_ACTUAL_WIDTH_OLD else Constants.MAP_ACTUAL_WIDTH).toFloat())
        pos.y *= Constants.MAP_HEIGHT / toRes((if (isOld) Constants.MAP_ACTUAL_HEIGHT_OLD else Constants.MAP_ACTUAL_HEIGHT).toFloat())
        return pos
    }

    @JvmStatic
    fun direction(x: Float, y: Float): Float {
        if (x == 0f && y == 0f) {
            return 0f
        }

        return atan2(y, x)
    }

    @JvmStatic
    fun direction(vector: Vector2): Float {
        return direction(vector.x, vector.y)
    }

    @JvmStatic
    fun direction(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return direction(x2 - x1, y2 - y1)
    }

    @JvmStatic
    fun length(vector: Vector2): Float {
        return sqrt(vector.x * vector.x + vector.y * vector.y)
    }

    @JvmStatic
    fun normalize(vector: Vector2): Vector2 {
        val len = length(vector)
        if (abs(len) < 0.0001f) {
            return Vector2(0, 0)
        }
        return Vector2(vector.x / len, vector.y / len)
    }

    @JvmStatic
    fun distance(v1: Vector2, v2: Vector2): Float {
        return hypot(v1.x - v2.x, v1.y - v2.y)
    }

    @JvmStatic
    fun squaredDistance(v1: Vector2, v2: Vector2): Float {
        return ((v2.x - v1.x) * (v2.x - v1.x) + (v2.y - v1.y) * (v2.y - v1.y))
    }

    @JvmStatic
    fun squaredDistance(v1x: Float, v1y: Float, v2x: Float, v2y: Float): Float {
        return ((v2x - v1x) * (v2x - v1x) + (v2y - v1y) * (v2y - v1y))
    }

    @JvmStatic
    fun isEmpty(str: String?): Boolean {
        return str == null || str.isEmpty()
    }

    @JvmStatic
    fun toRes(i: Int): Int {
        return i / 1
    }

    @JvmStatic
    fun toRes(i: Float): Float {
        return i / 1
    }

    @JvmStatic
    fun toFSValidString(s: String): String {
        val nameBuilder = StringBuilder()
        for (i in 0 until s.length) {
            if (FSReservedChars.indexOf(s[i]) == -1) {
                nameBuilder.append(s[i])
            }
        }
        return nameBuilder.toString()
    }

    @JvmStatic
    fun tryParseFloat(str: String, defaultVal: Float): Float {
        var `val`: Float
        try {
            `val` = str.toFloat()
        } catch (ignored: NumberFormatException) {
            `val` = defaultVal
        }
        return `val`
    }

    @JvmStatic
    fun tryParseInt(str: String, defaultVal: Int): Int {
        var `val`: Int
        try {
            `val` = str.toInt()
        } catch (ignored: NumberFormatException) {
            `val` = defaultVal
        }
        return `val`
    }

    @JvmStatic
    fun tryParseDouble(str: String, defaultVal: Double): Double {
        var `val`: Double
        try {
            `val` = str.toDouble()
        } catch (ignored: NumberFormatException) {
            `val` = defaultVal
        }
        return `val`
    }
}
