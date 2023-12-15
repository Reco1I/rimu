package com.reco1l.api.net.request

import android.net.Uri
import androidx.core.net.toUri
import com.reco1l.toolkt.data.buildRequest
import okhttp3.*
import java.io.IOException
import java.util.*

/**
 * @author Reco1l
 */
open class Requester(private val client: OkHttpClient, uri: Uri) : AutoCloseable
{

    constructor(client: OkHttpClient, url: String) : this(client, url.toUri())

    /**
     * The request.
     */
    var request: Request? = buildRequest { url(uri.toString()) }

    /**
     * The call executed by the client, it'll automatically call [execute] if it wasn't called.
     */
    var call: Call? = null
        get()
        {
            if (field == null)
                execute()
            return field
        }
        private set

    /**
     * The response get, it'll automatically call [execute] if it wasn't called.
     */
    var response: Response? = null
        get()
        {
            if (field == null)
                execute()
            return field
        }
        private set


    /**
     * Called as soon the response and response body is created.
     */
    @Throws(Exception::class)
    protected open fun onResponse(response: Response) = Unit

    /**
     * Called when response code isn't 200-299, this can also be called from inherited [onResponseSuccess] exception thrown.
     */
    @Throws(Exception::class)
    protected open fun onResponseError(exception: Exception): Unit = throw exception

    /**
     * Called when response code is 200-299.
     */
    @Throws(Exception::class)
    protected open fun onResponseSuccess(response: Response) = Unit


    override fun close()
    {
        response?.close()
        response = null
    }


    /**
     * Make sure to call before call close() or inside the try-with-resources statement.
     */
    open fun execute(): Requester
    {
        if (request == null)
            throw NullPointerException("The request cannot be null.")

        try
        {
            call = client.newCall(request!!)
            response = call!!.execute()

            onResponse(response!!)

            if (response!!.isSuccessful)
                onResponseSuccess(response!!)
            else
                throw ResponseException(response!!)
        }
        catch (e: Exception)
        {
            onResponseError(e)
        }
        return this
    }
}


class ResponseException(response: Response) : IOException("Unexpected response: $response")
{
    val code: Int = response.code
}