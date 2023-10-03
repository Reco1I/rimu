package com.reco1l.api.groove

import androidx.core.net.toUri
import com.reco1l.api.groove.exceptions.InvalidCredentialException
import com.reco1l.api.groove.exceptions.NotFoundException
import com.reco1l.framework.extensions.mapInto
import com.reco1l.framework.extensions.mapIntoListOf
import com.reco1l.framework.extensions.putGroup
import com.reco1l.framework.net.request.JsonRequester
import com.reco1l.framework.net.request.Requester
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.util.UUID

/**
 * Kotlin's bindings for Groove API (rimu! backend).
 */
object GrooveAPI
{

    /**
     * The client where all request will be done, it cannot be `null` before making any request.
     */
    var client: OkHttpClient? = null

    /**
     * The hostname.
     */
    const val HOST = "https://beta.acivev.de"

    /**
     * Endpoint to get user statistics from API V1.
     */
    const val USER_STATISTICS = "$HOST/api/profile/stats/"

    /**
     * Endpoint to get user avatar.
     */
    const val USER_AVATAR = "$HOST/api2/avatar/"

    /**
     * Endpoint to get beatmap leaderboard.
     */
    const val BEATMAP_LEADERBOARD = "$HOST/api2/rank/map-file"

    /**
     * Endpoint to create a login token.
     */
    const val TOKEN_CREATE = "$HOST/api2/token-create"

    /**
     * Endpoint to revoke a login token.
     */
    const val TOKEN_REMOVE = "$HOST/api2/token-remove"

    /**
     * Endpoint to get the user ID from a token.
     */
    const val TOKEN_IDENTIFIER = "$HOST/api2/token-user-id"


    // Token

    /**
     * Create a login token in the server.
     *
     * @throws InvalidCredentialException if the user credentials are not valid.
     */
    fun createToken(username: String?, passwd: String?): UUID?
    {
        JsonRequester(client!!, TOKEN_CREATE).use {

            it.dataInsertion = JSONObject().apply {

                put("username", username)
                put("passwd", passwd)
            }

            if (it.jsonObject!!.getBoolean("usernameFalse"))
                throw InvalidCredentialException(Credential.USERNAME)

            if (it.jsonObject!!.getBoolean("passwdFalse"))
                throw InvalidCredentialException(Credential.PASSWORD)


            return UUID.fromString(it.jsonObject!!.getString("token"))
        }
    }

    /**
     * Remove a login token in the server.
     *
     * @return `true` if it was successfully done.
     */
    fun revokeToken(token: UUID?): Boolean
    {
        JsonRequester(client!!, TOKEN_REMOVE).use {

            it.dataInsertion = JSONObject().put("token", token)

            return it.jsonObject!!.getBoolean("hasWork")
        }
    }


    // User

    /**
     * Get the user ID from a token previously created.
     *
     * @throws NotFoundException if the token was not found in the server.
     */
    fun getUserID(token: UUID?): Long = JsonRequester(client!!, TOKEN_IDENTIFIER.toUri()).use {

        it.dataInsertion = JSONObject().put("token", token)

        return it.jsonObject!!.getLong("value")
    }

    /**
     * Get the user avatar from the server as a [ByteArray].
     */
    fun getUserAvatar(userId: Long, size: Int): ByteArray
    {
        Requester(client!!, "$USER_AVATAR$size/$userId")
            .use { return it.response!!.body!!.bytes() }
    }

    /**
     * Get the user statistics and map the JSON entries into the defined [T].
     */
    inline fun <reified T : Any> getUserStatistics(userId: Long): T?
    {
        JsonRequester(client!!, "$USER_STATISTICS$userId").use {

            return it.jsonObject!!.mapInto()
        }
    }


    // Leaderboard

    /**
     * Get the beatmap leaderboard and map the JSON entries into the defined [T] type.
     *
     * Keep in mind JSON mapper will only map entries that its key matches the property name in the data class.
     *
     * @throws NotFoundException if the beatmap was not found.
     */
    inline fun <reified T : Any> getBeatmapLeaderboard(filename: String, hash: String, token: UUID?): MutableList<T>?
    {
        JsonRequester(client!!, BEATMAP_LEADERBOARD).use {

            it.dataInsertion = JSONObject().apply {

                putGroup("header")
                {
                    put("token", token)
                    put("hashBodyData", null)
                }

                putGroup("body")
                {
                    put("Filename", filename)
                    put("FileHash", hash)
                }
            }

            return it.jsonArray!!.mapIntoListOf()
        }
    }
}
