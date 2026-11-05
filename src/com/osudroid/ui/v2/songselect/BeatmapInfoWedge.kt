package com.osudroid.ui.v2.songselect

import com.osudroid.GameMode
import com.osudroid.beatmaps.*
import com.osudroid.beatmaps.DroidHitWindow.Companion.hitWindow300ToOverallDifficulty
import com.osudroid.data.*
import com.osudroid.mods.*
import com.osudroid.ui.v2.StarRatingBadge
import com.osudroid.ui.v2.modmenu.*
import com.osudroid.utils.applyMods
import com.osudroid.utils.calculateRate
import com.reco1l.framework.math.roundBy
import com.reco1l.verktex.data.ClipMode
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.px
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.UIBadge
import com.reco1l.verktex.ui.UILabeledBadge
import com.reco1l.verktex.ui.badge
import com.reco1l.verktex.ui.container.UIExtendIntoSafeAreaContainer
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.labeledBadge
import com.reco1l.verktex.ui.shape.UIWedge
import com.reco1l.verktex.ui.text.UIText
import com.reco1l.verktex.ui.text.faSolidIcon
import com.reco1l.verktex.ui.text.text
import java.text.*
import java.util.*
import kotlin.math.roundToInt
import com.reco1l.verktex.ui.themed

class BeatmapInfoWedge : UIExtendIntoSafeAreaContainer() {


    private lateinit var titleText: UIText
    private lateinit var artistText: UIText

    private lateinit var lengthText: UIBadge
    private lateinit var bpmText: UIBadge

    private lateinit var arText: UILabeledBadge
    private lateinit var csText: UILabeledBadge
    private lateinit var odText: UILabeledBadge
    private lateinit var hpText: UILabeledBadge

    private lateinit var circlesBadge: UILabeledBadge
    private lateinit var slidersBadge: UILabeledBadge
    private lateinit var spinnersBadge: UILabeledBadge

    private lateinit var starsText: StarRatingBadge
    private lateinit var versionText: UIText


    init {
        width = Dimension.FillAvailable

        // Reaches the edges of the window, the outline of those sides is left outside of it.
        background = UIWedge().apply {
            translationX = (-2f).px
            translationY = (-2f).px
            alpha = 0.75f

            color = themed { palette.accent.shade(0.1f).alpha(0.75f) }
            radius = 12f.dip
            strokeWidth = 2f.px
            strokeColor = themed { palette.accent.shade(0.2f) }
        }

        linearContainer {
            width = Dimension.FillAvailable
            orientation = UILinearContainer.Orientation.Vertical
            padding = Padding(16f.dip)
            spacing = 6f.dip

            linearContainer {
                orientation = UILinearContainer.Orientation.Horizontal
                spacing = 10f.px
                width = Dimension.FillAvailable

                linearContainer {
                    width = Dimension.FillAvailable
                    orientation = UILinearContainer.Orientation.Vertical

                    artistText = text {
                        text = "Unknown"
                        fontSettings = themed { typography.h3 }
                        fontSize = themed { typography.body.size }
                        color = themed { palette.accent.shade(0.8f) }
                    }

                    titleText = text {
                        width = Dimension.FillAvailable
                        text = "No selected beatmap"
                        fontSettings = themed { typography.h3 }
                        fontSize = themed { typography.body.size }
                        color = themed { palette.accent }
                        clipping = ClipMode.Bounds
                    }

                    versionText = text {
                        text = "Unknown"
                        color = themed { palette.accent.shade(0.8f) }
                    }
                }
                starsText = StarRatingBadge()
                +starsText
            }

            linearContainer {
                orientation = UILinearContainer.Orientation.Horizontal
                spacing = 8f.dip

                fun StatisticBadge(iconCode: Int) = badge {
                    leadingIcon = faSolidIcon(iconCode)
                    density = UIBadge.Density.Small
                    backgroundColor = Color4.Transparent
                    color = themed { palette.accent }
                }

                lengthText = StatisticBadge(FAIcon.Clock).apply { text = "00:00" }
                bpmText = StatisticBadge(FAIcon.HeartPulse).apply { text = "0" }
            }

            linearContainer {
                orientation = UILinearContainer.Orientation.Vertical
                spacing = 4f.dip

                linearContainer {
                    orientation = UILinearContainer.Orientation.Horizontal
                    spacing = 4f.dip

                    circlesBadge = labeledBadge {
                        label = "Circles"
                        value = "0"
                        density = UIBadge.Density.Small
                    }
                    slidersBadge = labeledBadge {
                        label = "Sliders"
                        value = "0"
                        density = UIBadge.Density.Small
                    }
                    spinnersBadge = labeledBadge {
                        label = "Spinners"
                        value = "0"
                        density = UIBadge.Density.Small
                    }
                }

                linearContainer {
                    orientation = UILinearContainer.Orientation.Horizontal
                    spacing = 4f.dip

                    arText = labeledBadge {
                        label = "AR"
                        value = "00.0"
                        density = UIBadge.Density.Small
                    }
                    odText = labeledBadge {
                        label = "OD"
                        value = "00.0"
                        density = UIBadge.Density.Small
                    }
                    csText = labeledBadge {
                        label = "CS"
                        value = "00.0"
                        density = UIBadge.Density.Small
                    }
                    hpText = labeledBadge {
                        label = "HP"
                        value = "00.0"
                        density = UIBadge.Density.Small
                    }
                }
            }
        }
    }


    fun onBeatmapSelected(beatmapInfo: BeatmapInfo?) {
        titleText.text = beatmapInfo?.titleText ?: "No selected beatmap"
        artistText.text = beatmapInfo?.artistText ?: "Unknown"
        versionText.text = beatmapInfo?.version ?: "Unknown"
        circlesBadge.value = beatmapInfo?.hitCircleCount?.toString() ?: "0"
        slidersBadge.value = beatmapInfo?.sliderCount?.toString() ?: "0"
        spinnersBadge.value = beatmapInfo?.spinnerCount?.toString() ?: "0"

        setDifficultyStatistics(beatmapInfo)
    }

    /**
     * Change the displayed difficulty statistics.
     */
    fun setDifficultyStatistics(beatmapInfo: BeatmapInfo?) {

        if (beatmapInfo == null) {
            arText.value = "00.0"
            odText.value = "00.0"
            csText.value = "00.0"
            hpText.value = "00.0"
            bpmText.text = "0"
            lengthText.text = "00:00"
            starsText.rating = 0.0
            return
        }

        val mods = ModMenu.enabledMods
        val isPreciseMod = ModPrecise::class in mods
        val totalSpeedMultiplier = mods.values.calculateRate(Double.POSITIVE_INFINITY)

        val difficulty = beatmapInfo.getBeatmapDifficulty()

        difficulty.applyMods(GameMode.Droid, mods.values, true)

        if (isPreciseMod) {
            // Special case for OD. The Precise mod changes the hit window and not the OD itself, but we must
            // map the hit window back to the original hit window for the user to understand the difficulty
            // increase of the mod.
            val greatWindow = PreciseDroidHitWindow(difficulty.od).greatWindow
            difficulty.od = hitWindow300ToOverallDifficulty(greatWindow).toFloat()
        }

        // Round to 2 decimal places.
        // Using difficulty circle size is quite inaccurate here as the real circle size changes
        // depending on the height of the running device, but for the sake of comparison across
        // players, we assume the height of the device to be fixed.
        difficulty.difficultyCS = difficulty.difficultyCS.roundBy(2)
        difficulty.ar = difficulty.ar.roundBy(2)
        difficulty.od = difficulty.od.roundBy(2)
        difficulty.hp = difficulty.hp.roundBy(2)

        val minBpm = (beatmapInfo.bpmMin * totalSpeedMultiplier).roundToInt()
        val maxBpm = (beatmapInfo.bpmMax * totalSpeedMultiplier).roundToInt()
        val commonBpm = (beatmapInfo.mostCommonBPM * totalSpeedMultiplier).roundToInt()
        val length = (beatmapInfo.length / totalSpeedMultiplier).toLong()

        val sdf = SimpleDateFormat(if (length > 3600 * 1000) "HH:mm:ss" else "mm:ss")
        sdf.timeZone = TimeZone.getTimeZone("GMT+0")
        lengthText.text = sdf.format(length)

        bpmText.text = (if (minBpm == maxBpm) commonBpm.toString() else "$minBpm-$maxBpm ($commonBpm)")

        arText.value = difficulty.ar.toString()
        odText.value = difficulty.od.toString()
        csText.value = difficulty.difficultyCS.toString()
        hpText.value = difficulty.hp.toString()

        setStarRatingDisplay(beatmapInfo.getStarRating().toDouble())
    }

    /**
     * Change the displayed star rating.
     */
    fun setStarRatingDisplay(value: Double) {
        starsText.rating = value
    }

}

