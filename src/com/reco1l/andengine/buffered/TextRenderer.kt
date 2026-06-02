package com.reco1l.andengine.buffered

import com.reco1l.andengine.UIRenderer
import com.reco1l.framework.math.Vec2
import com.reco1l.toolkt.kotlin.fastForEachIndexed
import org.anddev.andengine.opengl.font.Font
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas

object TextRenderer : BufferRenderer() {

    fun renderLines(
        lines: List<String>,
        linesWidth: IntArray,
        font: Font,
        viewportX: Float,
        viewportY: Float,
        viewportWidth: Float,
        viewportHeight: Float,
        alignment: Vec2,
    ) {
        val lineHeight = font.lineHeight + font.lineGap

        UIRenderer.setState(texture = font.texture as BitmapTextureAtlas)

        lines.fastForEachIndexed { lineIndex, line ->

            var lineX = viewportX + viewportWidth * alignment.x - linesWidth[lineIndex] * alignment.x
            val lineY = viewportY + viewportHeight * alignment.y - lines.size * lineHeight * alignment.y + lineIndex * lineHeight

            var charIndex = 0
            while (charIndex < line.length) {
                val codePoint = line.codePointAt(charIndex)
                val charCount = Character.charCount(codePoint)

                val characterString = line.substring(charIndex, charIndex + charCount)
                val letter = font.getLetter(characterString)

                // Vertex positions
                val letterX = lineX + letter.mWidth
                val letterY = lineY + font.lineHeight

                // Texture coordinates
                val letterTextureX = letter.mTextureX
                val letterTextureY = letter.mTextureY
                val letterTextureX2 = letterTextureX + letter.mTextureWidth
                val letterTextureY2 = letterTextureY + letter.mTextureHeight

                addVertex(lineX, lineY, letterTextureX, letterTextureY)
                addVertex(lineX, letterY, letterTextureX, letterTextureY2)
                addVertex(letterX, letterY, letterTextureX2, letterTextureY2)

                addVertex(letterX, letterY, letterTextureX2, letterTextureY2)
                addVertex(letterX, lineY, letterTextureX2, letterTextureY)
                addVertex(lineX, lineY, letterTextureX, letterTextureY)

                lineX += letter.mAdvance
                charIndex += charCount
            }

            //GLHelper.enableTexCoordArray(gl)
            //gl.glTexCoordPointer(VERTEX_STRIDE, GL10.GL_FLOAT, 0, glyphTextureCoordinatesBuffer.internalBuffer)
        }

    }

    fun renderCharacter(
        character: String,
        font: Font,
        viewportX: Float,
        viewportY: Float,
        viewportWidth: Float,
        viewportHeight: Float,
        alignment: Vec2,
    ) {
        val lineHeight = font.lineHeight + font.lineGap
        val letter = font.getLetter(character)

        val lineX = viewportX + viewportWidth * alignment.x - letter.mAdvance * alignment.x
        val lineY = viewportY + viewportHeight * alignment.y - lineHeight * alignment.y

        UIRenderer.setState(texture = font.texture as BitmapTextureAtlas)

        // Vertex positions
        val letterX = lineX + letter.mWidth
        val letterY = lineY + font.lineHeight

        // Texture coordinates
        val letterTextureX = letter.mTextureX
        val letterTextureY = letter.mTextureY
        val letterTextureX2 = letterTextureX + letter.mTextureWidth
        val letterTextureY2 = letterTextureY + letter.mTextureHeight

        addVertex(lineX, lineY, letterTextureX, letterTextureY)
        addVertex(lineX, letterY, letterTextureX, letterTextureY2)
        addVertex(letterX, letterY, letterTextureX2, letterTextureY2)

        addVertex(letterX, letterY, letterTextureX2, letterTextureY2)
        addVertex(letterX, lineY, letterTextureX2, letterTextureY)
        addVertex(lineX, lineY, letterTextureX, letterTextureY)

    }

}