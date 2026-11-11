package com.osudroid.ui.v2.songselect

import com.osudroid.data.DatabaseManager
import com.osudroid.resources.Res.string
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.data.ClipMode
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.data.pct
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.UIButton
import com.reco1l.verktex.ui.UIIconButton
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.UIScrollableContainer
import com.reco1l.verktex.ui.control.UITextInput
import com.reco1l.verktex.ui.dialog.UIConfirmDialog
import com.reco1l.verktex.ui.dialog.UIDialog
import com.reco1l.verktex.ui.dialog.UIPromptDialog
import com.reco1l.verktex.ui.text.UIText
import com.reco1l.verktex.ui.text.faSolidIcon
import com.reco1l.verktex.ui.themed
import com.osudroid.utils.async
import com.osudroid.utils.updateThread
import kotlin.concurrent.Volatile

/**
 * Lists the beatmap collections to add the beatmap set in [directory] to or remove it from, and to
 * create or delete collections.
 */
class BeatmapCollectionsDialog(private val directory: String) : UIDialog<UIScrollableContainer>(
    UIScrollableContainer().apply {
        scrollAxes = Axis.Y
        width = fill()
        maxHeight = 0.55f.pct
        clipping = ClipMode.Bounds
    }
) {

    /**
     * Called once the dialog is hidden, if a collection or the beatmap set's membership changed.
     */
    var onCollectionsChanged: (() -> Unit)? = null


    private val list = UILinearContainer().apply {
        orientation = UILinearContainer.Orientation.Vertical
        width = fill()
        padding = themed { Padding(gap.md) }
        spacing = themed { gap.xs }
    }

    // Also set by the coroutines that write the changes.
    @Volatile
    private var hasChanges = false


    init {
        title = "Collections"
        innerContent += list

        addButton {
            text = "New collection"
            leadingIcon = faSolidIcon(FAIcon.FolderPlus)
            variant = UIButton.Variant.Solid
            onActionUp = { showNewCollectionPrompt() }
        }

        addButton {
            text = "Close"
            onActionUp = { hide() }
        }

        loadCollections()
    }


    // What a row shows of a collection.
    private class CollectionState(val name: String, val count: Int, val containsSet: Boolean)

    private suspend fun readCollection(name: String): CollectionState {
        val table = DatabaseManager.beatmapCollectionsTable
        return CollectionState(name, table.getBeatmaps(name)?.size ?: 0, table.inCollection(name, directory))
    }

    /**
     * Reads the collections from the database and shows them.
     */
    private fun loadCollections() {
        async {
            val collections = DatabaseManager.beatmapCollectionsTable.getCollections().sorted().map { readCollection(it) }
            updateThread { showCollections(collections) }
        }
    }

    private fun showCollections(collections: List<CollectionState>) {
        list.clearChildren()

        if (collections.isEmpty()) {
            list += UIText().apply {
                width = fill()
                alignment = Anchor.Center
                padding = themed { Padding(gap.md) }
                color = Color4.White.alpha(0.6f)
                text = "There are no collections yet."
            }
            return
        }

        collections.forEach { list += CollectionRow(it) }
    }

    private fun showNewCollectionPrompt() {
        UIPromptDialog(UITextInput()).apply {
            title = "New collection"
            input.placeholder = "Name"

            addButton {
                text = "Create"
                variant = UIButton.Variant.Solid
                onActionUp = {
                    val name = value.trim()

                    if (name.isNotEmpty() && name != string.favorite_default.toString()) {
                        async {
                            val table = DatabaseManager.beatmapCollectionsTable

                            if (!table.collectionExists(name)) {
                                table.insertCollection(name)
                                hasChanges = true
                                loadCollections()
                            }
                        }
                    }
                    hide()
                }
            }

            addButton {
                text = "Cancel"
                onActionUp = { hide() }
            }
        }.show()
    }

    override fun onHidden() {
        super.onHidden()

        if (hasChanges) {
            hasChanges = false
            onCollectionsChanged?.invoke()
        }
    }


    private inner class CollectionRow(state: CollectionState) : UILinearContainer() {

        private val name = state.name

        private val countText = UIText().apply {
            fontSettings = themed { typography.smallBody }
            color = Color4.White.alpha(0.6f)
        }

        private val toggleButton = UIIconButton().apply {
            anchor = Anchor.CenterLeft
            origin = Anchor.CenterLeft
            variant = UIButton.Variant.Ghost
            onActionUp = {
                // Before the change is written, so that closing the dialog right away still reports it.
                hasChanges = true

                async {
                    val table = DatabaseManager.beatmapCollectionsTable

                    if (table.inCollection(name, directory)) {
                        table.removeBeatmap(name, directory)
                    } else {
                        table.addBeatmap(name, directory)
                    }

                    val state = readCollection(name)
                    updateThread { show(state) }
                }
            }
        }


        init {
            orientation = Orientation.Horizontal
            width = fill()
            spacing = themed { gap.sm }

            +toggleButton

            +UILinearContainer().apply {
                orientation = Orientation.Vertical
                width = fill()
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft

                +UIText().apply { text = name }
                +countText
            }

            +UIIconButton().apply {
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                variant = UIButton.Variant.Ghost
                icon = FAIcon.Trash
                color = Color4("#FFBFBF")
                onActionUp = { confirmDeletion() }
            }

            show(state)
        }


        private fun show(state: CollectionState) {
            toggleButton.icon = if (state.containsSet) FAIcon.Check else FAIcon.Plus
            countText.text = if (state.count == 1) "1 beatmap set" else "${state.count} beatmap sets"
        }

        private fun confirmDeletion() {
            UIConfirmDialog().apply {
                title = "Delete collection"
                text = "Delete the collection \"$name\"? ${string.favorite_ensure}"
                onConfirm = {
                    hasChanges = true

                    async {
                        DatabaseManager.beatmapCollectionsTable.clearCollection(name)
                        DatabaseManager.beatmapCollectionsTable.deleteCollection(name)
                        loadCollections()
                    }
                }
            }.show()
        }
    }
}
