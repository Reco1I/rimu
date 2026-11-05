package ru.nsu.ccfit.zuev.osu.online

import com.reco1l.framework.net.HttpBody

class URLEncodedPostBuilder : PostBuilder() {
    private val fields = mutableListOf<Pair<String, String>>()

    override fun addParamInternal(key: String, value: String) {
        fields += key to value
    }

    override fun getRequestBody(): HttpBody {
        return HttpBody.Form(fields.toList())
    }
}
