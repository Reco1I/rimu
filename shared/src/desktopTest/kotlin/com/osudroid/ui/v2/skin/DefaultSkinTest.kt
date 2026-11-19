package com.osudroid.ui.v2.skin

import com.reco1l.verktex.data.Dimension
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultSkinTest {

    @Test
    fun `it provides the circles, sliders, approach circles and judgements`() {
        for (component in listOf(
            SkinComponent.HitCircle, SkinComponent.SliderHeadCircle, SkinComponent.SliderTailCircle,
            SkinComponent.ApproachCircle, SkinComponent.TraceableApproachCircle, SkinComponent.Judgement("hit300"),
            SkinComponent.SliderBody, SkinComponent.SliderBall, SkinComponent.FollowCircle, SkinComponent.ReverseArrow,
            SkinComponent.SliderTick, SkinComponent.Spinner,
        )) {
            assertTrue("$component", DefaultSkin.provides(component))
        }

        // Only texture skins light their judgements.
        assertFalse(DefaultSkin.provides(SkinComponent.HitLighting))
    }

    @Test
    fun `the approach circle has the size of the approachcircle texture`() {
        val piece = DefaultApproachCircle()

        assertEquals(SkinDimensions.APPROACH_CIRCLE.width, (piece.width as Dimension.Fixed).toPixels(), 0f)
        assertEquals(SkinDimensions.APPROACH_CIRCLE.height, (piece.height as Dimension.Fixed).toPixels(), 0f)
    }
}
