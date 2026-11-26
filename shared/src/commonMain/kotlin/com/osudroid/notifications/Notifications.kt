package com.osudroid.notifications

import com.osudroid.utils.currentTimeMillis
import com.reco1l.verktex.ui.toast.Toasts
import kotlin.concurrent.Volatile
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.incrementAndFetch
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * What the game tells the player outside of a scene: messages and the progress of the tasks that
 * run in the background (imports, difficulty calculation, downloads).
 *
 * Each message pops up as a [toast][Toasts], and so does a task when it starts and when it ends,
 * its progress showing in the
 * [notification center][com.osudroid.ui.v2.notifications.NotificationCenter], which lists them all
 * until they're cleared. It can be posted and changed from any thread, and works without the engine (e.g.
 * in tests), where nothing is shown.
 */
object Notifications {

    /**
     * How many notifications are kept, the oldest finished ones are forgotten first.
     */
    const val MAX_ENTRIES = 50

    private val lock = SynchronizedObject()

    // Newest first.
    private val list = ArrayList<Notification>()

    private val revisionCounter = AtomicInt(0)


    /**
     * Changes each time a notification is posted, changed or removed, so that the UI knows when to
     * show them again.
     */
    val revision: Int
        get() = revisionCounter.load()

    /**
     * The notifications, newest first.
     */
    val entries: List<Notification>
        get() = synchronized(lock) { list.toList() }

    /**
     * How many tasks are running.
     */
    val activeCount: Int
        get() = synchronized(lock) { list.count { it.state == Notification.State.Active } }

    /**
     * How many notifications weren't seen in the notification center.
     */
    val unreadCount: Int
        get() = synchronized(lock) { list.count { !it.isRead } }


    /**
     * Posts a message.
     *
     * @param isError Whether it tells of something that failed.
     * @param action The label of a button of the notification center's row, which calls
     * [onAction], none if null.
     */
    fun post(
        title: CharSequence,
        text: CharSequence,
        isError: Boolean = false,
        action: CharSequence? = null,
        onAction: () -> Unit = {}
    ): Notification {
        val notification = Notification(title, text, if (isError) Notification.State.Failed else Notification.State.Info)
        if (action != null) {
            notification.setAction(action, onAction)
        }
        add(notification)
        notification.showToast()
        return notification
    }

    /**
     * Posts a task, which runs until [Notification.complete] or [Notification.fail] are called.
     */
    fun startTask(title: CharSequence, text: CharSequence, isIndeterminate: Boolean = true): Notification {
        val notification = Notification(title, text, Notification.State.Active)
        notification.isIndeterminate = isIndeterminate
        add(notification)
        notification.showToast()
        return notification
    }

    /**
     * Marks every notification as seen.
     */
    fun markAllRead() {
        synchronized(lock) { list.forEach { it.isRead = true } }
        changed()
    }

    /**
     * Removes the notifications that aren't running tasks.
     */
    fun clearFinished() {
        synchronized(lock) { list.removeAll { it.state != Notification.State.Active } }
        changed()
    }

    /**
     * Removes every notification, for tests.
     */
    internal fun reset() {
        synchronized(lock) { list.clear() }
        changed()
    }


    private fun add(notification: Notification) {
        synchronized(lock) {
            list.add(0, notification)

            while (list.size > MAX_ENTRIES) {
                // The oldest finished one, or the oldest one if they're all running.
                val index = list.indexOfLast { it.state != Notification.State.Active }.takeIf { it >= 0 } ?: list.lastIndex
                list.removeAt(index)
            }
        }
        changed()
    }

    internal fun remove(notification: Notification) {
        synchronized(lock) { list.remove(notification) }
        changed()
    }

    internal fun changed() {
        revisionCounter.incrementAndFetch()
    }
}

/**
 * A message or a task of [Notifications]. Its properties can be changed from any thread.
 */
class Notification internal constructor(title: CharSequence, text: CharSequence, state: State) {

    enum class State {
        /** A message. */
        Info,
        /** A task that runs. */
        Active,
        /** A task that ended. */
        Completed,
        /** A task that failed, or an error message. */
        Failed
    }

    /**
     * When it was posted or, for a task that ended, when it did, in milliseconds since the epoch.
     */
    @Volatile
    var time = currentTimeMillis()
        private set

    /**
     * The state of the message or the task.
     */
    @Volatile
    var state = state
        private set

    /**
     * Whether it was seen in the notification center.
     */
    @Volatile
    var isRead = false
        internal set

    /**
     * The title above the message.
     */
    var title: CharSequence = title
        set(value) {
            field = value
            Notifications.changed()
        }

    /**
     * The message.
     */
    var text: CharSequence = text
        set(value) {
            field = value
            Notifications.changed()
        }

    /**
     * The progress of a task, from 0 to 1, or null for none.
     */
    @Volatile
    var progress: Float? = null
        set(value) {
            field = value
            Notifications.changed()
        }

    /**
     * Whether a task runs without a known progress.
     */
    @Volatile
    var isIndeterminate = false
        set(value) {
            field = value
            Notifications.changed()
        }

    /**
     * The label of its button in the notification center, null for none. A task's is removed
     * when it ends.
     */
    @Volatile
    var actionLabel: CharSequence? = null
        private set

    /**
     * What its button does.
     */
    @Volatile
    var onAction: () -> Unit = {}
        private set


    /**
     * Shows a button labeled [label] in the notification center, which calls [onClick], or
     * removes it when [label] is null.
     */
    fun setAction(label: CharSequence?, onClick: () -> Unit = {}) {
        actionLabel = label
        onAction = onClick
        Notifications.changed()
    }

    /**
     * Ends the task, telling [text].
     */
    fun complete(text: CharSequence = this.text) = finish(State.Completed, text)

    /**
     * Ends the task as failed, telling [text].
     */
    fun fail(text: CharSequence = this.text) = finish(State.Failed, text)

    /**
     * Forgets it, ending a task without telling anything, e.g. one that had nothing to do.
     */
    fun dismiss() {
        if (state == State.Active) {
            state = State.Completed
        }
        Notifications.remove(this)
    }

    private fun finish(state: State, text: CharSequence) {
        if (this.state != State.Active) {
            return
        }

        this.state = state
        time = currentTimeMillis()
        actionLabel = null
        onAction = {}
        progress = null
        isIndeterminate = false
        isRead = false
        this.text = text

        showToast()
    }

    // Pops up as a toast, after those shown before it.
    internal fun showToast() = Toasts.show(text, Toasts.Long, title)
}
