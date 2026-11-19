package ru.nsu.ccfit.zuev.osu.scoring

import com.osudroid.math.Vector2
import com.osudroid.mods.ModCustomSpeed
import com.osudroid.mods.ModDifficultyAdjust
import com.osudroid.mods.ModDoubleTime
import com.osudroid.mods.ModFlashlight
import com.osudroid.mods.ModHardRock
import com.osudroid.mods.ModHidden
import com.osudroid.mods.ModReplayV6
import com.osudroid.utils.BitSet
import com.osudroid.utils.ModHashMap
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.EnumSet
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Replays written before version 10, as upstream's `Replay.save` wrote them.
 */
class LegacyReplayTest {

    // Stand-ins for upstream's classes: only their data is read, not their names.
    class ReplayVersion(@JvmField var version: Int) : Serializable {
        companion object {
            private const val serialVersionUID = 4643121693566795335L
        }
    }

    @Suppress("unused")
    enum class GameMod { MOD_NOFAIL, MOD_HARDROCK, MOD_HIDDEN, MOD_DOUBLETIME, MOD_FLASHLIGHT, MOD_UNKNOWN }


    private class Cursor(val time: Int, val type: TouchType, val x: Float = 0f, val y: Float = 0f)

    private val cursors = listOf(
        listOf(Cursor(100, TouchType.DOWN, 10f, 20f), Cursor(150, TouchType.MOVE, 110f, 220f), Cursor(200, TouchType.UP)),
        listOf(),
        listOf(Cursor(300, TouchType.DOWN, 400f, 300f), Cursor(350, TouchType.UP)),
    )

    private val ticks = BitSet().also { it.set(0); it.set(3); it.set(9) }

    /**
     * Writes a replay of [version] as upstream did. [mods] are the mods' JSON string from version 7,
     * the set of `GameMod`s before.
     */
    private fun legacyReplay(
        version: Int,
        mods: Any? = "[]",
        extraModString: String = "",
        score: Long = 1234,
        objectCount: Int = 2,
    ): ByteArray {
        val bytes = ByteArrayOutputStream()

        ZipOutputStream(bytes).use { zip ->
            zip.putNextEntry(ZipEntry("data"))
            val os = ObjectOutputStream(zip)

            if (version >= 1) {
                os.writeObject(ReplayVersion(version))
            }
            os.writeObject("Artist - Title")
            os.writeObject("Hard")
            os.writeObject(MD5)

            if (version >= 3) {
                os.writeLong(1_600_000_000_000L)
                os.writeInt(1)
                os.writeInt(2)
                os.writeInt(3)
                os.writeInt(4)
                os.writeInt(5)
                os.writeInt(6)
                if (version >= 9) os.writeLong(score) else os.writeInt(score.toInt())
                os.writeInt(321)

                if (version < 6) {
                    os.writeFloat(0.95f)
                    os.writeBoolean(false)
                }

                os.writeObject("player")

                if (version >= 7) {
                    os.writeObject(mods)
                } else {
                    os.writeObject(mods)
                    if (version >= 4) os.writeObject(extraModString)
                }
            }

            os.writeInt(cursors.size)
            for (cursor in cursors) {
                os.writeInt(cursor.size)
                for (movement in cursor) {
                    os.writeInt((movement.time shl 2) + movement.type.id)
                    if (movement.type != TouchType.UP) {
                        if (version < 5) {
                            os.writeShort(movement.x.toInt())
                            os.writeShort(movement.y.toInt())
                        } else {
                            os.writeFloat(movement.x)
                            os.writeFloat(movement.y)
                        }
                    }
                }
            }

            os.writeInt(objectCount)
            for (i in 0 until objectCount) {
                val tickSet = if (i == 1) ticks else null
                os.writeShort(if (i == 1) -34 else 12)

                if (tickSet == null) {
                    os.writeByte(0)
                } else {
                    val tickBytes = ByteArray((tickSet.length() + 7) / 8)
                    for (j in 0 until tickSet.length()) {
                        if (tickSet[j]) {
                            tickBytes[tickBytes.size - j / 8 - 1] = (tickBytes[tickBytes.size - j / 8 - 1].toInt() or (1 shl (j % 8))).toByte()
                        }
                    }
                    os.writeByte(tickBytes.size)
                    os.write(tickBytes)
                }

                if (version >= 1) {
                    os.writeByte(if (i == 1) ResultType.HIT100.id.toInt() else ResultType.HIT300.id.toInt())
                }
            }

            os.flush()
            zip.closeEntry()
        }

        return bytes.toByteArray()
    }

    private fun load(bytes: ByteArray, withGameplayData: Boolean = true, objectCount: Int = 2): Replay? {
        val replay = Replay()
        replay.setObjectCount(objectCount)
        return replay.takeIf { it.load(Buffer().write(bytes), "legacy.odr", withGameplayData) }
    }

    private fun assertGameplay(replay: Replay, version: Int) {
        val isHardRock = ModHardRock::class in (replay.stat?.mod ?: ModHashMap())

        assertEquals(cursors.size, replay.cursorMoves.size)
        assertEquals(cursors.size, replay.cursorIndex.size)
        assertTrue(replay.lastMoveIndex.all { it == -1 })

        for (i in cursors.indices) {
            val expected = cursors[i]
            val actual = replay.cursorMoves[i]
            assertEquals(expected.size, actual.size)

            for (j in expected.indices) {
                val e = expected[j]
                val a = actual.movements[j]
                assertEquals(e.time, a.time)
                assertEquals(e.type, a.touchType)

                if (e.type != TouchType.UP) {
                    // In the playfield's coordinates, which the first versions didn't store.
                    val point = Vector2(e.x, e.y)
                    val track = if (version > 1) point else firstVersionScreenToTrack(point, isHardRock)
                    assertEquals(track.x, a.x, 0f)
                    assertEquals(track.y, a.y, 0f)
                }
            }
        }

        val objects = replay.objectData!!
        assertEquals(12.toShort(), objects[0]!!.accuracy)
        assertNull(objects[0]!!.tickSet)
        assertEquals((-34).toShort(), objects[1]!!.accuracy)
        assertEquals(ticks, objects[1]!!.tickSet)

        if (version >= 1) {
            assertEquals(ResultType.HIT300.id, objects[0]!!.result)
            assertEquals(ResultType.HIT100.id, objects[1]!!.result)
        }
    }

    private fun assertStatistics(replay: Replay) {
        assertEquals("Artist - Title", replay.beatmapsetName)
        assertEquals("Hard", replay.beatmapName)
        assertEquals(MD5, replay.md5)

        val stat = replay.stat!!
        assertEquals("legacy.odr", stat.replayFilename)
        assertEquals(MD5, stat.beatmapMD5)
        assertEquals(1_600_000_000_000L, stat.time)
        assertEquals(1, stat.hit300k)
        assertEquals(2, stat.hit300)
        assertEquals(3, stat.hit100k)
        assertEquals(4, stat.hit100)
        assertEquals(5, stat.hit50)
        assertEquals(6, stat.misses)
        assertEquals(321, stat.scoreMaxCombo)
        assertEquals("player", stat.playerName)
    }


    @Test
    fun testVersion9() {
        val mods = ModHashMap().apply { put(ModHardRock()); put(ModHidden()) }
        val replay = load(legacyReplay(9, mods.serializeMods(), score = 12_345_678_901L))!!

        assertEquals(9, replay.replayVersion)
        assertStatistics(replay)
        assertEquals(12_345_678_901L, replay.stat!!.totalScore)
        assertEquals(mods.serializeMods(), replay.stat!!.mod.serializeMods())
        assertFalse(ModReplayV6::class in replay.stat!!.mod)
        assertGameplay(replay, 9)
    }

    @Test
    fun testVersion8() {
        val replay = load(legacyReplay(8, score = 987_654))!!

        assertEquals(8, replay.replayVersion)
        assertStatistics(replay)
        assertEquals(987_654L, replay.stat!!.totalScore)
        assertGameplay(replay, 8)
    }

    @Test
    fun testVersion6WithEnumSet() {
        val replay = load(legacyReplay(6, EnumSet.of(GameMod.MOD_DOUBLETIME, GameMod.MOD_FLASHLIGHT), "x1.25|FLD0.24|CS4.5|AR9.0", score = 55_555))!!

        assertEquals(6, replay.replayVersion)
        assertStatistics(replay)
        // The score with its multiplier, as replays before version 8 stored it.
        assertEquals(55_555L, replay.stat!!.totalScore)
        assertEquals(55_555L, replay.stat!!.totalScoreWithMultiplier)

        val mods = replay.stat!!.mod
        assertTrue(ModDoubleTime::class in mods)
        assertTrue(ModReplayV6::class in mods)
        assertEquals(1.25f, mods.ofType<ModCustomSpeed>()!!.trackRateMultiplier)
        assertEquals(0.24f, mods.ofType<ModFlashlight>()!!.followDelay)
        val difficultyAdjust = mods.ofType<ModDifficultyAdjust>()!!
        assertEquals(4.5f, difficultyAdjust.cs)
        assertEquals(9f, difficultyAdjust.ar)
        assertNull(difficultyAdjust.od)
        assertNull(difficultyAdjust.hp)
        assertGameplay(replay, 6)
    }

    @Test
    fun testVersion5WithHashSet() {
        // Accuracy and perfect are still there, and the mods a HashSet.
        val replay = load(legacyReplay(5, hashSetOf(GameMod.MOD_HARDROCK, GameMod.MOD_NOFAIL)))!!

        assertStatistics(replay)
        val mods = replay.stat!!.mod
        assertTrue(ModHardRock::class in mods)
        assertTrue(ModReplayV6::class in mods)
        assertGameplay(replay, 5)
    }

    @Test
    fun testVersion4WithShortPositions() {
        val replay = load(legacyReplay(4, EnumSet.of(GameMod.MOD_HIDDEN)))!!

        assertStatistics(replay)
        assertTrue(ModHidden::class in replay.stat!!.mod)
        assertGameplay(replay, 4)
    }

    @Test
    fun testVersion3WithoutExtraModString() {
        val replay = load(legacyReplay(3, EnumSet.noneOf(GameMod::class.java)))!!

        assertStatistics(replay)
        assertEquals(setOf(ModReplayV6::class), replay.stat!!.mod.keys)
        assertGameplay(replay, 3)
    }

    @Test
    fun testVersion1WithoutStatistics() {
        val replay = load(legacyReplay(1))!!

        assertEquals(1, replay.replayVersion)
        assertNull(replay.stat)
        assertEquals("Hard", replay.beatmapName)
        assertGameplay(replay, 1)
    }

    @Test
    fun testVersion0WithoutVersion() {
        val replay = load(legacyReplay(0))!!

        assertEquals(0, replay.replayVersion)
        assertEquals("Artist - Title", replay.beatmapsetName)
        assertGameplay(replay, 0)
    }

    @Test
    fun testLoadWithoutGameplayData() {
        val replay = load(legacyReplay(9), withGameplayData = false)!!

        assertStatistics(replay)
        assertTrue(replay.cursorMoves.isEmpty())
    }

    @Test
    fun testLoadFromFile() {
        val file = File.createTempFile("legacy", ".odr")
        file.deleteOnExit()
        file.writeBytes(legacyReplay(9))

        val replay = Replay()
        replay.setObjectCount(2)
        assertTrue(replay.load(file.path, true))
        assertEquals(file.name, replay.stat!!.replayFilename)
        assertGameplay(replay, 9)
    }

    @Test
    fun testRejectsBrokenReplays() {
        // Fewer objects than the beatmap has.
        assertNull(load(legacyReplay(9), objectCount = 3))
        // A mod upstream never had.
        assertNull(load(legacyReplay(6, EnumSet.of(GameMod.MOD_UNKNOWN))))
        // Cut short.
        val bytes = legacyReplay(9)
        assertNull(load(bytes.copyOf(bytes.size / 2)))
    }

    companion object {
        private const val MD5 = "0123456789abcdef0123456789abcdef"
    }
}
