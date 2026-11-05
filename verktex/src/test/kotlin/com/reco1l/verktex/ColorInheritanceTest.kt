package com.osudroid.verktex

import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.data.Vec4
import com.reco1l.verktex.graphics.DrawCommand
import com.reco1l.verktex.graphics.GraphicsBuffer
import com.reco1l.verktex.graphics.GraphicsContext
import com.reco1l.verktex.graphics.GraphicsDevice
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.graphics.GraphicsShader
import com.reco1l.verktex.graphics.GraphicsTexture
import com.reco1l.verktex.graphics.GraphicsWindow
import com.reco1l.verktex.ui.UIComponent
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.shape.UIBox
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * A component can opt in to multiplying its RGB into its descendants, alpha is always inherited.
 */
class ColorInheritanceTest {

    private object FakeDevice : GraphicsDevice {
        override val capabilities = GraphicsDevice.Capabilities(maxTextureSize = 4096f)
        override fun createBuffer(): GraphicsBuffer = throw UnsupportedOperationException()
        override fun createShader(source: GraphicsShader.Source): GraphicsShader = throw UnsupportedOperationException()
        override fun createTexture(width: Float, height: Float): GraphicsTexture = throw UnsupportedOperationException()
        override fun createExternalTexture(width: Float, height: Float): GraphicsTexture = throw UnsupportedOperationException()
    }

    private object FakeWindow : GraphicsWindow {
        override var windowWidth = 1280
        override var windowHeight = 720
        override var insets = GraphicsWindow.Insets(Vec4(0f, 0f, 1280f, 720f), Vec2(0f, 0f))
        override fun shouldClose() = false
        override fun pollEvents() = Unit
    }

    private class FakeRenderer : GraphicsRenderer() {
        override val device: GraphicsDevice = FakeDevice
        override val context: GraphicsContext get() = throw UnsupportedOperationException()
        override val window: GraphicsWindow = FakeWindow
        override var version = "fake"
        override fun drawCommand(command: DrawCommand) = Unit
    }

    private class ProbeBox : UIBox() {
        var seen: Color4? = null
        override fun onDraw(renderer: GraphicsRenderer) {
            seen = renderer.transformColor(Color4.White)
        }
    }

    @Test
    fun tintChildrenMultipliesRgb() {
        val renderer = FakeRenderer()
        renderer.pushTint(Color4(0.5f, 0.5f, 1f, 1f))

        assertEquals(Color4(0.5f, 0.5f, 1f, 1f), renderer.transformColor(Color4.White))
    }

    @Test
    fun untintedPushKeepsRgb() {
        val renderer = FakeRenderer()
        renderer.pushColor(Color4(0.5f, 0.5f, 1f, 0.5f))

        assertEquals(Color4(1f, 1f, 1f, 0.5f), renderer.transformColor(Color4.White))
    }

    @Test
    fun tintStacksAndPops() {
        val renderer = FakeRenderer()
        renderer.pushTint(Color4(0.5f, 1f, 1f, 1f))
        renderer.pushTint(Color4(1f, 0.5f, 1f, 1f))

        val both = renderer.transformColor(Color4.White)
        assertEquals(0.5f, both.red, 0f)
        assertEquals(0.5f, both.green, 0f)

        renderer.popTint()
        val outer = renderer.transformColor(Color4.White)
        assertEquals(0.5f, outer.red, 0f)
        assertEquals(1f, outer.green, 0f)

        renderer.popTint()
        assertEquals(1f, renderer.transformColor(Color4.White).red, 0f)
    }

    /** Records the colour transform in effect when the component starts drawing, as its background would see it. */
    private class ProbeContainer : UIContainer() {
        var atBackground: Color4? = null
        override fun drawContent(renderer: GraphicsRenderer) {
            atBackground = renderer.transformColor(Color4.White)
            super.drawContent(renderer)
        }
    }

    private fun drawn(component: UIComponent) {
        val renderer = FakeRenderer()
        renderer.begin()
        component.draw(renderer)
        renderer.end()
    }

    @Test
    fun tintingLeafDrawsItsOwnColourOnce() {
        val leaf = ProbeBox().apply {
            tintChildren = true
            color = Color4(0.5f, 0.5f, 1f, 1f)
        }

        drawn(leaf)

        // The leaf applies its own colour through contentColor, the renderer must add nothing.
        assertEquals(Color4.White, leaf.seen)
    }

    @Test
    fun tintingContainerDoesNotTintItself() {
        val container = ProbeContainer().apply {
            tintChildren = true
            color = Color4(0.5f, 0.5f, 1f, 1f)
        }

        drawn(container)

        assertEquals(Color4.White, container.atBackground)
    }

    @Test
    fun untintedContainerLeavesChildUntinted() {
        val child = ProbeBox()
        val container = UIContainer().apply { color = Color4(0.5f, 0.5f, 1f, 1f) }
        container += child

        drawn(container)

        assertEquals(Color4.White, child.seen)
    }

    @Test
    fun tintDoesNotLeakPastTheContainer() {
        val renderer = FakeRenderer()
        val container = UIContainer().apply {
            tintChildren = true
            color = Color4(0.5f, 0.5f, 1f, 1f)
        }
        renderer.begin()
        container.draw(renderer)
        renderer.end()

        assertEquals(Color4.White, renderer.transformColor(Color4.White))
    }

    @Test
    fun componentFlagReachesChildDraw() {
        val renderer = FakeRenderer()
        val child = ProbeBox()
        val container = UIContainer().apply {
            tintChildren = true
            color = Color4(0.5f, 0.5f, 1f, 1f)
        }
        container += child

        renderer.begin()
        container.draw(renderer)
        renderer.end()

        assertEquals(Color4(0.5f, 0.5f, 1f, 1f), child.seen)
    }
}
