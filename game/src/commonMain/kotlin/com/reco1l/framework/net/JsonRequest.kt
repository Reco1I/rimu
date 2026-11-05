package com.reco1l.framework.net

import com.osudroid.OsuPlatform
import com.osudroid.utils.JsonException
import com.osudroid.utils.LenientJson
import com.reco1l.verktex.Logger
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

sealed class JsonRequest<T : Any>(url: String) : WebRequest(url) {


    /**
     * The response JSON.
     */
    lateinit var json: T
        private set


    protected abstract fun onCreateResponseJson(responseBody: String): T


    override fun onResponse(response: HttpResponse) {
        json = onCreateResponseJson(responseBody.readUtf8())
    }


    @Suppress("UNCHECKED_CAST")
    override fun execute() = super.execute() as JsonRequest<T>


    protected fun log(json: Any) {
        if (OsuPlatform.isInitialized && OsuPlatform.implementation.isDebug) {
            Logger.i("JsonRequester", "Received JSON: $json")
        }
    }
}


class JsonObjectRequest(url: String) : JsonRequest<JsonObject>(url) {


    override fun onCreateResponseJson(responseBody: String): JsonObject {

        if (!responseBody.startsWith('{') || !responseBody.endsWith('}')) {
            throw JsonException("Response is not a JSON object:\n$responseBody")
        }

        val json = LenientJson.parseToJsonElement(responseBody) as? JsonObject
            ?: throw JsonException("Response is not a JSON object:\n$responseBody")

        log(json)

        return json
    }

}

class JsonArrayRequest(url: String) : JsonRequest<JsonArray>(url) {


    override fun onCreateResponseJson(responseBody: String): JsonArray {

        if (!responseBody.startsWith('[') || !responseBody.endsWith(']')) {
            throw JsonException("Response is not a JSON array:\n$responseBody")
        }

        val json = LenientJson.parseToJsonElement(responseBody) as? JsonArray
            ?: throw JsonException("Response is not a JSON array:\n$responseBody")

        log(json)

        return json
    }
}
