package com.reco1l.verktex.ui

import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.time.FramedClock
import com.reco1l.verktex.time.ManualClock
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ChoreographyTest {

    private class TestComponent : UIComponent() {
        override fun onDraw(renderer: GraphicsRenderer) = Unit
    }

    private lateinit var component: UIComponent
    private lateinit var time: ManualClock
    private lateinit var frames: FramedClock

    @Before
    fun setUp() {
        component = TestComponent()
        time = ManualClock()
        frames = FramedClock(time, false)
        component.clock = time
    }

    private fun seek(to: Float) {
        time.time = to
        frames.processFrame()
        component.update(frames)
    }

    @Test
    fun `Step interpolates between its values`() {
        component.alpha = 0f
        component.choreograph { at(10f).fadeIn(2f) }

        seek(11f)
        assertEquals(0.5f, component.alpha, 1e-4f)

        seek(12f)
        assertEquals(1f, component.alpha, 1e-4f)
    }

    @Test
    fun `Next step starts from where the previous one ended`() {
        component.alpha = 0f
        component.choreograph { at(0f).fadeTo(0.5f, 1f).then().fadeTo(1f, 1f) }

        seek(1.5f)
        assertEquals(0.75f, component.alpha, 1e-4f)
    }

    @Test
    fun `Going back in time restores the earlier state`() {
        component.alpha = 0f
        component.choreograph { at(1f).fadeIn(1f).then().fadeOut(1f) }

        seek(3f)
        assertEquals(0f, component.alpha, 1e-4f)

        seek(1.5f)
        assertEquals(0.5f, component.alpha, 1e-4f)

        seek(0f)
        assertEquals(0f, component.alpha, 1e-4f)
    }

    @Test
    fun `State doesn't depend on the frames in between`() {
        val other = TestComponent()
        other.clock = time

        component.alpha = 0f
        other.alpha = 0f

        component.choreograph { at(0f).fadeIn(1f).then().delay(1f).fadeOut(1f) }
        other.choreograph { at(0f).fadeIn(1f).then().delay(1f).fadeOut(1f) }

        for (i in 0..25) {
            seek(i / 10f)
        }

        time.time = 2.5f
        frames.processFrame()
        other.update(frames)

        assertEquals(component.alpha, other.alpha, 1e-4f)
    }

    @Test
    fun `Callback is called once even if time goes back`() {
        var calls = 0

        component.choreograph { at(0f).fadeOut(1f).after { calls++ } }

        seek(0.5f)
        assertEquals(0, calls)

        seek(1f)
        assertEquals(1, calls)

        seek(0f)
        seek(2f)
        assertEquals(1, calls)
    }

    @Test
    fun `Lifetime end is reported once`() {
        var calls = 0

        component.choreography().apply {
            at(0f).fadeOut(1f)
            lifetimeEnd = 2f
            onLifetimeEnd = OnTransformFinished { calls++ }
        }

        seek(1f)
        assertEquals(0, calls)

        seek(2f)
        seek(3f)
        assertEquals(1, calls)
        assertEquals(true, component.choreography().isFinished)
    }

    @Test
    fun `Finish applies the end state and calls what's left`() {
        var calls = 0

        component.alpha = 1f
        component.choreograph { at(5f).fadeOut(1f).after { calls++ } }

        component.choreography().finish()

        assertEquals(0f, component.alpha, 1e-4f)
        assertEquals(1, calls)
    }

    @Test
    fun `Clear allows reusing it with new steps`() {
        component.alpha = 1f
        component.choreograph { at(0f).fadeOut(1f) }

        seek(1f)
        assertEquals(0f, component.alpha, 1e-4f)

        component.choreography().clear()
        component.choreograph { at(1f).fadeIn(1f) }

        seek(1.5f)
        assertEquals(0.5f, component.alpha, 1e-4f)
    }

    @Test
    fun `Scale and movement are applied`() {
        component.choreograph { at(0f).scaleTo(2f, 1f).moveTo(10f, 20f, 1f).rotateTo(90f, 1f) }

        seek(1f)

        assertEquals(2f, component.scaleX, 1e-4f)
        assertEquals(2f, component.scaleY, 1e-4f)
        assertEquals(10f, component.translationX.resolve(0f), 1e-4f)
        assertEquals(20f, component.translationY.resolve(0f), 1e-4f)
        assertEquals(90f, component.rotationZ, 1e-4f)
    }
}
