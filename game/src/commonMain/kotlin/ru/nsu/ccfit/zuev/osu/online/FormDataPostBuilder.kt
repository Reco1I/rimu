package ru.nsu.ccfit.zuev.osu.online

import com.osudroid.online.httpClient
import com.reco1l.verktex.Platform
import io.ktor.client.request.forms.FormBuilder
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import okio.Path.Companion.toPath

class FormDataPostBuilder : PostBuilder() {
    private val parts = mutableListOf<FormBuilder.() -> Unit>()

    /**
     * Adds the file at [filePath] as a part named [key].
     */
    fun addParam(key: String, filename: String, contentType: String, filePath: String) {
        parts += {
            append(key, Platform.fileSystem.read(filePath.toPath()) { readByteArray() }, Headers.build {
                append(HttpHeaders.ContentType, contentType)
                append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
            })
        }
    }

    override fun addParamInternal(key: String, value: String) {
        parts += { append(key, value) }
    }

    override suspend fun post(url: String): String {
        val parts = parts.toList()
        return httpClient.submitFormWithBinaryData(url, formData { parts.forEach { it() } }).bodyAsText()
    }
}
