package ru.nsu.ccfit.zuev.osu.scoring

import com.osudroid.GameMode
import com.osudroid.beatmaps.DroidHitWindow
import com.osudroid.beatmaps.IBeatmap
import com.osudroid.beatmaps.PreciseDroidHitWindow
import com.osudroid.beatmaps.hitobjects.Slider
import com.osudroid.beatmaps.hitobjects.sliderobject.SliderHead
import com.osudroid.beatmaps.hitobjects.sliderobject.SliderRepeat
import com.osudroid.beatmaps.hitobjects.sliderobject.SliderTick
import com.osudroid.math.Vector2
import com.osudroid.mods.LegacyModConverter
import com.osudroid.mods.ModHardRock
import com.osudroid.mods.ModPrecise
import com.osudroid.mods.ModReplayV6
import com.osudroid.resources.Res
import com.osudroid.utils.BitSet
import com.osudroid.utils.ModHashMap
import com.reco1l.verktex.Platform
import com.osudroid.utils.applyMods
import com.osudroid.utils.deserializeMods
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlin.math.min
import okio.use
import okio.EOFException
import okio.IOException
import okio.Path.Companion.toPath
import okio.Source
import com.reco1l.verktex.ui.toast.Toasts
import ru.nsu.ccfit.zuev.osu.Utils
import ru.nsu.ccfit.zuev.osu.helper.Debug

class Replay @JvmOverloads constructor(allocateMoves: Boolean = false) {
    private var pointsSkipped = 0
    @JvmField
    var cursorMoves = ArrayList<MoveArray>()
    @JvmField
    var cursorIndex: IntArray
    @JvmField
    var lastMoveIndex: IntArray
    @JvmField
    var objectData: Array<ReplayObjectData?>? = null
    @JvmField
    var replayVersion = 0
    @JvmField
    var stat: StatisticV2? = null
    var md5 = ""
        private set
    var beatmapName = ""
        private set
    var beatmapsetName = ""
        private set
    private var isSaving = false

    init {
        if (allocateMoves) {
            cursorMoves.add(MoveArray(200))
            cursorMoves.add(MoveArray(50))
            for (i in 2 until CursorCount) {
                cursorMoves.add(MoveArray(15))
            }
        }

        cursorIndex = IntArray(CursorCount)
        lastMoveIndex = IntArray(CursorCount)
        for (i in 0 until CursorCount) {
            cursorIndex[i] = 0
            lastMoveIndex[i] = -1
        }
    }

    fun setBeatmap(beatmapsetName: String, beatmapName: String, md5: String) {
        this.beatmapsetName = beatmapsetName
        this.md5 = md5
        this.beatmapName = beatmapName
    }

    fun setObjectCount(count: Int) {
        objectData = arrayOfNulls(count)
    }

    fun addObjectResult(id: Int, accuracy: Short, ticks: BitSet?) {
        val objectData = objectData
        if (id < 0 || objectData == null || id >= objectData.size)
            return

        val data = objectData[id] ?: ReplayObjectData()
        data.accuracy = accuracy
        data.tickSet = ticks
        objectData[id] = data
    }

    fun addObjectScore(id: Int, score: ResultType) {
        val objectData = objectData
        if (id < 0 || objectData == null || id >= objectData.size)
            return

        if (objectData[id] == null)
            objectData[id] = ReplayObjectData()

        objectData[id]!!.result = score.id
    }

    fun addPress(timeMs: Int, pos: Vector2, pid: Int) {
        if (pid > cursorMoves.size || isSaving) return
        cursorMoves[pid].pushBack(this, timeMs, pos.x, pos.y, TouchType.DOWN)
    }

    fun addMove(timeMs: Int, pos: Vector2, pid: Int) {
        if (pid > cursorMoves.size || isSaving) return
        cursorMoves[pid].pushBack(this, timeMs, pos.x, pos.y, TouchType.MOVE)
    }

    fun addUp(timeMs: Int, pid: Int) {
        if (pid > cursorMoves.size || isSaving) return
        cursorMoves[pid].pushBack(timeMs, TouchType.UP)
    }

    fun save(filename: String) {
        isSaving = true

        for (i in cursorMoves.indices)
            Debug.i("Replay contains " + cursorMoves[i].size + " moves for finger " + i)
        Debug.i("Skipped " + pointsSkipped + " points")
        Debug.i("Replay contains " + objectData!!.size + " objects")

        try {
            openReplayOutput(filename).use { os ->
                os.writeObject(ReplayVersion())
                os.writeObject(beatmapsetName)
                os.writeObject(beatmapName)
                os.writeObject(md5)

                val stat = stat
                if (stat != null) {
                    os.writeLong(stat.time)
                    os.writeInt(stat.hit300k)
                    os.writeInt(stat.hit300)
                    os.writeInt(stat.hit100k)
                    os.writeInt(stat.hit100)
                    os.writeInt(stat.hit50)
                    os.writeInt(stat.misses)
                    os.writeLong(stat.totalScore)
                    os.writeInt(stat.scoreMaxCombo)
                    os.writeObject(stat.playerName)
                    os.writeObject(stat.mod.serializeMods())
                }

                os.writeInt(cursorMoves.size)
                //Storing all moves
                for (move in cursorMoves) {
                    move.writeTo(os)
                }
                os.writeInt(objectData!!.size)
                for (objectData in objectData!!) {
                    val data = objectData ?: ReplayObjectData()
                    os.writeShort(data.accuracy.toInt())
                    val tickSet = data.tickSet
                    if (tickSet == null || tickSet.isEmpty()) {
                        os.writeByte(0)
                    } else {
                        val bytes = ByteArray((tickSet.length() + 7) / 8)
                        for (i in 0 until tickSet.length()) {
                            if (tickSet[i]) {
                                bytes[bytes.size - i / 8 - 1] = (bytes[bytes.size - i / 8 - 1].toInt() or (1 shl (i % 8))).toByte()
                            }
                        }
                        os.writeByte(bytes.size)
                        os.write(bytes)
                    }
                    os.writeByte(data.result.toInt())
                }
            }
        } catch (e: IOException) {
            Debug.e("IOException: " + e.message, e)
        } finally {
            isSaving = false
        }
    }

    fun load(replayFilePath: String, withGameplayData: Boolean): Boolean {
        val path = replayFilePath.toPath()

        val source = try {
            Platform.fileSystem.source(path)
        } catch (e: IOException) {
            Debug.e("Cannot load replay: " + e.message, e)
            return false
        }

        return source.use { load(it, path.name, withGameplayData) }
    }

    fun load(source: Source, replayFilename: String, withGameplayData: Boolean): Boolean {
        try {
            openReplayInput(source).use { os ->
                cursorMoves.clear()
                var version = 0

                val mBeatmapsetName: String
                val firstObject = os.readObject()
                Debug.i("Read object: " + firstObject!!::class.simpleName)
                if (firstObject::class == ReplayVersion::class) {
                    Debug.i("Other replay version")
                    version = (firstObject as ReplayVersion).version
                    replayVersion = version
                    mBeatmapsetName = os.readObject() as String
                } else {
                    mBeatmapsetName = firstObject as String
                }
                val mBeatmapName = os.readObject() as String
                val mMD5 = os.readObject() as String

                beatmapsetName = mBeatmapsetName
                beatmapName = mBeatmapName
                md5 = mMD5

                if (version >= 3) {
                    val stat = StatisticV2()
                    this.stat = stat
                    stat.replayFilename = replayFilename
                    stat.beatmapMD5 = md5
                    stat.time = os.readLong()
                    stat.hit300k = os.readInt()
                    stat.hit300 = os.readInt()
                    stat.hit100k = os.readInt()
                    stat.hit100 = os.readInt()
                    stat.hit50 = os.readInt()
                    stat.misses = os.readInt()

                    if (version >= 9) {
                        stat.totalScore = os.readLong()
                    } else if (version == 8) {
                        stat.totalScore = os.readInt().toLong()
                    } else {
                        stat.setForcedScore(os.readInt().toLong())
                    }

                    stat.scoreMaxCombo = os.readInt()

                    if (version < 6) {
                        // Consume `accuracy` in replays older than v6 (which is not used anymore as the result is derived from hit results)
                        os.readFloat()

                        // Consume `perfect` in replays older than v6 (which is not used anymore as the result is derived from hit results)
                        os.readBoolean()
                    }

                    stat.playerName = os.readObject() as String

                    if (version >= 7) {
                        val modJsonStr = os.readObject() as String
                        stat.mod = deserializeMods(modJsonStr)
                    } else {
                        val mod = asGameModSet(os.readObject())
                        var extraModString = ""

                        if (version >= 4) {
                            extraModString = os.readObject() as String
                        }

                        stat.mod = LegacyModConverter.convert(mod, extraModString)
                        stat.mod.put(ModReplayV6())
                    }
                }

                if (withGameplayData) {
                    val msize = os.readInt()
                    cursorIndex = IntArray(msize)
                    lastMoveIndex = IntArray(msize)

                    for (i in 0 until msize) {
                        cursorMoves.add(MoveArray.readFrom(os, this, stat!!.mod))
                        cursorIndex[i] = 0
                        lastMoveIndex[i] = -1
                    }

                    os.readInt()
                    val objectData = objectData!!
                    for (i in objectData.indices) {
                        val data = ReplayObjectData()
                        data.accuracy = os.readShort()
                        val len = os.readByte().toInt()
                        if (len > 0) {
                            val tickSet = BitSet()
                            data.tickSet = tickSet
                            val bytes = ByteArray(len)
                            var remainingLen = len

                            while (remainingLen > 0) {
                                val readBytes = os.read(bytes, len - remainingLen, remainingLen)

                                if (readBytes < 0) {
                                    throw EOFException("EOF reached when reading tickset with $remainingLen bytes left to read")
                                }

                                remainingLen -= readBytes
                            }

                            for (j in 0 until len * 8) {
                                tickSet.set(j, (bytes[len - j / 8 - 1].toInt() and (1 shl (j % 8))) != 0)
                            }
                        }
                        if (version >= 1) {
                            data.result = os.readByte()
                        }
                        objectData[i] = data
                    }
                }
            }
        } catch (e: EOFException) {
            Debug.e("O_o eof...")
            Debug.e(e)
            Toasts.showLong(Res.string.replay_corrupted)
            return false
        } catch (e: Exception) {
            Toasts.showLong(Res.string.replay_corrupted)
            Debug.e("Cannot load replay: " + e.message, e)
            return false
        }

        if (withGameplayData) {
            for (i in cursorMoves.indices)
                Debug.i("Loaded " + cursorMoves[i].size + " moves for finger " + i)
            Debug.i("Loaded " + objectData!!.size + " objects")
        }

        return true
    }

    /**
     * Populates a [StatisticV2] based on information from this [Replay].
     *
     * @param beatmap The [IBeatmap] that this [Replay] was played on.
     * @param stat The [StatisticV2] to populate.
     */
    fun populateStatistics(beatmap: IBeatmap, stat: StatisticV2) {
        val objectData = objectData

        // Check for a replay that is potentially malformed or failed to be loaded.
        if (objectData == null || beatmap.hitObjects.objects.size != objectData.size) {
            return
        }

        stat.sliderHeadHits = 0
        stat.sliderTickHits = 0
        stat.sliderRepeatHits = 0
        stat.sliderEndHits = 0

        val difficulty = beatmap.difficulty.clone()
        difficulty.applyMods(GameMode.Droid, stat.mod.values)

        val hitWindow = if (ModPrecise::class in stat.mod)
            PreciseDroidHitWindow(difficulty.od)
        else
            DroidHitWindow(difficulty.od)
        val mehWindow = hitWindow.mehWindow.toDouble()

        val objects = beatmap.hitObjects.objects

        for (i in objects.indices) {
            val obj = objects[i]

            if (obj !is Slider) {
                continue
            }
            val slider = obj

            val objReplayData = objectData[i]!!

            // Miss result means all slider nested hit objects were missed.
            if (objReplayData.result == ResultType.MISS.id) {
                continue
            }

            // Great result means all slider nested hit objects were hit.
            if (objReplayData.result == ResultType.HIT300.id) {
                stat.addSliderHeadHit()
                stat.addSliderEndHit()

                for (j in 1 until slider.nestedHitObjects.size - 1) {
                    val nestedObject = slider.nestedHitObjects[j]

                    if (nestedObject is SliderHead) {
                        stat.addSliderHeadHit()
                    } else if (nestedObject is SliderTick) {
                        stat.addSliderTickHit()
                    } else if (nestedObject is SliderRepeat) {
                        stat.addSliderRepeatHit()
                    } else {
                        stat.addSliderEndHit()
                    }
                }

                continue
            }

            // For other results, we need to individually check judgement for each nested object.
            // Slider head is unique in that the result is not stored in tickSet, but in the form of hit offset.
            // Logic borrowed from GameplaySlider.onSliderHeadHit().
            val accuracy = objReplayData.accuracy
            val sliderDuration = slider.duration

            if (replayVersion >= 6 || mehWindow <= sliderDuration) {
                if (-mehWindow <= accuracy && accuracy <= min(mehWindow, sliderDuration)) {
                    stat.addSliderHeadHit()
                }
            } else if (accuracy <= sliderDuration) {
                // In replays older than version 6, when the 50 hit window is longer than the duration of the slider,
                // the slider head is considered to *not* exist if it was not hit until the slider is over.
                // It is a very weird behavior, but that's what it actually was...
                stat.addSliderHeadHit()
            }

            val tickSet = objReplayData.tickSet
            if (tickSet != null) {
                for (j in 1 until slider.nestedHitObjects.size) {
                    if (!tickSet[j - 1]) {
                        continue
                    }

                    val nestedObject = slider.nestedHitObjects[j]

                    if (nestedObject is SliderTick) {
                        stat.addSliderHeadHit()
                    } else if (nestedObject is SliderRepeat) {
                        stat.addSliderRepeatHit()
                    } else {
                        stat.addSliderEndHit()
                    }
                }
            }
        }
    }

    fun getStat(): StatisticV2? {
        return stat
    }

    fun setStat(stat: StatisticV2?) {
        this.stat = stat
    }

    /*
        Object used to store data about current replay version for compatibility purposes.
        Version 4: Adds ExtraModString's save and load in save()/load()/loadInfo()
        Version 5: Changes coordinates to use the float primitive type
        Version 6: Removed accuracy and perfect, slider ends no longer give combo when not hit
        Version 7: Reworked mod storage to not serialize GameMod, object stacking behavior overhaul, device-independent
                   object scaling
        Version 8: Slider head hit window is extended to its 50 hit window;
                   score stored as raw totalScore instead of totalScoreWithMultiplier
        Version 9: score stored as long
     */
    class ReplayVersion : ReplaySerializable {
        // Written as it is into replays, so its name and type must not change.
        @JvmField
        var version = 9

        companion object {
            private const val serialVersionUID = 4643121693566795335L
        }
    }

    class ReplayObjectData {
        @JvmField
        var accuracy: Short = 0
        @JvmField
        var tickSet: BitSet? = null
        @JvmField
        var result: Byte = 0
    }

    class ReplayMovement {
        var time = 0
            internal set
        var x = 0f
            internal set
        var y = 0f
            internal set
        var touchType: TouchType? = null
            internal set
    }

    class MoveArray(startSize: Int) {
        @JvmField
        var movements: Array<ReplayMovement>
        @JvmField
        var size = 0
        @JvmField
        var allocated = startSize

        init {
            @Suppress("UNCHECKED_CAST")
            movements = arrayOfNulls<ReplayMovement>(allocated) as Array<ReplayMovement>
        }

        fun reallocate(newSize: Int) {
            if (newSize <= allocated) return
            @Suppress("UNCHECKED_CAST")
            val newMovements = arrayOfNulls<ReplayMovement>(newSize) as Array<ReplayMovement>
            movements.copyInto(newMovements, 0, 0, size)
            movements = newMovements
            allocated = newSize
        }

        fun checkNewPoint(px: Float, py: Float): Boolean {
            if (size < 2) return false

            val minusTwoMovement = movements[size - 2]
            val previousMovement = movements[size - 1]

            val tx = (px + minusTwoMovement.x) * 0.5f
            val ty = (py + minusTwoMovement.y) * 0.5f

            return (Utils.sqr(previousMovement.x - tx) + Utils.sqr(previousMovement.y - ty)) <= 25
        }

        fun pushBack(replay: Replay, time: Int, x: Float, y: Float, touchType: TouchType) {
            var idx = size
            if (touchType == TouchType.MOVE && checkNewPoint(x, y)) {
                idx = size - 1
                replay.pointsSkipped++
            } else {
                if (size + 1 >= allocated) {
                    reallocate((allocated * 3) / 2)
                }
                size++
            }
            val movement = ReplayMovement()
            movements[idx] = movement
            movement.time = time
            movement.x = x
            movement.y = y
            movement.touchType = touchType
        }

        fun pushBack(time: Int, touchType: TouchType) {
            if (size >= allocated) {
                reallocate((allocated * 3) / 2)
            }
            movements[size] = ReplayMovement()
            val movement = movements[size]
            movement.time = time
            movement.touchType = touchType
            size++
        }

        fun writeTo(os: ReplayOutput) {
            os.writeInt(size)
            for (i in 0 until size) {
                val movement = movements[i]
                os.writeInt((movement.time shl 2) + movement.touchType!!.id)
                if (movement.touchType != TouchType.UP) {
                    os.writeFloat(movement.x)
                    os.writeFloat(movement.y)
                }
            }
        }

        companion object {
            private fun readTouchPoint(`is`: ReplayInput, replay: Replay): Float {
                return if (replay.replayVersion < 5) `is`.readShort().toFloat() else `is`.readFloat()
            }

            @JvmStatic
            fun readFrom(`is`: ReplayInput, replay: Replay, mods: ModHashMap): MoveArray {
                val isHardRock = ModHardRock::class in mods
                val size = `is`.readInt()
                val array = MoveArray(size)
                array.size = size
                for (i in 0 until size) {
                    val movement = ReplayMovement()
                    array.movements[i] = movement
                    movement.time = `is`.readInt()
                    movement.touchType = TouchType.getByID((movement.time and 3).toByte())
                    movement.time = movement.time shr 2
                    if (movement.touchType != TouchType.UP) {
                        val baseX = readTouchPoint(`is`, replay)
                        val baseY = readTouchPoint(`is`, replay)
                        val gamePoint = Vector2(
                            baseX / 1,
                            baseY / 1
                        )
                        val realPoint = if (replay.replayVersion > 1)
                            Utils.trackToRealCoords(gamePoint, isHardRock)
                        else
                            Utils.trackToRealCoords(
                                Utils.realToTrackCoords(gamePoint, 1024f, 600f, true),
                                isHardRock
                            )
                        movement.x = realPoint.x
                        movement.y = realPoint.y
                    }
                }

                return array
            }
        }
    }

    companion object {
        /**
         * The amount of cursors replays record, as many as the game tracks.
         */
        const val CursorCount = 10
    }
}
