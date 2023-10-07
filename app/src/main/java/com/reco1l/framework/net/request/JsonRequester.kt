package com.reco1l.framework.net.request

import android.net.Uri
import androidx.core.net.toUri
import com.reco1l.framework.net.buildRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

class JsonRequester(client: OkHttpClient, uri: Uri) : Requester(client, uri)
{

    constructor(client: OkHttpClient, url: String) : this(client, url.toUri())


    /**
     * The response JSON object, it'll automatically call [execute] if it wasn't called.
     */
    var jsonObject: JSONObject? = null
        get()
        {
            if (field == null)
                execute()

            return field
        }
        private set

    /**
     * The response JSON Array, it'll automatically call [execute] if it wasn't called.
     */
    var jsonArray: JSONArray? = null
        get()
        {
            if (field == null)
                execute()

            return field
        }
        private set

    /**
     * Data insertion for POST requests.
     */
    var dataInsertion: JSONObject? = null
        set(value)
        {
            if (value != null) buildRequest {

                post(value.toString().toRequestBody(JSON_UTF8))
            }
            field = value
        }


    @Throws(Exception::class)
    override fun onResponse(response: Response)
    {
        val t = JSONTokener(response.body!!.string())

        when(val value = t.nextValue())
        {
            is JSONObject -> jsonObject = value
            is JSONArray -> jsonArray = value
            else -> throw ResponseException(response)
        }
    }


    @Throws(Exception::class)
    override fun execute() = super.execute() as JsonRequester


    companion object
    {
        val JSON_UTF8 = "application/json; charset=utf-8".toMediaTypeOrNull()
    }
}
