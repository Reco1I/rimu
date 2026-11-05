package com.reco1l.verktex.testing

import com.reco1l.verktex.Verktex
import com.reco1l.verktex.ui.UIScene
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UITestStageTest {

    private class RecordingScene(private val events: MutableList<String>, private val name: String) : UIScene() {
        override fun onEnter() {
            events += "$name enter"
        }

        override fun onLeave() {
            events += "$name leave"
        }
    }

    private val events = mutableListOf<String>()
    private lateinit var stage: UITestStage

    @Before
    fun setUp() {
        stage = UITestStage {}
        Verktex.sceneHost = stage
    }

    @After
    fun tearDown() {
        Verktex.sceneHost = null
    }

    @Test
    fun `Scenes pushed to the engine go into the host`() {
        stage.allowsNavigation = true
        val first = RecordingScene(events, "first")
        val second = RecordingScene(events, "second")

        Verktex.pushScene(first)
        Verktex.pushScene(second)

        assertSame(second, Verktex.scene)
        assertSame(stage, second.parent)
        assertTrue(second.isEmbedded)
        assertFalse(first.isVisible)
        assertEquals(listOf("first enter", "first leave", "second enter"), events)
    }

    @Test
    fun `Popping goes back through the host scenes only`() {
        stage.allowsNavigation = true
        val first = RecordingScene(events, "first")
        Verktex.pushScene(first)

        assertSame(first, Verktex.popScene())
        assertNull(first.parent)
        assertNull(Verktex.popScene())
        assertNull(stage.currentScene)
    }

    @Test
    fun `A scene can't leave nor open others unless the stage allows navigation`() {
        val scene = RecordingScene(events, "scene")
        val other = RecordingScene(events, "other")

        Verktex.pushScene(scene)
        Verktex.pushScene(other)

        assertSame(scene, Verktex.scene)
        assertNull(other.parent)
        assertNull(Verktex.popScene())
        assertSame(stage, scene.parent)
        assertEquals(listOf("scene enter"), events)
    }

    @Test
    fun `A scene of a previous stage moves to the new one`() {
        val scene = RecordingScene(events, "scene")
        val oldStage = UITestStage {}
        oldStage.pushScene(scene)

        stage.pushScene(scene)

        assertSame(stage, scene.parent)
        assertEquals(0, oldStage.childCount)
    }

    @Test
    fun `Disposing pops the scenes and runs the dispose blocks in reverse`() {
        stage.pushScene(RecordingScene(events, "scene"))
        stage.onDispose { events += "first dispose" }
        stage.onDispose { events += "second dispose" }

        stage.dispose()

        assertNull(stage.currentScene)
        assertEquals(listOf("scene enter", "scene leave", "second dispose", "first dispose"), events)
    }
}
