package com.reco1l.framework.net

import java.io.File
import okhttp3.Call
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.BufferedSource

/**
 * Makes the game's HTTP requests with OkHttp.
 */
class OkHttpClientImplementation(private val client: OkHttpClient = OkHttpClient()) : Http.Implementation {

    override fun newCall(request: HttpRequest): HttpCall {
        val builder = Request.Builder().url(request.url)

        for ((name, value) in request.headers) {
            builder.addHeader(name, value)
        }

        builder.method(request.method, request.body?.toRequestBody())

        return OkHttpCall(client.newCall(builder.build()))
    }


    private fun HttpBody.toRequestBody(): RequestBody = when (this) {
        is HttpBody.Bytes -> bytes.toRequestBody(contentType.toMediaTypeOrNull())

        is HttpBody.Form -> FormBody.Builder().apply {
            for ((name, value) in fields) add(name, value)
        }.build()

        is HttpBody.Multipart -> MultipartBody.Builder().setType(MultipartBody.FORM).apply {
            for (part in parts) {
                if (part.filePath != null) {
                    addFormDataPart(part.name, part.filename, File(part.filePath!!).asRequestBody(part.contentType?.toMediaTypeOrNull()))
                } else {
                    addFormDataPart(part.name, part.value ?: "")
                }
            }
        }.build()
    }


    private class OkHttpCall(private val call: Call) : HttpCall {

        override fun execute(): HttpResponse = OkHttpResponse(call.execute())

        override fun cancel() = call.cancel()
    }

    private class OkHttpResponse(private val response: Response) : HttpResponse {

        override val code get() = response.code

        override val message: String get() = response.message

        override val contentLength get() = response.body?.contentLength() ?: -1L

        override val body: BufferedSource get() = response.body!!.source()

        override fun header(name: String) = response.header(name)

        override fun close() = response.close()
    }
}
