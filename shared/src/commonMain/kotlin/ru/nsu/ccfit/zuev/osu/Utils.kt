package ru.nsu.ccfit.zuev.osu

import com.osudroid.math.Vector2
import kotlin.jvm.JvmStatic
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt

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
