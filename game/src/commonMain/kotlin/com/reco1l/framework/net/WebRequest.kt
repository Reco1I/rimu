package com.reco1l.framework.net

import com.osudroid.OsuPlatform
import com.reco1l.verktex.Logger
import kotlin.jvm.JvmField
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import okio.BufferedSource
import okio.IOException

open class WebRequest(private var url: String) : AutoCloseable {


    /**
     * The response got upon request, this will not be initialized until [execute] has been
     * successfully called.
     */
    lateinit var response: HttpResponse
        protected set

    /**
     * The response body got upon request, this will not be initialized until [execute] has been
     * successfully called.
     */
    lateinit var responseBody: BufferedSource
        protected set

    /**
     * The call object used to execute the request.
     * It will be initialized upon [execute] call.
     */
    lateinit var call: HttpCall
        protected set


    private var request = HttpRequestBuilder(url).build()


    /**
     * Builds a new request upon the current one.
     */
    fun buildRequest(block: HttpRequestBuilder.() -> Unit) {

        request = request.newBuilder().apply(block).build()
    }

    /**
     * Builds a JSON request body with data to be sent to server.
     */
    fun buildRequestBody(block: JsonObjectBuilder.() -> Unit) {

        val json = buildJsonObject(block)

        buildRequest {
            post(HttpBody.Bytes(HttpBody.JSON_UTF_8, json.toString().encodeToByteArray()))
        }
    }

    /**
     * Builds an HTTP URL upon the current one.
     */
    fun buildUrl(block: HttpUrlBuilder.() -> Unit) {

        url = HttpUrlBuilder(url).apply(block).build()

        // Rebuild the request to apply the URL change.
        buildRequest {
            url(url)
        }
    }


    /**
     * Called as soon the response and response body is created.
     */
    protected open fun onResponse(response: HttpResponse) = Unit

    /**
     * Called when response code isn't 200-299, this can also be called from inherited
     * [onResponseSuccess] exception thrown.
     */
    protected open fun onResponseFail(exception: Exception): Unit = throw exception

    /**
     * Called when response code is 200-299.
     */
    protected open fun onResponseSuccess(response: HttpResponse) = Unit


    override fun close() {

        if (::response.isInitialized) {
            response.close()
        }


        if (::call.isInitialized) {
            call.cancel()
        }
    }


    /**
     * Cancels the current request.
     * This will have no effect if [execute] wasn't called or the request has already finished.
     */
    open fun cancel() {
        if (::call.isInitialized) {
            call.cancel()
        }
    }


    /**
     * Executes the request.
     *
     * Make sure to call before call close() or inside the try-with-resources statement.
     */
    @Throws(Exception::class)
    open fun execute(): WebRequest {

        try {
            call = Http.newCall(request)

            if (OsuPlatform.isInitialized && OsuPlatform.implementation.isDebug) {
                Logger.i("WebRequest", "Request to: " + request.url)
            }

            response = call.execute()
            responseBody = response.body

            onResponse(response)

            if (response.isSuccessful) {
                onResponseSuccess(response)
            } else {
                throw UnsuccessfulResponseException(response)
            }

        } catch (e: Exception) {
            onResponseFail(e)
        }

        return this
    }


    companion object {

        /**
         * The Json UTF-8 media type specification.
         */
        const val JSON_UTF_8 = HttpBody.JSON_UTF_8

    }
}


/**
 * Denotes a response that is not successful.
 */
class UnsuccessfulResponseException(response: HttpResponse) : IOException(response.message) {

    /**
     * The response code.
     */
    @JvmField
    val code: Int = response.code

}
