package ru.nsu.ccfit.zuev.osu.online

import kotlin.time.Duration.Companion.seconds
import com.reco1l.verktex.Platform
import com.osudroid.data.BeatmapInfo
import com.osudroid.ui.SendingPanel
import com.osudroid.utils.async
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import com.reco1l.verktex.ui.toast.Toasts
import ru.nsu.ccfit.zuev.osu.helper.Debug
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import kotlin.coroutines.cancellation.CancellationException

class OnlineScoring {
    private val onlineMutex = Any()
    var isAvatarLoaded = false
        private set

    private var loginJob: Job? = null
    private var profileAssetsJob: Job? = null

    fun login() {
        if (!OnlineManager.getInstance().isStayOnline)
            return
        isAvatarLoaded = false

        loginJob?.cancel(CancellationException("Login cancelled"))

        loginJob = async {
            synchronized(onlineMutex) {
                var success = false

                //Trying to send request
                for (i in 0 until 3) {

                    try {
                        ensureActive()
                        success = OnlineManager.getInstance().logIn()
                    } catch (e: OnlineManager.OnlineManagerException) {
                        Debug.e("Login error: " + e.message)
                        try {
                            Thread.sleep(3000)
                        } catch (_: InterruptedException) {
                            break
                        }
                        continue
                    }
                    break
                }
                if (success) {
                    OnlineManager.getInstance().isStayOnline = true
                    loadProfileAssets(true)
                } else {
                    OnlineManager.getInstance().isStayOnline = false

                    if (OnlineManager.getInstance().failMessage == "Cannot connect to server") {
                        showConnectionFailure()
                    }
                }
            }
        }
    }

    fun sendRecord(beatmap: BeatmapInfo, record: StatisticV2, panel: SendingPanel, replayPath: String?) {
        if (!OnlineManager.getInstance().isStayOnline)
            return

        Debug.i("Sending score")

        val recordData = record.compile()

        async {
            var success = false
            synchronized(onlineMutex) {
                var i = 0
                while (i < attemptCount) {
                    if (!record.isScoreValid) {
                        Debug.e("Detected illegal actions.")
                        break
                    }

                    try {
                        success = OnlineManager.getInstance().sendRecord(beatmap, recordData, replayPath!!)
                    } catch (e: OnlineManager.OnlineManagerException) {
                        Debug.e("Login error: " + e.message)
                        success = false
                    }

                    if (OnlineManager.getInstance().failMessage.isNotEmpty()) {
                        Toasts.showLong(OnlineManager.getInstance().failMessage)
                        if (OnlineManager.getInstance().failMessage == "Invalid record data")
                            i = attemptCount
                    } else if (success) {
                        val mgr = OnlineManager.getInstance()
                        panel.show(mgr.mapRank.toLong(), mgr.rank, mgr.score, mgr.accuracy, mgr.pp)
                        break
                    }

                    try {
                        Thread.sleep(5000)
                    } catch (_: InterruptedException) {
                    }
                    i++
                }

                if (!success) {
                    panel.setFail()
                }
            }
        }
    }

    fun loadProfileAssets(both: Boolean) {
        if (!OnlineManager.getInstance().isStayOnline) return
        val avatarUrl = OnlineManager.getInstance().avatarURL
        val profileBannerUrl = OnlineManager.getInstance().profileBannerURL
        if (avatarUrl.isNullOrEmpty() && profileBannerUrl.isNullOrEmpty())
            return

        profileAssetsJob?.cancel(CancellationException("Profile asset loading cancelled"))

        profileAssetsJob = async {
            synchronized(onlineMutex) {
                isAvatarLoaded = !avatarUrl.isNullOrEmpty() && OnlineManager.getInstance().loadAvatarToTextureManager()
                if (!profileBannerUrl.isNullOrEmpty()) {
                    OnlineManager.getInstance().loadProfileBannerToTextureManager()
                }
                ensureActive()
            }
        }
    }

    companion object {
        private const val attemptCount = 5
        private var instance: OnlineScoring? = null

        @JvmStatic
        fun getInstance(): OnlineScoring {
            if (instance == null)
                instance = OnlineScoring()
            return instance!!
        }
    }
}


private const val LOGIN_FAIL_HELP_URL = "https://neroyuki.github.io/osudroid-guide/help/login_fail"

/**
 * Tells the player that the game couldn't connect to the server to log in, with a link to the
 * troubleshooting guide.
 */
private fun showConnectionFailure() {
    Toasts.show(
        "Cannot connect to server. Please check the following article for troubleshooting.",
        duration = 10.seconds,
        action = "Check",
        onAction = { Platform.openUrl(LOGIN_FAIL_HELP_URL) }
    )
}
