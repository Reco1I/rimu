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
import com.osudroid.mods.ModHardRock
import com.osudroid.mods.ModPrecise
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
import okio.IOException
import okio.Path.Companion.toPath
import okio.Source
import okio.buffer
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
            val stat = stat
            val file = ReplayFile(
                version = ReplayFormatVersion,
                beatmapsetName = beatmapsetName,
                beatmapName = beatmapName,
                md5 = md5,
                stat = stat?.let {
                    ReplayStatistics(
                        time = it.time,
                        hit300k = it.hit300k,
                        hit300 = it.hit300,
                        hit100k = it.hit100k,
                        hit100 = it.hit100,
                        hit50 = it.hit50,
                        misses = it.misses,
                        totalScore = it.totalScore,
                        maxCombo = it.scoreMaxCombo,
                        playerName = it.playerName,
                        mods = ReplayJson.parseToJsonElement(it.mod.serializeMods()),
                    )
                },
                cursors = cursorMoves.map { it.toCursor() },
                objects = objectData!!.map { data ->
                    val tickSet = data?.tickSet
                    ReplayObject(
                        accuracy = data?.accuracy ?: 0,
                        ticks = if (tickSet == null || tickSet.isEmpty()) null else (0 until tickSet.length()).filter { tickSet[it] },
                        result = data?.result ?: 0,
                    )
                },
            )

            Platform.fileSystem.write(filename.toPath()) {
                writeUtf8(ReplayJson.encodeToString(ReplayFile.serializer(), file))
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
            val file = ReplayJson.decodeFromString(ReplayFile.serializer(), source.buffer().readUtf8())

            cursorMoves.clear()
            replayVersion = file.version
            beatmapsetName = file.beatmapsetName
            beatmapName = file.beatmapName
            md5 = file.md5

            val fileStat = file.stat
            if (fileStat != null) {
                val stat = StatisticV2()
                this.stat = stat
                stat.replayFilename = replayFilename
                stat.beatmapMD5 = md5
                stat.time = fileStat.time
                stat.hit300k = fileStat.hit300k
                stat.hit300 = fileStat.hit300
                stat.hit100k = fileStat.hit100k
                stat.hit100 = fileStat.hit100
                stat.hit50 = fileStat.hit50
                stat.misses = fileStat.misses
                stat.totalScore = fileStat.totalScore
                stat.scoreMaxCombo = fileStat.maxCombo
                stat.playerName = fileStat.playerName
                stat.mod = deserializeMods(fileStat.mods.toString())
            }

            if (withGameplayData) {
                val mods = stat?.mod ?: ModHashMap()

                cursorIndex = IntArray(file.cursors.size)
                lastMoveIndex = IntArray(file.cursors.size)

                for (i in file.cursors.indices) {
                    cursorMoves.add(MoveArray.fromCursor(file.cursors[i], mods))
                    cursorIndex[i] = 0
                    lastMoveIndex[i] = -1
                }

                val objectData = objectData!!
                require(file.objects.size >= objectData.size) {
                    "The replay has ${file.objects.size} objects, the beatmap ${objectData.size}"
                }

                for (i in objectData.indices) {
                    val fileObject = file.objects[i]
                    val data = ReplayObjectData()
                    data.accuracy = fileObject.accuracy
                    data.tickSet = fileObject.ticks?.let { ticks -> BitSet().also { set -> ticks.forEach { set.set(it) } } }
                    data.result = fileObject.result
                    objectData[i] = data
                }
            }
        } catch (e: Exception) {
            // Including replays written before version 10, which aren't JSON.
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
        The versions of replays, for compatibility purposes.
        Version 4: Adds ExtraModString's save and load in save()/load()/loadInfo()
        Version 5: Changes coordinates to use the float primitive type
        Version 6: Removed accuracy and perfect, slider ends no longer give combo when not hit
        Version 7: Reworked mod storage to not serialize GameMod, object stacking behavior overhaul, device-independent
                   object scaling
        Version 8: Slider head hit window is extended to its 50 hit window;
                   score stored as raw totalScore instead of totalScoreWithMultiplier
        Version 9: score stored as long
        Version 10: stored as JSON (see ReplayFile) instead of Java's object serialization; older replays aren't read
     */

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

        /**
         * The movements of this array, as a replay file stores them.
         */
        internal fun toCursor(): ReplayCursor {
            val time = IntArray(size)
            val x = FloatArray(size)
            val y = FloatArray(size)
            val type = ByteArray(size)

            for (i in 0 until size) {
                val movement = movements[i]
                time[i] = movement.time
                type[i] = movement.touchType!!.id
                if (movement.touchType != TouchType.UP) {
                    x[i] = movement.x
                    y[i] = movement.y
                }
            }

            return ReplayCursor(time, x, y, type)
        }

        companion object {
            /**
             * Reads the movements of a cursor of a replay file, converting their positions from the
             * playfield's coordinates they were recorded in.
             */
            internal fun fromCursor(cursor: ReplayCursor, mods: ModHashMap): MoveArray {
                val isHardRock = ModHardRock::class in mods
                val size = cursor.time.size
                require(cursor.x.size == size && cursor.y.size == size && cursor.type.size == size) {
                    "The arrays of a cursor have different sizes"
                }

                val array = MoveArray(size)
                array.size = size
                for (i in 0 until size) {
                    val movement = ReplayMovement()
                    array.movements[i] = movement
                    movement.time = cursor.time[i]
                    movement.touchType = requireNotNull(TouchType.getByID(cursor.type[i])) { "Unknown touch type ${cursor.type[i]}" }
                    if (movement.touchType != TouchType.UP) {
                        val realPoint = Utils.trackToRealCoords(Vector2(cursor.x[i], cursor.y[i]), isHardRock)
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
         * The version replays are written in, see the versions above.
         */
        const val ReplayFormatVersion = 10

        /**
         * The amount of cursors replays record, as many as the game tracks.
         */
        const val CursorCount = 10
    }
}
