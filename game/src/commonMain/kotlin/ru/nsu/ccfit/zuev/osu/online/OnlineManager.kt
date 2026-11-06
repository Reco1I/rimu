package ru.nsu.ccfit.zuev.osu.online

import com.osudroid.online.OsuHttp
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.osudroid.settings.GameFolders
import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.settings.Settings
import com.osudroid.OsuPlatform
import com.osudroid.data.BeatmapInfo
import com.osudroid.utils.JsonException
import com.osudroid.utils.LenientJson
import com.reco1l.verktex.Platform
import com.osudroid.utils.optInt
import com.osudroid.utils.sha256Checksum

import ru.nsu.ccfit.zuev.osu.helper.Debug

import kotlin.jvm.JvmField
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlinx.serialization.json.JsonObject
import okio.IOException
import okio.Path.Companion.toPath

import com.osudroid.ui.v2.CachedTextures
import ru.nsu.ccfit.zuev.osu.RankedStatus
import ru.nsu.ccfit.zuev.osu.helper.MD5Calculator
import ru.nsu.ccfit.zuev.osu.online.PostBuilder.RequestException
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

    @Throws(OnlineManagerException::class)
    private suspend fun sendRequest(post: PostBuilder, url: String): ArrayList<String>? {
        val response: ArrayList<String>
        try {
            response = post.requestWithAttempts(url, 3)
        } catch (e: RequestException) {
            Debug.e(e.message, e)
            failMessage = "Cannot connect to server"
            throw OnlineManagerException("Cannot connect to server", e)
        }
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

            val post: PostBuilder = URLEncodedPostBuilder()
            post.addParam("username", username)
            post.addParam("password", MD5Calculator.getStringMD5(password.trim() + "taikotaiko"))
            post.addParam("version", onlineVersion)

            val response = sendRequest(post, endpoint + "login.php")

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

        val post = FormDataPostBuilder()
        post.addParam("userID", userId.toString())
        post.addParam("ssid", ssid)
        post.addParam("filename", beatmap.fullBeatmapName.trim())
        post.addParam("hash", beatmap.md5)
        post.addParam("data", scoreData)
        post.addParam("version", onlineVersion)

        post.addParam("cs", beatmap.circleSize.toString())
        post.addParam("ar", beatmap.approachRate.toString())
        post.addParam("od", beatmap.overallDifficulty.toString())
        post.addParam("hp", beatmap.hpDrainRate.toString())

        post.addParam("replayFile", replayFile.name, "application/octet-stream", replayPath)
        post.addParam("replayFileChecksum", sha256Checksum(replayPath))

        val response = sendRequest(post, endpoint + "submit.php")

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

    @Throws(OnlineManagerException::class)
    suspend fun getTop(hash: String): ArrayList<String> {
        val post: PostBuilder = URLEncodedPostBuilder()
        post.addParam("hash", hash)
        post.addParam("uid", userId.toString())

        if (OsuSettings.beatmapLeaderboardScoringMode == BeatmapLeaderboardScoringMode.PP) {
            post.addParam("type", "pp")
        } else {
            post.addParam("type", "score")
        }

        val response = sendRequest(post, endpoint + "getrank.php")

        if (response == null) {
            return ArrayList()
        }

        response.removeAt(0)

        return response
    }

    @Throws(OnlineManagerException::class)
    suspend fun getBeatmapStatus(md5: String): RankedStatus? {
        try {
            val json = OsuHttp.getJsonObject("https://osu.direct/api/v2/md5/$md5")
            return RankedStatus.valueOf(json.optInt("ranked"))
        } catch (e: IOException) {
            Debug.e("getBeatmapStatus IOException " + e.message, e)
        } catch (e: JsonException) {
            Debug.e("getBeatmapStatus JSONException " + e.message, e)
        } catch (e: kotlinx.serialization.SerializationException) {
            Debug.e("getBeatmapStatus JSONException " + e.message, e)
        } catch (e: IllegalArgumentException) {
            Debug.e("getBeatmapStatus IllegalArgumentException " + e.message, e)
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
        val post: PostBuilder = URLEncodedPostBuilder()
        post.addParam("playID", playid.toString())

        if (OsuSettings.beatmapLeaderboardScoringMode == BeatmapLeaderboardScoringMode.PP) {
            post.addParam("type", "pp")
        } else {
            post.addParam("type", "score")
        }

        val response = sendRequest(post, endpoint + "gettop.php")

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
