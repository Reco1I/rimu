package com.osudroid.verktex

import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.LayoutConstraints
import com.reco1l.verktex.data.Vec4
import com.reco1l.verktex.data.px
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.shape.UIBox
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A static container lays out once and then skips the pass until something it depends on changes.
 * Frames are driven the way a scene does: update, then measure and layout from the root.
 */
class StaticLayoutTest {

    private class FixedClock : FrameClock() {
        override val time = 0f
        override val isRunning = true
        override val elapsedFrameTime = 1f / 60f
        override val lastFrameTime = 0f
    }

    private class CountingContainer : UIContainer() {
        var layoutPasses = 0

        override fun onLayout(bounds: Vec4) {
            layoutPasses++
            super.onLayout(bounds)
        }
    }

    private val clock = FixedClock()
    private val windowBounds = Vec4(0f, 0f, 800f, 600f)
    private val windowConstraints = LayoutConstraints(0f, 800f, 0f, 600f)

    private fun frame(root: UIContainer) {
        root.update(clock)
        root.measure(windowConstraints)
        root.layout(windowBounds)
    }

    private fun box(size: Float) = UIBox().apply {
        width = size.px
        height = size.px
    }

    private fun staticContainer(vararg children: UIBox) = CountingContainer().apply {
        staticLayout = true
        children.forEach { this += it }
    }

    @Test
    fun staticLayoutSkipsIdleFrames() {
        val container = staticContainer(box(10f), box(20f))

        frame(container)
        assertEquals(1, container.layoutPasses)
        assertFalse(container.layoutInvalid)

        repeat(5) { frame(container) }
        assertEquals(1, container.layoutPasses)
    }

    @Test
    fun staticLayoutRelaysOutWhenAChildMoves() {
        val moved = box(10f)
        val container = staticContainer(moved, box(20f))

        frame(container)
        frame(container)
        val before = moved.toSceneSpace(0f, 0f)

        moved.x = 30f.px
        // Moving a child invalidates the measure up the tree, the measure pass then invalidates the static layout.
        assertTrue(container.measureInvalid)

        frame(container)
        assertEquals(2, container.layoutPasses)
        assertNotEquals(before, moved.toSceneSpace(0f, 0f))
        assertEquals(30f, moved.toSceneSpace(0f, 0f).x, 0f)

        frame(container)
        assertEquals(2, container.layoutPasses)
    }

    @Test
    fun staticLayoutRelaysOutWhenAChildAnchorChanges() {
        val child = box(10f)
        val container = staticContainer(child, box(20f))

        frame(container)
        child.anchor = Anchor.BottomRight
        frame(container)

        assertEquals(2, container.layoutPasses)
    }

    @Test
    fun staticLayoutRelaysOutWhenAChildIsAdded() {
        val container = staticContainer(box(10f))

        frame(container)
        container += box(20f).apply { x = 50f.px }
        frame(container)

        assertEquals(2, container.layoutPasses)
    }

    @Test
    fun nonStaticLayoutRunsEveryFrame() {
        val container = CountingContainer().apply {
            this += box(10f)
        }

        repeat(3) { frame(container) }

        assertEquals(3, container.layoutPasses)
        assertTrue(container.layoutInvalid)
    }
}
