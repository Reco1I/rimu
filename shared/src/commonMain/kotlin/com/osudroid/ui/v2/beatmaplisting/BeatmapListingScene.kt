package com.osudroid.ui.v2.beatmaplisting

import com.osudroid.beatmaplisting.BeatmapDownloader
import com.osudroid.beatmaplisting.BeatmapMirror
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.OrderType
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.SortType
import com.osudroid.beatmaplisting.BeatmapPreviewPlayer
import com.osudroid.beatmaplisting.BeatmapSearch
import com.osudroid.beatmaplisting.BeatmapSearchSource
import com.osudroid.beatmaplisting.BeatmapSetModel
import com.osudroid.beatmaplisting.MirrorSearchSource
import com.osudroid.data.DatabaseManager
import com.osudroid.resources.Res
import com.osudroid.ui.v2.BeatmapBackground
import com.osudroid.ui.v2.songselect.SongSelect
import com.osudroid.utils.currentTimeMillis
import com.osudroid.utils.updateThread
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.*
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.input.KeyboardEvent
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.*
import com.reco1l.verktex.ui.container.*
import com.reco1l.verktex.ui.container.LinearContainer.Orientation
import com.reco1l.verktex.ui.control.Select
import com.reco1l.verktex.ui.control.TextInput
import com.reco1l.verktex.ui.control.textInput
import com.reco1l.verktex.ui.text.Text
import com.reco1l.verktex.ui.text.faSolidIcon
import com.reco1l.verktex.ui.text.text
import com.reco1l.verktex.ui.toast.Toasts
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import ru.nsu.ccfit.zuev.osu.RankedStatus

/**
 * The beatmap listing: searches beatmap sets on the selected [BeatmapMirror], or on [source], a page at a
 * time while they're scrolled through, and shows them in a grid of [BeatmapSetCard]s to preview and
 * download them.
 */
class BeatmapListingScene(private val source: BeatmapSearchSource = MirrorSearchSource) : Scene() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val preview = BeatmapPreviewPlayer(scope)

    private val state: BeatmapListingState = object : BeatmapListingState() {

        override fun togglePreview(set: BeatmapSetModel) = preview.toggle(set)

        override fun download(set: BeatmapSetModel, withVideo: Boolean) {
            val url = BeatmapMirror.selected.download.request(set.id, withVideo)
            BeatmapDownloader.download(url, set.archiveName(withVideo), set.id)
        }

        override fun showDetails(set: BeatmapSetModel) = details.show(set)

        override fun isShownInListing(setId: Long) = recycler.boundComponents.keys.any { it.id == setId }
    }

    private val details = BeatmapSetDetails(state)

    private lateinit var recycler: RecyclerContainer<BeatmapSetModel, BeatmapSetCard>
    private lateinit var loadingIndicator: LoadingIndicator
    private lateinit var retryButton: IconButton
    private lateinit var filtersButton: TextButton
    private lateinit var filtersBar: Container
    private lateinit var orderButton: TextButton
    private lateinit var searchInput: TextInput
    private lateinit var messageText: Text
    private lateinit var topBar: Container
    private lateinit var results: Container

    private var search = BeatmapSearch()
    private var searchJob: Job? = null

    // The offset of the next page, and whether there may be one.
    private var offset = 0
    private var hasMorePages = true

    private var isLoading = false
        set(value) {
            field = value
            loadingIndicator.isVisible = value
        }

    // When the query was last typed, it's searched once it stays the same for a moment.
    private var queryChangeMillis = -1L

    // Whether a set was imported while the listing was shown, song select loads the library again.
    private var importedAnySet = false


    init {
        // On the scene, so it also covers the safe area its padding keeps the content out of.
        background = Background.Color(themed { palette.accent.shade(0.1f).alpha(0.9f) })

        linearContainer {
            width = fill()
            height = fill()
            orientation = Orientation.Vertical


            topBar = topBar()
            filtersBar()

            results = extendIntoSafeAreaContainer {
                width = fill()
                height = fill()

                recycler = RecyclerContainer<BeatmapSetModel, BeatmapSetCard>(12).apply {
                    width = fill()
                    height = fill()
                    clipping = ClipMode.Bounds
                    padding = Padding(12f.dip)
                    minColumnWidth = BeatmapSetCard.MIN_WIDTH
                    columnSpacing = 8f.dip
                    spacing = 8f.dip
                    endThreshold = 400f.dip

                    onCreateComponent = { BeatmapSetCard(state) }
                    onScrolledToEnd = {
                        if (!isLoading && hasMorePages && data.isNotEmpty()) {
                            search(reset = false)
                        }
                    }
                }
                +recycler

                messageText = text {
                    anchor = Anchor.Center
                    origin = Anchor.Center
                    fontSize = themed { typography.smallBody.size }
                    isVisible = false
                }
            }
        }

        state.onChange = ::refreshCards

        scope.launch {
            BeatmapDownloader.current.collect { download -> updateThread { state.download = download } }
        }

        scope.launch {
            BeatmapDownloader.imported.collect { setId ->
                updateThread {
                    importedAnySet = true
                    state.downloadedSetIds += setId
                }
            }
        }

        scope.launch {
            preview.playingSetId.collect { setId -> updateThread { state.playingSetId = setId } }
        }

        scope.launch {
            try {
                val ids = DatabaseManager.beatmapInfoTable.getBeatmapSetList().mapNotNullTo(HashSet()) { it.id?.toLong() }
                updateThread { state.downloadedSetIds += ids }
            } catch (e: Exception) {
                Logger.e("BeatmapListingScene", "Failed to read the beatmap sets of the library", e)
            }
        }

        search(reset = true)
    }


    private fun Container.topBar() = extendIntoSafeAreaContainer {
        width = fill()
        background = Background.Color(themed { palette.accent.shade(0.5f).alpha(0.1f) })
        border = Border(1f.dip, colorBottom = Color4.White.alpha(0.15f))
        padding = Padding(16f.dip, 12f.dip)

        linearContainer {
            anchor = Anchor.CenterLeft
            origin = Anchor.CenterLeft
            spacing = 12f.dip

            textButton {
                leadingIcon = faSolidIcon(FAIcon.ArrowLeft)
                text = Res.string.beatmap_listing_back
                onActionUp = { back() }
            }

            loadingIndicator = loadingIndicator {
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                width = 20f.dip
                height = 20f.dip
                isVisible = false
            }
        }

        linearContainer {
            anchor = Anchor.CenterRight
            origin = Anchor.CenterRight
            spacing = 8f.dip

            searchInput = textInput {
                width = 280f.dip
                key = "query"
                placeholder = Res.string.beatmap_listing_search
                trailingIcon = faSolidIcon(FAIcon.MagnifyingGlass)
                onValueChange = { queryChangeMillis = currentTimeMillis() }
                onConfirm = { submitQuery() }
            }

            retryButton = iconButton {
                icon = FAIcon.Rotate
                isVisible = false
                onActionUp = { search(reset = recycler.data.isEmpty()) }
            }

            filtersButton = textButton {
                leadingIcon = faSolidIcon(FAIcon.Sliders)
                text = Res.string.beatmap_listing_filters
                onActionUp = { toggleFilters() }
            }

            +Select(listOf(BeatmapMirror.selected)).apply {
                width = 160f.dip
                options = BeatmapMirror.entries.map { Select.Option(it, it.description, leadingIcon = faSolidIcon(FAIcon.Server)) }
                onValueChange = { values ->
                    val mirror = values.firstOrNull()

                    if (mirror != null && mirror != BeatmapMirror.selected) {
                        BeatmapMirror.selected = mirror
                        search(reset = true)
                    }
                }
            }
        }
    }

    private fun Container.filtersBar() {
        filtersBar = extendIntoSafeAreaContainer {
            width = fill()
            isVisible = false
            background = Background.Color(themed { palette.accent.shade(0.15f).alpha(0.6f) })
            border = Border(1f.dip, colorBottom = Color4.White.alpha(0.1f))
            padding = Padding(16f.dip, 8f.dip)

            linearContainer {
                spacing = 12f.dip

                filterLabel(Res.string.beatmap_listing_sort)

                +Select(listOf(search.sort)).apply {
                    width = 200f.dip
                    options = SortType.entries.map { Select.Option(it, it.description) }
                    onValueChange = { values -> values.firstOrNull()?.let { changeSearch(search.copy(sort = it)) } }
                }

                filterLabel(Res.string.beatmap_listing_order)

                orderButton = textButton {
                    onActionUp = {
                        val order = if (search.order == OrderType.Descending) OrderType.Ascending else OrderType.Descending
                        changeSearch(search.copy(order = order))
                    }
                }

                filterLabel(Res.string.beatmap_listing_status)

                +Select(listOf(STATUS_ALL)).apply {
                    width = 160f.dip
                    options = listOf(Select.Option(STATUS_ALL, Res.string.beatmap_listing_status_all)) +
                        RankedStatus.entries.map { Select.Option(it.name, it.stringId) }
                    onValueChange = { values ->
                        val status = values.firstOrNull()?.takeIf { it != STATUS_ALL }?.let { name -> RankedStatus.entries.first { it.name == name } }
                        changeSearch(search.copy(status = status))
                    }
                }
            }
        }

        updateOrderButton()
    }

    private fun LinearContainer.filterLabel(label: CharSequence) = text {
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterLeft
        text = label
        fontSize = themed { typography.smallCaption.size }
        color = themed { palette.accent.shade(0.8f) }
    }

    private fun updateOrderButton() {
        orderButton.text = search.order.description
        orderButton.leadingIcon = faSolidIcon(
            if (search.order == OrderType.Descending) FAIcon.ArrowDownWideShort else FAIcon.ArrowUpWideShort
        )
    }

    private fun toggleFilters() {
        filtersBar.isVisible = !filtersBar.isVisible
        filtersButton.isSelected = filtersBar.isVisible

        if (filtersBar.isVisible) {
            filtersBar.alpha = 0f
            filtersBar.fadeIn(0.2f)
        }
    }


    //region Searching

    private fun submitQuery() {
        queryChangeMillis = -1L
        changeSearch(search.copy(query = searchInput.value.trim()))
    }

    private fun changeSearch(newSearch: BeatmapSearch) {
        if (newSearch != search) {
            search = newSearch
            updateOrderButton()
            search(reset = true)
        }
    }

    /**
     * Searches the next page of sets, or the first one if [reset], replacing the ones shown.
     */
    private fun search(reset: Boolean) {
        searchJob?.cancel()

        if (reset) {
            offset = 0
            hasMorePages = true
            recycler.stopScroll()
            recycler.scrollY = 0f
            recycler.data = emptyList()
        }

        if (!hasMorePages) {
            return
        }

        isLoading = true
        retryButton.isVisible = false
        messageText.isVisible = false

        val params = search
        val pageOffset = offset

        searchJob = scope.launch {
            val page = try {
                source.search(params, pageOffset, PAGE_SIZE)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e("BeatmapListingScene", "Failed to search beatmap sets", e)

                updateThread {
                    if (params == search && pageOffset == offset) {
                        onSearchFailed()
                    }
                }
                return@launch
            }

            updateThread {
                if (params == search && pageOffset == offset) {
                    onPageLoaded(page)
                }
            }
        }
    }

    private fun onPageLoaded(page: List<BeatmapSetModel>) {
        isLoading = false
        offset += page.size
        hasMorePages = page.isNotEmpty()

        // Sets are keys of the recycler: one listed in an earlier page (results can shift as they're
        // loaded) isn't listed again.
        val listedIds = recycler.data.mapTo(HashSet()) { it.id }
        val newSets = page.filter { listedIds.add(it.id) }

        if (recycler.data.isEmpty()) {
            recycler.data = newSets
        } else {
            recycler.addData(newSets)
        }

        if (recycler.data.isEmpty()) {
            messageText.text = Res.string.beatmap_listing_no_results
            messageText.isVisible = true
        }
    }

    private fun onSearchFailed() {
        isLoading = false
        retryButton.isVisible = true
        Toasts.showLong(Res.string.beatmap_listing_connection_failed)

        if (recycler.data.isEmpty()) {
            messageText.text = Res.string.beatmap_listing_connection_failed
            messageText.isVisible = true
        }
    }

    //endregion


    private fun refreshCards() {
        recycler.boundComponents.values.forEach { it.refresh() }

        if (details.isShown) {
            details.refresh()
        }
    }


    override fun onEnter() {
        super.onEnter()
        BeatmapBackground.fadeBrightness(0.3f)

        // Entrance: the top bar slides down, the results rise.
        topBar.apply {
            clearTransforms()
            alpha = 0f
            translationY = (-20f).dip
            transformSequence {
                fadeIn(0.4f, Easing.OutCubic)
                moveToY(0f, 0.55f, Easing.OutCubic)
            }
        }
        results.apply {
            clearTransforms()
            alpha = 0f
            translationY = 32f.dip
            transformSequence(0.05f) {
                fadeIn(0.4f, Easing.OutCubic)
                moveToY(0f, 0.55f, Easing.OutCubic)
            }
        }
    }

    override fun onLeave() {
        super.onLeave()
        preview.stop()
        details.hide()
    }

    override fun onUpdate(clock: FrameClock) {
        if (queryChangeMillis > 0L && currentTimeMillis() - queryChangeMillis > QUERY_DEBOUNCE_MILLIS) {
            submitQuery()
        }
        super.onUpdate(clock)
    }

    // Back leaves the listing, once no component consumed it.
    override fun onInputEvent(event: InputEvent): Boolean {
        if (super.onInputEvent(event)) {
            return true
        }

        if (event is KeyboardEvent && event.isBackPress) {
            back()
            return true
        }
        return false
    }

    override fun onDetached() {
        super.onDetached()
        scope.cancel()
    }


    /**
     * Leaves the listing. Song select below it loads the library again if sets were imported.
     */
    fun back() {
        preview.stop()
        details.hide()
        scope.cancel()

        // The cards are recycled first, so no sprite shows a cover once it's unloaded.
        val sets = recycler.data
        recycler.data = emptyList()
        sets.forEach { set -> set.thumbnail?.let(BeatmapCovers::unload) }

        Verktex.withTreeLock {
            Verktex.popScene()

            if (importedAnySet) {
                (Verktex.scene as? SongSelect)?.loadBeatmaps()
            }
        }
    }


    companion object {
        private const val PAGE_SIZE = 50
        private const val QUERY_DEBOUNCE_MILLIS = 500L
        private const val STATUS_ALL = "all"
    }
}
