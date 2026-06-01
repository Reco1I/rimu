package com.reco1l.andengine.buffered

import com.reco1l.andengine.ColorStack
import com.reco1l.framework.Color4
import com.reco1l.framework.math.Vec2
import com.reco1l.toolkt.kotlin.fastForEachIndexed
import org.anddev.andengine.opengl.font.Font
import org.anddev.andengine.opengl.util.GLHelper
import javax.microedition.khronos.opengles.GL10

object TextRenderer : BufferRenderer() {

    private val glyphTextureCoordinatesBuffer = ResizableFloatBuffer()


    fun renderLines(
        gl: GL10,
        lines: List<String>,
        linesWidth: IntArray,
        font: Font,
        viewportX: Float,
        viewportY: Float,
        viewportWidth: Float,
        viewportHeight: Float,
        alignment: Vec2,
        color: Color4? = null
    ) {
        val lineHeight = font.lineHeight + font.lineGap

        if (color != null) ColorStack.pushColor(gl, color, false)

        render(gl) {

            GLHelper.enableTextures(gl)
            font.texture.bind(gl)

            glyphTextureCoordinatesBuffer.begin()

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

                    addVertex(lineX, lineY)
                    addVertex(lineX, letterY)
                    addVertex(letterX, letterY)
                    addVertex(letterX, letterY)
                    addVertex(letterX, lineY)
                    addVertex(lineX, lineY)

                    // Texture coordinates
                    val letterTextureX = letter.mTextureX
                    val letterTextureY = letter.mTextureY
                    val letterTextureX2 = letterTextureX + letter.mTextureWidth
                    val letterTextureY2 = letterTextureY + letter.mTextureHeight

                    glyphTextureCoordinatesBuffer.apply {
                        addVertex(letterTextureX, letterTextureY)
                        addVertex(letterTextureX, letterTextureY2)
                        addVertex(letterTextureX2, letterTextureY2)
                        addVertex(letterTextureX2, letterTextureY2)
                        addVertex(letterTextureX2, letterTextureY)
                        addVertex(letterTextureX, letterTextureY)
                    }

                    lineX += letter.mAdvance
                    charIndex += charCount
                }
            }

            glyphTextureCoordinatesBuffer.end()

            GLHelper.enableTexCoordArray(gl)
            gl.glTexCoordPointer(VERTEX_2D_SIZE, GL10.GL_FLOAT, 0, glyphTextureCoordinatesBuffer.internalBuffer)
        }

        if (color != null) ColorStack.popColor(gl)
    }

    fun renderCharacter(
        gl: GL10,
        character: String,
        font: Font,
        viewportX: Float,
        viewportY: Float,
        viewportWidth: Float,
        viewportHeight: Float,
        alignment: Vec2,
        color: Color4? = null
    ) {
        val lineHeight = font.lineHeight + font.lineGap
        val letter = font.getLetter(character)

        val lineX = viewportX + viewportWidth * alignment.x - letter.mAdvance * alignment.x
        val lineY = viewportY + viewportHeight * alignment.y - lineHeight * alignment.y

        if (color != null) ColorStack.pushColor(gl, color, false)

        render(gl) {

            GLHelper.enableTextures(gl)
            font.texture.bind(gl)

            glyphTextureCoordinatesBuffer.begin()

            // Vertex positions
            val letterX = lineX + letter.mWidth
            val letterY = lineY + font.lineHeight

            addVertex(lineX, lineY)
            addVertex(lineX, letterY)
            addVertex(letterX, letterY)
            addVertex(letterX, letterY)
            addVertex(letterX, lineY)
            addVertex(lineX, lineY)

            // Texture coordinates
            val letterTextureX = letter.mTextureX
            val letterTextureY = letter.mTextureY
            val letterTextureX2 = letterTextureX + letter.mTextureWidth
            val letterTextureY2 = letterTextureY + letter.mTextureHeight

            glyphTextureCoordinatesBuffer.apply {
                addVertex(letterTextureX, letterTextureY)
                addVertex(letterTextureX, letterTextureY2)
                addVertex(letterTextureX2, letterTextureY2)
                addVertex(letterTextureX2, letterTextureY2)
                addVertex(letterTextureX2, letterTextureY)
                addVertex(letterTextureX, letterTextureY)
            }

            glyphTextureCoordinatesBuffer.end()

            GLHelper.enableTexCoordArray(gl)
            gl.glTexCoordPointer(VERTEX_2D_SIZE, GL10.GL_FLOAT, 0, glyphTextureCoordinatesBuffer.internalBuffer)
        }

        if (color != null) ColorStack.popColor(gl)
    }
}