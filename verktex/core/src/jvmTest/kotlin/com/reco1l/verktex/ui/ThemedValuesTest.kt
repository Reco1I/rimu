package com.reco1l.verktex.ui

import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Border
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.px
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.shape.UIBox
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Themed values are resolved with the theme of their component, and again when it changes.
 */
class ThemedValuesTest {

    private val redTheme = Theme(palette = Theme.ColorPalette(Color4.Red500), radius = breakpoints(4f))
    private val blueTheme = Theme(palette = Theme.ColorPalette(Color4.Blue500), radius = breakpoints(8f))

    private lateinit var previousTheme: Theme

    @Before
    fun setUp() {
        previousTheme = Verktex.theme
        Verktex.theme = redTheme
    }

    @After
    fun tearDown() {
        Verktex.theme = previousTheme
    }

    private fun breakpoints(value: Float) = Theme.FixedSizeBreakpoints(value.px, value.px, value.px, value.px, value.px)


    @Test
    fun themedValuesResolveWhenAssigned() {
        val box = UIBox()
        box.color = themed { palette.accent }
        box.radius = themed { radius.md }

        assertEquals(Color4.Red500, box.color)
        assertEquals(4f.px, box.radius)
    }

    @Test
    fun themedValuesResolveAgainWhenTheGlobalThemeChanges() {
        val box = UIBox()
        box.color = themed { palette.accent }

        Verktex.theme = blueTheme
        box.applyTheme()

        assertEquals(Color4.Blue500, box.color)
    }

    @Test
    fun themedValuesFollowTheThemeOfTheirParent() {
        val parent = UIContainer().apply { theme = blueTheme }
        val box = UIBox()
        box.radius = themed { radius.md }

        parent += box
        assertEquals(8f.px, box.radius)

        parent.theme = redTheme
        assertEquals(4f.px, box.radius)
    }

    @Test
    fun plainValuesReplaceTheBinding() {
        val box = UIBox().apply { theme = redTheme }
        box.color = themed { palette.accent }
        box.color = Color4.White

        box.theme = blueTheme

        assertEquals(Color4.White, box.color)
    }

    @Test
    fun changingTheAlphaKeepsTheColorThemed() {
        val box = UIBox().apply { theme = redTheme }
        box.color = themed { palette.accent }
        box.alpha = 0.5f

        box.theme = blueTheme

        assertEquals(Color4.Blue500.alpha(0.5f), box.color)
    }

    @Test
    fun nestedThemedValuesAreResolved() {
        val box = UIBox().apply { theme = redTheme }
        box.padding = Padding(themed { radius.md })
        box.border = Border(1f.px, themed { palette.accent })

        box.theme = blueTheme

        assertEquals(Padding(8f.px), box.padding)
        assertEquals(Border(1f.px, Color4.Blue500), box.border)
        assertFalse(box.padding.isThemed)
    }

    @Test
    fun boundValuesAreAppliedAgain() {
        val box = UIBox().apply { theme = redTheme }
        var applied: Color4? = null
        box.bindTheme("key", { palette.accent }) { applied = it }
        assertEquals(Color4.Red500, applied)

        box.theme = blueTheme
        assertEquals(Color4.Blue500, applied)

        box.unbindTheme("key")
        box.theme = redTheme
        assertEquals(Color4.Blue500, applied)
    }

    @Test
    fun themedValuesFallBackToTheGlobalTheme() {
        val radius = themed { radius.md }
        assertTrue(radius.isThemed)
        assertEquals(4f, radius.toPixels())

        Verktex.theme = blueTheme
        assertEquals(8f, radius.toPixels())
    }

    @Test
    fun plainColorsKeepTheirValueSemantics() {
        val color = Color4(0.1f, 0.2f, 0.3f, 0.4f)
        assertEquals(Color4(0.1f, 0.2f, 0.3f, 0.4f), color)
        assertEquals(color.hashCode(), Color4(0.1f, 0.2f, 0.3f, 0.4f).hashCode())
        assertEquals(Color4(0.1f, 0.2f, 0.3f, 1f), color.alpha(1f))
        assertNotEquals(color, themed { color })

        val (r, g, b, a) = color
        assertEquals(listOf(0.1f, 0.2f, 0.3f, 0.4f), listOf(r, g, b, a))
    }
}
