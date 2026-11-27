package com.osudroid.ui.v2.results

import com.reco1l.verktex.data.extendIntoSafeArea
import com.osudroid.data.BeatmapInfo
import com.osudroid.mods.ModCustomSpeed
import com.osudroid.mods.ModDifficultyAdjust
import com.osudroid.mods.ModFlashlight
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.resources.Res.string
import com.osudroid.scoring.Statistic
import com.osudroid.ui.v2.BeatmapBackground
import com.osudroid.ui.v2.ModsIndicator
import com.osudroid.ui.v2.SkinSounds
import com.osudroid.ui.v2.StarRatingBadge
import com.osudroid.ui.v2.skin.DefaultJudgementPiece
import com.osudroid.utils.async
import com.osudroid.utils.formatDateTime
import com.osudroid.utils.formatNumber
import com.osudroid.utils.updateThread
import com.reco1l.verktex.BuiltIn
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Platform
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.data.Background
import com.reco1l.verktex.data.Border
import com.reco1l.verktex.data.ClipMode
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.CornerRadius
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.data.px
import com.reco1l.verktex.fonts.Font
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.KeyboardEvent
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.Badge
import com.reco1l.verktex.ui.Component
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.Scene
import com.reco1l.verktex.ui.animatableFloat
import com.reco1l.verktex.ui.badge
import com.reco1l.verktex.ui.clearTransforms
import com.reco1l.verktex.ui.container.Container
import com.reco1l.verktex.ui.container.LinearContainer
import com.reco1l.verktex.ui.container.LinearContainer.Orientation
import com.reco1l.verktex.ui.container.container
import com.reco1l.verktex.ui.container.gridContainer
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.container.scrollableContainer
import com.reco1l.verktex.ui.iconButton
import com.reco1l.verktex.ui.loadingIndicator
import com.reco1l.verktex.ui.shape.box
import com.reco1l.verktex.ui.text.Text
import com.reco1l.verktex.ui.text.faSolidIcon
import com.reco1l.verktex.ui.text.text
import com.reco1l.verktex.ui.textButton
import com.reco1l.verktex.ui.themed
import com.reco1l.verktex.ui.transformSequence
import com.reco1l.verktex.localization.formatString
import com.rian.framework.RollingLongCounter
import kotlin.math.abs
import kotlin.math.max

/**
 * The results of a score: its rank and score beside a scrolling column of everything else about it,
 * see [Results], which shows it.
 */
class ResultsScene(
    val results: Results,
    private val stat: Statistic,
    private val beatmapInfo: BeatmapInfo,
    private val action: Action,
    /**
     * Whether the score was just played, which animates its entrance.
     */
    private val isFreshPlay: Boolean,
    /**
     * Whether its difficulty, performance and timing are shown.
     */
    showStatistics: Boolean,
    private val replayPath: String?,
    private val mapMD5: String?,
    private val beatmapToReplay: BeatmapInfo?,
) : Scene() {

    /**
     * What the results offer to do besides going back.
     */
    enum class Action { None, Retry, WatchReplay }


    private val scoreCounter = RollingLongCounter(0L).apply {
        rollingDuration = 1.2f
        rollingEasing = Easing.OutQuint
    }

    private lateinit var rank: RankText
    private lateinit var scoreText: Text
    private lateinit var sections: LinearContainer
    private lateinit var submissionSlot: LinearContainer
    private lateinit var leaderboardSlot: Container
    private lateinit var topBar: Container
    private lateinit var body: LinearContainer

    private var performanceSection: ResultsSection? = null

    private var hasEntered = false


    init {
        // On the scene, so it also covers the safe area its padding keeps the content out of.
        background = Background.Color(themed { palette.inset.alpha(0.75f) })

        linearContainer {
            width = fill()
            height = fill()
            orientation = Orientation.Vertical

            topBar = topBar()

            body = linearContainer {
                width = fill()
                height = fill()
                spacing = 16f.dip
                padding = Padding(
                    left = 16f.dip,
                    top = 12f.dip,
                    right = 16f.dip,
                    // Above the room's chat button.
                    bottom = (12f.dip.toPixels() + if (Multiplayer.isConnected) Multiplayer.roomScene?.chat?.buttonHeight ?: 0f else 0f).px
                )

                hero()
                details(showStatistics)

                leaderboardSlot = container {
                    height = fill()
                }
            }
        }

        if (isFreshPlay) {
            // Shown by the entrance, see onEnter.
            rank.alpha = 0f
            rank.scale = 2.2f
            rank.effectsAlpha = 0f

            sections.forEach {
                it.alpha = 0f
                it.translationY = 24f.dip
            }
        } else {
            scoreCounter.setValueWithoutRolling(stat.totalScoreWithMultiplier)
        }

        scoreText.text = formatScore(scoreCounter.currentValue)
    }


    //region Layout

    private fun Container.topBar() = container {
        width = fill()
        background = Background.Color(themed { palette.inset.alpha(0.5f) })
        border = Border(1f.dip, colorBottom = Color4.White.alpha(0.15f))
        padding = Padding(12f.dip).extendIntoSafeArea()

        linearContainer {
            width = fill()
            spacing = 12f.dip

            iconButton {
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                icon = FAIcon.ArrowLeft
                onActionUp = {
                    SkinSounds["menuback"].play()
                    results.back()
                }
            }

            linearContainer {
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                orientation = Orientation.Vertical
                width = fill()

                text {
                    width = fill()
                    text = "${beatmapInfo.artistText} - ${beatmapInfo.titleText}"
                    fontSettings = themed { typography.h3 }
                    wrapText = false
                    autoScrollAxes = Axis.X
                }

                text {
                    width = fill()
                    text = "[${beatmapInfo.version}]"
                    fontSettings = themed { typography.caption }
                    color = themed { palette.accent.alpha(0.7f) }
                    wrapText = false
                    autoScrollAxes = Axis.X
                }
            }

            when (action) {
                Action.Retry -> textButton {
                    anchor = Anchor.CenterRight
                    origin = Anchor.CenterRight
                    leadingIcon = faSolidIcon(FAIcon.RotateRight)
                    text = string.results_retry
                    onActionUp = {
                        SkinSounds["menuback"].play()
                        results.retry()
                    }
                }

                Action.WatchReplay -> textButton {
                    anchor = Anchor.CenterRight
                    origin = Anchor.CenterRight
                    leadingIcon = faSolidIcon(FAIcon.Play)
                    text = string.results_watch_replay
                    onActionUp = {
                        SkinSounds["menuback"].play()
                        results.watchReplay()
                    }
                }

                Action.None -> Unit
            }
        }
    }

    /**
     * The rank, the score and the mods, which don't scroll.
     */
    private fun Container.hero() = container {
        // Narrower beside the scores of a multiplayer match.
        width = if (Multiplayer.isMultiplayer) 260f.dip else 320f.dip
        height = fill()

        linearContainer {
            anchor = Anchor.Center
            origin = Anchor.Center
            orientation = Orientation.Vertical

            rank = RankText(stat.mark, 112f.dip).apply {
                anchor = Anchor.TopCenter
                origin = Anchor.TopCenter
                scaleOrigin = Anchor.Center
            }
            +rank

            if (stat.isPerfect) {
                badge {
                    anchor = Anchor.TopCenter
                    origin = Anchor.TopCenter
                    variant = Badge.Variant.Solid
                    text = string.results_perfect
                    leadingIcon = faSolidIcon(FAIcon.Award)
                    background = Background.Color(Color4("#FFCC22"))
                    color = Color4("#2B1A00")
                }
            }

            scoreText = text {
                anchor = Anchor.TopCenter
                origin = Anchor.TopCenter
                fontFamily = BuiltIn.Fonts.Fredoka
                fontWeight = Font.Weight.Bold
                fontSize = 40f.dip
                color = Color4.White
                wrapText = false
            }

            +ModsIndicator().apply {
                anchor = Anchor.TopCenter
                origin = Anchor.TopCenter
                useSkinTextures = false
                iconSize = 30f.dip
                spacing = 4f.dip
                mods = stat.mod.values
            }
        }
    }

    /**
     * Everything else, in a column of sections that scrolls.
     */
    private fun Container.details(showStatistics: Boolean) = scrollableContainer {
        scrollAxes = Axis.Y
        width = fill()
        height = fill()
        clipping = ClipMode.Bounds

        sections = linearContainer {
            orientation = Orientation.Vertical
            width = fill()
            spacing = 12f.dip

            submissionSlot = linearContainer {
                orientation = Orientation.Vertical
                width = fill()
            }

            summary()
            judgements()

            if (showStatistics) {
                performance()

                if (stat.unstableRate > 0) {
                    timing()
                }
            }

            beatmap()
            play()
        }
    }

    private fun Container.summary() = linearContainer {
        width = fill()
        spacing = 8f.dip

        // On the surface of the sections, as they're the first thing read.
        resultsTile(string.results_accuracy, formatString("%.2f%%", stat.accuracy * 100f)) { onSurface() }
        resultsTile(string.results_max_combo, "${formatNumber(stat.scoreMaxCombo)}x") { onSurface() }
    }

    private fun Container.judgements() = resultsSection(string.results_judgements, FAIcon.Bullseye) {
        val notes = max(stat.notesHit, 1).toFloat()

        gridContainer {
            width = fill()
            columns = 3
            columnSpacing = 8f.dip
            rowSpacing = 8f.dip

            +JudgementTile(string.results_great.toString(), stat.hit300, stat.hit300 / notes, DefaultJudgementPiece.GREAT)
            +JudgementTile(string.results_ok.toString(), stat.hit100, stat.hit100 / notes, DefaultJudgementPiece.OK)
            +JudgementTile(string.results_meh.toString(), stat.hit50, stat.hit50 / notes, DefaultJudgementPiece.MEH)
            +JudgementTile(string.results_geki.toString(), stat.hit300k, stat.hit300k / notes, GEKI)
            +JudgementTile(string.results_katu.toString(), stat.hit100k, stat.hit100k / notes, KATU)
            +JudgementTile(string.results_miss.toString(), stat.misses, stat.misses / notes, DefaultJudgementPiece.MISS)
        }
    }

    /**
     * The star rating and performance, calculated in the background, see [onEnter].
     */
    private fun Container.performance() {
        performanceSection = resultsSection(string.results_performance, FAIcon.ChartLine) {
            loadingIndicator {
                width = 22f.dip
                height = 22f.dip
            }
        }
    }

    private fun showPerformance(performance: ResultsPerformance?) {
        val section = performanceSection ?: return

        if (performance == null) {
            section.isVisible = false
            return
        }

        val unit = if (performance.isDroid) "dpp" else "pp"

        // The title stays.
        while (section.childCount > 1) {
            section[section.childCount - 1].removeSelf()
        }

        section.linearContainer {
            width = fill()
            spacing = 8f.dip

            resultsTile(string.results_star_rating, "") {
                valueText.isVisible = false
                +StarRatingBadge().apply {
                    rating = performance.starRating
                }
            }

            resultsTile(
                string.results_performance,
                string.results_pp_of_max.format(formatString("%.2f$unit", performance.pp), formatString("%.2f", performance.maxPp))
            )
        }

        section.progressBar((performance.pp / max(performance.maxPp, 0.01)).toFloat(), Color4("#FFCC22"))
    }

    private fun Container.timing() = resultsSection(string.results_timing, FAIcon.Stopwatch) {
        linearContainer {
            width = fill()
            spacing = 8f.dip

            resultsTile(string.results_early, formatString("%.2fms", stat.negativeHitError))
            resultsTile(string.results_late, formatString("+%.2fms", stat.positiveHitError))
            resultsTile(string.results_unstable_rate, formatString("%.2f", stat.unstableRate))
        }

        // The average early and late errors on both sides of a perfect hit.
        val range = max(max(abs(stat.negativeHitError), abs(stat.positiveHitError)), 1.0) * 1.25

        container {
            width = fill()
            height = 12f.dip
            radius = CornerRadius(6f.dip)
            background = Background.Color(Color4.White.alpha(0.08f))

            box {
                anchor = Anchor.Center
                origin = Anchor.CenterRight
                width = fill((abs(stat.negativeHitError) / range / 2).toFloat())
                height = fill()
                radius = CornerRadius(6f.dip)
                color = DefaultJudgementPiece.GREAT
            }

            box {
                anchor = Anchor.Center
                origin = Anchor.CenterLeft
                width = fill((abs(stat.positiveHitError) / range / 2).toFloat())
                height = fill()
                radius = CornerRadius(6f.dip)
                color = DefaultJudgementPiece.MEH
            }

            box {
                anchor = Anchor.Center
                origin = Anchor.Center
                width = 2f.dip
                height = fill()
                color = Color4.White
            }
        }
    }

    private fun Container.beatmap() = resultsSection(string.results_beatmap, FAIcon.Music) {
        text {
            width = fill()
            text = "${beatmapInfo.artistText} - ${beatmapInfo.titleText} [${beatmapInfo.version}]"
            fontSettings = themed { typography.body }
            color = themed { palette.accent }
        }

        text {
            text = string.results_mapped_by.format(beatmapInfo.creator)
            fontSettings = themed { typography.smallBody }
            color = themed { palette.accent.alpha(0.75f) }
        }

        val adjustments = adjustments()

        if (adjustments.isNotEmpty()) {
            linearContainer {
                spacing = 6f.dip

                text {
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                    text = string.results_adjusted
                    fontSettings = themed { typography.smallCaption }
                    color = themed { palette.accent.alpha(0.65f) }
                }

                for (adjustment in adjustments) {
                    badge {
                        anchor = Anchor.CenterLeft
                        origin = Anchor.CenterLeft
                        density = Badge.Density.Small
                        text = adjustment
                    }
                }
            }
        }
    }

    private fun Container.play() = resultsSection(string.results_play, FAIcon.User) {
        text {
            text = string.results_played_by.format(stat.playerName)
            fontSettings = themed { typography.body }
            color = themed { palette.accent }
        }

        text {
            text = formatDateTime(stat.time)
            leadingIcon = faSolidIcon(FAIcon.CalendarDays)
            iconSpacing = 6f.dip
            fontSettings = themed { typography.smallBody }
            color = themed { palette.accent.alpha(0.75f) }
        }

        text {
            text = string.results_game_version.format(Platform.application.versionName, Platform.application.buildType)
            fontSettings = themed { typography.smallCaption }
            color = themed { palette.accent.alpha(0.55f) }
        }
    }

    /**
     * The settings of the mods changing the beatmap that aren't their defaults: the speed, the
     * difficulty overrides and Flashlight's.
     */
    private fun adjustments(): List<String> {
        val mods = stat.mod
        val list = mutableListOf<String>()

        val customSpeed = mods.ofType(ModCustomSpeed::class)
        if (customSpeed != null && customSpeed.trackRateMultiplier != 1f) {
            list += formatString("%.2fx", customSpeed.trackRateMultiplier)
        }

        mods.ofType(ModDifficultyAdjust::class)?.let { adjust ->
            adjust.ar?.let { list += formatString("AR%.1f", it) }
            adjust.od?.let { list += formatString("OD%.1f", it) }
            adjust.cs?.let { list += formatString("CS%.1f", it) }
            adjust.hp?.let { list += formatString("HP%.1f", it) }
        }

        val flashlight = mods.ofType(ModFlashlight::class)
        if (flashlight != null && !flashlight.usesDefaultSettings) {
            if (flashlight.followDelay != ModFlashlight.DEFAULT_FOLLOW_DELAY) {
                list += formatString("FLD%.2f", flashlight.followDelay)
            }

            if (flashlight.sizeMultiplier != ModFlashlight.DEFAULT_SIZE_MULTIPLIER) {
                list += formatString("FL %.2fx", flashlight.sizeMultiplier)
            }
        }

        return list
    }

    //endregion

    //region Updates from the results

    /**
     * Shows the submission of the score to the server first in the column.
     */
    fun showSubmission(card: ScoreSubmissionCard) {
        submissionSlot.clearChildren()
        submissionSlot += card
    }

    /**
     * Shows the scores of the multiplayer match beside the results.
     */
    fun showLeaderboard(leaderboard: ResultsLeaderboard) {
        leaderboardSlot.clearChildren()
        leaderboardSlot += leaderboard
    }

    //endregion


    override fun onEnter() {
        super.onEnter()

        // Tinted by the scene, it's as bright as in the menu (gameplay dimmed it).
        BeatmapBackground.fadeBrightness(1f)

        // Entrance: the top bar slides down and the body rises, but for a score just played,
        // whose rank and sections have an entrance of their own (see enter()).
        topBar.apply {
            clearTransforms()
            alpha = 0f
            translationY = (-20f).dip
            transformSequence {
                fadeIn(0.4f, Easing.OutCubic)
                moveToY(0f, 0.55f, Easing.OutCubic)
            }
        }
        if (hasEntered || !isFreshPlay) {
            body.apply {
                clearTransforms()
                alpha = 0f
                translationY = 32f.dip
                transformSequence(0.05f) {
                    fadeIn(0.4f, Easing.OutCubic)
                    moveToY(0f, 0.55f, Easing.OutCubic)
                }
            }
        }

        if (hasEntered) {
            return
        }
        hasEntered = true

        if (performanceSection != null) {
            async {
                val performance = try {
                    calculateResultsPerformance(beatmapInfo, stat, replayPath, beatmapToReplay, mapMD5)
                } catch (e: Exception) {
                    Logger.e("ResultsScene", "Failed to calculate the performance", e)
                    null
                }

                updateThread { showPerformance(performance) }
            }
        }

        if (isFreshPlay) {
            enter()
        }
    }

    /**
     * The entrance of a score just played: the rank lands and glows, the score rolls up and the
     * sections rise one after the other.
     */
    private fun enter() {
        rank.transformSequence {
            fadeIn(0.35f)
            scaleTo(1f, 0.6f, Easing.OutQuint)
            delay(0.3f)
            animate(animatableFloat({ rank.effectsAlpha }, { rank.effectsAlpha = it }), 1f, 0.6f, Easing.OutQuint)
        }

        scoreCounter.targetValue = stat.totalScoreWithMultiplier

        var index = 0
        sections.forEach { riseIn(it, 0.2f + index++ * 0.06f) }
    }

    private fun riseIn(component: Component, delay: Float) {
        component.transformSequence(delay) {
            fadeIn(0.4f, Easing.OutQuint)
            moveToY(0f, 0.5f, Easing.OutQuint)
        }
    }

    override fun onUpdate(clock: FrameClock) {
        if (scoreCounter.isRolling) {
            scoreCounter.onUpdate(clock.elapsedFrameTime)
            scoreText.text = formatScore(scoreCounter.currentValue)
        }

        super.onUpdate(clock)
    }

    // Back leaves the results, once no component consumed it.
    override fun onInputEvent(event: InputEvent): Boolean {
        if (super.onInputEvent(event)) {
            return true
        }

        if (event is KeyboardEvent && event.isBackPress) {
            results.back()
            return true
        }
        return false
    }


    private companion object {

        // Lighter tones of the judgements they're the perfect sections of.
        val GEKI = Color4("#B3E6FF")
        val KATU = Color4("#D6EC9A")

        // Padded to eight digits as osu! shows scores.
        fun formatScore(score: Long) = score.toString().padStart(8, '0')
    }
}
