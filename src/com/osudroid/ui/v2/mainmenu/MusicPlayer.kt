package com.osudroid.ui.v2.mainmenu

import com.osudroid.MusicManager
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.px
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.UIButton
import com.reco1l.verktex.ui.UIIconButton
import com.reco1l.verktex.ui.UIModal
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.container
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.control.UISlider
import com.reco1l.verktex.ui.iconButton
import com.reco1l.verktex.ui.text.UIText
import com.reco1l.verktex.ui.text.text
import ru.nsu.ccfit.zuev.osu.LibraryManager
import java.text.SimpleDateFormat

class MusicPlayer(private val trigger: UIButton) : UIModal() {

    private lateinit var titleText: UIText
    private lateinit var artistText: UIText
    private lateinit var progressSlider: UISlider
    private lateinit var playPauseButton: UIIconButton
    private lateinit var currentTime: UIText
    private lateinit var totalTime: UIText

    private var draggingProgressSlider = false

    private val timeFormat = SimpleDateFormat("mm:ss")
    private val timeFormatWithHours = SimpleDateFormat("HH:mm:ss")


    init {
        // Removes the background dim
        style = Style {}

        card.apply {
            anchor = Anchor.TopLeft
            origin = Anchor.TopRight
            scaleOrigin = Anchor.Center
            width = 320f.dip
            padding = Padding(16f.dip)

            linearContainer {
                width = Dimension.FillAvailable
                orientation = UILinearContainer.Orientation.Vertical
                spacing = 8f.dip

                linearContainer {
                    width = Dimension.FillAvailable
                    orientation = UILinearContainer.Orientation.Vertical

                    titleText = text {
                        width = Dimension.FillAvailable
                        style = Style { theme ->
                            color = theme.palette.accent
                        }
                    }

                    artistText = text {
                        width = Dimension.FillAvailable
                        style = Style { theme ->
                            fontSize = theme.typography.smallCaption.size
                            color = theme.palette.accent * 0.9f
                        }
                    }
                }

                container {
                    width = Dimension.FillAvailable

                    currentTime = text {
                        anchor = Anchor.CenterLeft
                        origin = Anchor.CenterLeft
                        style = Style { theme ->
                            fontSize = theme.typography.smallCaption.size
                            color = theme.palette.accent * 0.8f
                        }
                    }

                    totalTime = text {
                        anchor = Anchor.CenterRight
                        origin = Anchor.CenterRight
                        style = Style { theme ->
                            fontSize = theme.typography.smallCaption.size
                            color = theme.palette.accent * 0.8f
                        }
                    }
                }

                +UISlider().apply {
                    width = Dimension.FillAvailable
                    step = 0.01f
                    onStartDragging = { draggingProgressSlider = true }
                    onStopDragging = {
                        MusicManager.position = (value * 1000).toInt()
                        draggingProgressSlider = false
                    }
                    progressSlider = this
                }

                linearContainer {
                    orientation = UILinearContainer.Orientation.Horizontal
                    anchor = Anchor.TopCenter
                    origin = Anchor.TopCenter
                    spacing = 16f.dip

                    iconButton {
                        icon = FAIcon.BackwardFast
                        variant = UIButton.Variant.Ghost
                        anchor = Anchor.CenterLeft
                        origin = Anchor.CenterLeft
                        scaleOrigin = Anchor.Center

                        onActionUp = {
                            MusicManager.currentBeatmap = LibraryManager.selectPreviousBeatmapSet().beatmaps.random()
                            MusicManager.load()
                            MusicManager.play()
                        }
                    }

                    playPauseButton = iconButton {
                        icon = FAIcon.Play
                        density = UIButton.Density.Large
                        variant = UIButton.Variant.Solid
                        style += Style {
                            radius = FULL_RADIUS
                        }

                        onActionUp = {
                            if (MusicManager.isPlaying) {
                                MusicManager.pause()
                            } else {
                                MusicManager.play()
                            }
                        }
                    }

                    iconButton {
                        icon = FAIcon.ForwardFast
                        variant = UIButton.Variant.Ghost
                        anchor = Anchor.CenterLeft
                        origin = Anchor.CenterLeft
                        scaleOrigin = Anchor.Center

                        onActionUp = {
                            MusicManager.currentBeatmap = LibraryManager.selectNextBeatmapSet().beatmaps.random()
                            MusicManager.load()
                            MusicManager.play()
                        }
                    }
                }

            }
        }
    }

    override fun onShow() {
        // Placed below the button that opens this modal, which only has a known position when shown.
        val triggerCorner = trigger.toSceneSpace(trigger.bounds.width, trigger.bounds.height)

        card.x = triggerCorner.x.px
        card.y = (triggerCorner.y + 12f.dip.toPixels()).px

        super.onShow()
    }

    override fun onUpdate(clock: FrameClock) {
        titleText.text = MusicManager.currentBeatmap?.titleText ?: "Unknown"
        artistText.text = MusicManager.currentBeatmap?.artistText ?: "Unknown"

        val format = if (MusicManager.length >= 3600000) timeFormatWithHours else timeFormat
        currentTime.text = format.format(MusicManager.position)
        totalTime.text = format.format(MusicManager.length)

        playPauseButton.icon = if (MusicManager.isPlaying) FAIcon.Pause else FAIcon.Play

        progressSlider.max = MusicManager.length / 1000f
        if (!draggingProgressSlider) {
            progressSlider.value = MusicManager.position / 1000f
        }

        super.onUpdate(clock)
    }


    private companion object {
        val FULL_RADIUS = 9999f.px
    }
}
