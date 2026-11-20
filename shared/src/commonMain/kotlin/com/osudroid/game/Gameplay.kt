package com.osudroid.game

import com.osudroid.OsuGame
import com.osudroid.beatmaps.DifficultyCalculationManager
import com.osudroid.data.BeatmapInfo
import com.osudroid.game.loading.LoadedPlay
import com.osudroid.game.loading.PlayLoader
import com.osudroid.game.loading.PlayRequest
import com.osudroid.game.session.GameplaySession
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.resources.Res
import com.osudroid.settings.GameFolders
import com.osudroid.ui.v2.GameLoaderScene
import com.osudroid.ui.v2.SkinSounds
import com.osudroid.ui.v2.multi.LobbyScene
import com.osudroid.utils.ModHashMap
import com.osudroid.utils.async
import com.osudroid.utils.stopAsync
import com.osudroid.utils.updateThread
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.input.Input
import com.reco1l.verktex.ui.Scene
import com.reco1l.verktex.ui.toast.Toasts
import kotlin.concurrent.Volatile
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitAll
import ru.nsu.ccfit.zuev.osu.game.GameObjectPool
import ru.nsu.ccfit.zuev.osu.helper.MD5Calculator
import ru.nsu.ccfit.zuev.osu.online.OnlineFileOperator
import ru.nsu.ccfit.zuev.osu.scoring.ScoringScene
import ru.nsu.ccfit.zuev.skins.BeatmapSkinManager

/**
 * A play of one beatmap: its attempts, each a [GameplaySession], and the replays watched from its [scoring] scene.
 * Created by whoever starts the play, with [start].
 */
class Gameplay {

    /**
     * The results of this play.
     */
    val scoring: ScoringScene by lazy { ScoringScene(this) }

    /**
     * The attempt being played, or the last one played. Null before the first is loaded.
     */
    var session: GameplaySession? = null
        private set

    /**
     * Whether the attempt being loaded is ready to start: in multiplayer, once every player loaded it.
     */
    @Volatile
    var isReadyToStart = false

    /**
     * Whether the player failed the last attempt and kept playing, as in multiplayer: its score isn't submitted.
     */
    val hasFailed: Boolean
        get() = session?.hasFailed == true

    // What was asked last, which retrying asks again.
    private var request: PlayRequest? = null

    // The beatmap loaded for the last attempt, which the next reuses.
    private var loaded: LoadedPlay? = null

    private val loadingRequestId = AtomicInt(0)
    private var loadingPipeline: Job? = null
    private var gameLoadingJob: Job? = null
    private var timedDifficultyJob: Job? = null


    /**
     * Starts playing [request], with the loader in front of the game while it loads.
     */
    fun start(request: PlayRequest) {
        start(request.copy(mods = request.mods.deepCopy()), isRestart = false)
    }

    /**
     * Plays the last request again, as it was played: a replay is watched again.
     */
    fun retry() {
        start(request ?: return, isRestart = true)
    }

    /**
     * Watches the replay [replay] of [beatmap], played with [mods].
     */
    fun watchReplay(beatmap: BeatmapInfo, replay: String, mods: ModHashMap) {
        val request = request?.copy(beatmap = beatmap, mods = mods, replay = replay, isHUDEditor = false)
            ?: PlayRequest(beatmap, mods, replay)

        start(request.copy(mods = mods.deepCopy()), isRestart = false)
    }

    private fun start(request: PlayRequest, isRestart: Boolean) {
        isReadyToStart = false
        this.request = request

        val requestId = loadingRequestId.incrementAndFetch()

        Verktex.withTreeLock {
            // Retrying from the pause menu, the attempt paused is replaced rather than kept below the next one.
            if (session != null && Verktex.scene === session?.scene) {
                Verktex.popScene()
            }

            Verktex.pushScene(GameLoaderScene(this, request.beatmap, request.mods, isRestart))
        }

        SkinSounds.get("failsound")!!.stop()

        // A play loading while another starts is stopped first, the loads share the object pool.
        val previousGame = lastStartedGame?.takeIf { it !== this }
        lastStartedGame = this

        val stopping = cancelLoading(false)

        val pipeline = async {
            stopping.await()
            previousGame?.cancelLoading(false)?.await()
            DifficultyCalculationManager.stopCalculation().await()

            if (requestId != loadingRequestId.load()) {
                return@async
            }

            // Not a child of the pipeline (com.osudroid.utils.async, not CoroutineScope.async):
            // the pipeline completes once the loading has started, as before.
            gameLoadingJob = async {
                var succeeded = false
                var cancelled = false

                try {
                    if (requestId != loadingRequestId.load()) {
                        return@async
                    }

                    // Online replays are downloaded first, the session reads a file.
                    val replay = request.replay?.let { replay ->
                        if (replay.startsWith("https://")) downloadReplay(replay) ?: return@async else replay
                    }

                    if (requestId != loadingRequestId.load()) {
                        return@async
                    }

                    // Retrying watches the downloaded replay.
                    val loadingRequest = request.copy(replay = replay)
                    this@Gameplay.request = loadingRequest

                    val previous = loaded
                    val play = PlayLoader.load(loadingRequest, previous, this) { timedDifficultyJob = it } ?: return@async
                    loaded = play

                    // The attempt before, still shown below the loader when retrying from the pause menu.
                    session?.release()

                    val session = GameplaySession(this@Gameplay, play, loadingRequest)
                    this@Gameplay.session = session

                    succeeded = session.prepare(this, isNewBeatmap = previous?.parsedBeatmap !== play.parsedBeatmap)

                    if (succeeded && requestId == loadingRequestId.load()) {
                        session.prepareScene()
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) {
                        cancelled = true
                        throw e
                    }

                    Logger.e(TAG, "Failed to load gameplay", e)
                } finally {
                    if (requestId == loadingRequestId.load()) {
                        if (!succeeded && !cancelled) {
                            updateThread { quit() }
                        }

                        gameLoadingJob = null
                    }
                }
            }
        }

        loadingPipeline = pipeline

        pipeline.invokeOnCompletion {
            if (loadingPipeline === pipeline) {
                loadingPipeline = null
            }
        }
    }

    /**
     * Starts the attempt loaded, once the loader is done.
     *
     * @param beatmapOffset The offset of the beatmap set, in milliseconds, from its options.
     */
    fun begin(beatmapOffset: Int) {
        session?.start(beatmapOffset)
    }

    /**
     * Stops loading this play. The returned [Deferred] completes once every loading job has stopped.
     */
    fun cancelLoading(): Deferred<Unit> {
        return cancelLoading(true)
    }

    private fun cancelLoading(invalidatePendingStart: Boolean): Deferred<Unit> {
        if (invalidatePendingStart) {
            loadingRequestId.incrementAndFetch()
        }

        val gameLoadingJob = this.gameLoadingJob
        val timedDifficultyJob = this.timedDifficultyJob
        val loadingPipeline = this.loadingPipeline

        this.gameLoadingJob = null
        this.timedDifficultyJob = null

        val jobCancellation = listOf(
            gameLoadingJob.stopAsync(),
            session?.takeVideoLoadingJob().stopAsync(),
            timedDifficultyJob.stopAsync()
        )

        val stopped = CompletableDeferred<Unit>()

        async {
            jobCancellation.awaitAll()
            // Whether it completed, failed or was cancelled.
            loadingPipeline?.join()
            stopped.complete(Unit)
        }

        return stopped
    }

    /**
     * Called by the session once its attempt ended and its results are shown: the beatmap is loaded again for the
     * next one.
     */
    internal fun onFinished() {
        loaded = null
    }

    /**
     * Leaves the play, for the scene before it, or the multiplayer room.
     */
    fun quit() {
        if (lastStartedGame === this) {
            lastStartedGame = null
        }

        // Disable historical event processing for more efficient ACTION_MOVE reports.
        Input.dispatchHistoricalMoves = false

        val session = session

        BeatmapSkinManager.setSkinEnabled(false)
        GameObjectPool.getInstance().purge()
        session?.exit()
        BeatmapSkinManager.getInstance().clearSkin()

        loaded = null

        // A replay isn't watched again by retrying once left.
        if (session?.replaying == true) {
            request = request?.copy(replay = null)
        }

        if (Multiplayer.isMultiplayer) {
            session?.releaseVideo()

            OsuGame.music?.stop()

            val roomScene = Multiplayer.roomScene

            if (Multiplayer.isConnected && roomScene != null) {
                returnToPreviousScene()

                if (Verktex.scene !== roomScene) {
                    roomScene.show()
                }
            } else {
                replaceCurrentScene(LobbyScene())
            }

            return
        }

        SkinSounds.get("failsound")!!.stop()
        returnToPreviousScene()
        session?.releaseVideo()

        // Resume difficulty calculation.
        DifficultyCalculationManager.calculateDifficulties()
    }

    /**
     * Downloads the online replay at [url] into the cache, returning the file it was saved to, or `null` if it
     * couldn't be downloaded.
     */
    private suspend fun downloadReplay(url: String): String? {
        val path = GameFolders.cachePath + "/" + MD5Calculator.getStringMD5(url) + ".odr"
        Logger.i(TAG, "ReplayFile = $path")

        if (!OnlineFileOperator.downloadFile(url, path)) {
            Toasts.showLong(Res.string.replay_cantdownload)
            return null
        }

        return path
    }

    companion object {
        private const val TAG = "Gameplay"

        // The play started last, whose loading a new play stops first. Quitting it clears it, otherwise
        // it's kept until the next play starts.
        @Volatile
        private var lastStartedGame: Gameplay? = null

        /**
         * The attempt shown in the current scene, null when gameplay isn't the current scene.
         */
        val current: GameplaySession?
            get() = GameplaySession.current

        internal fun replaceCurrentScene(newScene: Scene) {
            Verktex.withTreeLock {
                Verktex.popScene()
                Verktex.pushScene(newScene)
            }
        }

        internal fun returnToPreviousScene() {
            Verktex.withTreeLock {
                Verktex.popScene()
            }
        }
    }
}
