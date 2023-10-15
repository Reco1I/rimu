package com.reco1l.api.chimu

import androidx.core.net.toUri
import com.reco1l.framework.data.mapInto
import com.reco1l.framework.data.mapIntoListOf
import com.reco1l.framework.net.request.JsonRequester
import okhttp3.OkHttpClient

/**
 * Kotlin's bindings for Chimu Cheesegull API.
 *
 * [Chimu docs...](https://chimu.moe/docs)
 */
object CheesegullAPI
{

    /**
     * The client where all request will be done, it cannot be `null` before making any request.
     */
    var client: OkHttpClient? = null

    /**
     * The hostname.
     */
    const val HOST = "http://api.chimu.moe"

    /**
     * The endpoint to search with a query.
     */
    const val SEARCH = "$HOST/cheesegull/search"

    /**
     * The endpoint to get a beatmap by its ID.
     */
    const val BEATMAP_BY_ID = "$HOST/cheesegull/b/"

    /**
     * The endpoint to get a beatmap by its MD5 checksum.
     */
    const val BEATMAP_BY_MD5 = "$HOST/cheesegull/md5/"

    /**
     * The endpoint to get a beatmap set by its ID.
     */
    const val BEATMAP_SET_BY_ID = "$HOST/cheesegull/s/"


    /**
     * Get beatmap information from its MD5.
     */
    inline fun <reified T : Any> getBeatmapInfo(md5: String): T?
    {
        JsonRequester(client!!, BEATMAP_BY_MD5 + md5).use {

            return it.jsonObject!!.mapInto()
        }
    }

    /**
     * Get beatmap information from its ID.
     */
    inline fun <reified T : Any> getBeatmapInfo(id: Long): T?
    {
        JsonRequester(client!!, BEATMAP_BY_ID + id).use {

            return it.jsonObject!!.mapInto()
        }
    }

    /**
     * Get beatmap set information from its ID.
     */
    inline fun <reified T : Any> getBeatmapSetInfo(id: Long): T?
    {
        JsonRequester(client!!, BEATMAP_SET_BY_ID + id).use {

            return it.jsonObject!!.mapInto()
        }
    }

    /**
     * Search in the database with a query.
     */
    inline fun <reified T : Any> getBeatmapList(query: Map<String, String> = emptyMap()): MutableList<T>?
    {
        JsonRequester(client!!, SEARCH.toUri().buildUpon().apply {

            for (key in query.keys)
                appendQueryParameter(key, query[key])

        }.build()).use { return it.jsonArray?.mapIntoListOf() }
    }
}
