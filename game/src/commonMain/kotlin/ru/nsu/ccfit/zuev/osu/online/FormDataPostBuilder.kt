package ru.nsu.ccfit.zuev.osu.online

import com.osudroid.online.FormPart
import com.osudroid.online.OsuHttp
import okio.Path.Companion.toPath

class FormDataPostBuilder : PostBuilder() {
    private val parts = mutableListOf<FormPart>()

    /**
     * Adds the file at [filePath] as a part named [key].
     */
    fun addParam(key: String, filename: String, contentType: String, filePath: String) {
        parts += FormPart.File(key, filename, contentType, filePath.toPath())
    }

    override fun addParamInternal(key: String, value: String) {
        parts += FormPart.Value(key, value)
    }

    override suspend fun post(url: String) = OsuHttp.postMultipart(url, parts.toList())
}
