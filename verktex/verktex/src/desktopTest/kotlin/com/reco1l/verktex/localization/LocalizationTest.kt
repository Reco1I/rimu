package com.reco1l.verktex.localization

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

class LocalizationTest {

    private val source = object : StringSource {
        val strings = mapOf(
            "en" to mapOf("hello" to "Hello", "count" to "%d items", "only_en" to "English"),
            "pt" to mapOf("hello" to "Olá", "count" to "%d itens"),
            "pt-BR" to mapOf("hello" to "Oi"),
        )

        override val locales get() = strings.keys

        override fun get(locale: String, key: String) = strings[locale]?.get(key)

        override fun getArray(locale: String, key: String): List<String>? = if (key == "days" && locale == "en") listOf("Mon", "Tue") else null
    }

    @Before
    fun setUp() {
        Localization.addSource(source)
    }

    @After
    fun tearDown() {
        Localization.removeSource(source)
        Localization.locale = Localization.DEFAULT_LOCALE
    }


    @Test
    fun `tags are normalized`() {
        assertEquals("pt-BR", Localization.normalize("pt-rBR"))
        assertEquals("pt-BR", Localization.normalize("pt_br"))
        assertEquals("zh-Hans-CN", Localization.normalize("ZH-hans-cn"))
        assertEquals("en", Localization.normalize(""))
    }

    @Test
    fun `strings fall back to more general locales`() {
        assertEquals(listOf("pt-BR", "pt", "en"), Localization.fallbackChainOf("pt-rBR"))

        Localization.locale = "pt-rBR"
        assertEquals("Oi", Localization.get("hello"))
        assertEquals("%d itens", Localization.get("count"))
        assertEquals("English", Localization.get("only_en"))
        assertEquals("missing", Localization.get("missing"))
        assertEquals(listOf("Mon", "Tue"), Localization.getArray("days"))
    }

    @Test
    fun `localized strings follow the locale`() {
        val hello = LocalizedString("hello")
        val count = LocalizedString("count").format(3)

        assertEquals("Hello", hello.toString())
        assertEquals("3 items", count.toString())

        Localization.locale = "pt"
        assertEquals("Olá", hello.toString())
        assertEquals("3 itens", count.toString())
        assertEquals(3, hello.length)
    }

    @Test
    fun `localized strings are equal by key and arguments`() {
        assertEquals(LocalizedString("count").format(1), LocalizedString("count").format(1))
        assertNotEquals(LocalizedString("count").format(1), LocalizedString("count").format(2))
        assertNotEquals(LocalizedString("count"), LocalizedString("count").format())
    }

    @Test
    fun `formats like java's formatter`() {
        assertEquals("a 1 b", formatString("%s %d %s", "a", 1, "b"))
        assertEquals("b a", formatString("%2\$s %1\$s", "a", "b"))
        assertEquals("1.50 100%", formatString("%.2f %d%%", 1.5, 100))
        assertEquals("1.01", formatString("%.2f", 1.005))
        assertEquals("-0.13", formatString("%.2f", -0.125))
        assertEquals("3", formatString("%.0f", 2.5))
        assertEquals("0.333333", formatString("%f", 1.0 / 3))
        assertEquals("1,234,567", formatString("%,d", 1234567))
        assertEquals("007|-07|  7|7  |+7", formatString("%03d|%03d|%3d|%-3d|%+d", 7, -7, 7, 7, 7))
        assertEquals("ab", formatString("%.2s", "abc"))
        assertEquals("ff FF", formatString("%x %X", 255, 255))
        assertEquals("null", formatString("%s", null))
        assertEquals("a\nb", formatString("a%nb"))

        for (value in listOf(0.0, 0.1, 0.5, 1.25, 99.995, 123456.789, 1e-7)) {
            assertEquals(String.format(java.util.Locale.ROOT, "%.3f", value), formatString("%.3f", value))
        }
    }
}
