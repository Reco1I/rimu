package ru.nsu.ccfit.zuev.osu.online

import io.ktor.client.HttpClient
import ru.nsu.ccfit.zuev.osu.SecurityUtils
import okio.Path
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds
import io.ktor.http.Parameters
import io.ktor.http.HttpHeaders
import io.ktor.http.Headers
import io.ktor.client.statement.bodyAsText
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.forms.formData
import com.osudroid.online.httpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.osudroid.settings.GameFolders
import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.settings.Settings
import com.osudroid.OsuPlatform
import com.osudroid.data.BeatmapInfo
import com.reco1l.verktex.Platform
import com.osudroid.utils.sha256Checksum

import ru.nsu.ccfit.zuev.osu.helper.Debug

import kotlin.jvm.JvmField
import kotlin.jvm.JvmName
import kotlin.jvm.JvmStatic
import okio.Path.Companion.toPath

import com.osudroid.ui.v2.CachedTextures
import ru.nsu.ccfit.zuev.osu.RankedStatus
import ru.nsu.ccfit.zuev.osu.helper.MD5Calculator
import ru.nsu.ccfit.zuev.osu.scoring.BeatmapLeaderboardScoringMode

class OnlineManager {
    private val loginMutex = Mutex()

    var failMessage = ""
        private set

    var isStayOnline = true
    private var ssid = ""
    var userId = -1L
        private set

    var username = ""
        private set
    var password = ""
        private set
    var deviceID = ""
        private set
    var rank = 0L
        private set
    var score = 0L
        private set
    var accuracy = 0f
        private set
    @get:JvmName("getPP")
    var pp = 0f
        private set
    var avatarURL = ""
        private set
    var profileBannerURL = ""
        private set
    var mapRank = 0
        private set

    val sessionId: String
        get() = ssid

    fun init() {
        this.isStayOnline = OsuSettings.isStayOnline
        this.username = OsuSettings.onlineUsername
        this.password = Settings[OsuSettings.OnlinePassword]
        this.deviceID = OsuSettings.installId
    }

    /**
     * Posts [fields] to [url] as the osu!droid server expects them: their values are signed (see
     * [SecurityUtils.signRequest]) and the request is tried up to [REQUEST_ATTEMPTS] times, three
     * seconds apart, until the server answers `SUCCESS` or `FAIL` on its first line. With a [replay],
     * it's sent as a multipart form along with the fields.
     *
     * @param client The client, another one only in tests.
     * @return The lines of the last response, a single empty one if none came.
     */
    internal suspend fun post(url: String, fields: List<Pair<String, String>>, replay: Path? = null, client: HttpClient = httpClient): List<String> {
        val signature = SecurityUtils.signRequest(fields.joinToString("_") { urlEncode(it.second) })
        val signedFields = if (signature != null) fields + ("sign" to signature) else fields

        var lines = emptyList<String>()

        for (attempt in 1..REQUEST_ATTEMPTS) {
            lines = try {
                val response = if (replay == null) {
                    client.submitForm(url, Parameters.build { signedFields.forEach { (name, value) -> append(name, value) } })
                } else {
                    client.submitFormWithBinaryData(url, formData {
                        signedFields.forEach { (name, value) -> append(name, value) }
                        append("replayFile", Platform.fileSystem.read(replay) { readByteArray() }, Headers.build {
                            append(HttpHeaders.ContentType, "application/octet-stream")
                            append(HttpHeaders.ContentDisposition, "filename=\"${replay.name}\"")
                        })
                    })
                }
                readLines(response.bodyAsText())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Debug.e("request $url failed: " + e.message, e)
                emptyList()
            }

            Debug.i("request url=$url")
            lines.forEachIndexed { i, line -> Debug.i("request [$i]: $line") }

            if (lines.firstOrNull() == "SUCCESS" || lines.firstOrNull() == "FAIL") {
                break
            }
            if (attempt < REQUEST_ATTEMPTS) {
                delay(3.seconds)
            }
        }

        return lines.ifEmpty { listOf("") }
    }

    private suspend fun sendRequest(url: String, fields: List<Pair<String, String>>, replay: Path? = null): List<String>? {
        val response = post(url, fields, replay)
        failMessage = ""

        //TODO debug code
        /*Debug.i("Received " + response.size() + " lines");
        for(String str: response)
        {
            Debug.i(str);
        }*/

        if (response.size == 0 || response[0].length == 0) {
            failMessage = "Got empty response"
            Debug.i("Received empty response!")
            return null
        }

        if (response[0] != "SUCCESS") {
            Debug.i("sendRequest response code:  " + response[0])
            if (response.size >= 2) {
                failMessage = response[1]
            } else
                failMessage = "Unknown server error"
            Debug.i("Received fail: $failMessage")
            return null
        }


        return response
    }

    @Throws(OnlineManagerException::class)
    suspend fun logIn(): Boolean {
        return logIn(username, password)
    }

    @Throws(OnlineManagerException::class)
    suspend fun logIn(username: String?): Boolean {
        return logIn(username, password)
    }

    @Throws(OnlineManagerException::class)
    suspend fun logIn(username: String?, password: String?): Boolean {
        return loginMutex.withLock {
            this.username = username ?: ""
            this.password = password ?: ""

            if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
                failMessage = "Username or password is empty"
                return false
            }

            val response = sendRequest(endpoint + "login.php", listOf(
                "username" to username,
                "password" to MD5Calculator.getStringMD5(password.trim() + "taikotaiko"),
                "version" to onlineVersion,
            ))

            if (response == null) {
                return false
            }
            if (response.size < 2) {
                failMessage = "Invalid server response"
                return false
            }

            val params = response[1].split(Regex("\\s+"))
            if (params.size < 7) {
                failMessage = "Invalid server response"
                return false
            }
            userId = params[0].toLong()
            ssid = params[1]
            rank = params[2].toInt().toLong()
            score = params[3].toLong()
            pp = params[4].toFloat()
            accuracy = params[5].toFloat()
            this.username = params[6]
            if (params.size >= 8) {
                avatarURL = params[7]
            } else {
                avatarURL = ""
            }
            profileBannerURL = getProfileBannerURL(userId)

            OsuPlatform.implementation.logAnalyticsEvent("login", mapOf("method" to "ingame"))

            return true
        }
    }

    @Throws(OnlineManagerException::class)
    internal suspend fun tryToLogIn(): Boolean {
        if (logIn(username, password) == false) {
            isStayOnline = false
            return false
        }
        return true
    }

    @Throws(OnlineManagerException::class)
    suspend fun sendRecord(beatmap: BeatmapInfo, scoreData: String, replayPath: String): Boolean {
        Debug.i("Sending record...")

        val replayFile = replayPath.toPath()
        if (!Platform.fileSystem.exists(replayFile)) {
            failMessage = "Replay file not found"
            Debug.e("Replay file not found")
            return false
        }

        val response = sendRequest(endpoint + "submit.php", listOf(
            "userID" to userId.toString(),
            "ssid" to ssid,
            "filename" to beatmap.fullBeatmapName.trim(),
            "hash" to beatmap.md5,
            "data" to scoreData,
            "version" to onlineVersion,
            "cs" to beatmap.circleSize.toString(),
            "ar" to beatmap.approachRate.toString(),
            "od" to beatmap.overallDifficulty.toString(),
            "hp" to beatmap.hpDrainRate.toString(),
            "replayFileChecksum" to sha256Checksum(replayPath),
        ), replay = replayFile)

        if (response == null) {
            return false
        }

        if (failMessage == "Invalid record data")
            return false

        if (response.size < 2) {
            failMessage = "Invalid server response"
            return false
        }

        val resp = response[1].split(Regex("\\s+"))
        if (resp.size < 4) {
            failMessage = "Invalid server response"
            return false
        }

        rank = resp[0].toInt().toLong()
        score = resp[1].toLong()
        accuracy = resp[2].toFloat()
        mapRank = resp[3].toInt()
        pp = resp[4].toFloat()

        return true
    }

    // How the leaderboards are ranked, as the server takes it.
    private fun scoringType() =
        if (OsuSettings.beatmapLeaderboardScoringMode == BeatmapLeaderboardScoringMode.PP) "pp" else "score"

    /**
     * The ranked status of a beatmap on osu.direct, as it answers it.
     */
    @Serializable
    private class BeatmapStatusResponse(val ranked: Int = 0)

    suspend fun getBeatmapStatus(md5: String): RankedStatus? {
        try {
            val response = httpClient.get("https://osu.direct/api/v2/md5/$md5").body<BeatmapStatusResponse>()
            return RankedStatus.valueOf(response.ranked)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Debug.e("getBeatmapStatus: " + e.message, e)
        }

        return null
    }

    suspend fun loadAvatarToTextureManager(): Boolean {
        return loadAvatarToTextureManager(avatarURL)
    }

    suspend fun loadProfileBannerToTextureManager(): Boolean {
        return loadProfileBannerToTextureManager(profileBannerURL)
    }

    // The textures are only loaded from files that are valid images, which replaces checking their
    // size with Android's BitmapFactory.
    suspend fun loadAvatarToTextureManager(avatarURL: String?): Boolean {
        if (avatarURL == null || avatarURL.isEmpty()) return false

        val filename = MD5Calculator.getStringMD5(avatarURL)
        Debug.i("Loading avatar from $avatarURL")
        Debug.i("filename = $filename")
        val picfile = GameFolders.cachePath.toPath() / filename
        OnlineFileOperator.downloadFile(avatarURL, picfile.toString(), true)

        if (Platform.fileSystem.exists(picfile)) {
            // Avatar has been cached locally
            if (CachedTextures[avatarURL] != null) {
                return true
            }
        } else {
            // Avatar not found, download the default avatar
            val defaultAvatarFilename = MD5Calculator.getStringMD5(defaultAvatarURL)
            val avatarFile = GameFolders.cachePath.toPath() / defaultAvatarFilename
            OnlineFileOperator.downloadFile(defaultAvatarURL, avatarFile.toString())

            if (Platform.fileSystem.exists(avatarFile)) {
                //Avatar has been cached locally
                if (CachedTextures[defaultAvatarURL] != null) {
                    return true
                }
            }
        }

        Debug.i("Success!")
        return false
    }

    suspend fun loadProfileBannerToTextureManager(bannerURL: String?): Boolean {
        if (bannerURL == null || bannerURL.isEmpty()) return false

        if (CachedTextures[bannerURL] != null) {
            return true
        }

        val filename = MD5Calculator.getStringMD5(bannerURL)
        Debug.i("Loading profile banner from $bannerURL")
        val bannerFile = GameFolders.cachePath.toPath() / filename
        OnlineFileOperator.downloadFile(bannerURL, bannerFile.toString(), true)

        if (!Platform.fileSystem.exists(bannerFile)) {
            return false
        }

        return CachedTextures[bannerURL] != null
    }

    @Throws(OnlineManagerException::class)
    suspend fun getScorePack(playid: Int): String {
        val response = sendRequest(endpoint + "gettop.php", listOf(
            "playID" to playid.toString(),
            "type" to scoringType(),
        ))

        if (response == null || response.size < 2) {
            return ""
        }

        return response[1]
    }

    class OnlineManagerException : Exception {
        constructor(message: String, cause: Throwable) : super(message, cause)

        constructor(message: String) : super(message)
    }

    companion object {
        private const val REQUEST_ATTEMPTS = 3

        private const val HEX = "0123456789ABCDEF"

        /**
         * Encodes [value] as `java.net.URLEncoder` does with UTF-8: letters, digits and `.-*_` are kept,
         * a space becomes `+`, and everything else is percent-encoded. The values are signed encoded so.
         */
        @JvmStatic
        fun urlEncode(value: String) = buildString {
            for (byte in value.encodeToByteArray()) {
                val c = byte.toInt() and 0xFF
                when {
                    c in 'a'.code..'z'.code || c in 'A'.code..'Z'.code || c in '0'.code..'9'.code
                        || c == '.'.code || c == '-'.code || c == '*'.code || c == '_'.code -> append(c.toChar())
                    c == ' '.code -> append('+')
                    else -> {
                        append('%')
                        append(HEX[c shr 4])
                        append(HEX[c and 0x0F])
                    }
                }
            }
        }

        /**
         * Splits [text] in lines as `BufferedReader.readLine` does.
         */
        private fun readLines(text: String): List<String> {
            if (text.isEmpty()) {
                return emptyList()
            }
            val lines = text.lines()
            return if (text.endsWith('\n') || text.endsWith('\r')) lines.dropLast(1) else lines
        }

        const val hostname = "osudroid.moe"
        const val endpoint = "https://$hostname/api/"
        const val updateEndpoint = endpoint + "update.php?lang="
        @JvmField
        val defaultAvatarURL = getAvatarURL(0)
        const val profileBannerEndpoint = "https://$hostname/user/banner/"
        const val profileEndpoint = "https://$hostname/game/profile.php?uid="

        /**
         * The user agent the profile pages are opened with, which shows them in the game's layout.
         */
        const val profileUserAgent = "osudroid"
        private const val onlineVersion = "65"

        private var instance: OnlineManager? = null

        @JvmStatic
        fun getInstance(): OnlineManager {
            if (instance == null) {
                instance = OnlineManager()
            }
            return instance!!
        }

        @JvmStatic
        fun getReplayURL(playID: Int): String {
            return when (OsuSettings.beatmapLeaderboardScoringMode) {
                BeatmapLeaderboardScoringMode.SCORE -> endpoint + "upload/" + playID + ".odr"
                BeatmapLeaderboardScoringMode.PP -> endpoint + "bestpp/" + playID + ".odr"
            }
        }

        @JvmStatic
        fun getAvatarURL(userId: Long): String {
            return "https://$hostname/user/avatar/$userId.png"
        }

        @JvmStatic
        fun getProfileBannerURL(userId: Long): String {
            return profileBannerEndpoint + userId + ".png"
        }
    }

}
