package com.osudroid.notifications

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Without the engine nothing is shown, the notifications are only kept.
 */
class NotificationsTest {

    @Before
    fun setUp() = Notifications.reset()

    @After
    fun tearDown() = Notifications.reset()


    @Test
    fun testMessagesAreListedNewestFirstAndUnread() {
        Notifications.post("First", "a")
        val second = Notifications.post("Second", "b", isError = true)

        assertEquals(listOf("Second", "First"), Notifications.entries.map { it.title.toString() })
        assertEquals(Notification.State.Failed, second.state)
        assertEquals(2, Notifications.unreadCount)
        assertEquals(0, Notifications.activeCount)

        Notifications.markAllRead()
        assertEquals(0, Notifications.unreadCount)
    }

    @Test
    fun testATaskRunsUntilItEnds() {
        val task = Notifications.startTask("Task", "Running", isIndeterminate = false)
        task.progress = 0.5f
        task.setAction("Cancel") {}

        assertEquals(Notification.State.Active, task.state)
        assertEquals(1, Notifications.activeCount)

        task.complete("Done")

        assertEquals(Notification.State.Completed, task.state)
        assertEquals("Done", task.text.toString())
        assertNull(task.progress)
        assertNull(task.actionLabel)
        assertEquals(0, Notifications.activeCount)

        // It already ended.
        task.fail("Failed")
        assertEquals(Notification.State.Completed, task.state)
    }

    @Test
    fun testATaskThatFails() {
        val task = Notifications.startTask("Task", "Running")
        task.fail("Broken")

        assertEquals(Notification.State.Failed, task.state)
        assertEquals("Broken", task.text.toString())
    }

    @Test
    fun testDismissingForgetsIt() {
        val task = Notifications.startTask("Task", "Running")
        task.dismiss()

        assertTrue(Notifications.entries.isEmpty())
    }

    @Test
    fun testChangesChangeTheRevision() {
        val task = Notifications.startTask("Task", "Running")
        val revision = Notifications.revision

        task.text = "Still running"
        assertNotEquals(revision, Notifications.revision)
    }

    @Test
    fun testClearingKeepsTheRunningTasks() {
        Notifications.post("Message", "a")
        val task = Notifications.startTask("Task", "Running")

        Notifications.clearFinished()

        assertEquals(listOf(task), Notifications.entries)
    }

    @Test
    fun testTheOldestFinishedOnesAreForgottenFirst() {
        val task = Notifications.startTask("Task", "Running")
        repeat(Notifications.MAX_ENTRIES) { Notifications.post("Message $it", "") }

        val entries = Notifications.entries
        assertEquals(Notifications.MAX_ENTRIES, entries.size)
        assertTrue(task in entries)
        assertFalse(entries.any { it.title == "Message 0" })
        assertEquals("Message ${Notifications.MAX_ENTRIES - 1}", entries.first().title.toString())
    }
}
