package com.osudroid.ui.v2.skin

import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.ui.shape.PathShape
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class SkinPiecesTest {

    private class Style(val name: String) : SliderBodyStyle {
        override fun apply(body: PathShape, bodyColor: Color4, borderColor: Color4, skinScale: Float) = Unit
        override fun toString() = name
    }

    /**
     * A skin providing the components in [provided], which creates [style] for the slider body, if any.
     */
    private class FakeSkin(
        val style: Style?,
        val provided: Set<SkinComponent<*>> = emptySet(),
    ) : GameplaySkin {
        override fun provides(component: SkinComponent<*>) = component in provided

        @Suppress("UNCHECKED_CAST")
        override fun <T : Any> create(component: SkinComponent<T>): T? =
            if (component == SkinComponent.SliderBody) style as T? else null
    }

    private val legacyStyle = Style("legacy")
    private val defaultStyle = Style("default")

    @Test
    fun `a component a texture skin provides comes from it`() {
        val pieces = SkinPieces(listOf(
            FakeSkin(legacyStyle, provided = setOf(SkinComponent.SliderBody)),
            FakeSkin(defaultStyle, provided = setOf(SkinComponent.SliderBody)),
        ))

        assertSame(legacyStyle, pieces.create(SkinComponent.SliderBody))
    }

    @Test
    fun `a component a texture skin lacks comes from the default skin`() {
        val pieces = SkinPieces(listOf(
            FakeSkin(legacyStyle),
            FakeSkin(defaultStyle, provided = setOf(SkinComponent.SliderBody)),
        ))

        assertSame(defaultStyle, pieces.create(SkinComponent.SliderBody))
    }

    @Test
    fun `a component no skin provides comes from the first that can create it`() {
        // The default textures of a texture skin, for what the default skin doesn't draw yet.
        val pieces = SkinPieces(listOf(FakeSkin(legacyStyle), FakeSkin(null)))

        assertSame(legacyStyle, pieces.create(SkinComponent.SliderBody))
    }

    @Test
    fun `a skin providing a component it can't create falls through`() {
        val pieces = SkinPieces(listOf(
            FakeSkin(null, provided = setOf(SkinComponent.SliderBody)),
            FakeSkin(defaultStyle, provided = setOf(SkinComponent.SliderBody)),
        ))

        assertSame(defaultStyle, pieces.create(SkinComponent.SliderBody))
    }

    @Test
    fun `a component no skin can create is null`() {
        val pieces = SkinPieces(listOf(FakeSkin(null), FakeSkin(null)))

        assertNull(pieces.createOrNull(SkinComponent.SliderBody))
    }

    @Test(expected = IllegalStateException::class)
    fun `creating a component no skin can create throws`() {
        SkinPieces(listOf(FakeSkin(null))).create(SkinComponent.SliderBody)
    }
}
