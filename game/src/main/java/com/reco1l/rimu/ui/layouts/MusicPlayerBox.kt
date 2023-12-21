package com.reco1l.rimu.ui.layouts

import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import com.reco1l.basskt.AudioState
import com.reco1l.toolkt.android.setConstraints
import com.reco1l.rimu.constants.Ease
import com.reco1l.toolkt.animation.cancelAnimators
import com.reco1l.toolkt.animation.toAlpha
import com.reco1l.toolkt.animation.toScale
import com.reco1l.toolkt.animation.toTranslationX
import com.reco1l.toolkt.animation.toTranslationY
import com.reco1l.toolkt.graphics.Anchor
import com.reco1l.toolkt.kotlin.dateFormatFor
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.R
import com.reco1l.rimu.mainThread
import com.reco1l.rimu.management.beatmap.IBeatmapObserver
import com.reco1l.rimu.management.beatmap.WorkingBeatmap
import com.reco1l.rimu.ui.BaseLayer
import com.reco1l.rimu.ui.LayerOverlay
import com.reco1l.rimu.ui.views.IconButton
import com.reco1l.rimu.ui.views.SeekBar
import com.reco1l.rimu.ui.views.TextView
import com.reco1l.rimu.ui.views.addons.setTouchHandler
import com.reco1l.rimu.ui.views.setTextAnimated
import org.andengine.engine.handler.IUpdateHandler
import kotlin.math.pow
import kotlin.math.sqrt
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

        setDimensions {
            fontSize = 10
        }

        setSkinning {
            fontColorFactor = 0.75f
        }

        setConstraints(
            target = titleText,
            topToTarget = Anchor.BOTTOM
        )
    }

    private val timeText = TextView {

        text = dateFormat.format(0L)

        setDimensions {
            fontSize = 8
            marginTop = 12
        }

        setSkinning {
            fontColorFactor = 0.8f
        }

        setConstraints(
            target = artistText,
            topToTarget = Anchor.BOTTOM
        )
    }

    private val lengthText = TextView {

        text = dateFormat.format(0L)

        setDimensions {
            fontSize = 8
        }

        setSkinning {
            fontColorFactor = 0.8f
        }

        setConstraints(target = timeText, topToTarget = Anchor.TOP)
        setConstraints(rightToTarget = Anchor.RIGHT)
    }


    private val seekBar = SeekBar {

        setDimensions {
            marginTop = 8
        }

        setConstraints(
            target = timeText,
            topToTarget = Anchor.BOTTOM
        )

        onStartSeek = { isSeeking = true }
        onEndSeek = {

            ctx.beatmaps.current?.stream?.position = it.toLong()
            post { isSeeking = false }
        }
    }

    private val playButton = IconButton {

        setSkinning {
            image = "icon-pause"
        }

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

        setDimensions {
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

    private var initialX = 0f

    private var initialY = 0f


    init
    {
        isClickable = true

        IconButton {

            rules.image = "icon-previous"

            setConstraints(
                target = playButton,
                topToTarget = Anchor.TOP,
                bottomToTarget = Anchor.BOTTOM,
                rightToTarget = Anchor.LEFT
            )

            setDimensions {
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

            setDimensions {
                marginLeft = 8
                size(50)
            }

            setTouchHandler {
                onActionUp = { ctx.beatmaps.next() }
            }
        }

        setDimensions {
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
            seekBar.max = length.toFloat()
            //seekBar.setLabelFormatter { dateFormat.format(it) }
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
                seekBar.progress = position.toFloat()
        }
    }


    override fun onTouchEvent(event: MotionEvent): Boolean
    {
        when (event.action)
        {
            ACTION_DOWN ->
            {
                cancelAnimators()
                initialX = event.x
                initialY = event.y
            }

            ACTION_MOVE ->
            {
                val dX = event.x - initialX
                val dY = event.y - initialY

                val length = sqrt(dX * dX + dY * dY)

                translationX = dX * if (length <= 0) 0f else length.pow(0.7f) / length
                translationY = dY * if (length <= 0) 0f else length.pow(0.7f) / length
            }

            ACTION_UP ->
            {
                if (translationX != 0f || translationY != 0f)
                {
                    toTranslationX(0f, 300, ease = Ease.BOUNCE_OUT)
                    toTranslationY(0f, 300, ease = Ease.BOUNCE_OUT)
                }
            }
        }

        return super.onTouchEvent(event)
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