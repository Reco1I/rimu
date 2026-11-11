package ru.nsu.ccfit.zuev.osu.scoring

import com.osudroid.math.Vector2
import com.osudroid.mods.ModDoubleTime
import com.osudroid.mods.ModHardRock
import com.osudroid.mods.ModHidden
import com.osudroid.utils.BitSet
import java.io.File
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.nsu.ccfit.zuev.osu.Utils

class ReplayTest {

    private fun recordedReplay(): Replay {
        val replay = Replay(true)
        replay.setBeatmap("Artist - Title", "Hard", "0123456789abcdef0123456789abcdef")
        replay.setObjectCount(3)

        replay.addPress(100, Vector2(10f, 20f), 0)
        replay.addMove(150, Vector2(110f, 220f), 0)
        replay.addUp(200, 0)
        replay.addPress(300, Vector2(400f, 300f), 1)
        replay.addUp(350, 1)

        replay.addObjectResult(0, 12, null)
        replay.addObjectScore(0, ResultType.HIT300)
        replay.addObjectResult(1, -34, BitSet().also { it.set(0); it.set(3); it.set(64) })
        replay.addObjectScore(1, ResultType.HIT100)
        replay.addObjectScore(2, ResultType.MISS)

        replay.stat = StatisticV2().apply {
            time = 1_700_000_000_123L
            hit300k = 1
            hit300 = 2
            hit100k = 3
            hit100 = 4
            hit50 = 5
            misses = 6
            totalScore = 12_345_678_901L
            scoreMaxCombo = 789
            playerName = "player"
            mod.put(ModHardRock())
            mod.put(ModHidden())
            mod.put(ModDoubleTime())
        }

        return replay
    }

    private fun saved(replay: Replay): File {
        val file = File.createTempFile("replay", ".odr")
        file.deleteOnExit()
        replay.save(file.path)
        return file
    }

    @Test
    fun testRoundTrip() {
        val recorded = recordedReplay()
        val file = saved(recorded)

        val loaded = Replay()
        loaded.setObjectCount(3)
        assertTrue(loaded.load(file.path, true))

        assertEquals(Replay.ReplayFormatVersion, loaded.replayVersion)
        assertEquals("Artist - Title", loaded.beatmapsetName)
        assertEquals("Hard", loaded.beatmapName)
        assertEquals("0123456789abcdef0123456789abcdef", loaded.md5)

        val stat = loaded.stat!!
        assertEquals(1_700_000_000_123L, stat.time)
        assertEquals(1, stat.hit300k)
        assertEquals(2, stat.hit300)
        assertEquals(3, stat.hit100k)
        assertEquals(4, stat.hit100)
        assertEquals(5, stat.hit50)
        assertEquals(6, stat.misses)
        assertEquals(12_345_678_901L, stat.totalScore)
        assertEquals(789, stat.scoreMaxCombo)
        assertEquals("player", stat.playerName)
        assertEquals(file.name, stat.replayFilename)
        assertEquals(recorded.stat!!.mod.serializeMods(), stat.mod.serializeMods())

        assertEquals(recorded.cursorMoves.size, loaded.cursorMoves.size)
        for (i in recorded.cursorMoves.indices) {
            val expected = recorded.cursorMoves[i]
            val actual = loaded.cursorMoves[i]
            assertEquals(expected.size, actual.size)

            for (j in 0 until expected.size) {
                val e = expected.movements[j]
                val a = actual.movements[j]
                assertEquals(e.time, a.time)
                assertEquals(e.touchType, a.touchType)

                if (e.touchType != TouchType.UP) {
                    // Positions are recorded in the playfield's coordinates and loaded in the screen's.
                    val real = Utils.trackToRealCoords(Vector2(e.x, e.y), true)
                    assertEquals(real.x, a.x, 0f)
                    assertEquals(real.y, a.y, 0f)
                }
            }
        }

        val objects = loaded.objectData!!
        assertEquals(12.toShort(), objects[0]!!.accuracy)
        assertNull(objects[0]!!.tickSet)
        assertEquals(ResultType.HIT300.id, objects[0]!!.result)
        assertEquals((-34).toShort(), objects[1]!!.accuracy)
        assertEquals(BitSet().also { it.set(0); it.set(3); it.set(64) }, objects[1]!!.tickSet)
        assertEquals(ResultType.HIT100.id, objects[1]!!.result)
        assertEquals(ResultType.MISS.id, objects[2]!!.result)
    }

    @Test
    fun testLoadWithoutGameplayData() {
        val file = saved(recordedReplay())

        val loaded = Replay()
        assertTrue(loaded.load(file.path, false))
        assertEquals("Hard", loaded.beatmapName)
        assertEquals(789, loaded.stat!!.scoreMaxCombo)
        assertTrue(loaded.cursorMoves.isEmpty())
    }

    @Test
    fun testReplayWithoutStatistics() {
        val recorded = recordedReplay()
        recorded.stat = null
        val file = saved(recorded)

        val loaded = Replay()
        loaded.setObjectCount(3)
        assertTrue(loaded.load(file.path, true))
        assertNull(loaded.stat)
        assertEquals(recorded.cursorMoves.size, loaded.cursorMoves.size)
    }

    @Test
    fun testFewerObjectsThanTheBeatmap() {
        val file = saved(recordedReplay())

        val loaded = Replay()
        loaded.setObjectCount(4)
        assertFalse(loaded.load(file.path, true))
    }

    @Test
    fun testRejectsOtherFiles() {
        // The start of a replay before version 10: a zip with Java's object serialization.
        val legacy = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x14, 0x00, 0x08, 0x00, 0x08, 0x00, 0xAC.toByte(), 0xED.toByte(), 0x00, 0x05)
        assertFalse(Replay().load(Buffer().write(legacy), "legacy.odr", false))
        assertFalse(Replay().load(Buffer().writeUtf8("{}"), "empty.odr", false))
        assertFalse(Replay().load(Buffer(), "nothing.odr", false))
    }

    @Test
    fun testFileIsJson() {
        val text = saved(recordedReplay()).readText()
        assertTrue(text.startsWith("{\"version\":10,"))
    }
}
