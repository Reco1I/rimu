package com.osudroid.ui.v2.songselect

import com.osudroid.data.DatabaseManager
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.px
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.UIIcon
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.container
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.control.UISelect
import com.reco1l.verktex.ui.control.UITextInput
import com.reco1l.verktex.ui.form.FormCheckbox
import com.reco1l.verktex.ui.iconButton
import com.reco1l.verktex.ui.text.faSolidIcon
import ru.nsu.ccfit.zuev.osu.Config
import com.reco1l.verktex.ui.themed

class SongSelectFilterBar : UILinearContainer() {

    /**
     * The filter set by the bar.
     */
    var filter = BeatmapFilter(
        sortOrder = BeatmapSortOrder.entries.getOrElse(Config.getInt(SORT_ORDER_KEY, 0)) { BeatmapSortOrder.Title },
        grouping = BeatmapGrouping.entries.getOrElse(Config.getInt(GROUPING_KEY, 0)) { BeatmapGrouping.BeatmapSet }
    )
        private set(value) {
            if (field != value) {
                field = value
                onFilterChanged?.invoke(value)
            }
        }

    /**
     * Called when the user changes the filter.
     */
    var onFilterChanged: ((BeatmapFilter) -> Unit)? = null


    private val optionsContainer: UILinearContainer
    private lateinit var collectionSelect: UISelect<String>


    init {
        width = Dimension.FillAvailable
        anchor = Anchor.TopRight
        origin = Anchor.TopRight
        orientation = Orientation.Vertical
        padding = Padding(32f.px, 16f.px)
        spacing = 8f.dip

        linearContainer {
            width = Dimension.FillAvailable
            spacing = 14f.px

            iconButton {
                icon = FAIcon.Filter
                onActionUp = { toggleOptions() }
            }

            container {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable

                +object : UITextInput("") {

                    private var lastValueChangeMillis = -1L

                    init {
                        key = "query"
                        width = Dimension.FillAvailable
                        height = Dimension.FillAvailable
                        placeholder = "Search"
                        onConfirm = { submit() }
                    }

                    override fun onValueChanged() {
                        super.onValueChanged()
                        lastValueChangeMillis = System.currentTimeMillis()
                    }

                    override fun onUpdate(clock: FrameClock) {
                        super.onUpdate(clock)

                        if (lastValueChangeMillis > 0L && System.currentTimeMillis() - lastValueChangeMillis > 300) {
                            submit()
                        }
                    }

                    private fun submit() {
                        lastValueChangeMillis = -1L
                        filter = filter.copy(query = value.trim())
                    }
                }

                +UIIcon(FAIcon.MagnifyingGlass).apply {
                    anchor = Anchor.CenterRight
                    origin = Anchor.CenterRight
                    x = (-12f).px
                    color = themed { palette.accent }
                }
            }
        }

        optionsContainer = linearContainer {
            width = Dimension.FillAvailable
            orientation = Orientation.Vertical
            isVisible = false
            backgroundColor = themed { palette.surface }
            radius = themed { radius.xl }
            padding = themed { Padding(gap.md) }
            spacing = themed { gap.sm }

            linearContainer {
                width = Dimension.FillAvailable
                spacing = 8f.dip

                +UISelect(listOf(filter.sortOrder)).apply {
                    width = Dimension.FillAvailable
                    options = BeatmapSortOrder.entries.map { UISelect.Option(it, it.label, leadingIcon = faSolidIcon(FAIcon.ArrowDownWideShort)) }
                    onValueChange = { values ->
                        val order = values.firstOrNull() ?: BeatmapSortOrder.Title
                        Config.setInt(SORT_ORDER_KEY, order.ordinal)
                        filter = filter.copy(sortOrder = order)
                    }
                }

                +UISelect(listOf(filter.grouping)).apply {
                    width = Dimension.FillAvailable
                    options = BeatmapGrouping.entries.map { UISelect.Option(it, it.label, leadingIcon = faSolidIcon(FAIcon.LayerGroup)) }
                    onValueChange = { values ->
                        val grouping = values.firstOrNull() ?: BeatmapGrouping.BeatmapSet
                        Config.setInt(GROUPING_KEY, grouping.ordinal)
                        filter = filter.copy(grouping = grouping)
                    }
                }
            }

            linearContainer {
                width = Dimension.FillAvailable
                spacing = 8f.dip

                collectionSelect = UISelect(listOf(ALL_COLLECTIONS)).apply {
                    width = Dimension.FillAvailable
                    onValueChange = { values ->
                        filter = filter.copy(collection = values.firstOrNull()?.takeIf { it != ALL_COLLECTIONS })
                    }
                }
                +collectionSelect

                +FormCheckbox(filter.favoritesOnly).apply {
                    width = Dimension.FillAvailable
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                    label = "Favorites only"
                    showResetButton = false
                    onValueChanged = { filter = filter.copy(favoritesOnly = it) }
                }
            }
        }
    }


    private fun toggleOptions() {
        if (!optionsContainer.isVisible) {
            updateCollections()
        }
        optionsContainer.isVisible = !optionsContainer.isVisible
    }

    /**
     * Updates the collections the user can choose from, they can change from the beatmap options.
     */
    fun updateCollections() {
        val collections = DatabaseManager.beatmapCollectionsTable.getCollections()

        collectionSelect.options = listOf(UISelect.Option(ALL_COLLECTIONS, "All collections", leadingIcon = faSolidIcon(FAIcon.FolderOpen))) +
            collections.map { UISelect.Option(it, it, leadingIcon = faSolidIcon(FAIcon.FolderOpen)) }

        // The chosen collection could have been deleted.
        if (filter.collection != null && filter.collection !in collections) {
            collectionSelect.value = listOf(ALL_COLLECTIONS)
        }
    }


    private companion object {
        const val SORT_ORDER_KEY = "songSelectSortOrder"
        const val GROUPING_KEY = "songSelectGrouping"

        // A value no collection can have, collections names can't be empty.
        const val ALL_COLLECTIONS = ""
    }
}
