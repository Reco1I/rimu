package com.reco1l.framework.net

import org.junit.Assert.assertEquals
import org.junit.Test

class HttpUrlBuilderTest {

    @Test
    fun `Test parameters are appended and percent-encoded`() {
        val url = HttpUrlBuilder("https://osu.direct/api/v2/search")
            .addQueryParameter("sort", "title:asc")
            .addQueryParameter("query", "ünï cødé & more+")
            .addQueryParameter("flag", null)
            .build()

        assertEquals("https://osu.direct/api/v2/search?sort=title%3Aasc&query=%C3%BCn%C3%AF%20c%C3%B8d%C3%A9%20%26%20more%2B&flag", url)
    }

    @Test
    fun `Test existing query and fragment are kept`() {
        assertEquals("https://a.b/c?x=1&y=2#top", HttpUrlBuilder("https://a.b/c?x=1#top").addQueryParameter("y", "2").build())
        assertEquals("https://a.b/c", HttpUrlBuilder("https://a.b/c").build())
    }
}
