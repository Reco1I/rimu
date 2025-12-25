package com.osudroid.verktex

import com.reco1l.verktex.Verktex
import com.reco1l.verktex.input.Input
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.PointerEvent
import com.reco1l.verktex.input.PointerEvent.Action
import com.reco1l.verktex.input.PointerEvent.Button
import com.reco1l.verktex.ui.UIScene
import com.reco1l.verktex.ui.container.UIClickableContainer
import com.reco1l.verktex.ui.container.UIScrollableContainer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pointer events carry their time, and a cancel is its own action that releases like an unknown one.
 */
class PointerEventTest {

    private class RecordingScene : UIScene() {
        val actions = mutableListOf<Action>()
        val times = mutableListOf<Long>()

        override fun onInputEvent(event: InputEvent): Boolean {
            if (event is PointerEvent) {
                actions += event.action
                times += event.time
            }
            return true
        }
    }

    private lateinit var scene: RecordingScene

    @Before
    fun pushScene() {
        scene = RecordingScene()
        Verktex.pushScene(scene)
    }

    @After
    fun popScene() {
        Verktex.popScene()
    }

    @Test
    fun dispatchCarriesTheEventTime() {
        Input.dispatchPointerEvent(Action.Down, Button.Left, 0, 1f, 1f, time = 1234)
        Input.dispatchPointerEvent(Action.Up, Button.Left, 0, 1f, 1f)

        assertEquals(listOf(1234L, 0L), scene.times)
    }

    @Test
    fun cancelIsDeliveredAsItsOwnAction() {
        Input.dispatchPointerEvent(Action.Down, Button.Left, 0, 1f, 1f)
        Input.dispatchPointerEvent(Action.Cancel, Button.Left, 0, 1f, 1f)

        assertEquals(listOf(Action.Down, Action.Cancel), scene.actions)
    }

    @Test
    fun cancelReleasesLikeUnknown() {
        fun clickable(release: Action): Pair<Boolean, Int> {
            var cancels = 0
            val container = UIClickableContainer().apply { onActionCancel = { cancels++ } }
            container.onInputEvent(PointerEvent(Action.Down, Button.Left, 0, 1f, 1f))
            assertTrue(container.isPressed)
            val consumed = container.onInputEvent(PointerEvent(release, Button.Left, 0, 1f, 1f))
            assertFalse(consumed)
            return container.isPressed to cancels
        }
        assertEquals(clickable(Action.Unknown), clickable(Action.Cancel))
        assertEquals(false to 1, clickable(Action.Cancel))

        fun scrollable(release: Action): Pair<Int, Boolean> {
            val container = UIScrollableContainer()
            val dragPointerId = UIScrollableContainer::class.java.getDeclaredField("dragPointerId").apply { isAccessible = true }
            container.onInputEvent(PointerEvent(Action.Down, Button.Left, 0, 1f, 1f))
            assertEquals(0, dragPointerId.getInt(container))
            container.onInputEvent(PointerEvent(release, Button.Left, 0, 1f, 1f))
            return dragPointerId.getInt(container) to container.isUserScrolling
        }
        assertEquals(scrollable(Action.Unknown), scrollable(Action.Cancel))
        assertEquals(-1 to false, scrollable(Action.Cancel))
    }
}
