package ru.nsu.ccfit.zuev.osu.online

import com.osudroid.online.OsuHttp

class URLEncodedPostBuilder : PostBuilder() {
    private val fields = mutableListOf<Pair<String, String>>()

    override fun addParamInternal(key: String, value: String) {
        fields += key to value
    }

    override suspend fun post(url: String) = OsuHttp.postForm(url, fields.toList())
}
