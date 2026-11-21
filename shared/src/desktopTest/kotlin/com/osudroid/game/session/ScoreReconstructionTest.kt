package com.osudroid.game.session

import TestResourceManager
import com.osudroid.beatmaps.DroidPlayableBeatmap
import com.osudroid.beatmaps.hitobjects.HitCircle
import com.osudroid.beatmaps.parser.BeatmapParser
import com.osudroid.game.rules.HealthProcessor
import com.osudroid.game.rules.ScoreProcessor
import com.osudroid.scoring.Statistic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.nsu.ccfit.zuev.osu.scoring.Replay
import ru.nsu.ccfit.zuev.osu.scoring.ResultType

class ScoreReconstructionTest {

    private val beatmap: DroidPlayableBeatmap = BeatmapParser(
        TestResourceManager.getBeatmapFile("Kenji Ninuma - DISCOPRINCE (peppy) [Normal]")!!.path
    ).parse(true)!!.createDroidPlayableBeatmap()

    private val objects = beatmap.hitObjects.objects.toTypedArray()

    private val stat = Statistic().apply {
        beatmapNoteCount = objects.size
        beatmapMaxCombo = beatmap.maxCombo
        healthDrain = beatmap.difficulty.hp
    }

    private val reconstruction = ScoreReconstruction(
        objects = objects,
        timingPoints = beatmap.controlPoints.timing.controlPoints.toTypedArray(),
        breaks = beatmap.events.breaks.toTypedArray(),
        hitWindow = beatmap.hitWindow,
        overallDifficulty = beatmap.difficulty.od,
        replayVersion = 0,
        stat = stat,
        scoreProcessor = ScoreProcessor(stat),
        healthProcessor = HealthProcessor(stat, beatmap.difficulty.hp)
    )

    private val endTime = (objects.last().endTime + 1000).toFloat()


    @Test
    fun `Autoplay is rebuilt with every object hit, in one combo`() {
        val result = reconstruction.reconstruct(endTime, null)

        assertEquals(objects.size, result.objectCount)
        assertTrue(result.isStarted)

        assertEquals(objects.size, stat.hit300)
        assertEquals(0, stat.hit100 + stat.hit50 + stat.misses)
        assertEquals(beatmap.maxCombo, stat.combo)
        assertTrue(stat.hp > 0)
    }

    @Test
    fun `Only the objects judged by the time are rebuilt`() {
        val time = (objects[objects.size / 2].startTime + 1).toFloat()
        val result = reconstruction.reconstruct(time, null)

        assertTrue(result.objectCount in 1 until objects.size)
        assertTrue(objects[result.objectCount - 1].endTime <= time + beatmap.hitWindow.mehWindow)
        assertEquals(result.objectCount, stat.hit300)
    }

    @Test
    fun `Nothing is rebuilt before the first object`() {
        val result = reconstruction.reconstruct(0f, null)

        assertEquals(0, result.objectCount)
        assertEquals(0, stat.notesHit)
    }

    @Test
    fun `A missed object recorded by a replay breaks the combo`() {
        // The first object is a circle, the replay of the rest has no results: they're hit.
        assertTrue(objects[0] is HitCircle)

        val objectData = arrayOfNulls<Replay.ReplayObjectData>(objects.size)
        objectData[0] = Replay.ReplayObjectData().apply { result = ResultType.MISS.id }

        reconstruction.reconstruct(endTime, objectData)

        assertEquals(1, stat.misses)
        assertEquals(objects.size - 1, stat.hit300)
        assertEquals(beatmap.maxCombo - 1, stat.combo)
    }

    @Test
    fun `Rebuilding again starts from a reset statistic`() {
        reconstruction.reconstruct(endTime, null)
        val score = stat.totalScore

        reconstruction.reconstruct(endTime, null)

        assertEquals(score, stat.totalScore)
        assertEquals(objects.size, stat.hit300)
    }
}
