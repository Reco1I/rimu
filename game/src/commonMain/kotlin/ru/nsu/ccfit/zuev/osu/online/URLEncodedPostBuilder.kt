package ru.nsu.ccfit.zuev.osu.online

import com.osudroid.online.httpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters

class URLEncodedPostBuilder : PostBuilder() {
    private val fields = mutableListOf<Pair<String, String>>()

    override fun addParamInternal(key: String, value: String) {
        fields += key to value
    }

    override suspend fun post(url: String): String {
        val parameters = Parameters.build { fields.forEach { (name, value) -> append(name, value) } }
        return httpClient.submitForm(url, parameters).bodyAsText()
    }
}
