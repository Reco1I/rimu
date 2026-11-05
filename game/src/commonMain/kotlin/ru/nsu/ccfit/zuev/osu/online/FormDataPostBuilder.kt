package ru.nsu.ccfit.zuev.osu.online

import com.reco1l.framework.net.HttpBody

class FormDataPostBuilder : PostBuilder() {
    private val parts = mutableListOf<HttpBody.Part>()

    /**
     * Adds the file at [filePath] as a part named [key].
     */
    fun addParam(key: String, filename: String, contentType: String, filePath: String) {
        parts += HttpBody.Part(key, filename = filename, contentType = contentType, filePath = filePath)
    }

    override fun addParamInternal(key: String, value: String) {
        parts += HttpBody.Part(key, value)
    }

    override fun getRequestBody(): HttpBody {
        return HttpBody.Multipart(parts.toList())
    }
}
