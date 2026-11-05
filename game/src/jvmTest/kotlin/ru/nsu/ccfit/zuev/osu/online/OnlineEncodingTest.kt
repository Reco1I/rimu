package ru.nsu.ccfit.zuev.osu.online

import com.osudroid.utils.sha256Checksum
import java.io.File
import java.math.BigInteger
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.nsu.ccfit.zuev.osu.SecurityUtils

class OnlineEncodingTest {

    private val samples = listOf("", "abc", "a b+c", "ünï/cødé?&=%*._-~", "{\"json\": [1, 2]}", "日本語 テキスト", "line\nbreak\ttab")

    @Test
    fun `Test URL encoding matches URLEncoder`() {
        for (sample in samples) {
            assertEquals(URLEncoder.encode(sample, "UTF-8"), PostBuilder.urlEncode(sample))
        }
    }

    @Test
    fun `Test HTTP dates match SimpleDateFormat`() {
        val format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH).apply { timeZone = TimeZone.getTimeZone("GMT") }

        for (millis in listOf(0L, 951_782_400_000L, 1_700_000_000_123L, 4_102_444_799_000L, -86_400_000L, 1_709_164_800_000L)) {
            assertEquals(format.format(Date(millis)) + " GMT", OnlineFileOperator.formatHttpDate(millis))
        }
    }

    @Test
    fun `Test request signatures match javax HMAC`() {
        SecurityUtils.appSignature = "0123456789abcdef0123456789abcdef01234567"

        for (sample in samples) {
            val mac = Mac.getInstance("HmacSHA1")
            mac.init(SecretKeySpec(SecurityUtils.getSecretKey(), mac.algorithm))
            val expected = mac.doFinal("${SecurityUtils.appSignature}_$sample".toByteArray()).joinToString("") { "%02x".format(it) }

            assertEquals(expected, SecurityUtils.signRequest(sample))
        }

        SecurityUtils.appSignature = null
        assertEquals(null, SecurityUtils.signRequest("x"))
    }

    @Test
    fun `Test SHA-256 checksum matches MessageDigest`() {
        val file = File.createTempFile("checksum", ".bin").apply { deleteOnExit() }
        file.writeBytes(ByteArray(5000) { (it * 31).toByte() })

        val digest = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
        val expected = BigInteger(1, digest).toString(16).padStart(64, '0')

        assertEquals(expected, sha256Checksum(file.path))
    }
}
