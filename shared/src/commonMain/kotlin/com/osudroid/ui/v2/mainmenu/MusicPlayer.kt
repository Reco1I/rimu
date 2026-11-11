package com.osudroid.ui.v2.mainmenu

import com.osudroid.OsuGame
import com.osudroid.BeatmapSelection

import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Color4
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
import com.reco1l.verktex.ui.themed
import com.reco1l.verktex.localization.formatString

class MusicPlayer(private val trigger: UIButton) : UIModal() {

    private lateinit var titleText: UIText
    private lateinit var artistText: UIText
    private lateinit var progressSlider: UISlider
    private lateinit var playPauseButton: UIIconButton
    private lateinit var currentTime: UIText
    private lateinit var totalTime: UIText

    private var draggingProgressSlider = false


    init {
        // Without the dimmed backdrop of modals.
        backgroundColor = Color4.Transparent

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
                        color = themed { palette.accent }
                    }

                    artistText = text {
                        width = Dimension.FillAvailable
                        fontSize = themed { typography.smallCaption.size }
                        color = themed { palette.accent.shade(0.9f) }
                    }
                }

                container {
                    width = Dimension.FillAvailable

                    currentTime = text {
                        anchor = Anchor.CenterLeft
                        origin = Anchor.CenterLeft
                        fontSize = themed { typography.smallCaption.size }
                        color = themed { palette.accent.shade(0.8f) }
                    }

                    totalTime = text {
                        anchor = Anchor.CenterRight
                        origin = Anchor.CenterRight
                        fontSize = themed { typography.smallCaption.size }
                        color = themed { palette.accent.shade(0.8f) }
                    }
                }

                +UISlider().apply {
                    width = Dimension.FillAvailable
                    step = 0.01f
                    onStartDragging = { draggingProgressSlider = true }
                    onStopDragging = {
                        OsuGame.music.seekTo((value * 1000).toInt())
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

                        onActionUp = { OsuGame.music.previous() }
                    }

                    playPauseButton = iconButton {
                        icon = FAIcon.Play
                        density = UIButton.Density.Large
                        variant = UIButton.Variant.Solid
                        radius = FULL_RADIUS

                        onActionUp = {
                            if (OsuGame.music.isPlaying) {
                                OsuGame.music.pause()
                            } else {
                                OsuGame.music.play()
                            }
                        }
                    }

                    iconButton {
                        icon = FAIcon.ForwardFast
                        variant = UIButton.Variant.Ghost
                        anchor = Anchor.CenterLeft
                        origin = Anchor.CenterLeft
                        scaleOrigin = Anchor.Center

                        onActionUp = { OsuGame.music.next() }
                    }
                }

            }
        }
    }

    override fun onShow() {
        placeBelowTrigger()
        super.onShow()
    }

    // Below the button that opens this modal, right edges aligned. It follows it, the button may
    // still be sliding in.
    private fun placeBelowTrigger() {
        val (sceneX, sceneY) = trigger.toSceneSpace(trigger.bounds.width, trigger.bounds.height)

        // The card is laid out inside this modal's padding, the safe area.
        val (x, y) = toLocalSpaceFromScene(sceneX, sceneY)

        card.x = (x - paddingLeft.toPixels()).px
        card.y = (y - paddingTop.toPixels() + 12f.dip.toPixels()).px
    }

    override fun onUpdate(clock: FrameClock) {
        if (isShown) {
            placeBelowTrigger()
        }

        titleText.text = BeatmapSelection.beatmap?.titleText ?: "Unknown"
        artistText.text = BeatmapSelection.beatmap?.artistText ?: "Unknown"

        val showHours = OsuGame.music.length >= 3600000
        currentTime.text = formatDuration(OsuGame.music.position, showHours)
        totalTime.text = formatDuration(OsuGame.music.length, showHours)

        playPauseButton.icon = if (OsuGame.music.isPlaying) FAIcon.Pause else FAIcon.Play

        progressSlider.max = OsuGame.music.length / 1000f
        if (!draggingProgressSlider) {
            progressSlider.value = OsuGame.music.position / 1000f
        }

        super.onUpdate(clock)
    }


    private companion object {
        val FULL_RADIUS = 9999f.px

        // A date format would apply the time zone to the duration.
        fun formatDuration(millis: Int, showHours: Boolean): String {
            val totalSeconds = (millis / 1000).coerceAtLeast(0)
            val hours = totalSeconds / 3600
            val minutes = totalSeconds / 60 % 60
            val seconds = totalSeconds % 60

            return if (showHours) formatString("%d:%02d:%02d", hours, minutes, seconds) else formatString("%02d:%02d", totalSeconds / 60, seconds)
        }
    }
}
