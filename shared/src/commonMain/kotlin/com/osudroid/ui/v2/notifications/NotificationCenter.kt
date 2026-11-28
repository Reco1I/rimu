package com.osudroid.ui.v2.notifications

import com.osudroid.notifications.Notification
import com.osudroid.notifications.Notifications
import com.osudroid.resources.Res
import com.osudroid.utils.currentTimeMillis
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Background
import com.reco1l.verktex.data.ClipMode
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.px
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.Button
import com.reco1l.verktex.ui.Modal
import com.reco1l.verktex.ui.TextButton
import com.reco1l.verktex.ui.container.LinearContainer
import com.reco1l.verktex.ui.container.container
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.container.scrollableContainer
import com.reco1l.verktex.ui.container.ScrollableContainer
import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.ui.text.Text
import com.reco1l.verktex.ui.text.text
import com.reco1l.verktex.ui.textButton
import com.reco1l.verktex.ui.themed

/**
 * The list of [Notifications], below the button that opens it ([trigger]): the tasks running, with
 * their progress, and the messages and tasks that ended, until they're cleared, closed or swiped
 * away.
 */
class NotificationCenter(private val trigger: Button) : Modal() {

    private lateinit var list: LinearContainer
    private lateinit var emptyText: Text
    private lateinit var scroll: ScrollableContainer

    // The row of each notification shown, in the order of the list.
    private val rows = LinkedHashMap<Notification, NotificationRow>()

    private var shownRevision = -1

    // The ages of the rows are updated every few seconds.
    private var ageUpdateTime = 0f


    init {
        // Without the dimmed backdrop of modals.
        background = Background.Color(Color4.Transparent)
        // Placed below its trigger, in the modal's space over the whole window.
        isCardInSafeArea = false

        card.apply {
            anchor = Anchor.TopLeft
            origin = Anchor.TopRight
            scaleOrigin = Anchor.Center
            width = 360f.dip
            padding = Padding(12f.dip)

            linearContainer {
                width = Dimension.FillAvailable
                orientation = LinearContainer.Orientation.Vertical
                spacing = 8f.dip

                container {
                    width = Dimension.FillAvailable

                    text {
                        anchor = Anchor.CenterLeft
                        origin = Anchor.CenterLeft
                        text = Res.string.notifications_title
                        color = themed { palette.accent }
                    }

                    textButton {
                        anchor = Anchor.CenterRight
                        origin = Anchor.CenterRight
                        variant = Button.Variant.Ghost
                        density = Button.Density.Small
                        text = Res.string.notifications_clear
                        onActionUp = { Notifications.clearFinished() }
                    }
                }

                emptyText = text {
                    width = Dimension.FillAvailable
                    alignment = Anchor.Center
                    padding = Padding(top = 16f.dip, bottom = 16f.dip)
                    fontSettings = themed { typography.smallBody }
                    color = themed { palette.contentSecondary }
                    text = Res.string.notifications_empty
                }

                scroll = scrollableContainer {
                    width = Dimension.FillAvailable
                    // The rows scrolled out of it stay under the header.
                    clipping = ClipMode.Bounds
                    scrollAxes = Axis.Y
                    showHorizontalIndicator = false

                    list = linearContainer {
                        width = Dimension.FillAvailable
                        orientation = LinearContainer.Orientation.Vertical
                        spacing = 6f.dip
                    }
                }
            }
        }
    }

    override fun onShow() {
        placeBelowTrigger()
        Notifications.markAllRead()
        updateRows()
        super.onShow()
    }

    // Below the button that opens this modal, right edges aligned, as the music player.
    private fun placeBelowTrigger() {
        val (sceneX, sceneY) = trigger.toSceneSpace(trigger.bounds.width, trigger.bounds.height)

        // The card is laid out over the whole modal, as the trigger is placed.
        val (x, y) = toLocalSpaceFromScene(sceneX, sceneY)

        val cardY = y + 12f.dip.toPixels()
        card.x = x.px
        card.y = cardY.px

        // The list scrolls within what's left of the window below it, the header and the margins.
        val available = bounds.height - safeArea.w - cardY - 96f.dip.toPixels()
        scroll.maxHeight = available.coerceAtLeast(96f.dip.toPixels()).px
    }

    override fun onUpdate(clock: FrameClock) {
        if (isShown) {
            placeBelowTrigger()

            ageUpdateTime += clock.elapsedFrameTime

            // What's posted while it's open is seen.
            if (Notifications.revision != shownRevision || ageUpdateTime >= 5f) {
                if (Notifications.unreadCount > 0) {
                    Notifications.markAllRead()
                }
                updateRows()
            }
        }

        super.onUpdate(clock)
    }

    /**
     * Shows the notifications as they are now, reusing the rows of those already shown.
     */
    private fun updateRows() {
        shownRevision = Notifications.revision
        ageUpdateTime = 0f

        val entries = Notifications.entries
        val now = currentTimeMillis()

        // The order changes only when one is posted or removed.
        if (entries != rows.keys.toList()) {
            val previous = HashMap(rows)
            rows.clear()
            list.clearChildren()

            for (notification in entries) {
                val row = previous[notification] ?: createRow()
                rows[notification] = row
                list += row
            }
        }

        for ((notification, row) in rows) {
            row.title = titleOf(notification, now)
            row.text = notification.text
            row.progress = notification.progress
            row.isIndeterminate = notification.isIndeterminate
            // The action may open a scene, which the center would cover.
            row.setAction(notification.actionLabel) {
                hide()
                notification.onAction()
            }

            // Running tasks stay until they end, the others can be closed or swiped away.
            row.isDismissible = notification.state != Notification.State.Active
            row.onDismiss = { notification.dismiss() }
        }

        emptyText.isVisible = entries.isEmpty()
    }

    private fun createRow() = NotificationRow().apply {
        width = Dimension.FillAvailable
        maxWidth = null
        background = Background.Color(themed { palette.controlInactiveBackground })
    }

    companion object {

        /**
         * The title of a notification, with how long ago it ended for those that did.
         */
        fun titleOf(notification: Notification, now: Long): CharSequence {
            if (notification.state == Notification.State.Active) {
                return notification.title
            }

            val minutes = (now - notification.time) / 60_000
            val age = when {
                minutes < 1 -> Res.string.notifications_just_now
                minutes < 60 -> Res.string.notifications_minutes_ago.format(minutes)
                else -> Res.string.notifications_hours_ago.format(minutes / 60)
            }
            return "${notification.title} · $age"
        }
    }
}
