package com.reco1l.verktex

import com.reco1l.verktex.ui.UIScene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class SceneStackTest {

    private class RecordingScene(private val events: MutableList<String>, private val name: String) : UIScene() {
        override fun onEnter() {
            events += "$name enter"
        }

        override fun onLeave() {
            events += "$name leave"
        }
    }

    @Test
    fun `Resetting the scenes leaves only the new one`() {
        val events = mutableListOf<String>()
        val splash = RecordingScene(events, "splash")
        val loading = RecordingScene(events, "loading")
        val menu = RecordingScene(events, "menu")

        Verktex.pushScene(splash)
        Verktex.pushScene(loading)
        events.clear()

        Verktex.resetScenes(menu)

        assertSame(menu, Verktex.scene)
        assertEquals(listOf("loading leave", "menu enter"), events)

        // The scenes it replaced can't be gone back to.
        assertSame(menu, Verktex.popScene())
        assertSame(UIScene.EmptyScene, Verktex.scene)
    }
}
