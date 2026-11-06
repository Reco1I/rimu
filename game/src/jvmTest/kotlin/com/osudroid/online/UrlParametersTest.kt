package com.osudroid.online

import io.ktor.http.URLBuilder
import io.ktor.http.Url
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What the requests rely on from Ktor's URLs, which replaced the game's own builder.
 */
class UrlParametersTest {

    @Test
    fun `parameters are percent-encoded and read back as they were`() {
        val url = URLBuilder("https://osu.direct/api/v2/search").apply {
            parameters.append("sort", "title:asc")
            parameters.append("query", "ünï cødé & more+")
        }.buildString()

        // A space is written as "+", which query strings decode as a space as well as "%20".
        assertTrue(url, "+c%C3%B8d%C3%A9+%26+more%2B" in url)

        val parsed = Url(url).parameters
        assertEquals("title:asc", parsed["sort"])
        assertEquals("ünï cødé & more+", parsed["query"])
    }

    @Test
    fun `a parameter without values is sent as its name alone`() {
        val url = URLBuilder("https://a.b/c").apply {
            parameters.appendAll("flag", emptyList())
            parameters.append("y", "2")
        }.buildString()

        assertEquals("https://a.b/c?flag&y=2", url)
    }

    @Test
    fun `an existing query is kept`() {
        val url = URLBuilder("https://a.b/c?x=1").apply { parameters.append("y", "2") }.buildString()
        assertEquals("https://a.b/c?x=1&y=2", url)
    }
}
