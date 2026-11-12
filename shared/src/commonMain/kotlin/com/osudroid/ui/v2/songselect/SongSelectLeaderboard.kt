package com.osudroid.ui.v2.songselect

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
import com.reco1l.verktex.ui.UIButton
import com.reco1l.verktex.ui.UIComponent
import com.reco1l.verktex.ui.UILoadingIndicator
import com.reco1l.verktex.ui.UISprite
import com.reco1l.verktex.ui.clearTransforms
import com.reco1l.verktex.ui.container.RecyclableComponent
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.UIRecyclerContainer
import com.reco1l.verktex.ui.container.container
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.control.UITabSelector
import com.reco1l.verktex.ui.fadeIn
import com.reco1l.verktex.ui.fadeOut
import com.reco1l.verktex.ui.scaleTo
import com.reco1l.verktex.ui.text.UIText
import com.reco1l.verktex.ui.text.text
import kotlinx.coroutines.*
import ru.nsu.ccfit.zuev.osu.menu.ScoreBoardItem
import ru.nsu.ccfit.zuev.osu.online.*
import kotlin.coroutines.cancellation.CancellationException
import com.reco1l.verktex.ui.themed
import com.reco1l.verktex.localization.formatString
import com.osudroid.utils.formatNumber

class SongSelectLeaderboard : UILinearContainer() {


    /**
     * The current leaderboard type.
     */
    var leaderboardType: LeaderboardType = Local
        set(value) {
            if (field != value) {
                field = value
                scoresContainer.onCreateComponent = { LeaderboardItem(value == Global) }
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

    private val scoresContainer = UIRecyclerContainer<BeatmapScoreModel, LeaderboardItem>(18)

    private val overlayContainer = UILinearContainer()


    private var currentLoadingJob: Job? = null


    init {
        height = Dimension.FillAvailable
        width = Dimension.FillAvailable
        orientation = Orientation.Vertical
        // The right side leaves space for the shear of the wedge.
        padding = Padding(left = 16f.dip, right = 40f.dip)

        +UITabSelector().apply {
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
                onCreateComponent = { LeaderboardItem(false) }
            }
        }

    }


    private fun switchContainers(adverseContainer: UIComponent, showingContainer: UIComponent) {

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

            +UILoadingIndicator().apply {
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


    class LeaderboardItem(withAvatars: Boolean) : RecyclableComponent<BeatmapScoreModel>() {

        override val isRecyclable: Boolean
            get() = !button.isPressed


        private val button: UIButton
        private val nameText = UIText()
        private val rankText = UIText()
        private val modsIndicator = ModsIndicator()

        private val avatarSprite = if (withAvatars) UISprite() else null

        private lateinit var scoreText: UIText
        private lateinit var maxComboText: UIText
        private lateinit var accuracyText: UIText


        init {
            width = Dimension.FillAvailable

            button = UIButton().apply {
                width = Dimension.FillAvailable
                height = Dimension.WrapContent
                padding = Padding.Zero

                linearContainer {
                    width = Dimension.FillAvailable

                    orientation = UILinearContainer.Orientation.Horizontal
                    padding = Padding(8f.px)
                    spacing = 8f.px
                    radius = CornerRadius(12f.px)
                    borderWidth = 2f.px

                    background = Background.Color(themed { (palette.accent.shade(0.1f)).alpha(0.75f) })
                    borderColor = themed { palette.accent.shade(0.2f) }

                    container {
                        width = Dimension.FillAvailable

                        linearContainer {
                            anchor = Anchor.CenterLeft
                            origin = Anchor.CenterLeft
                            orientation = UILinearContainer.Orientation.Horizontal
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
                                    textureRegion = SkinTextures["offline-avatar"]
                                }
                            }

                            linearContainer {
                                orientation = UILinearContainer.Orientation.Vertical
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
                            orientation = UILinearContainer.Orientation.Horizontal
                            spacing = 8f.px
                            anchor = Anchor.CenterRight
                            origin = Anchor.CenterRight

                            fun MetricText(name: String, placeholder: String): UIText {

                                val metricText = UIText().apply {
                                    text = placeholder
                                    color = themed { palette.accent }
                                }

                                linearContainer {
                                    orientation = UILinearContainer.Orientation.Vertical
                                    spacing = (-2f).px

                                    text {
                                        text = name.uppercase()
                                        color = themed { palette.accent.shade(0.8f) }
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

                avatarSprite.textureRegion = (if (avatarUrl != null) CachedTextures[avatarUrl] else null) ?: SkinTextures["offline-avatar"]
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
