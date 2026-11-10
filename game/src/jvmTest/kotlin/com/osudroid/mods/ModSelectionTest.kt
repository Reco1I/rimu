package com.osudroid.mods

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModSelectionTest {
    private val changes = mutableListOf<List<Mod>>()

    init {
        ModSelection.clear()
        ModSelection.addListener(this) { changes += it }
    }

    @After
    fun tearDown() {
        ModSelection.removeListener(this)
        ModSelection.clear()
    }

    @Test
    fun `Test adding a mod notifies it`() {
        val hidden = ModHidden()
        ModSelection.add(hidden)

        assertTrue(ModHidden::class in ModSelection.mods)
        assertEquals(listOf(listOf<Mod>(hidden)), changes)
    }

    @Test
    fun `Test adding a selected mod does nothing`() {
        val hidden = ModHidden()
        ModSelection.add(hidden)
        ModSelection.add(hidden)

        assertEquals(1, changes.size)
    }

    @Test
    fun `Test adding a mod notifies the incompatible mods it unselects`() {
        val doubleTime = ModDoubleTime()
        val halfTime = ModHalfTime()
        ModSelection.add(doubleTime)
        ModSelection.add(halfTime)

        assertFalse(ModDoubleTime::class in ModSelection.mods)
        assertEquals(listOf(doubleTime, halfTime), changes.last())
    }

    @Test
    fun `Test removing a mod notifies it`() {
        val hidden = ModHidden()
        ModSelection.add(hidden)
        ModSelection.remove(ModHidden::class)
        ModSelection.remove(ModHidden::class)

        assertTrue(ModSelection.mods.isEmpty())
        assertEquals(listOf(listOf<Mod>(hidden), listOf<Mod>(hidden)), changes)
    }

    @Test
    fun `Test clearing keeps the mods asked`() {
        ModSelection.add(ModHidden())
        ModSelection.add(ModScoreV2())
        ModSelection.clear { it is ModScoreV2 }

        assertEquals(setOf(ModScoreV2::class), ModSelection.mods.keys.toSet())
        assertEquals(1, changes.last().size)
    }
}
