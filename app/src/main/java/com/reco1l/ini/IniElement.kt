package com.reco1l.ini

import com.reco1l.framework.kotlin.toBooleanOrNull
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import kotlin.text.StringBuilder


// Array

/**
 * Creates an [IniArray] from a string.
 */
fun IniArray(source: String) = IniArray(source.split(','))

/**
 * Wraps a sequence of primitives into a structure.
 */
class IniArray(source: List<String>? = null)
{

    private val elements = source?.map { it.trim() }?.toMutableList() ?: mutableListOf()


    // Getters

    fun getString(index: Int) = elements.getOrNull(index)?.trim()

    fun getInt(index: Int) = elements.getOrNull(index)?.toIntOrNull()
    fun getLong(index: Int) = elements.getOrNull(index)?.toLongOrNull()
    fun getFloat(index: Int) = elements.getOrNull(index)?.toFloatOrNull()
    fun getDouble(index: Int) = elements.getOrNull(index)?.toDoubleOrNull()
    fun getBoolean(index: Int) = elements.getOrNull(index)?.toBooleanOrNull()


    // Setters

    fun add(value: String) = elements.add(value.trim())
    fun add(value: Number) = elements.add(value.toString())


    // Encoding

    fun encodeToJson() = buildJsonArray { elements.forEach { add(it) } }

    fun encodeToString(builder: StringBuilder = StringBuilder()): StringBuilder
    {
        elements.forEachIndexed { index, value ->

            if (index != 0)
                builder.append(',')

            builder.append(value)
        }

        return builder
    }


    override fun toString() = encodeToString().toString()
}


class IniSection(

    val name: String,

    val parent: IniSection? = null,

    elements: MutableMap<String?, String> = mutableMapOf()

)
{

    private val keys = elements.keys.map { it?.trim() }.toMutableList()

    private val values = elements.values.map { it.trim() }.toMutableList()


    private val nestedSections = mutableListOf<IniSection>()

    private val parentsHierarchy by lazy {

        var hierarchy = name

        fun prependParentName(parent: IniSection?)
        {
            if (parent == null)
                return

            hierarchy = parent.name + '.' + hierarchy

            if (parent.parent != null)
                prependParentName(parent.parent)
        }

        prependParentName(parent)
        hierarchy
    }



    // Nested sections

    fun putSection(key: String): IniSection
    {
        val section = getSection(key) ?: IniSection(key, this)
        nestedSections.add(section)
        return section
    }

    fun getSection(key: String) = nestedSections.find { it.name == key }


    // Getters

    fun getString(key: String) = get(key)
    fun getArray(key: String) = get(key)?.let { IniArray(it) }

    fun getInt(key: String) = get(key)?.toIntOrNull()
    fun getLong(key: String) = get(key)?.toLongOrNull()
    fun getFloat(key: String) = get(key)?.toFloatOrNull()
    fun getDouble(key: String) = get(key)?.toDoubleOrNull()
    fun getBoolean(key: String) = get(key)?.toBooleanOrNull()


    private operator fun get(key: String): String?
    {
        val index = keys.indexOf(key)
        if (index == -1)
            return null

        return values[index]
    }


    // Setters

    fun set(key: String?, value: Collection<Any>) = set(key, value.joinToString(","))

    fun set(key: String?, value: Number) = set(key, value.toString())

    operator fun set(key: String?, value: String)
    {
        // We cannot set from a null key, commands without name share the same key in the list.
        if (key == null)
        {
            keys.add(null)
            values.add(value.trim())
            return
        }

        val index = keys.indexOf(key)

        if (index == -1)
        {
            keys.add(key)
            values.add(value.trim())
            return
        }

        values[index] = value
    }


    /**
     * Encodes the section into an string format.
     *
     * @param delimiter The delimiter to delimit key-value entries.
     * @param prependParentName Determines if the parent section names should be prepend at the nested
     * sections declaration with a dot when the entry corresponds to a [IniSection].
     */
    fun encodeToString(

        delimiter: Char = ':',

        builder: StringBuilder = StringBuilder()

    ): StringBuilder = builder.apply {

        // Appending section declaration.
        append('[').append(parentsHierarchy).append(']')
        appendLine()

        // Appending entries.
        keys.forEachIndexed { index, key ->

            // Appending key and delimiter if it has a key.
            if (key != null)
                append(key).append(delimiter).append(' ')

            append(values[index])
            appendLine()
        }

        // Appending nested sections
        nestedSections.forEach { it.encodeToString(delimiter, this) }
    }


    fun encodeToJson(): JsonElement
    {
        val isOnlyValues = keys.all { it == null }

        // If the section only contains structures we can just return a JsonArray instead.
        if (isOnlyValues && nestedSections.isEmpty())
            return buildJsonArray { values.forEach { add(it) } }

        return buildJsonObject {

            // Adding nested sections first.
            nestedSections.forEach { put(it.name, it.encodeToJson()) }

            var nullCount = 0

            // Adding key-value pairs.
            keys.forEachIndexed { index, key ->

                if (key == null)
                    nullCount++
                else
                    put(key, values[index])
            }

            if (nullCount > 0)
            {
                // Since Json doesn't allow null key entries we add all entries without key into an array
                // with default key 'NO_KEY'.
                put("NO_KEY", buildJsonArray {

                    keys.forEachIndexed { index, key -> if (key == null) add(values[index]) }
                })
            }
        }
    }


    override fun toString() = encodeToString().toString()
}


class Ini(val elements: MutableMap<String, IniSection> = mutableMapOf())
{

    operator fun get(name: String) = elements[name]

    operator fun set(name: String, section: IniSection) = elements.put(name, section)


    fun put(name: String) = get(name) ?: IniSection(name).also { set(name, it) }


    fun encodeToString(

        delimiter: Char = ':',

        builder: StringBuilder = StringBuilder()

    ): StringBuilder
    {
        elements.values.forEach { it.encodeToString(delimiter, builder) }
        return builder
    }

    fun encodeToFile(

        file: File,

        delimiter: Char = ':',

        builder: StringBuilder = StringBuilder()
    )
    {
        if (file.exists())
            file.delete()

        file.createNewFile()
        file.writeText(encodeToString(delimiter, builder).toString())
    }

    fun encodeToJson() = buildJsonObject {

        elements.forEach { (key, value) -> put(key, value.encodeToJson()) }
    }


    override fun toString() = encodeToString().toString()
}