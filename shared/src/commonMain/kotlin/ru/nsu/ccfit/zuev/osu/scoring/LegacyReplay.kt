package ru.nsu.ccfit.zuev.osu.scoring

import com.osudroid.math.Vector2
import com.osudroid.mods.LegacyModConverter
import com.osudroid.mods.ModHardRock
import com.osudroid.mods.ModReplayV6
import com.osudroid.scoring.Statistic
import com.osudroid.utils.BitSet
import com.osudroid.utils.ModHashMap
import com.osudroid.utils.deserializeMods
import com.reco1l.javaserialization.JavaObject
import com.reco1l.javaserialization.JavaObjectInputStream
import com.reco1l.javaserialization.JavaSerialization
import com.reco1l.javaserialization.JavaSerializationException
import com.reco1l.javaserialization.decodeFromJavaValue
import com.reco1l.javaserialization.decodeObject
import kotlinx.serialization.Serializable
import okio.Buffer
import okio.BufferedSource
import okio.ByteString.Companion.decodeHex
import okio.IOException
import okio.Inflater
import okio.Source
import okio.inflate
import okio.use
import ru.nsu.ccfit.zuev.osu.Constants

// Replays before version 10, upstream's: a zip with a single "data" entry, written with Java's object
// serialization. They're read through com.reco1l.javaserialization, on any platform, and never
// written: their version decides how they're played, so they're kept as they are.

/**
 * Reading replays written before version 10.
 */
internal object LegacyReplay {

    private val ZipLocalFileSignature = "504b0304".decodeHex()

    private const val ZipStored = 0
    private const val ZipDeflated = 8

    // The bit of the zip entry's flags telling its sizes come after its data instead.
    private const val ZipDataDescriptorFlag = 0x08

    /**
     * Whether [source] holds a replay written before version 10, without consuming it.
     */
    fun isLegacy(source: BufferedSource) = source.rangeEquals(0, ZipLocalFileSignature)

    /**
     * Opens the data of the zip [source] starts with: its first entry.
     */
    fun openData(source: BufferedSource): Source {
        if (!isLegacy(source)) {
            throw IOException("Not a zip file")
        }

        source.skip(4 + 2) // The signature, the version needed to extract.
        val flags = source.readShortLe().toInt() and 0xFFFF
        val method = source.readShortLe().toInt() and 0xFFFF
        source.skip(2 + 2 + 4) // The modification time and date, the CRC-32.
        val compressedSize = source.readIntLe().toLong() and 0xFFFFFFFFL
        source.skip(4) // The uncompressed size.
        val nameLength = source.readShortLe().toLong() and 0xFFFF
        val extraLength = source.readShortLe().toLong() and 0xFFFF
        source.skip(nameLength + extraLength)

        return when (method) {
            // java.util.zip.ZipOutputStream writes the sizes of deflated entries after them, so the
            // data is inflated up to its end instead.
            // Raw deflate, without a zlib header (nowrap).
            ZipDeflated -> source.inflate(Inflater(true))

            ZipStored -> {
                if (flags and ZipDataDescriptorFlag != 0) {
                    throw IOException("A stored zip entry without its size")
                }

                Buffer().also { source.readFully(it, compressedSize) }
            }

            else -> throw IOException("Unsupported zip compression method $method")
        }
    }
}

/**
 * Loads the replay written before version 10 in [source], as [Replay.load] does.
 *
 * @throws IOException If it can't be read.
 */
internal fun Replay.loadLegacy(source: BufferedSource, replayFilename: String, withGameplayData: Boolean) {
    JavaObjectInputStream(LegacyReplay.openData(source)).use { input ->
        cursorMoves.clear()

        // Replays have started with an object holding their version since version 1.
        var version = 0
        val beatmapsetName = when (val first = input.readObject()) {
            is JavaObject -> {
                version = JavaSerialization.decodeFromJavaValue<LegacyReplayVersion>(first).version
                input.decodeObject<String>()
            }

            is String -> first
            else -> throw JavaSerializationException("Not a replay: it starts with $first")
        }

        replayVersion = version
        val beatmapName = input.decodeObject<String>()
        setBeatmap(beatmapsetName, beatmapName, input.decodeObject<String>())

        if (version >= 3) {
            val stat = Statistic()
            this.stat = stat
            stat.replayFilename = replayFilename
            stat.beatmapMD5 = md5
            stat.time = input.readLong()
            stat.hit300k = input.readInt()
            stat.hit300 = input.readInt()
            stat.hit100k = input.readInt()
            stat.hit100 = input.readInt()
            stat.hit50 = input.readInt()
            stat.misses = input.readInt()

            when {
                version >= 9 -> stat.totalScore = input.readLong()
                version == 8 -> stat.totalScore = input.readInt().toLong()
                else -> stat.setForcedScore(input.readInt().toLong())
            }

            stat.scoreMaxCombo = input.readInt()

            if (version < 6) {
                // The accuracy and whether the play was perfect, derived from the hit results since.
                input.readFloat()
                input.readBoolean()
            }

            stat.playerName = input.decodeObject<String>()

            if (version >= 7) {
                stat.mod = deserializeMods(input.decodeObject<String>())
            } else {
                // The GameMod constants, decoded by their names.
                val modNames = input.decodeObject<Set<String>?>().orEmpty()
                val extraModString = if (version >= 4) input.decodeObject<String>() else ""

                stat.mod = LegacyModConverter.convert(modNames, extraModString)
                stat.mod.put(ModReplayV6())
            }
        }

        if (!withGameplayData) {
            return
        }

        val mods = stat?.mod ?: ModHashMap()
        val cursorCount = input.readInt()
        cursorIndex = IntArray(cursorCount)
        lastMoveIndex = IntArray(cursorCount) { -1 }

        repeat(cursorCount) {
            cursorMoves.add(input.readLegacyCursor(version, mods))
        }

        val objectData = objectData!!
        val objectCount = input.readInt()
        if (objectCount < objectData.size) {
            throw JavaSerializationException("The replay has $objectCount objects, the beatmap ${objectData.size}")
        }

        for (i in objectData.indices) {
            val data = Replay.ReplayObjectData()
            data.accuracy = input.readShort()

            // The bits of the slider's nested objects, the first in the lowest bit of the last byte.
            val length = input.readUnsignedByte()
            if (length > 0) {
                val bytes = input.readFully(length)
                val tickSet = BitSet()

                for (j in 0 until length * 8) {
                    tickSet.set(j, (bytes[length - j / 8 - 1].toInt() and (1 shl (j % 8))) != 0)
                }

                data.tickSet = tickSet
            }

            if (version >= 1) {
                data.result = input.readByte()
            }

            objectData[i] = data
        }
    }
}

/**
 * Reads the movements of a cursor, in the playfield's coordinates as [Replay.MoveArray.fromCursor] keeps them,
 * converted from the screen coordinates of the first versions for replays older than version 2.
 */
private fun JavaObjectInputStream.readLegacyCursor(version: Int, mods: ModHashMap): Replay.MoveArray {
    val isHardRock = ModHardRock::class in mods
    val size = readInt()
    val array = Replay.MoveArray(size)
    array.size = size

    for (i in 0 until size) {
        val movement = Replay.ReplayMovement()
        array.movements[i] = movement

        // The time, shifted by the two bits of the touch type.
        val packed = readInt()
        movement.time = packed shr 2
        movement.touchType = requireNotNull(TouchType.getByID((packed and 3).toByte())) { "Unknown touch type ${packed and 3}" }

        if (movement.touchType != TouchType.UP) {
            // Positions were shorts until version 5.
            val point = if (version < 5) Vector2(readShort().toFloat(), readShort().toFloat()) else Vector2(readFloat(), readFloat())
            val trackPoint = if (version > 1) point else firstVersionScreenToTrack(point, isHardRock)

            movement.x = trackPoint.x
            movement.y = trackPoint.y
        }
    }

    return array
}

/**
 * Converts a position on the screen of the first replay versions, 1024x600 with an 820x570 playfield in its center,
 * to the playfield's coordinates, not flipped by Hard Rock.
 */
internal fun firstVersionScreenToTrack(point: Vector2, isHardRock: Boolean): Vector2 {
    val y = if (isHardRock) FIRST_VERSION_SCREEN_HEIGHT - point.y else point.y

    return Vector2(
        (point.x - (FIRST_VERSION_SCREEN_WIDTH - FIRST_VERSION_PLAYFIELD_WIDTH) / 2f) * Constants.MAP_WIDTH / FIRST_VERSION_PLAYFIELD_WIDTH,
        (y - (FIRST_VERSION_SCREEN_HEIGHT - FIRST_VERSION_PLAYFIELD_HEIGHT) / 2f) * Constants.MAP_HEIGHT / FIRST_VERSION_PLAYFIELD_HEIGHT
    )
}

private const val FIRST_VERSION_SCREEN_WIDTH = 1024f
private const val FIRST_VERSION_SCREEN_HEIGHT = 600f
private const val FIRST_VERSION_PLAYFIELD_WIDTH = 820f
private const val FIRST_VERSION_PLAYFIELD_HEIGHT = 570f

/**
 * The object replays have started with since version 1, upstream's `Replay.ReplayVersion`.
 */
@Serializable
private class LegacyReplayVersion(val version: Int)
