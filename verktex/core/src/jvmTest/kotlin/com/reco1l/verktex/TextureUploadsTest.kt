package com.reco1l.verktex

import com.reco1l.verktex.graphics.GraphicsResource
import com.reco1l.verktex.graphics.GraphicsTexture
import com.reco1l.verktex.texture.FramedTextureRegion
import org.junit.Assert.assertEquals
import org.junit.Test
import com.reco1l.verktex.graphics.PixelBuffer

/**
 * Sources write their pixels when the texture flushes its uploads, either through bind() or
 * flushUploads(), and only once until they are added or moved again.
 */
class TextureUploadsTest {

    private class RecordingTexture : GraphicsTexture() {
        override val width = 64f
        override val height = 64f

        val writes = mutableListOf<List<Int>>()
        var binds = 0

        override fun onCreateTexture() = Unit
        override fun onBindTexture() {
            binds++
        }
        override fun onUnload() = Unit

        override fun onWritePixels(x: Int, y: Int, width: Int, height: Int, pixels: PixelBuffer) {
            writes += listOf(x, y, width, height, pixels.remaining())
        }
    }

    private class SolidSource(size: Int) : GraphicsTexture.Source {
        override val width = size.toFloat()
        override val height = size.toFloat()

        override fun writePixels(target: GraphicsTexture.PixelTarget, x: Int, y: Int) {
            val size = width.toInt()
            target.write(x, y, size, size, java.nio.ByteBuffer.allocate(size * size * 4))
        }
    }

    @Test
    fun flushUploadsLoadsAndWritesWithoutBinding() {
        val texture = RecordingTexture()
        val region = texture.addSource(SolidSource(8))!!

        texture.flushUploads()

        assertEquals(GraphicsResource.State.Loaded, texture.state)
        assertEquals(0, texture.binds)
        // The region is written with its one pixel gutter.
        assertEquals(listOf(listOf(region.x.toInt() - 1, region.y.toInt() - 1, 10, 10, 10 * 10 * 4)), texture.writes)
    }

    @Test
    fun regionsKeepAGutterAndItRepeatsTheirEdges() {
        val texture = RecordingTexture()
        val first = texture.addSource(SolidSource(8))!!
        val second = texture.addSource(SolidSource(8))!!

        val gap = maxOf(second.x - (first.x + first.width), second.y - (first.y + first.height), first.x - (second.x + second.width), first.y - (second.y + second.height))
        assertEquals(2f, gap)

        // Two by two pixels, 1 2 / 3 4, extruded by one pixel.
        val pixels = java.nio.ByteBuffer.allocateDirect(16).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        pixels.asIntBuffer().put(intArrayOf(1, 2, 3, 4))

        val extruded = com.reco1l.verktex.graphics.extrudePixels(pixels, 2, 2, 1).order(java.nio.ByteOrder.LITTLE_ENDIAN).asIntBuffer()
        val result = IntArray(16).also { extruded.get(it) }

        assertEquals(listOf(1, 1, 2, 2, 1, 1, 2, 2, 3, 3, 4, 4, 3, 3, 4, 4), result.toList())
        assertEquals(0, pixels.position())
    }

    @Test
    fun sourcesAreWrittenOnce() {
        val texture = RecordingTexture()
        texture.addSource(SolidSource(8))

        texture.flushUploads()
        texture.flushUploads()
        texture.bind()

        assertEquals(1, texture.writes.size)
        assertEquals(1, texture.binds)
    }

    @Test
    fun bindWritesPendingSources() {
        val texture = RecordingTexture()
        texture.addSource(SolidSource(8))
        texture.addSource(SolidSource(4))

        texture.bind()

        assertEquals(2, texture.writes.size)
    }

    @Test
    fun framedSourceWritesEveryFrameAtItsOffset() {
        val atlas = RecordingTexture()
        val frames = listOf(
            GraphicsTexture.Region(atlas, SolidSource(4), 0f, 0f, 4f, 4f),
            GraphicsTexture.Region(atlas, SolidSource(4), 4f, 0f, 4f, 4f),
        )
        val source = FramedTextureRegion.Source(frames)

        val writes = mutableListOf<Pair<Int, Int>>()
        source.writePixels({ x, y, _, _, _ -> writes += x to y }, 10, 20)

        assertEquals(listOf(10 to 20, 14 to 20), writes)
    }
}
