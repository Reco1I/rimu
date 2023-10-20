package game.rimu.ui.layouts

import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.SeekBar.OnSeekBarChangeListener
import com.reco1l.basskt.AudioState
import com.reco1l.framework.android.views.animate
import com.reco1l.framework.android.views.localized
import com.reco1l.framework.android.views.scale
import com.reco1l.framework.android.views.setConstraints
import com.reco1l.framework.android.views.setListeners
import com.reco1l.framework.android.views.setScale
import com.reco1l.framework.graphics.Anchor
import com.reco1l.framework.lang.dateFormatFor
import game.rimu.R
import game.rimu.android.RimuContext
import game.rimu.management.beatmap.IBeatmapObserver
import game.rimu.management.beatmap.WorkingBeatmap
import game.rimu.ui.LayerOverlay
import game.rimu.ui.LayoutLayer
import game.rimu.ui.views.IconButton
import game.rimu.ui.views.SeekBar
import game.rimu.ui.views.TextView
import game.rimu.ui.views.addons.setTouchHandler
import game.rimu.ui.views.setTextAnimated
import org.andengine.engine.handler.IUpdateHandler
import kotlin.reflect.KClass
import android.widget.SeekBar as AndroidSeekBar

class MusicPlayerBox(ctx: RimuContext) :
    ModelLayout(ctx),
    IBeatmapObserver,
    IUpdateHandler
{

    override var layer: KClass<out LayoutLayer> = LayerOverlay::class


    private var dateFormat = dateFormatFor(1L)

    // Determines if the seekbar is being change by the user.
    private var isSeeking = false


    private val titleText = TextView {

        text = localized(R.string.metadata_unknown)

    }

    private val artistText = TextView {

        dimensions.fontSize = 10
        rules.fontColorFactor = 0.75f

        text = localized(R.string.metadata_unknown)

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

        setOnSeekBarChangeListener(object : OnSeekBarChangeListener
        {
            override fun onProgressChanged(
                seekBar: AndroidSeekBar,
                progress: Int,
                fromUser: Boolean
            ) = Unit

            override fun onStartTrackingTouch(seekBar: AndroidSeekBar)
            {
                isSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: AndroidSeekBar)
            {
                ctx.beatmaps.current?.stream?.position = seekBar.progress.toLong()
                isSeeking = false
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
                ?: localized(R.string.metadata_unknown))

            artistText.setTextAnimated(beatmap?.source?.let { "${it.artist}\n${it.version}" }
                ?: localized(R.string.metadata_unknown))

            lengthText.text = dateFormat.format(length)

            // Seekbar max value will equal to the length of the song so we can use absolute
            // positioning when seeking.
            seekBar.max = length.toInt()
        }
    }

    override fun onUpdate(secElapsed: Float)
    {
        val position = when
        {
            // If seeking the position is determined by the thumb position.
            isSeeking -> seekBar.progress

            // If not then it's determined by the audio stream position.
            else -> ctx.beatmaps.current?.stream?.position ?: 0L
        }

        mainThread {

            // Timestamp text will always update to either thumb position or audios stream position.
            timeText.text = dateFormat.format(position)

            // If seeking we should avoid update twice the seekbar progress.
            if (!isSeeking)
                seekBar.progress = position.toInt()
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

        alpha = 0f
        setScale(0.8f)

        animate {
            scale(1f)
            alpha(1f)

            duration = 300
            interpolator = BounceInterpolator()
        }

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
        animate {
            scale(0.9f)
            alpha(0f)

            duration = 150
            interpolator = DecelerateInterpolator()

            setListeners(onEnd = { super.hide() })
        }
    }
}