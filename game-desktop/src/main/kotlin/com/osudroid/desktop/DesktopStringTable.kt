package com.osudroid.desktop

import com.osudroid.resources.StringArrayResource
import com.osudroid.resources.StringResource
import org.w3c.dom.Element
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The game's [StringTable] on desktop: the English strings of the language pack, read from the
 * resources the build copies, then the strings not in the language pack yet.
 */
class DesktopStringTable : StringTable.Implementation {

    private val strings = HashMap<String, String>()

    private val arrays = HashMap<String, Array<String>>()


    init {
        read("strings/strings_temporary_for_locals.xml")
        read("strings/language-pack.xml")
    }


    override fun get(resource: StringResource) = strings[resource.name] ?: resource.name

    override fun getArray(resource: StringArrayResource) = arrays[resource.name] ?: emptyArray()

    override fun get(resid: Int): String = throw UnsupportedOperationException("Android resource IDs aren't available on desktop")

    override fun format(format: String, vararg args: Any?): String = String.format(format, *args)


    private fun read(path: String) {
        val input = javaClass.classLoader.getResourceAsStream(path) ?: return
        val document = input.use { DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it) }
        val nodes = document.documentElement.childNodes

        for (i in 0 until nodes.length) {
            val node = nodes.item(i) as? Element ?: continue
            val name = node.getAttribute("name")

            when (node.tagName) {
                "string" -> strings[name] = unescape(node.textContent)

                "string-array" -> {
                    val items = node.getElementsByTagName("item")
                    arrays[name] = Array(items.length) { unescape(items.item(it).textContent) }
                }
            }
        }
    }

    // Android's escapes in string resources.
    private fun unescape(text: String): String {
        var value = text.trim()

        if (value.length >= 2 && value.startsWith('"') && value.endsWith('"')) {
            value = value.substring(1, value.length - 1)
        }

        return buildString {
            var i = 0
            while (i < value.length) {
                val c = value[i]

                if (c == '\\' && i + 1 < value.length) {
                    when (val next = value[i + 1]) {
                        'n' -> append('\n')
                        't' -> append('\t')
                        else -> append(next)
                    }
                    i += 2
                    continue
                }

                append(c)
                i++
            }
        }
    }
}
