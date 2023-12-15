package com.reco1l.ini

import com.reco1l.toolkt.kotlin.between
import com.reco1l.toolkt.kotlin.withTranslatedEscapes
import com.reco1l.toolkt.kotlin.forEachLet
import com.reco1l.toolkt.kotlin.times
import okio.BufferedSource
import okio.buffer
import okio.source
import java.io.Closeable
import java.io.InputStream


class IniParser(private val inputStream: InputStream) : Closeable
{

    /**
     * Define the format delimiter, by default `:`
     */
    var delimiter = ':'

    /**
     * Define if nesting sections is allowed.
     */
    var allowNesting = true

    /**
     * If `true` the the parent hierarchy of nested sections will be created if they were not created
     * yet. If `false` an exception is thrown due to missing sections.
     */
    var createParentHierarchy = false

    /**
     * Determine which delimiters should be used to consider the next segment as comment.
     */
    var commentDelimiters = arrayOf("#", "//")


    private lateinit var bufferedSource: BufferedSource

    private lateinit var ini: Ini


    private var lineCount = 0

    private var lineIndent = 0

    private var currentLine: String? = null

    private var currentSection: IniSection? = null


    private fun nextLine(): Boolean
    {
        val line = bufferedSource.readUtf8Line() ?: return false

        // Validating the line, if it starts with any of the comment delimiter characters we must
        // skip the line.
        if (line.isBlank() || commentDelimiters.any(line::startsWith))
            return nextLine()

        // Trimming line to the non comment segments.
        currentLine = commentDelimiters.forEachLet { line.substringBefore(" $it") }

        // Finding the amount of indentation
        lineIndent = currentLine!!.indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0)

        return true
    }


    private fun decodeLine()
    {
        val trimmed = currentLine!!.trim()

        if (trimmed.startsWith('['))
        {
            if (!trimmed.endsWith(']'))
                throwException(message = "Line must end with closing bracket ]")

            decodeSection()
            return
        }

        if (currentSection == null)
            return

        val key = decodeSegment(false)
        val value = decodeSegment(true) ?: return

        currentSection!![key] = value
    }

    private fun decodeSection()
    {
        // Extracting the declaration body between the brackets.
        val nameBody = currentLine!!.between('[', ']')

            ?: throwException(1, "Expected section name.")

        // If nesting is allowed we must find the parents and its hierarchy in case it has more than one
        if (allowNesting && '.' in nameBody)
        {
            // The default parent indicator is the dot so we're splitting the name with it.
            val hierarchy = nameBody.split('.')

            var previousParent = currentSection!!
            var sectionIndex = 2

            hierarchy.forEachIndexed { index, section ->

                // If the dot is at the start of the name we take as parent the previous declared
                // section, if there's any section between dots that is empty then it is considered
                // as malformed declaration.
                if (index != 0 && section.isEmpty())
                    throwException(sectionIndex, "Expected nested section name.")

                when (index)
                {
                    // The first declaration can be empty meaning the previous declared section should
                    // be the parent or it can be anyone else.
                    0 ->
                    {
                        // The previous declared section is used in the case of a single dot "[.SectionName]"
                        if (section.isEmpty())
                            return@forEachIndexed

                        previousParent = when
                        {
                            !createParentHierarchy -> ini[section]
                            // The expected section wasn't declared yet.
                                ?: throwException(sectionIndex, "Parent section not found.")

                            else -> ini.put(section)
                        }
                    }

                    // We reach the end of the hierarchy which correspond to the new section.
                    hierarchy.lastIndex ->
                    {
                        // Creating the section if it wasn't put yet.
                        currentSection = previousParent.putSection(section)
                    }

                    else ->
                    {
                        previousParent = when
                        {
                            !createParentHierarchy -> previousParent.getSection(section)

                            // The expected section wasn't declared yet.
                                ?: throwException(sectionIndex, "Parent section not found.")

                            else -> previousParent.putSection(section)
                        }
                    }
                }

                sectionIndex += section.length + 1
            }
            return
        }

        // No nesting found or disabled.
        currentSection = ini.put(nameBody)
    }


    private fun decodeSegment(afterDelimiter: Boolean): String? = when (afterDelimiter)
    {
        true -> currentLine!!.substringAfter(delimiter, missingDelimiterValue = "")
        else -> currentLine!!.substringBefore(delimiter, missingDelimiterValue = "")

    }.takeUnless { it.isBlank() }?.trim()?.withTranslatedEscapes()


    private fun throwException(

        charIndex: Int = currentLine!!.length - lineIndent,

        message: String

    ): Nothing = throw IniException(currentLine!!, lineCount, lineIndent + charIndex, message)


    /**
     * Decodes the source into a map of sections and
     */
    fun decode(): Ini
    {
        if (::bufferedSource.isInitialized)
            throw IllegalStateException("This decoder was already used.")

        bufferedSource = inputStream.source().buffer()

        ini = Ini()
        lineCount = 0

        while (true)
        {
            if (!nextLine())
                break

            decodeLine()
            lineCount++
        }

        close()
        return ini
    }


    /**
     * Closes the internal source buffer, if the decoder is still running this will cancel it.
     */
    override fun close()
    {
        if (::bufferedSource.isInitialized)
            bufferedSource.close()
    }


    companion object
    {
        /**
         * The `ini` extension constant.
         */
        const val EXTENSION = "ini"

        /**
         * The INI structure regular expression.
         */
        private val STRUCTURE_REGEX = "^.[^,]*(?:,.[^,]*)+$".toRegex()
    }
}


private class IniException(line: String, lineIndex: Int, charIndex: Int, message: String) :
    Exception(
        """
            Error while reading line $lineIndex ($charIndex): $message
            "$line"
            ${' ' * (charIndex)}^
        """.trimIndent()
    )