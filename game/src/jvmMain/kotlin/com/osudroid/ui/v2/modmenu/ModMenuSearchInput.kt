package com.osudroid.ui.v2.modmenu

import com.reco1l.verktex.data.dip
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.control.UITextInput
import com.reco1l.verktex.ui.text.faSolidIcon

/**
 * A search input field for filtering mods and mod presets in the mod menu.
 */
class ModMenuSearchInput : UITextInput("") {
    /**
     * The function to call when the search term changes.
     */
    var onSearchTermUpdate: (searchTerm: String) -> Unit = {}

    private var lastValueChange = Long.MAX_VALUE

    init {
        minWidth = 160f.dip
        maxWidth = 320f.dip
        trailingIcon = faSolidIcon(FAIcon.MagnifyingGlass)
        placeholder = "Search..."
    }

    override fun onValueChanged() {
        lastValueChange = System.currentTimeMillis()
        super.onValueChanged()
    }

    override fun onUpdate(clock: FrameClock) {
        if (lastValueChange != Long.MAX_VALUE && System.currentTimeMillis() - lastValueChange >= DEBOUNCE_MS) {
            onSearchTermUpdate(value)
            lastValueChange = Long.MAX_VALUE
        }

        super.onUpdate(clock)
    }

    companion object {
        private const val DEBOUNCE_MS = 200L
    }
}