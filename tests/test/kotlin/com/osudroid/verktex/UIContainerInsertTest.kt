package com.osudroid.verktex

import com.reco1l.verktex.data.Vec4
import com.reco1l.verktex.ui.shape.UIBox
import com.reco1l.verktex.ui.container.UIContainer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Children can be inserted at an index, so later attached objects can sit behind earlier ones.
 */
class UIContainerInsertTest {

    private class CountingBox : UIBox() {
        var attached = 0
        override fun onAttached() {
            super.onAttached()
            attached++
        }
    }

    @Test
    fun addAtIndexPlacesTheChildThere() {
        val container = UIContainer()
        val a = UIBox()
        val b = UIBox()
        val c = UIBox()
        val d = UIBox()
        container += a
        container += b

        container.add(0, c)
        container.add(1, d)

        assertEquals(listOf(c, d, a, b), container.toList())
    }

    @Test
    fun addToBackPutsTheChildFirst() {
        val container = UIContainer()
        val a = UIBox()
        val b = UIBox()
        container += a

        container.addToBack(b)

        assertSame(b, container[0])
        assertSame(a, container[1])
    }

    @Test
    fun addAtIndexAttachesAndInvalidates() {
        val container = UIContainer()
        val child = CountingBox()

        container.add(0, child)

        assertSame(container, child.parent)
        assertEquals(1, child.attached)
    }

    @Test
    fun moveToBackInvalidatesAStaticContainer() {
        val container = UIContainer().apply { staticLayout = true }
        val a = UIBox()
        val b = UIBox()
        container += a
        container += b
        container.layout(Vec4(0f, 0f, 100f, 100f))
        assertFalse(container.layoutInvalid)

        with(container) { b.moveToBack() }

        assertTrue(container.layoutInvalid)
    }
}
