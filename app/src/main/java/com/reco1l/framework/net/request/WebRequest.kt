package com.reco1l.framework.net.request

import android.net.Uri
import com.reco1l.framework.kotlin.orCatch
import com.reco1l.framework.data.buildRequest
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException


abstract class WebRequest<T : Any>(

    val client: OkHttpClient,

    val uri: Uri,

    val post: Pair<String, String?>? = null,

    )
{

    var onCall: ((Call) -> Unit)? = null

    var onSuccess: ((T) -> Unit)? = null

    var onError: ((Exception) -> Unit)? = null

    var result: T? = null


    protected abstract fun onResolveResult(response: Response): T?


    protected open fun onCall(call: Call) = onCall?.invoke(call)

    protected open fun onResponse(response: Response) = {

        if (!response.isSuccessful)
            throw UnexpectedResponseException(response)

        val result = onResolveResult(response)
            ?:
            throw ResponseResolveException(response)

        onSuccess?.invoke(result)
        Unit

    }.orCatch {
        onError?.invoke(it)
        Unit
    }


    private fun execute()
    {
        val request = buildRequest {

            // Applying URI
            url(uri.toString())

            // Applying post data insertion.
            post?.apply { post(first.toRequestBody(second?.toMediaType())) }

        }

        val call = client.newCall(request)
        onCall(call)

        val response = call.execute()
        onResponse(response)

        response.close()
    }
}


class UnexpectedResponseException(response: Response) :
    IOException("Unexpected response: $response")
{
    val code: Int = response.code
}

class ResponseResolveException(response: Response) :
    IOException("Unable to resolve response body: $response")