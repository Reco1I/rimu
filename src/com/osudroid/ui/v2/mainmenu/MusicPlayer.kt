package com.osudroid.ui.v2.mainmenu

import com.osudroid.MusicManager
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.ui.container
import com.reco1l.verktex.ui.container.Orientation
import com.reco1l.verktex.ui.iconButton
import com.reco1l.verktex.ui.linearContainer
import com.reco1l.verktex.ui.text
import com.reco1l.verktex.ui.UIIcon
import com.reco1l.verktex.ui.text.UIText
import com.reco1l.verktex.theme.FontSize
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.theme.Radius
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.theme.srem
import com.reco1l.verktex.ui.ColorVariant
import com.reco1l.verktex.ui.SizeVariant
import com.reco1l.verktex.ui.UIButton
import com.reco1l.verktex.ui.UIModal
import com.reco1l.verktex.ui.control.UISlider
import com.reco1l.verktex.data.Vec4
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
        style = {}

        card.apply {
            anchor = Anchor.TopLeft
            origin = Anchor.TopRight
            scaleOrigin = Anchor.Center
            style + {
                width = 20f.rem
                padding = Vec4(4f.srem)
            }

            linearContainer {
                width = Dimension.FillAvailable
                orientation = Orientation.Vertical
                style = {
                    spacing = 2f.srem
                }

                linearContainer {
                    width = Dimension.FillAvailable
                    orientation = Orientation.Vertical

                    titleText = text {
                        width = Dimension.FillAvailable
                        style = {
                            color = it.accentColor
                        }
                    }

                    artistText = text {
                        width = Dimension.FillAvailable
                        style = {
                            fontSize = FontSize.XS
                            color = it.accentColor * 0.9f
                        }
                    }
                }

                container {
                    width = Dimension.FillAvailable

                    currentTime = text {
                        anchor = Anchor.CenterLeft
                        origin = Anchor.CenterLeft
                        style = {
                            fontSize = FontSize.XS
                            color = it.accentColor * 0.8f
                        }
                    }

                    totalTime = text {
                        anchor = Anchor.CenterRight
                        origin = Anchor.CenterRight
                        style = {
                            fontSize = FontSize.XS
                            color = it.accentColor * 0.8f
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
                    orientation = Orientation.Horizontal
                    anchor = Anchor.TopCenter
                    origin = Anchor.TopCenter
                    style = {
                        spacing = 4f.srem
                    }

                    iconButton {
                        icon = UIIcon(FAIcon.BackwardFast)
                        colorVariant = ColorVariant.Tertiary
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
                        icon = UIIcon(FAIcon.Play)
                        sizeVariant = SizeVariant.Large
                        colorVariant = ColorVariant.Primary
                        style + {
                            radius = Radius.Full
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
                        icon = UIIcon(FAIcon.ForwardFast)
                        colorVariant = ColorVariant.Tertiary
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

    override fun onAttached() {
        super.onAttached()

        val (triggerX, triggerY) = trigger.convertLocalToSceneCoordinates(trigger.width, trigger.height)
        card.x = triggerX
        card.y = triggerY + 3f.srem
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {
        titleText.text = MusicManager.currentBeatmap?.titleText ?: "Unknown"
        artistText.text = MusicManager.currentBeatmap?.artistText ?: "Unknown"

        val format = if (MusicManager.length >= 3600000) timeFormatWithHours else timeFormat
        currentTime.text = format.format(MusicManager.position)
        totalTime.text = format.format(MusicManager.length)

        (playPauseButton.icon as UIIcon).iconCode = if (MusicManager.isPlaying) FAIcon.Pause else FAIcon.Play

        progressSlider.max = MusicManager.length / 1000f
        if (!draggingProgressSlider) {
            progressSlider.value = MusicManager.position / 1000f
        }

        super.onManagedUpdate(deltaTimeSec)
    }

}