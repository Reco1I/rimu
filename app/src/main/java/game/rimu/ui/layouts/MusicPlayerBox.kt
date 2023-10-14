package game.rimu.ui.layouts

import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import com.reco1l.basskt.AudioState
import com.reco1l.framework.android.views.animate
import com.reco1l.framework.android.views.scale
import com.reco1l.framework.android.views.setConstraints
import com.reco1l.framework.android.views.setListeners
import com.reco1l.framework.android.views.setScale
import com.reco1l.framework.graphics.Anchor
import com.reco1l.framework.lang.dateFormatFor
import game.rimu.android.RimuContext
import game.rimu.management.beatmap.IBeatmapObserver
import game.rimu.management.beatmap.WorkingBeatmap
import game.rimu.ui.LayerOverlay
import game.rimu.ui.LayoutLayer
import game.rimu.ui.dimensions
import game.rimu.ui.views.IconButton
import game.rimu.ui.views.SeekBar
import game.rimu.ui.views.TextView
import game.rimu.ui.views.addons.setTouchHandler
import org.andengine.engine.handler.IUpdateHandler
import java.text.SimpleDateFormat
import kotlin.reflect.KClass

class MusicPlayerBox(ctx: RimuContext) :
    RimuLayout(ctx),
    IBeatmapObserver,
    IUpdateHandler
{

    override var layer: KClass<out LayoutLayer> = LayerOverlay::class


    val titleText = TextView {

        text = "Unknown"

    }

    val artistText = TextView {

        text = "Unknown"

        setConstraints(
            target = titleText,
            topToTarget = Anchor.BOTTOM
        )

        dimensions.fontSize = 10
        alpha = 0.8f
    }

    val timeText = TextView {

        text = "00:00"
        skinningRules.fontColor = "accentColor" to 0.8f

        setConstraints(
            target = artistText,
            topToTarget = Anchor.BOTTOM
        )

        dimensions {
            fontSize = 8
            marginTop = 12
        }

    }

    val lengthText = TextView {

        text = "00:00"
        dimensions.fontSize = 8
        skinningRules.fontColor = "accentColor" to 0.8f

        setConstraints(target = timeText, topToTarget = Anchor.TOP)
        setConstraints(rightToTarget = Anchor.RIGHT)
    }


    val seekBar = SeekBar {

        setConstraints(
            target = timeText,
            topToTarget = Anchor.BOTTOM
        )

        dimensions.marginTop = 8
    }


    val playButton = IconButton(texture = "icon-pause") {

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

        dimensions {
            marginTop = 8
            size(50)
        }

        setTouchHandler {
            onActionUp = {
                ctx.beatmaps.current?.apply {

                    if (stream.state == AudioState.PLAYING)
                    {
                        pause()
                        skinningRules.texture = "icon-play" to 0
                    }
                    else
                    {
                        play()
                        skinningRules.texture = "icon-pause" to 0
                    }

                    invalidateSkin()
                }
            }
        }
    }

    val previousButton = IconButton(texture = "icon-previous") {

        setConstraints(
            target = playButton,
            topToTarget = Anchor.TOP,
            bottomToTarget = Anchor.BOTTOM,
            rightToTarget = Anchor.LEFT
        )

        dimensions {
            marginRight = 8
            size(50)
        }

        setTouchHandler {
            onActionUp = { ctx.beatmaps.previous() }
        }
    }

    val nextButton = IconButton(texture = "icon-next") {

        setConstraints(
            target = playButton,
            topToTarget = Anchor.TOP,
            bottomToTarget = Anchor.BOTTOM,
            leftToTarget = Anchor.RIGHT
        )

        dimensions {
            marginLeft = 8
            size(50)
        }

        setTouchHandler {
            onActionUp = { ctx.beatmaps.next() }
        }
    }


    private lateinit var dateFormat: SimpleDateFormat


    init
    {
        dimensions {
            width = 200
            height = WRAP_CONTENT
            cornerRadius = 12f

            marginTop = ctx.layouts[TopBarLayout::class].dimensions.height + 10
            marginLeft = 10

            padding(12)
        }

        skinningRules.backgroundColor = "accentColor" to 0.1f

        ctx.beatmaps.bindObserver(observer = this)
    }


    override fun onMusicChange(beatmap: WorkingBeatmap?)
    {
        mainThread {

            titleText.text = beatmap?.source?.title ?: "Unknown"
            artistText.text = beatmap?.source?.let { "${it.artist}\n${it.version}" } ?: "Unknown"

            val length = beatmap?.stream?.length ?: 1L

            dateFormat = dateFormatFor(length)

            lengthText.text = dateFormat.format(length)
            seekBar.max = length.toInt()
        }
    }

    override fun onUpdate(secElapsed: Float)
    {
        mainThread {
            val position = ctx.beatmaps.current?.stream?.position ?: 0L

            timeText.text = dateFormat.format(position)
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

        alpha = 0f
        setScale(0.8f)

        animate {
            scale(1f)
            alpha(1f)

            duration = 300
            interpolator = BounceInterpolator()
        }

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

            scale(0.8f)
            alpha(0f)

            duration = 200
            interpolator = DecelerateInterpolator()

            setListeners(onEnd = { super.hide() })
        }
    }
}