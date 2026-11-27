package com.osudroid.ui.v2

import com.osudroid.utils.ModHashMap
import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.Logger
import com.osudroid.GameMode
import com.osudroid.beatmaps.BeatmapCache
import com.osudroid.data.*
import com.osudroid.multiplayer.api.data.*
import com.osudroid.ui.v2.modmenu.*
import com.osudroid.utils.async
import com.osudroid.utils.updateThread
import com.reco1l.verktex.data.ClipMode
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.px
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.Badge
import com.reco1l.verktex.ui.LabeledBadge
import com.reco1l.verktex.ui.badge
import com.reco1l.verktex.ui.container.LinearContainer
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.labeledBadge
import com.reco1l.verktex.ui.text.Text
import com.reco1l.verktex.ui.text.faSolidIcon
import com.reco1l.verktex.ui.text.text
import com.reco1l.framework.math.roundBy
import com.osudroid.utils.applyMods
import com.osudroid.utils.calculateRate
import kotlinx.coroutines.Job
import ru.nsu.ccfit.zuev.osu.*
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import com.reco1l.verktex.ui.themed
import com.osudroid.utils.formatDuration

class BeatmapInfoLayout(
    /**
     * Returns the mods to show the beatmap with.
     */
    private val mods: () -> ModHashMap
) : LinearContainer() {


    private lateinit var titleText: Text
    private lateinit var artistText: Text
    private lateinit var creatorText: Text

    private lateinit var lengthBadge: Badge
    private lateinit var bpmBadge: Badge

    private lateinit var arText: LabeledBadge
    private lateinit var csText: LabeledBadge
    private lateinit var odText: LabeledBadge
    private lateinit var hpText: LabeledBadge

    private lateinit var circlesBadge: LabeledBadge
    private lateinit var slidersBadge: LabeledBadge
    private lateinit var spinnersBadge: LabeledBadge

    private lateinit var starRatingBadge: StarRatingBadge
    private lateinit var versionText: Text

    private var calculationJob: Job? = null
    private var displayedBeatmap: BeatmapInfo? = null


    init {
        width = Dimension.FillAvailable
        orientation = Orientation.Vertical
        spacing = 6f.px

        linearContainer {
            width = Dimension.FillAvailable

            linearContainer {
                width = Dimension.FillAvailable
                orientation = Orientation.Vertical
                padding = Padding(right = 12f.px)

                artistText = text {
                    text = "Unknown"
                    fontSize = themed { typography.smallBody.size }
                    color = themed { palette.contentPrimary }
                    clipping = ClipMode.Bounds
                }

                titleText = text {
                    width = Dimension.FillAvailable
                    text = "No selected beatmap"
                    fontSize = themed { typography.smallBody.size }
                    color = themed { palette.accent }
                    clipping = ClipMode.Bounds
                }

                versionText = text {
                    text = "Unknown"
                    fontSize = themed { typography.smallBody.size }
                    color = themed { palette.contentSecondary }
                    clipping = ClipMode.Bounds
                }

                creatorText = text {
                    text = "Unknown"
                    fontSize = themed { typography.smallBody.size }
                    color = themed { palette.contentSecondary }
                }
            }

            starRatingBadge = StarRatingBadge()
            +starRatingBadge
        }

        linearContainer {
            orientation = Orientation.Vertical
            spacing = 4f.px

            linearContainer {
                orientation = Orientation.Horizontal
                spacing = 4f.px

                circlesBadge = labeledBadge {
                    label = "Circles"
                    value = "0"
                    density = Badge.Density.Small
                }
                slidersBadge = labeledBadge {
                    label = "Sliders"
                    value = "0"
                    density = Badge.Density.Small
                }
                spinnersBadge = labeledBadge {
                    label = "Spinners"
                    value = "0"
                    density = Badge.Density.Small
                }
                lengthBadge = badge {
                    text = "-:--"
                    leadingIcon = faSolidIcon(FAIcon.Clock)
                    density = Badge.Density.Small
                }
            }

            linearContainer {
                orientation = Orientation.Horizontal
                spacing = 4f.px

                arText = labeledBadge {
                    label = "AR"
                    value = "0.00"
                    density = Badge.Density.Small
                }
                odText = labeledBadge {
                    label = "OD"
                    value = "0.00"
                    density = Badge.Density.Small
                }
                csText = labeledBadge {
                    label = "CS"
                    value = "0.00"
                    density = Badge.Density.Small
                }
                hpText = labeledBadge {
                    label = "HP"
                    value = "0.00"
                    density = Badge.Density.Small
                }
                bpmBadge = badge {
                    leadingIcon = faSolidIcon(FAIcon.Music)
                    text = "0"
                    density = Badge.Density.Small
                }
            }
        }
    }


    fun setBeatmapInfo(beatmapInfo: BeatmapInfo?) {
        titleText.text = beatmapInfo?.titleText ?: "No selected beatmap"
        artistText.text = beatmapInfo?.artistText ?: "Unknown"
        creatorText.text = "Mapped by ${beatmapInfo?.creator ?: "Unknown"}"
        versionText.text = beatmapInfo?.version ?: "Unknown"
        setDifficultyStatistics(beatmapInfo)
    }

    fun setBeatmapInfo(roomBeatmap: RoomBeatmap?) {
        titleText.text = roomBeatmap?.title ?: "No selected beatmap"
        artistText.text = roomBeatmap?.artist ?: "Unknown"
        creatorText.text = "Mapped by ${roomBeatmap?.creator ?: "Unknown"}"
        versionText.text = roomBeatmap?.version ?: "Unknown"
        setDifficultyStatistics(null)
    }

    /**
     * Change the displayed difficulty statistics.
     */
    fun setDifficultyStatistics(beatmapInfo: BeatmapInfo?) {
        displayedBeatmap = beatmapInfo
        calculationJob?.cancel()
        calculationJob = null

        if (beatmapInfo == null) {
            circlesBadge.value = "0"
            slidersBadge.value = "0"
            spinnersBadge.value = "0"
            arText.value = "0.00"
            odText.value = "0.00"
            csText.value = "0.00"
            hpText.value = "0.00"
            starRatingBadge.rating = 0.0
            bpmBadge.text = "0"
            lengthBadge.text = "00:00"
            return
        }

        updateDisplay(beatmapInfo)

        if (beatmapInfo.needsDifficultyCalculation) {
            calculationJob = async {
                val beatmap = try {
                    BeatmapCache.getBeatmap(
                        beatmapInfo,
                        true,
                        OsuSettings.difficultyAlgorithm.toGameMode(),
                        this
                    )
                } catch (e: Exception) {
                    if (e is CancellationException) {
                        throw e
                    }

                    Logger.e("BeatmapInfoLayout", "Failed to calculate difficulty for ${beatmapInfo.filename}", e)
                    null
                }

                ensureActive()

                if (beatmap != null) {
                    val newInfo = BeatmapInfo(beatmap, beatmapInfo.dateImported, true, this)
                    beatmapInfo.apply(newInfo)
                    DatabaseManager.beatmapInfoTable.update(newInfo)

                    ensureActive()

                    updateThread {
                        if (displayedBeatmap === beatmapInfo) {
                            updateDisplay(beatmapInfo)
                        }
                    }
                }
            }
        }
    }

    fun cancelCalculation() {
        calculationJob?.cancel()
        calculationJob = null
    }

    private fun updateDisplay(beatmapInfo: BeatmapInfo) {
        circlesBadge.value = beatmapInfo.hitCircleCount.toString()
        slidersBadge.value = beatmapInfo.sliderCount.toString()
        spinnersBadge.value = beatmapInfo.spinnerCount.toString()

        val mods = mods()
        val totalSpeedMultiplier = mods.values.calculateRate(Double.POSITIVE_INFINITY)

        val difficulty = beatmapInfo.getBeatmapDifficulty()
        difficulty.applyMods(GameMode.Droid, mods.values, true)

        val minBpm = (beatmapInfo.bpmMin * totalSpeedMultiplier).roundToInt()
        val maxBpm = (beatmapInfo.bpmMax * totalSpeedMultiplier).roundToInt()
        val commonBpm = (beatmapInfo.mostCommonBPM * totalSpeedMultiplier).roundToInt()
        val length = (beatmapInfo.length / totalSpeedMultiplier).toLong()

        lengthBadge.text = formatDuration(length)

        bpmBadge.text = if (minBpm == maxBpm) commonBpm.toString() else "$minBpm-$maxBpm ($commonBpm)"

        // Round to 2 decimal places.
        arText.value = difficulty.ar.roundBy(2).toString()
        odText.value = difficulty.od.roundBy(2).toString()
        csText.value = difficulty.difficultyCS.roundBy(2).toString()
        hpText.value = difficulty.hp.roundBy(2).toString()
        starRatingBadge.rating = beatmapInfo.getStarRating().toDouble()
    }

}
