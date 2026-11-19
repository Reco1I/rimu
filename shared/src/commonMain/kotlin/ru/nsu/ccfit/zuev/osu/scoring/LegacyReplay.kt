package ru.nsu.ccfit.zuev.osu.scoring

import com.osudroid.math.Vector2
import com.osudroid.mods.LegacyModConverter
import com.osudroid.mods.ModHardRock
import com.osudroid.mods.ModReplayV6
import com.osudroid.utils.BitSet
import com.osudroid.utils.ModHashMap
import com.osudroid.utils.deserializeMods
import com.reco1l.javaserialization.JavaArray
import com.reco1l.javaserialization.JavaEnum
import com.reco1l.javaserialization.JavaObject
import com.reco1l.javaserialization.JavaObjectInputStream
import com.reco1l.javaserialization.JavaSerializationException
import okio.Buffer
import okio.BufferedSource
import okio.ByteString.Companion.decodeHex
import okio.IOException
import okio.Inflater
import okio.Source
import okio.inflate
import okio.use
import ru.nsu.ccfit.zuev.osu.Utils

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
                version = first.fields["version"] as? Int ?: throw JavaSerializationException("Not a replay version: $first")
                input.readString()
            }

            is String -> first
            else -> throw JavaSerializationException("Not a replay: it starts with $first")
        }

        replayVersion = version
        val beatmapName = input.readString()
        setBeatmap(beatmapsetName, beatmapName, input.readString())

        if (version >= 3) {
            val stat = StatisticV2()
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

            stat.playerName = input.readString()

            if (version >= 7) {
                stat.mod = deserializeMods(input.readString())
            } else {
                val modNames = legacyModNames(input.readObject())
                val extraModString = if (version >= 4) input.readString() else ""

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

private fun JavaObjectInputStream.readString() = when (val value = readObject()) {
    is String -> value
    else -> throw JavaSerializationException("Expected a string, found $value")
}

/**
 * Reads the movements of a cursor, converting their positions as [Replay.MoveArray.fromCursor] does,
 * from the coordinates of the first versions for replays older than version 2.
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

            val realPoint = if (version > 1) {
                Utils.trackToRealCoords(point, isHardRock)
            } else {
                Utils.trackToRealCoords(Utils.realToTrackCoords(point, 1024f, 600f, true), isHardRock)
            }

            movement.x = realPoint.x
            movement.y = realPoint.y
        }
    }

    return array
}

/**
 * The names of the `GameMod` constants of the mod set of a replay older than version 7, whichever
 * set it was: an `EnumSet` (written as its serialization proxy, with the constants in an array) or a
 * `HashSet` (with the constants after its fields).
 */
private fun legacyModNames(mods: Any?): List<String> {
    val names = ArrayList<String>()

    fun collect(value: Any?) {
        when (value) {
            is JavaEnum -> names += value.constant
            is JavaArray -> value.elements.forEach(::collect)

            is JavaObject -> {
                value.fields.values.forEach(::collect)
                value.annotations.values.forEach { it.forEach(::collect) }
            }
        }
    }

    collect(mods)
    return names
}
