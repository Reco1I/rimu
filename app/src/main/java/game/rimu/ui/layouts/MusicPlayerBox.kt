package game.rimu.ui.layouts

import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import com.google.android.material.slider.Slider
import com.reco1l.basskt.AudioState
import com.reco1l.framework.android.views.setConstraints
import com.reco1l.framework.animation.Ease
import com.reco1l.framework.animation.cancelAnimators
import com.reco1l.framework.animation.toAlpha
import com.reco1l.framework.animation.toScale
import com.reco1l.framework.graphics.Anchor
import com.reco1l.framework.lang.dateFormatFor
import game.rimu.R
import game.rimu.MainContext
import game.rimu.management.beatmap.IBeatmapObserver
import game.rimu.management.beatmap.WorkingBeatmap
import game.rimu.ui.LayerOverlay
import game.rimu.ui.BaseLayer
import game.rimu.ui.views.IconButton
import game.rimu.ui.views.SeekBar
import game.rimu.ui.views.TextView
import game.rimu.ui.views.addons.setTouchHandler
import game.rimu.ui.views.setTextAnimated
import org.andengine.engine.handler.IUpdateHandler
import kotlin.reflect.KClass

class MusicPlayerBox(ctx: MainContext) :
    ModelLayout(ctx),
    IBeatmapObserver,
    IUpdateHandler
{

    override var layer: KClass<out BaseLayer> = LayerOverlay::class


    private var dateFormat = dateFormatFor(1L)

    // Determines if the seekbar is being change by the user.
    private var isSeeking = false


    private val titleText = TextView {}

    private val artistText = TextView {

        dimensions.fontSize = 10
        rules.fontColorFactor = 0.75f

        setConstraints(
            target = titleText,
            topToTarget = Anchor.BOTTOM
        )
    }

    private val timeText = TextView {

        rules.fontColorFactor = 0.8f
        text = dateFormat.format(0L)

        dimensions.apply {
            fontSize = 8
            marginTop = 12
        }

        setConstraints(
            target = artistText,
            topToTarget = Anchor.BOTTOM
        )
    }

    private val lengthText = TextView {

        dimensions.fontSize = 8
        rules.fontColorFactor = 0.8f

        text = dateFormat.format(0L)

        setConstraints(target = timeText, topToTarget = Anchor.TOP)
        setConstraints(rightToTarget = Anchor.RIGHT)
    }


    private val seekBar = SeekBar {

        dimensions.marginTop = 8

        setConstraints(
            target = timeText,
            topToTarget = Anchor.BOTTOM
        )

        addOnSliderTouchListener(object : Slider.OnSliderTouchListener
        {
            override fun onStartTrackingTouch(slider: Slider)
            {
                isSeeking = true
            }

            override fun onStopTrackingTouch(slider: Slider)
            {
                ctx.beatmaps.current?.stream?.position = slider.value.toLong()

                // This fixes a visual bug when for one frame the non-updated position is shown in
                // the slider track, mostly because a mismatch between update and UI thread.
                slider.post { isSeeking = false }
            }
        })
    }

    private val playButton = IconButton {

        rules.image = "icon-pause"

        // Center horizontal
        setConstraints(
            leftToTarget = Anchor.LEFT,
            rightToTarget = Anchor.RIGHT
        )

        // Below seekbar
        setConstraints(
            target = seekBar,
            topToTarget = Anchor.BOTTOM
        )

        dimensions.apply {
            marginTop = 8
            size(50)
        }

        setTouchHandler {
            onActionUp = {
                ctx.beatmaps.current?.apply {

                    if (stream.state == AudioState.PLAYING)
                    {
                        pause()
                        rules.image = "icon-play"
                    }
                    else
                    {
                        play()
                        rules.image = "icon-pause"
                    }

                    invalidateSkin()
                }
            }
        }
    }


    init
    {

        IconButton {

            rules.image = "icon-previous"

            setConstraints(
                target = playButton,
                topToTarget = Anchor.TOP,
                bottomToTarget = Anchor.BOTTOM,
                rightToTarget = Anchor.LEFT
            )

            dimensions.apply {
                marginRight = 8
                size(50)
            }

            setTouchHandler {
                onActionUp = { ctx.beatmaps.previous() }
            }
        }

        IconButton {

            rules.image = "icon-next"

            setConstraints(
                target = playButton,
                topToTarget = Anchor.TOP,
                leftToTarget = Anchor.RIGHT,
                bottomToTarget = Anchor.BOTTOM
            )

            dimensions.apply {
                marginLeft = 8
                size(50)
            }

            setTouchHandler {
                onActionUp = { ctx.beatmaps.next() }
            }
        }

        dimensions.apply {
            width = 200
            height = WRAP_CONTENT
            cornerRadius = 12f

            marginTop = ctx.layouts[TopBarLayout::class].dimensions.height + 10
            marginLeft = 10

            padding(12)
        }

        rules.backgroundColor = "accentColor"
        rules.backgroundColorFactor = 0.1f

        ctx.beatmaps.bindObserver(observer = this)
    }


    override fun onMusicChange(beatmap: WorkingBeatmap?)
    {
        val length = beatmap?.stream?.length ?: 1L

        // Creating date format once instead of creating a new one everytime we want to format (consider
        // timestamps texts that indeed are formatted every tick in update thread)
        dateFormat = dateFormatFor(length)

        mainThread {

            titleText.setTextAnimated(beatmap?.source?.title
                ?:
                ctx.getString(R.string.metadata_unknown))

            artistText.setTextAnimated(beatmap?.source?.let { "${it.artist}\n${it.version}" }
                ?:
                ctx.getString(R.string.metadata_unknown))

            lengthText.text = dateFormat.format(length)

            // Seekbar max value will equal to the length of the song so we can use absolute
            // positioning when seeking.
            seekBar.valueTo = length.toFloat()
            seekBar.setLabelFormatter { dateFormat.format(it) }
        }
    }

    override fun onUpdate(secElapsed: Float)
    {
        val position = ctx.beatmaps.current?.stream?.position ?: 0L

        mainThread {

            // Timestamp text will always update to either thumb position or audios stream position.
            timeText.text = dateFormat.format(position)

            // If seeking we should avoid update twice the seekbar progress.
            if (!isSeeking)
                seekBar.value = position.toFloat()
        }
    }


    override fun onAttachedToWindow()
    {
        setConstraints(
            leftToTarget = Anchor.LEFT,
            topToTarget = Anchor.TOP
        )

        super.onAttachedToWindow()

        // Bounce animation

        cancelAnimators()
        toAlpha(0f)
        toScale(0.8f)
        toAlpha(1f, 100)
        toScale(1f, 300, ease = Ease.BOUNCE_OUT)

        // Updating information just in case.
        onMusicChange(ctx.beatmaps.current)

        ctx.engine.registerUpdateHandler(this)
    }

    override fun onDetachedFromWindow()
    {
        super.onDetachedFromWindow()

        ctx.engine.unregisterUpdateHandler(this)
    }

    override fun hide()
    {
        cancelAnimators()
        toScale(0.9f, 150, ease = Ease.DECELERATE)
        toAlpha(0f, 150, listener = { onEnd = { super.hide() } })
    }
}