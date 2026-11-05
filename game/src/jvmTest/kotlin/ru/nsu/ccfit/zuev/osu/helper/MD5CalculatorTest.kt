package ru.nsu.ccfit.zuev.osu.helper

import java.math.BigInteger
import java.security.MessageDigest
import org.junit.Assert
import org.junit.Test

class MD5CalculatorTest {
    @Test
    fun `Test string MD5 matches the padded hexadecimal digest`() {
        for (str in listOf("", "osu!droid", "taikotaiko", "ünïcödé ✓", "https://example.com/a.png")) {
            val digest = MessageDigest.getInstance("MD5").digest(str.toByteArray())
            val expected = BigInteger(1, digest).toString(16).padStart(32, '0')

            Assert.assertEquals(expected, MD5Calculator.getStringMD5(str))
        }
    }
}
