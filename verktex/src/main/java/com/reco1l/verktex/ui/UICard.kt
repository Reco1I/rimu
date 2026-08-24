package com.reco1l.verktex.ui

import com.reco1l.verktex.*
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.text.UIText
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.horizontalPadding
import com.reco1l.verktex.data.px
import com.reco1l.verktex.data.wrap
import com.reco1l.verktex.math.interpolateFloat
import com.reco1l.verktex.time.TickingClock
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.container.clickableContainer
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.shape.UIBox

fun UIContainer.card(block: UICard.() -> Unit) = UICard().apply(block).also { +it }

open class UICard : UILinearContainer() {

    /**
     * The title to show in the card's title bar. If `null` or empty, the title bar will not be shown.
     */
    var title: String? = null

    /**
     * Whether the card can be collapsed or not. If true, the title bar will be clickable and the
     * card can be collapsed or expanded by clicking on it. If false, the card will always be expanded
     * and the title bar will not be clickable.
     *
     * Note that if [isCollapsible] is false, [isExpanded] will always return true and [collapse] will
     * have no effect.
     */
    var isCollapsible = false
        set(value) {
            if (field == value) return
            field = value

            // No animation for now, just hide the title bar if the card is not collapsible.
            if (value) {
                titleBar.height = Verktex.theme.controlStyle.minimumHeight
                titleBar.isVisible = true
            } else {
                titleBar.height = 0f.px
                titleBar.isVisible = false
                isExpanded = true
            }
        }

    /**
     * Whether the card is currently expanded or collapsed.
     */
    var isExpanded: Boolean = true
        get() = if (isCollapsible) field else true
        private set(value) {
            if (field == value) return
            field = value
            if (isCollapsible) {
                onExpandStatusChange?.invoke(value)
            }
        }

    /**
     * A function that is called when this [UICard] is collapsed or expanded.
     *
     * This function will be called only if [isCollapsible] is true. If [isCollapsible] is false, this
     * function will never be called.
     */
    var onExpandStatusChange: ((isExpanded: Boolean) -> Unit)? = null

    //region Components

    private val titleBar = clickableContainer {

        style = Style { theme ->
            height = theme.controlStyle.minimumHeight
            padding = horizontalPadding(theme.gap.md)
        }

        onActionUp = {
            if (isCollapsible) {
                isExpanded = !isExpanded
            }
        }
    }

    private val titleText = UIText().apply {
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterLeft

        style = Style { theme ->
            fontSettings = theme.typography.caption
        }
    }

    private val chevron = UIIcon(FAIcon.ChevronDown).apply {
        anchor = Anchor.CenterRight
        origin = Anchor.CenterRight
        rotationOrigin = Anchor.Center

        style = Style { theme ->
            iconSize = theme.typography.caption.size
            color = theme.typography.caption.color
        }
    }

    private val content = linearContainer {
        height = wrap()
        orientation = Orientation.Vertical
    }

    //endregion


    init {
        orientation = Orientation.Vertical
        style = Style { theme ->
            backgroundColor = theme.palette.surface
            radius = theme.radius.md
        }

        titleBar += titleText
        titleBar += chevron

        // Divider
        titleBar += UIBox().apply {
            height = 2f.px
            anchor = Anchor.BottomCenter
            origin = Anchor.BottomCenter
            style = Style { theme ->
                backgroundColor = theme.palette.divider
            }
        }

        +content
    }


    override fun onUpdate(clock: TickingClock) {

        content.height = wrap(percentage = clock.interpolateFloat(
            duration = 0.2f,
            start = (content.height as Dimension.ContentSizeFraction).factor,
            end = if (isExpanded) 1f else 0f
        ))

        val chevronTargetRotation = if (isExpanded) 0f else 180f
        chevron.rotationZ = clock.interpolateFloat(
            duration = 0.2f,
            start = chevron.rotationZ,
            end = chevronTargetRotation
        )

        super.onUpdate(clock)
    }


    /**
     * Builds the content of the card using the internal content container.
     */
    fun content(block: UILinearContainer.() -> Unit) {
        content.block()
    }

    /**
     * Collapses the card content.
     */
    fun collapse() {
        isExpanded = false
    }

    /**
     * Expands the card content.
     */
    fun expand() {
        isExpanded = true
    }

}