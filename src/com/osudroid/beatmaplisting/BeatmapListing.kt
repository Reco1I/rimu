package com.osudroid.beatmaplisting

import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.settings.Settings
import android.graphics.BitmapFactory
import android.graphics.Color
import com.reco1l.verktex.Logger
import android.view.Choreographer
import android.view.Choreographer.FrameCallback
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.OnKeyListener
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.forEach
import androidx.core.view.get
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.edlplan.ui.fragment.BaseFragment
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.reco1l.verktex.Platform
import com.reco1l.verktex.audio.AudioStream
import com.osudroid.online.OsuHttp
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.OrderType
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.SortType
import com.osudroid.ui.OsuColors
import com.osudroid.utils.mainThread
import com.reco1l.osu.ui.Option
import com.reco1l.osu.ui.SelectDialog
import com.reco1l.osu.ui.SelectDropdown
import com.reco1l.framework.android.cornerRadius
import com.reco1l.framework.android.dp
import com.reco1l.framework.android.drawableLeft
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import ru.nsu.ccfit.zuev.audio.Status
import ru.nsu.ccfit.zuev.osu.GlobalManager
import com.reco1l.verktex.ui.toast.Toasts
import ru.nsu.ccfit.zuev.osuplus.R
import java.text.SimpleDateFormat
import java.util.TimeZone
import kotlinx.coroutines.CancellationException
import ru.nsu.ccfit.zuev.osu.RankedStatus


class BeatmapListing : BaseFragment(),
    OnEditorActionListener,
    OnKeyListener,
    FrameCallback {


    override val layoutID = R.layout.beatmap_downloader_fragment


    private val adapter = BeatmapSetAdapter()

    private val searchScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val filtersFragment = BeatmapListingFiltersFragment(this)

    private val scrollListener = object : OnScrollListener() {

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {

            // When the end is reach, load more.
            if (!recyclerView.canScrollVertically(1) && dy > 0) {
                offset += 50
                search(true)
            }

            super.onScrolled(recyclerView, dx, dy)
        }
    }


    private var pendingRequest: Job? = null

    private var offset = 0


    private lateinit var indicator: CircularProgressIndicator

    private lateinit var recyclerView: RecyclerView

    private lateinit var retryButton: Button

    private lateinit var searchBox: EditText

    private lateinit var logoView: Button


    init {
        current?.dismiss()

        mainThread {
            current = this
        }
    }


    override fun onLoadView() {

        val itemWidth = resources.getDimension(R.dimen.beatmap_downloader_item_width)
        val itemMargin = resources.getDimension(R.dimen.beatmap_downloader_item_margin) * 2

        val spanCount = resources.displayMetrics.widthPixels / (itemWidth + itemMargin)

        recyclerView = findViewById(R.id.beatmap_list)!!
        recyclerView.layoutManager = GridLayoutManager(context, spanCount.toInt())
        recyclerView.addOnScrollListener(scrollListener)
        recyclerView.adapter = adapter

        retryButton = findViewById(R.id.refresh)!!
        retryButton.setOnClickListener { search(true) }

        searchBox = findViewById(R.id.search)!!
        searchBox.setOnEditorActionListener(this)
        searchBox.setOnKeyListener(this)

        indicator = findViewById(R.id.indicator)!!

        logoView = findViewById(R.id.logo)!!
        logoView.text = mirror.description
        logoView.drawableLeft = requireContext().getDrawable(mirror.logoResource)
        logoView.setOnClickListener {
            SelectDialog()
                .setOptions(BeatmapMirror.entries.map { mirror ->
                    Option(
                        text = buildSpannedString {
                            append(mirror.description)
                            appendLine()
                            color(0xBFFFFFFF.toInt()) { append(mirror.homeUrl) }
                        },
                        value = mirror.ordinal,
                        icon = requireContext().getDrawable(mirror.logoResource)
                    )
                })
                .setSelected(mirror.ordinal)
                .setOnSelectListener { value ->
                    value as Int
                    if (value != mirror.ordinal) {
                        mirror = BeatmapMirror.entries[value]

                        logoView.text = mirror.description
                        logoView.drawableLeft = requireContext().getDrawable(mirror.logoResource)

                        search(false)
                    }
                }
                .setTitle("Select a beatmap mirror")
                .show()
        }

        findViewById<ImageButton>(R.id.close)!!.setOnClickListener {
            dismiss()
        }

        findViewById<TextView>(R.id.filters)!!.also { filtersButton ->

            filtersButton.setOnClickListener {

                if (!filtersFragment.isAdded) {
                    filtersButton.setBackgroundColor(0x29F27272)
                    filtersButton.cornerRadius = 15f.dp
                    filtersFragment.show()
                } else {
                    filtersButton.setBackgroundColor(Color.TRANSPARENT)
                    filtersButton.cornerRadius = 0f
                    filtersFragment.dismiss()
                }
            }
        }


        search(false)

        previewPlayCount = 0
        Choreographer.getInstance().postFrameCallback(this)
    }


    override fun doFrame(frameTimeNanos: Long) {

        // Restoring play count in case it's preventing user from play another preview.
        if (previewPlayCount >= 2 && System.currentTimeMillis() - lastTimePreviewPlayed > 5000) {
            previewPlayCount = 0
        }

        if (!isDetached) {
            Choreographer.getInstance().postFrameCallback(this)
        }
    }


    fun search(keepData: Boolean) {

        mainThread {
            indicator.visibility = VISIBLE
            retryButton.visibility = GONE
        }

        pendingRequest?.cancel()
        pendingRequest = searchScope.launch(CoroutineExceptionHandler { _, throwable ->

            Toasts.show("Failed to connect to server, please check your internet connection.")

            Logger.e("BeatmapListing", "Failed to connect to beatmap mirror.", throwable)

            mainThread {
                indicator.visibility = GONE
                retryButton.visibility = VISIBLE
            }
        }) {

            if (!keepData) {
                offset = 0

                mainThread {
                    val itemCount = adapter.data.size
                    adapter.data.clear()
                    adapter.notifyItemRangeRemoved(0, itemCount)
                }
            }

            ensureActive()

            val url = mirror.search.request(
                query = searchBox.text.toString(),
                offset = offset,
                limit = 50,
                sort = filtersFragment.sortType,
                order = filtersFragment.orderType,
                status = filtersFragment.rankedStatus
            )

            val json = OsuHttp.getJsonArray(url) { header(HttpHeaders.UserAgent, OsuHttp.MIRROR_USER_AGENT) }
            ensureActive()

            val beatmapSets = mirror.search.response(json)
            ensureActive()

            mainThread {
                adapter.data.addAll(beatmapSets)
                adapter.notifyItemRangeInserted(offset, beatmapSets.size)
                indicator.visibility = GONE
            }

            pendingRequest = null
        }
    }

    fun stopPreviews(shouldResumeMusic: Boolean) {
        if (!::recyclerView.isInitialized) {
            return
        }

        recyclerView.forEach { view ->

            val holder = recyclerView.getChildViewHolder(view) as BeatmapSetViewHolder
            holder.stopPreview(shouldResumeMusic)
        }
    }


    override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {

        if (actionId == EditorInfo.IME_ACTION_SEND) {
            search(false)
            return true
        }

        return false
    }

    override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {

        if (keyCode == KeyEvent.KEYCODE_ENTER && v is TextView) {
            onEditorAction(v, EditorInfo.IME_ACTION_SEND, event)
            return true
        }

        return false
    }


    override fun show() {
        isPlayingMusic = GlobalManager.getInstance().music.status == Status.PLAYING
        super.show()
    }

    override fun dismiss() {

        stopPreviews(true)

        pendingRequest?.cancel()

        mainThread {
            super.dismiss()
            current = null
        }
    }


    companion object {

        /**
         * The current [BeatmapListing] instance.
         */
        var current: BeatmapListing? = null

        /**
         * The current selected beatmap mirror.
         */
        var mirror
            get() = BeatmapMirror.entries.getOrElse(Settings[OsuSettings.BeatmapMirror]) { BeatmapMirror.entries[0] }
            set(value) {
                Settings[OsuSettings.BeatmapMirror] = value.ordinal
            }

        /**
         * Whether is a beatmap preview music playing or not.
         */
        var isPlayingMusic = false
            private set

        /**
         * The last time a song preview was played in milliseconds.
         */
        var lastTimePreviewPlayed = 0L

        /**
         * The count of previews played within a short time (5 seconds).
         */
        var previewPlayCount = 0

    }

}



// Information

class BeatmapSetDetails(val beatmapSet: BeatmapSetModel, val holder: BeatmapSetViewHolder) : BaseFragment() {


    override val layoutID = R.layout.beatmap_downloader_details


    lateinit var previewButton: Button


    private lateinit var cover: ImageView

    private lateinit var status: TextView

    private lateinit var metadata: TextView

    private lateinit var details: TextView

    private lateinit var downloadButton: Button

    private lateinit var downloadNoVideoButton: Button

    private lateinit var creator: TextView

    private lateinit var difficulty: LinearLayout


    init {
        holder.detailsFragment = this
    }


    override fun onLoadView() {

        (root as ViewGroup)[0].apply {
            outlineProvider = ViewOutlineProvider.BACKGROUND
            clipToOutline = true
        }

        cover = findViewById(R.id.cover)!!
        status = findViewById(R.id.status)!!
        details = findViewById(R.id.details)!!
        creator = findViewById(R.id.creator)!!
        metadata = findViewById(R.id.metadata)!!
        difficulty = findViewById(R.id.difficulty)!!
        previewButton = findViewById(R.id.preview_button)!!
        downloadButton = findViewById(R.id.download_button)!!
        downloadNoVideoButton = findViewById(R.id.download_no_video_button)!!

        status.text = holder.status.text
        creator.text = holder.creator.text
        metadata.setText(holder.metadata.text, TextView.BufferType.SPANNABLE)


        val beatmaps = beatmapSet.beatmaps
        for (i in beatmaps.indices) {

            val beatmap = beatmaps[i]
            val button = TextView(ContextThemeWrapper(context, R.style.beatmap_difficulty_icon))
            difficulty.addView(button)

            button.setTextColor(OsuColors.getStarRatingColor(beatmap.starRating).toHex())
            button.setOnClickListener { selectDifficulty(button, beatmap) }

        }
        selectDifficulty(difficulty[0] as TextView, beatmaps[0])


        // If it's already playing when shown.
        if (holder.previewStream != null) {
            previewButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.pause_24px, 0, 0, 0)
        }

        previewButton.setOnClickListener {

            if (holder.previewStream == null) {
                BeatmapListing.current!!.stopPreviews(false)
                holder.playPreview(beatmapSet)
            } else {
                holder.stopPreview(true)
            }
        }

        val suggestedDownloadFilename = "${beatmapSet.id} ${beatmapSet.artist} - ${beatmapSet.title}"

        downloadButton.setOnClickListener {
            BeatmapDownloader.download(
                url = BeatmapListing.mirror.download.request(beatmapSet.id, true).toString(),
                suggestedFilename = suggestedDownloadFilename
            )
        }

        downloadNoVideoButton.apply {
            if (BeatmapListing.mirror.supportsNoVideoDownloads && beatmapSet.hasVideo) {
                visibility = VISIBLE
                setOnClickListener {
                    BeatmapDownloader.download(
                        url = BeatmapListing.mirror.download.request(beatmapSet.id, false).toString(),
                        suggestedFilename = "$suggestedDownloadFilename [no video]"
                    )
                }
            } else {
                visibility = GONE
                setOnClickListener(null)
            }
        }

        cover.setImageDrawable(holder.cover.drawable)
        root!!.setOnClickListener { dismiss() }
    }


    private fun selectDifficulty(button: TextView, beatmap: BeatmapModel) {

        difficulty.forEach {
            if (it == button) {
                it.setBackgroundResource(R.drawable.rounded_rect)
                it.background.setTint(0xFF363653.toInt())
            } else {
                it.background = null
            }
        }

        val sdf = SimpleDateFormat(if (beatmap.lengthSec >= 3600) "HH:mm:ss" else "mm:ss").also {

            it.timeZone = TimeZone.getTimeZone("GMT+0")
        }

        details.setText(buildSpannedString {

            append(beatmap.version)
            appendLine()

            color(0xFFB2B2CC.toInt()) {

                append("Star rating: ${beatmap.starRating}")
                appendLine()

                append("AR: ${beatmap.ar} - OD: ${beatmap.od} - CS: ${beatmap.cs} - HP: ${beatmap.hp}")
                appendLine()

                append("Circles: ${beatmap.circleCount} - Sliders: ${beatmap.sliderCount} - Spinners: ${beatmap.spinnerCount}")
                appendLine()

                append("Length: ${sdf.format(beatmap.lengthSec * 1000)} - BPM: ${beatmap.bpm}")
            }

        }, TextView.BufferType.SPANNABLE)
    }


    override fun dismiss() {
        holder.detailsFragment = null
        super.dismiss()
    }

}


// List

class BeatmapSetAdapter : RecyclerView.Adapter<BeatmapSetViewHolder>() {


    var data = mutableListOf<BeatmapSetModel>()


    private val mediaScope = CoroutineScope(Dispatchers.IO)


    init {
        setHasStableIds(true)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BeatmapSetViewHolder {

        val inflater = LayoutInflater.from(parent.context)

        return BeatmapSetViewHolder(
            inflater.inflate(R.layout.beatmap_downloader_set_item, parent, false),
            mediaScope
        )
    }


    override fun getItemCount() = data.size

    override fun getItemId(position: Int) = position.toLong()


    override fun onBindViewHolder(holder: BeatmapSetViewHolder, position: Int) {
        holder.bind(data[position])
    }

}

class BeatmapSetViewHolder(itemView: View, private val mediaScope: CoroutineScope) : RecyclerView.ViewHolder(itemView) {


    var detailsFragment: BeatmapSetDetails? = null

    var previewStream: AudioStream? = null


    val cover: ImageView = itemView.findViewById(R.id.cover)

    val status: TextView = itemView.findViewById(R.id.status)

    val creator: TextView = itemView.findViewById(R.id.creator)

    val metadata: TextView = itemView.findViewById(R.id.metadata)

    val difficulty: TextView = itemView.findViewById(R.id.difficulty)

    val previewButton: Button = itemView.findViewById(R.id.preview_button)

    val downloadButton: Button = itemView.findViewById(R.id.download_button)

    val downloadNoVideoButton: Button = itemView.findViewById(R.id.download_no_video_button)


    private var coverJob: Job? = null

    private var previewJob: Job? = null


    init {
        itemView.outlineProvider = ViewOutlineProvider.BACKGROUND
        itemView.clipToOutline = true
    }


    fun bind(beatmapSet: BeatmapSetModel) {

        status.text = beatmapSet.status.stringId
        creator.text = "Mapped by ${beatmapSet.creator}"


        metadata.setText(buildSpannedString {

            append(if (Settings[OsuSettings.ForceRomanized]) beatmapSet.title else beatmapSet.titleUnicode)
            appendLine()
            color(0xFFB2B2CC.toInt()) {
                append(if (Settings[OsuSettings.ForceRomanized]) beatmapSet.artist else beatmapSet.artistUnicode)
            }

        }, TextView.BufferType.SPANNABLE)

        difficulty.setText(buildSpannedString {

            val beatmaps = beatmapSet.beatmaps

            for (i in beatmaps.indices) {
                val beatmap = beatmaps[i]

                color(OsuColors.getStarRatingColor(beatmap.starRating).toHex()) {
                    append("⦿")
                }

                if (i != beatmaps.size - 1) {
                    append(" ")
                }
            }

        }, TextView.BufferType.SPANNABLE)

        coverJob?.cancel()

        if (beatmapSet.thumbnail != null) {
            coverJob = mediaScope.launch {

                try {
                    val bytes = OsuHttp.getBytes(beatmapSet.thumbnail!!)
                    ensureActive()

                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                    mainThread { cover.setImageBitmap(bitmap) }

                } catch (e: Exception) {
                    mainThread { cover.setImageDrawable(null) }

                    if (e is CancellationException) {
                        throw e
                    }

                    Logger.e("BeatmapDownloader", "Failed to load cover.", e)
                }

                coverJob = null
            }
        } else {
            cover.setImageDrawable(null)
        }

        previewButton.setOnClickListener {

            if (previewStream == null) {
                BeatmapListing.current!!.stopPreviews(false)
                playPreview(beatmapSet)
            } else {
                stopPreview(true)
            }
        }

        val suggestedDownloadFilename = "${beatmapSet.id} ${beatmapSet.artist} - ${beatmapSet.title}"

        downloadButton.setOnClickListener {
            BeatmapDownloader.download(
                url = BeatmapListing.mirror.download.request(beatmapSet.id, true).toString(),
                suggestedFilename = suggestedDownloadFilename
            )
        }

        downloadNoVideoButton.apply {
            if (BeatmapListing.mirror.supportsNoVideoDownloads && beatmapSet.hasVideo) {
                visibility = VISIBLE
                setOnClickListener {
                    BeatmapDownloader.download(
                        url = BeatmapListing.mirror.download.request(beatmapSet.id, false).toString(),
                        suggestedFilename = "$suggestedDownloadFilename [no video]"
                    )
                }
            } else {
                visibility = GONE
                setOnClickListener(null)
            }
        }

        itemView.setOnClickListener {
            BeatmapSetDetails(beatmapSet, this).show()
        }
    }


    fun playPreview(beatmapSet: BeatmapSetModel) {

        if (BeatmapListing.previewPlayCount >= 2) {
            Toasts.show("Please wait a few seconds before trying again.")
            return
        }

        BeatmapListing.current!!.stopPreviews(true)

        previewJob = mediaScope.launch {

            try {
                previewStream = Platform.provider.createAudioUrlStreamSource(BeatmapListing.mirror.preview.request(beatmapSet.beatmaps[0].id).toString())!!.createStream().also {
                    it.onEnd = {
                        stopPreview(true)
    
                        if (BeatmapListing.isPlayingMusic) {
                            GlobalManager.getInstance().music.play()
                        }
                    }
                }

                ensureActive()

                GlobalManager.getInstance().music.pause()

                previewStream!!.volume = OsuSettings.musicVolume
                previewStream!!.play()

                BeatmapListing.previewPlayCount++
                BeatmapListing.lastTimePreviewPlayed = System.currentTimeMillis()

                mainThread {
                    previewButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.pause_24px, 0, 0, 0)

                    if (detailsFragment?.isLoaded == true) {
                        detailsFragment?.previewButton?.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.pause_24px,
                            0,
                            0,
                            0
                        )
                    }
                }

            } catch (e: Exception) {
                if (e is CancellationException) {
                    throw e
                }

                Logger.e("BeatmapListing", "Failed to load preview", e)
            }

            previewJob = null
        }
    }


    fun stopPreview(shouldResumeMusic: Boolean) {

        try {
            previewJob?.cancel()
            previewJob = null
            previewStream?.dispose()
            previewStream = null

        } catch (e: Exception) {
            Logger.e("BeatmapListing", "Failed to stop preview", e)
        }

        mainThread {
            previewButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.play_arrow_24px, 0, 0, 0)

            if (detailsFragment?.isLoaded == true) {
                detailsFragment?.previewButton?.setCompoundDrawablesWithIntrinsicBounds(R.drawable.play_arrow_24px, 0, 0, 0)
            }
        }

        if (shouldResumeMusic && BeatmapListing.isPlayingMusic) {
            GlobalManager.getInstance().music.play()
        }
    }

}


// Filters

class BeatmapListingFiltersFragment(private val beatmapListing: BeatmapListing) : BaseFragment() {


    override val layoutID = R.layout.beatmap_downloader_filters


    var sortType = SortType.RankedDate

    var orderType = OrderType.Descending

    var rankedStatus: RankedStatus? = null


    override fun onLoadView() {

        findViewById<View>(R.id.frg_body)!!.y = beatmapListing.findViewById<View>(R.id.bar)!!.height.toFloat()
        findViewById<View>(R.id.frg_background)!!.setOnClickListener { callDismissOnBackPress() }

        findViewById<Button>(R.id.sort)!!.also { sortButton ->

            sortButton.text = sortType.description
            sortButton.setOnClickListener {

                SelectDropdown(sortButton)
                    .setOptions(SortType.entries.map { type -> Option(type.description, type) })
                    .setSelected(sortType)
                    .setOnSelectListener {
                        it as SortType
                        if (it != sortType) {
                            sortType = it
                            sortButton.text = it.description
                            beatmapListing.search(false)
                        }
                    }
                    .show()
            }
        }

        findViewById<Button>(R.id.order)!!.also { orderButton ->

            orderButton.text = orderType.description
            orderButton.setOnClickListener {
                orderType = if (orderType == OrderType.Ascending) OrderType.Descending else OrderType.Ascending
                orderButton.text = orderType.description
                beatmapListing.search(false)
            }
        }

        findViewById<Button>(R.id.status)!!.also { statusButton ->

            if (rankedStatus == null) {
                statusButton.text = "All"
            } else {
                statusButton.text = rankedStatus!!.stringId
            }

            statusButton.setOnClickListener {

                val options = RankedStatus.entries.map { status ->
                    Option(status.stringId, status)
                }.toMutableList()
                options.add(0, Option("All", null))

                SelectDropdown(statusButton)
                    .setOptions(options)
                    .setSelected(rankedStatus)
                    .setOnSelectListener {
                        it as RankedStatus?
                        if (it != rankedStatus) {
                            rankedStatus = it

                            if (it == null) {
                                statusButton.text = "All"
                            } else {
                                statusButton.text = it.stringId
                            }

                            beatmapListing.search(false)
                        }
                    }
                    .show()
            }
        }

    }

    override fun dismiss() {
        beatmapListing.findViewById<TextView>(R.id.filters)!!.also { filtersButton ->
            filtersButton.setBackgroundColor(Color.TRANSPARENT)
            filtersButton.cornerRadius = 0f
        }
        super.dismiss()
    }


}