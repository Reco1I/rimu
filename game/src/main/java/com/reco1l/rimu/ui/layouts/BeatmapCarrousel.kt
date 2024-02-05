package com.reco1l.rimu.ui.layouts

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout.HORIZONTAL
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.text.scale
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.data.BeatmapSet
import com.reco1l.rimu.data.adapter.Adapter
import com.reco1l.rimu.data.adapter.IHeldView
import com.reco1l.rimu.ui.LayerScene
import com.reco1l.rimu.ui.scenes.SelectorScene
import com.reco1l.rimu.ui.views.CarrouselRecyclerView
import com.reco1l.rimu.ui.views.ConstraintLayout
import com.reco1l.rimu.ui.views.TextView
import com.reco1l.rimu.ui.views.view
import com.reco1l.toolkt.android.orientation
import com.reco1l.toolkt.android.setConstraints
import com.reco1l.toolkt.graphics.Anchor
import com.reco1l.toolkt.graphics.approximateSampleSize
import com.reco1l.toolkt.graphics.calculateDimensions
import com.reco1l.toolkt.graphics.createBitmap
import com.reco1l.toolkt.graphics.cropInCenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import android.graphics.BitmapFactory.Options as BitmapFactoryOptions

class BeatmapCarrousel(ctx: MainContext) :
    ModelLayout(
        ctx = ctx,
        layer = LayerScene::class,
        parents = arrayOf(SelectorScene::class)
    )
{

    /**
     * Scope intended to run image loading tasks.
     */
    val imageScope = CoroutineScope(Dispatchers.IO)

    val imageCache = mutableMapOf<String, Bitmap>()


    val listView = CarrouselRecyclerView {

        orientation = HORIZONTAL
        invertedTranslation = true

        adapter = Adapter(
            data = ctx.beatmaps.sets,
            onCreateView = { BeatmapSetView(ctx, imageScope, imageCache) }
        )

        setDimensions { height = LayoutParams.MATCH_PARENT }

        setConstraints(rightToTarget = Anchor.RIGHT)
    }

}


class BeatmapSetView(

    ctx: MainContext,

    private val imageScope: CoroutineScope,

    private val imageCache: MutableMap<String, Bitmap>

):
    ConstraintLayout(ctx),
    IHeldView<BeatmapSet>
{

    override var boundData: BeatmapSet? = null

    override val dimensions = super.dimensions.apply {

        width = MATCH_PARENT
        cornerRadius = 12f
    }

    override val skinningRules = super.skinningRules.apply {

    }

    val title = TextView {}


    private var imageJob: Job? = null


    override fun onBindData(data: BeatmapSet, position: Int)
    {
        super.onBindData(data, position)

        val source = data[0]

        title.text = buildSpannedString {

            append(source.title)
            appendLine()
            color(0xA5A5A5) { scale(0.9f) { append(source.artist) } }
        }
    }


    override fun onAttachedToWindow()
    {
        super.onAttachedToWindow()

        val data = boundData?.get(0)
            ?: return

        if (data.background == null || imageCache[data.parent] != null)
            return

        // Cancelling previous job.
        imageJob?.cancel()

        val job = imageScope.launch {

            val file = ctx.resources.getFile(data.parent, data.background) ?: return@launch

            val bitmap = BitmapFactoryOptions()
                .calculateDimensions(file)
                .approximateSampleSize(width, height)
                .createBitmap(file)
                .cropInCenter(width, height)

            background = BitmapDrawable(ctx.getResources(), bitmap)
        }

        imageJob = job

        job.invokeOnCompletion {

            // Avoids to release if a new job was assigned.
            if (imageJob == job)
                imageJob = null
        }
    }


    override fun onDetachedFromWindow()
    {
        super.onDetachedFromWindow()

        imageJob?.cancel()
    }
}
