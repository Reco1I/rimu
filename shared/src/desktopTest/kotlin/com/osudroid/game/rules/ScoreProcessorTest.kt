package com.osudroid.game.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2

class ScoreProcessorTest {

    private val statistic = StatisticV2().apply {
        beatmapNoteCount = 100
        beatmapMaxCombo = 100
    }

    private val processor = ScoreProcessor(statistic)

    @Test
    fun `A combo of 300s ends in a geki`() {
        assertEquals("hit300", processor.applyObject(JudgementResult.Great, false))
        assertEquals("hit300g", processor.applyObject(JudgementResult.Great, true))

        assertEquals(2, statistic.hit300)
        assertEquals(1, statistic.hit300k)
        assertEquals(2, statistic.combo)
    }

    @Test
    fun `A combo with a 100 or a 50 ends in a katu`() {
        processor.applyObject(JudgementResult.Ok, false)
        assertEquals("hit300k", processor.applyObject(JudgementResult.Great, true))

        processor.applyObject(JudgementResult.Meh, false)
        assertEquals("hit100k", processor.applyObject(JudgementResult.Ok, true))

        assertEquals(0, statistic.hit300k)
        assertEquals(1, statistic.hit100k)
    }

    @Test
    fun `A combo with a miss ends in neither`() {
        assertEquals("hit0", processor.applyObject(JudgementResult.Miss, false))
        assertEquals("hit300", processor.applyObject(JudgementResult.Great, true))
        assertEquals("hit300g", processor.applyObject(JudgementResult.Great, true))

        assertEquals(1, statistic.misses)
        assertEquals(1, statistic.hit300k)
    }

    @Test
    fun `A miss ending its combo keeps its flags into the next combo, as the game always did`() {
        processor.applyObject(JudgementResult.Miss, true)
        assertTrue(processor.comboWasMissed)

        assertEquals("hit300", processor.applyObject(JudgementResult.Great, true))
        assertFalse(processor.comboWasMissed)
    }

    @Test
    fun `A miss breaks the combo`() {
        repeat(5) { processor.applyObject(JudgementResult.Great, false) }
        processor.applyObject(JudgementResult.Miss, false)

        assertEquals(0, statistic.combo)
        assertEquals(5, statistic.scoreMaxCombo)
    }

    @Test
    fun `Slider parts add to the combo and to their counts`() {
        assertEquals("sliderpoint30", processor.applySliderPart(SliderPart.Head))
        assertEquals("sliderpoint10", processor.applySliderPart(SliderPart.Tick))
        assertEquals("sliderpoint30", processor.applySliderPart(SliderPart.Repeat))
        assertEquals("hit300g", processor.applySliderTail(JudgementResult.Great, true, true))

        assertEquals(1, statistic.sliderHeadHits)
        assertEquals(1, statistic.sliderTickHits)
        assertEquals(1, statistic.sliderRepeatHits)
        assertEquals(1, statistic.sliderEndHits)
        assertEquals(4, statistic.combo)
    }

    @Test
    fun `An untracked slider end judges the slider without adding to the combo`() {
        processor.applySliderPart(SliderPart.Head)
        processor.applySliderTail(JudgementResult.Ok, false, false)

        assertEquals(0, statistic.sliderEndHits)
        assertEquals(1, statistic.hit100)
        assertEquals(1, statistic.combo)
    }

    @Test
    fun `A missed slider part only breaks the combo`() {
        processor.applySliderPart(SliderPart.Head)
        processor.applySliderPartMiss()

        assertEquals(0, statistic.combo)
        assertEquals(0, statistic.misses)
    }

    @Test
    fun `Spinner bonus spins add score and nothing else`() {
        processor.applySpinnerBonus()

        assertEquals(1000L, statistic.totalScore)
        assertEquals(0, statistic.combo)
        assertEquals(0, statistic.notesHit)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `The tail of a slider is judged as the slider`() {
        processor.applySliderPart(SliderPart.Tail)
    }
}
