package com.osudroid.ui.v2.songselect

import com.reco1l.verktex.texture.reference
import com.osudroid.ui.v2.skinTexture
import com.osudroid.BeatmapSelection
import com.reco1l.verktex.data.Background
import com.reco1l.verktex.data.CornerRadius
import com.reco1l.verktex.ui.text.spans
import com.reco1l.verktex.ui.borderWidth
import com.reco1l.verktex.ui.borderColor
import com.osudroid.data.*
import com.osudroid.online.*
import com.osudroid.online.model.*
import com.osudroid.ui.v2.*
import com.osudroid.ui.v2.songselect.LeaderboardType.*
import com.osudroid.utils.updateThread
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.data.ClipMode
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.px
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.Button
import com.reco1l.verktex.ui.Component
import com.reco1l.verktex.ui.LoadingIndicator
import com.reco1l.verktex.ui.Sprite
import com.reco1l.verktex.ui.clearTransforms
import com.reco1l.verktex.ui.container.RecyclableComponent
import com.reco1l.verktex.ui.container.LinearContainer
import com.reco1l.verktex.ui.container.RecyclerContainer
import com.reco1l.verktex.ui.container.container
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.control.TabSelector
import com.reco1l.verktex.ui.fadeIn
import com.reco1l.verktex.ui.fadeOut
import com.reco1l.verktex.ui.scaleTo
import com.reco1l.verktex.ui.text.Text
import com.reco1l.verktex.ui.text.text
import kotlinx.coroutines.*
import ru.nsu.ccfit.zuev.osu.menu.ScoreBoardItem
import ru.nsu.ccfit.zuev.osu.online.*
import kotlin.coroutines.cancellation.CancellationException
import com.reco1l.verktex.ui.themed
import com.reco1l.verktex.localization.formatString
import com.osudroid.utils.formatNumber
import com.osudroid.OsuGame
import com.osudroid.game.Gameplay
import com.osudroid.resources.Res
import com.osudroid.scoring.Statistic
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Platform
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.ui.toast.Toasts
import okio.Path.Companion.toPath

class SongSelectLeaderboard : LinearContainer() {


    /**
     * The current leaderboard type.
     */
    var leaderboardType: LeaderboardType = Local
        set(value) {
            if (field != value) {
                field = value
                scoresContainer.onCreateComponent = { LeaderboardItem(value == Global, ::openScore) }
                onBeatmapSelected(BeatmapSelection.beatmap)
            }
        }

    /**
     * The scores currently shown, in the format used by the gameplay leaderboard.
     */
    val scoreBoardItems: List<ScoreBoardItem>
        get() = scoresContainer.data.mapIndexed { index, score ->
            ScoreBoardItem().apply {
                userName = score.playerName
                playScore = score.score
                scoreId = score.scoreId.toInt()
                rank = index + 1
                maxCombo = score.maxCombo
                accuracy = score.accuracy
            }
        }


    private val scoreLoadingScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val scoresContainer = RecyclerContainer<BeatmapScoreModel, LeaderboardItem>(18)

    private val overlayContainer = LinearContainer()


    private var currentLoadingJob: Job? = null

    // Loading the score pressed, to show it in the results.
    private var openingJob: Job? = null

    // The beatmap and the type of the scores shown.
    private var scoresBeatmap: BeatmapInfo? = null
    private var scoresType: LeaderboardType = Local


    init {
        height = Dimension.FillAvailable
        width = Dimension.FillAvailable
        orientation = Orientation.Vertical
        // The right side leaves space for the shear of the wedge.
        padding = Padding(left = 16f.dip, right = 40f.dip)

        +TabSelector().apply {
            width = Dimension.FillAvailable
            addButton("Local") { leaderboardType = Local }
            addButton("Global") { leaderboardType = Global }
            selectedTab = 0
        }

        container {
            width = Dimension.FillAvailable
            height = Dimension.FillAvailable

            +overlayContainer.apply {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                orientation = Orientation.Vertical
                spacing = 8f.px
                padding = Padding(24f.px)
                scaleOrigin = Anchor.Center
            }

            +scoresContainer.apply {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                scrollAxes = Axis.Y
                clipping = ClipMode.Bounds
                alpha = 0f
                isVisible = false
                scaleOrigin = Anchor.Center
                spacing = 4f.px

                componentWrapper.padding = Padding(bottom = 90f.px)

                // Without avatars because local is chosen by default
                onCreateComponent = { LeaderboardItem(false, ::openScore) }
            }
        }

    }


    private fun switchContainers(adverseContainer: Component, showingContainer: Component) {

        adverseContainer.apply {
            clearTransforms()
            fadeOut(0.2f).after {
                isVisible = false
            }
        }

        showingContainer.apply {
            clearTransforms()
            isVisible = true
            scale = 0.95f
            alpha = 0f

            scaleTo(1f, 0.1f)
            fadeIn(0.1f)
        }
    }


    private fun showMessage(message: String) {
        switchContainers(scoresContainer, overlayContainer)

        overlayContainer.apply {
            clearChildren()

            text {
                anchor = Anchor.TopCenter
                origin = Anchor.TopCenter
                alignment = Anchor.TopCenter
                color = themed { palette.accent }
                spans {
                    size(32f.px) { icon(FAIcon.CircleInfo) }
                    +"\n"
                    +message
                }
            }
        }
    }

    private fun showLoading() {
        switchContainers(scoresContainer, overlayContainer)

        overlayContainer.apply {
            clearChildren()

            +LoadingIndicator().apply {
                anchor = Anchor.TopCenter
                origin = Anchor.TopCenter
                width = 32f.px
                height = 32f.px
            }

            text {
                anchor = Anchor.TopCenter
                origin = Anchor.TopCenter
                color = themed { palette.accent }
                text = "Loading scores..."
            }
        }
    }


    fun onBeatmapSelected(beatmapInfo: BeatmapInfo?) {

        showLoading()
        scoresContainer.data = listOf()

        currentLoadingJob?.cancel(CancellationException())
        openingJob?.cancel(CancellationException())
        openingJob = null

        scoresBeatmap = beatmapInfo
        scoresType = leaderboardType

        if (beatmapInfo == null) {
            return
        }

        currentLoadingJob = scoreLoadingScope.launch {

            var itemModels = listOf<BeatmapScoreModel>()

            try {
                when (leaderboardType) {

                    Local -> {
                        ensureActive()
                        itemModels = DatabaseManager.scoreInfoTable.getBeatmapScores(beatmapInfo.md5).map { scoreInfo ->
                            ensureActive()
                            BeatmapScore(scoreInfo)
                        }
                    }

                    Global -> {
                        if (!OnlineManager.getInstance().isStayOnline) {
                            ensureActive()
                            updateThread { showMessage("You must be online to view global leaderboard.") }
                            return@launch
                        }

                        ensureActive()
                        itemModels = IBanchoAPI.getBeatmapLeaderboard(beatmapInfo.md5)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                ensureActive()
                updateThread { showMessage("Failed to load global leaderboard.") }
                return@launch
            }

            ensureActive()
            updateThread {
                if (itemModels.isNotEmpty()) {
                    scoresContainer.data = itemModels
                    switchContainers(overlayContainer, scoresContainer)
                } else {
                    showMessage("No scores found for this beatmap.")
                }
            }
        }

    }


    /**
     * Shows [score] of the leaderboard in the results, with its replay when it has one.
     */
    private fun openScore(score: BeatmapScoreModel) {
        val beatmap = scoresBeatmap ?: return
        val type = scoresType

        if (openingJob?.isActive == true) {
            return
        }

        openingJob = scoreLoadingScope.launch {
            val stat: Statistic
            val replay: String?

            try {
                when (type) {
                    Local -> {
                        val scoreInfo = DatabaseManager.scoreInfoTable.getScore(score.scoreId.toInt())

                        if (scoreInfo == null) {
                            updateThread { Toasts.show("Could not open score") }
                            return@launch
                        }

                        stat = scoreInfo.toStatistic(beatmap.getBeatmapDifficulty())
                        replay = scoreInfo.replayPath.takeIf {
                            scoreInfo.replayFilename.isNotEmpty() && Platform.fileSystem.exists(it.toPath())
                        }
                    }

                    Global -> {
                        updateThread { Toasts.show(Res.string.online_loadrecord) }

                        val onlineScore = IBanchoAPI.getScoreStatistic(score.scoreId, difficulty = beatmap.getBeatmapDifficulty())

                        if (onlineScore == null) {
                            updateThread { Toasts.show("Could not load score") }
                            return@launch
                        }

                        stat = onlineScore.stat
                        stat.playerName = score.playerName
                        stat.beatmapMD5 = beatmap.md5
                        stat.calculateModScoreMultiplier(beatmap.getBeatmapDifficulty())
                        replay = if (onlineScore.hasReplay) IBanchoAPI.getReplayUrl(score.scoreId) else null
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Logger.e("SongSelectLeaderboard", "Failed to open score", e)
                updateThread { Toasts.show("Could not open score") }
                return@launch
            }

            ensureActive()
            updateThread {
                // Another beatmap was selected meanwhile.
                if (scoresBeatmap !== beatmap) {
                    return@updateThread
                }

                val results = Gameplay().results
                results.load(stat, null, OsuGame.music, replay, null, beatmap)
                Verktex.pushScene(results.scene!!)
            }
        }
    }


    class LeaderboardItem(
        withAvatars: Boolean,
        private val onSelected: (BeatmapScoreModel) -> Unit,
    ) : RecyclableComponent<BeatmapScoreModel>() {

        override val isRecyclable: Boolean
            get() = !button.isPressed


        private val button: Button
        private val nameText = Text()
        private val rankText = Text()
        private val modsIndicator = ModsIndicator()

        private val avatarSprite = if (withAvatars) Sprite() else null

        private lateinit var scoreText: Text
        private lateinit var maxComboText: Text
        private lateinit var accuracyText: Text


        init {
            width = Dimension.FillAvailable

            button = Button().apply {
                width = Dimension.FillAvailable
                height = Dimension.WrapContent
                padding = Padding.Zero
                onActionUp = { boundData?.let(onSelected) }

                linearContainer {
                    width = Dimension.FillAvailable

                    orientation = LinearContainer.Orientation.Horizontal
                    padding = Padding(8f.px)
                    spacing = 8f.px
                    radius = CornerRadius(12f.px)
                    borderWidth = 2f.px

                    background = Background.Color(themed { palette.surface.alpha(0.75f) })
                    borderColor = themed { palette.border }

                    container {
                        width = Dimension.FillAvailable

                        linearContainer {
                            anchor = Anchor.CenterLeft
                            origin = Anchor.CenterLeft
                            orientation = LinearContainer.Orientation.Horizontal
                            spacing = 8f.px

                            +rankText.apply {
                                width = 18f.px
                                anchor = Anchor.CenterLeft
                                origin = Anchor.CenterLeft
                                alignment = Anchor.Center
                                color = themed { palette.accent }
                                text = ""
                            }

                            if (avatarSprite != null) {
                                +avatarSprite.apply {
                                    width = 32f.px
                                    height = 32f.px
                                    anchor = Anchor.CenterLeft
                                    origin = Anchor.CenterLeft
                                    radius = CornerRadius(8f.px)
                                    texture = skinTexture("offline-avatar")
                                }
                            }

                            linearContainer {
                                orientation = LinearContainer.Orientation.Vertical
                                spacing = 2f.px

                                +nameText.apply {
                                    text = "Unknown Player"
                                    color = themed { palette.accent }
                                }

                                +modsIndicator.apply {
                                    iconSize = 16f.px
                                    spacing = (-5f).px
                                }
                            }

                        }

                        linearContainer {
                            orientation = LinearContainer.Orientation.Horizontal
                            spacing = 8f.px
                            anchor = Anchor.CenterRight
                            origin = Anchor.CenterRight

                            fun MetricText(name: String, placeholder: String): Text {

                                val metricText = Text().apply {
                                    text = placeholder
                                    color = themed { palette.accent }
                                }

                                linearContainer {
                                    orientation = LinearContainer.Orientation.Vertical
                                    spacing = (-2f).px

                                    text {
                                        text = name.uppercase()
                                        color = themed { palette.contentSecondary }
                                    }
                                    +metricText
                                }

                                return metricText
                            }

                            scoreText = MetricText("Score", "0,000,000,000")
                            maxComboText = MetricText("Max combo", "00000x")
                            accuracyText = MetricText("Accuracy", "000.00%")
                        }

                    }
                }
            }
            +button
        }


        override fun onBind(data: BeatmapScoreModel) {
            nameText.text = data.playerName
            //modsIndicator.mods = data.mods

            if (avatarSprite != null) {
                val avatarUrl = data.avatarUrl

                avatarSprite.texture = (if (avatarUrl != null) CachedTextures[avatarUrl] else null)?.reference ?: skinTexture("offline-avatar")
            }

            scoreText.text = formatNumber(data.score)
            maxComboText.text = "${data.maxCombo}x"
            accuracyText.text = formatString("%.2f%%", data.accuracy)

            rankText.text = if (data.rank > 0) data.rank.toString() else ""
        }


    }

}

enum class LeaderboardType {
    Global,
    Local
}
