@file:OptIn(ExperimentalSerializationApi::class)

package com.reco1l.javaserialization

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE
import kotlinx.serialization.encoding.CompositeDecoder.Companion.UNKNOWN_NAME
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.modules.SerializersModule
import okio.ByteString

// Decoding is over the tree JavaObjectInputStream reads: each value has its own decoder, and each
// structure a composite decoder whose elements are decoded with decoders of their values.

/**
 * Decodes a single value read from a stream.
 */
internal class JavaValueDecoder(
    private val format: JavaSerialization,
    private val value: Any?,
) : Decoder {

    override val serializersModule: SerializersModule get() = format.serializersModule

    override fun decodeNotNullMark() = value != null

    override fun decodeNull(): Nothing? = null

    override fun decodeBoolean() = primitive<Boolean>("boolean")

    override fun decodeByte() = primitive<Byte>("byte")

    override fun decodeShort() = primitive<Short>("short")

    override fun decodeChar() = primitive<Char>("char")

    override fun decodeInt() = primitive<Int>("int")

    override fun decodeLong() = primitive<Long>("long")

    override fun decodeFloat() = primitive<Float>("float")

    override fun decodeDouble() = primitive<Double>("double")

    override fun decodeString() = when (value) {
        is String -> value
        is JavaEnum -> value.constant
        else -> throw mismatch("a string")
    }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        val name = when (value) {
            is JavaEnum -> value.constant
            is String -> value
            else -> throw mismatch("an enum constant of ${enumDescriptor.serialName}")
        }

        val index = enumDescriptor.getElementIndex(name)
        if (index == UNKNOWN_NAME) {
            throw SerializationException("${enumDescriptor.serialName} has no constant $name")
        }
        return index
    }

    override fun decodeInline(descriptor: SerialDescriptor): Decoder = this

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder = when (descriptor.kind) {
        StructureKind.CLASS -> ClassDecoder(format, value as? JavaObject ?: throw mismatch("an object of ${descriptor.serialName}"))
        StructureKind.OBJECT -> ClassDecoder(format, null)
        StructureKind.LIST -> SequenceDecoder(format, collectionElements(value) ?: throw mismatch("an array or a collection"), 1)
        StructureKind.MAP -> SequenceDecoder(format, mapEntries(value) ?: throw mismatch("a map"), 2)
        is PolymorphicKind -> PolymorphicDecoder(format, value)
        else -> throw SerializationException("Can't decode ${descriptor.serialName} of kind ${descriptor.kind}")
    }

    private inline fun <reified T> primitive(type: String): T = unbox(value) as? T ?: throw mismatch("a $type")

    private fun mismatch(expected: String) = SerializationException("Expected $expected, found ${describe(value)}")
}

/**
 * The elements of a structure, each decoded from its value.
 */
private abstract class JavaCompositeDecoder(protected val format: JavaSerialization) : CompositeDecoder {

    override val serializersModule: SerializersModule get() = format.serializersModule

    /**
     * The value of the element at [index].
     */
    abstract fun valueAt(descriptor: SerialDescriptor, index: Int): Any?

    private fun element(descriptor: SerialDescriptor, index: Int) = JavaValueDecoder(format, valueAt(descriptor, index))

    override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int) = element(descriptor, index).decodeBoolean()

    override fun decodeByteElement(descriptor: SerialDescriptor, index: Int) = element(descriptor, index).decodeByte()

    override fun decodeShortElement(descriptor: SerialDescriptor, index: Int) = element(descriptor, index).decodeShort()

    override fun decodeCharElement(descriptor: SerialDescriptor, index: Int) = element(descriptor, index).decodeChar()

    override fun decodeIntElement(descriptor: SerialDescriptor, index: Int) = element(descriptor, index).decodeInt()

    override fun decodeLongElement(descriptor: SerialDescriptor, index: Int) = element(descriptor, index).decodeLong()

    override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int) = element(descriptor, index).decodeFloat()

    override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int) = element(descriptor, index).decodeDouble()

    override fun decodeStringElement(descriptor: SerialDescriptor, index: Int) = element(descriptor, index).decodeString()

    override fun decodeInlineElement(descriptor: SerialDescriptor, index: Int): Decoder = element(descriptor, index)

    override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        previousValue: T?,
    ): T = deserializer.deserialize(element(descriptor, index))

    override fun <T : Any> decodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>,
        previousValue: T?,
    ): T? {
        val decoder = element(descriptor, index)
        return if (decoder.decodeNotNullMark()) deserializer.deserialize(decoder) else null
    }

    override fun endStructure(descriptor: SerialDescriptor) = Unit
}

/**
 * The properties of a class, from the fields of [obj] with their names. `null` for an `object`,
 * which has none.
 */
private class ClassDecoder(format: JavaSerialization, private val obj: JavaObject?) : JavaCompositeDecoder(format) {

    private var next = 0

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        val fields = obj?.fields ?: return DECODE_DONE

        // The properties the object has a field of, in order: the others take their default.
        while (next < descriptor.elementsCount) {
            val index = next++
            if (descriptor.getElementName(index) in fields) {
                return index
            }
        }

        return DECODE_DONE
    }

    override fun valueAt(descriptor: SerialDescriptor, index: Int) = obj!!.fields[descriptor.getElementName(index)]
}

/**
 * The elements of a list, or the keys and values of a map, one after the other ([valuesPerEntry]
 * is 2), all decoded in order.
 */
private class SequenceDecoder(
    format: JavaSerialization,
    private val values: List<Any?>,
    private val valuesPerEntry: Int,
) : JavaCompositeDecoder(format) {

    private var next = 0

    override fun decodeSequentially() = true

    override fun decodeCollectionSize(descriptor: SerialDescriptor) = values.size / valuesPerEntry

    override fun decodeElementIndex(descriptor: SerialDescriptor) = if (next < values.size) next++ else DECODE_DONE

    override fun valueAt(descriptor: SerialDescriptor, index: Int) = values[index]
}

/**
 * A polymorphic value: its Java class name as its type, then itself.
 */
private class PolymorphicDecoder(format: JavaSerialization, private val value: Any?) : JavaCompositeDecoder(format) {

    private var next = 0

    override fun decodeElementIndex(descriptor: SerialDescriptor) = if (next < 2) next++ else DECODE_DONE

    override fun valueAt(descriptor: SerialDescriptor, index: Int) = if (index == 0) javaClassName(value) else value
}


// The JDK's classes

private val boxes = setOf(
    "java.lang.Boolean",
    "java.lang.Byte",
    "java.lang.Short",
    "java.lang.Character",
    "java.lang.Integer",
    "java.lang.Long",
    "java.lang.Float",
    "java.lang.Double",
)

/**
 * [value], or the primitive in it if it's a box (`java.lang.Integer`…).
 */
private fun unbox(value: Any?): Any? = if (value is JavaObject && value.className in boxes) value.fields["value"] else value

/**
 * The Java class name of [value], which polymorphic values are decoded by.
 */
private fun javaClassName(value: Any?): String = when (value) {
    is JavaObject -> value.className
    is JavaEnum -> value.className
    is JavaArray -> value.className
    is JavaClass -> "java.lang.Class"
    is String -> "java.lang.String"
    null -> throw SerializationException("A polymorphic value can't be null")
    else -> throw SerializationException("A polymorphic value can't be the primitive $value")
}

private fun describe(value: Any?): String = when (value) {
    null -> "null"
    is String -> "a string"
    is JavaObject -> "an object of ${value.className}"
    is JavaEnum -> "the enum constant $value"
    is JavaArray -> "an array ${value.className}"
    is JavaClass -> "the class ${value.name}"
    else -> "the ${value::class.simpleName} $value"
}

/**
 * What the class [className] of [obj] wrote with its `writeObject` method, without its primitive
 * data: the elements of most collections, the keys and values of most maps, in order.
 */
private fun writtenObjects(obj: JavaObject, className: String): List<Any?>? =
    obj.annotations[className]?.filter { it !is ByteString }

/**
 * The elements of [value], a Java array or collection, or `null` if it's neither.
 */
private fun collectionElements(value: Any?): List<Any?>? {
    if (value is JavaArray) {
        return value.elements
    }

    if (value !is JavaObject) {
        return null
    }

    val name = value.className

    return when {
        name == "java.util.EnumSet\$SerializationProxy" -> (value.fields["elements"] as? JavaArray)?.elements
        name == "java.util.Arrays\$ArrayList" -> (value.fields["a"] as? JavaArray)?.elements

        name == "java.util.Vector" || name == "java.util.Stack" -> {
            val count = value.fields["elementCount"] as? Int ?: return null
            (value.fields["elementData"] as? JavaArray)?.elements?.take(count)
        }

        // The comparator, then the elements.
        name == "java.util.TreeSet" -> writtenObjects(value, name)?.drop(1)

        name == "java.util.Collections\$SingletonList" || name == "java.util.Collections\$SingletonSet" -> listOf(value.fields["element"])
        name == "java.util.Collections\$EmptyList" || name == "java.util.Collections\$EmptySet" -> emptyList()

        // Collections.unmodifiableList, synchronizedSet, checkedList… wrap the collection in c.
        name.startsWith("java.util.Collections\$") && "c" in value.fields -> collectionElements(value.fields["c"])

        // ArrayList, LinkedList, ArrayDeque, PriorityQueue, HashSet (and LinkedHashSet), List.of…
        else -> value.description.hierarchy.asReversed().firstNotNullOfOrNull { writtenObjects(value, it.name) }
    }
}

/**
 * The keys and values of [value], a Java map, one after the other, or `null` if it isn't a map.
 */
private fun mapEntries(value: Any?): List<Any?>? {
    if (value !is JavaObject) {
        return null
    }

    val name = value.className

    val entries = when {
        name == "java.util.Collections\$SingletonMap" -> listOf(value.fields["k"], value.fields["v"])
        name == "java.util.Collections\$EmptyMap" -> emptyList()
        name.startsWith("java.util.Collections\$") && "m" in value.fields -> return mapEntries(value.fields["m"])

        // A null key and value end its entries.
        name == "java.util.concurrent.ConcurrentHashMap" -> writtenObjects(value, name)?.dropLast(2)

        // HashMap (and LinkedHashMap), TreeMap, Hashtable, EnumMap, IdentityHashMap, Map.of…
        else -> value.description.hierarchy.asReversed().firstNotNullOfOrNull { writtenObjects(value, it.name) }
    } ?: return null

    if (entries.size % 2 != 0) {
        throw SerializationException("$name has a key without a value")
    }

    return entries
}
