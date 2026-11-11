package ru.nsu.ccfit.zuev.osu

import kotlin.jvm.JvmStatic
import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString

/**
 * 加解密工具类
 *
 * The parts that need the platform (the APK signature, the bundled assets) are in `AndroidSecurityUtils`
 * on Android.
 *
 * @author dgsrz (dgsrz@vip.qq.com)
 */
object SecurityUtils {

    private val BIT_MASK = intArrayOf(0x7f, 0x3f, 0x1f, 0x0f, 0x07, 0x03, 0x01)
    private val COMPRESSED = intArrayOf(
            0xd7, 0x32, 0x73, 0xc, 0x6b, 0x96, 0x1,
            0xd4, 0xf7, 0x8, 0x36, 0xaf, 0x87, 0x0
    )  // WeLc0MeTo#0su!

    /**
     * The SHA-1 hash of the signature of the app, set by the platform. Requests aren't signed without it.
     */
    @JvmStatic
    var appSignature: String? = null

    private var secretBuffer: ByteArray? = null

    /**
     * @return 得到整数x的非零位个数
     */
    @JvmStatic
    fun getNonZeroBitsCount(x: Int): Int {
        var x = x
        x = (x and 0x55555555) + ((x and 0xaaaaaaaa.toInt()) shr 1)
        x = (x and 0x33333333) + ((x and 0xcccccccc.toInt()) shr 2)
        x = (x and 0x0f0f0f0f) + ((x and 0xf0f0f0f0.toInt()) shr 4)
        x = (x and 0x00ff00ff) + ((x and 0xff00ff00.toInt()) shr 8)
        x = (x and 0x0000ffff) + ((x and 0xffff0000.toInt()) shr 16)
        return x
    }

    /**
     * @return 得到整数x高20位对3389取模后的结果
     */
    @JvmStatic
    fun getHigh16Bits(x: Int): Int {
        return (x shr 12) % 3389
    }

    @JvmStatic
    fun getHigh16Bits(x: Long): Int {
        return ((x shr 12) % 3389).toInt()
    }

    /**
     * @return 7-bit解码
     */
    @JvmStatic
    fun getSecretKey(): ByteArray {
        if (secretBuffer == null) {
            var highest = 0
            val secretBuffer = ByteArray(COMPRESSED.size)
            for (i in COMPRESSED.indices) {
                val index = i % 7
                highest = if (index == 0) 0 else highest
                val lowest = COMPRESSED[i] and BIT_MASK[index]
                secretBuffer[i] = ((lowest shl index) or highest).toByte()
                highest = (COMPRESSED[i] and (0xff xor BIT_MASK[index])) shr (7 - index)
            }
            this.secretBuffer = secretBuffer
        }
        return secretBuffer!!
    }

    /**
     * @return 对请求加签，包含请求文本、APk签名哈希、会话ID
     */
    @JvmStatic
    fun signRequest(request: String): String? {
        val appSignature = appSignature ?: return null
        val digest = "${appSignature}_$request"
        return hmacSha1(digest.encodeUtf8().toByteArray())
    }

    /**
     * The HMAC-SHA1 of [bytes] with the secret key, as lowercase hexadecimal digits.
     */
    @JvmStatic
    fun hmacSha1(bytes: ByteArray): String {
        return bytes.toByteString().hmacSha1(getSecretKey().toByteString()).hex()
    }

    /**
     * The SHA-1 hash of [bytes], as lowercase hexadecimal digits.
     */
    @JvmStatic
    fun getHashCode(bytes: ByteArray): String {
        return bytes.toByteString().sha1().hex()
    }
}
