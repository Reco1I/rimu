package com.reco1l.javaserialization

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialFormat
import kotlinx.serialization.SerializationException
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import okio.Buffer
import okio.IOException
import okio.Source
import okio.use

/**
 * A kotlinx.serialization format that decodes what `java.io.ObjectOutputStream` wrote into
 * `@Serializable` classes, without the JVM. It's decode-only.
 *
 * Values are read with [JavaObjectInputStream] and then mapped onto the serializer's descriptor:
 *
 * - Classes are decoded from [JavaObject]s, each property from the Java field with its serial name
 *   (rename it with `@SerialName` to match the field). Java fields the class doesn't have are
 *   ignored, and a missing property takes its default value, or throws if it has none.
 * - Primitives are decoded from Java primitives of the same type, or from their boxes
 *   (`java.lang.Integer`…). A `String` is decoded from a Java string, or from an enum constant as
 *   its name.
 * - Enums are decoded from [JavaEnum]s by the constant's name (or from strings).
 * - Lists, sets and arrays are decoded from Java arrays and the JDK's collections: `ArrayList`,
 *   `LinkedList`, `ArrayDeque`, `Vector`, `HashSet`, `LinkedHashSet`, `TreeSet`, `EnumSet`,
 *   `PriorityQueue`, `Arrays.asList`, `List.of`/`Set.of`, and the `Collections` wrappers.
 * - Maps are decoded from `HashMap`, `LinkedHashMap`, `TreeMap`, `Hashtable`, `EnumMap`,
 *   `IdentityHashMap`, `ConcurrentHashMap`, `Map.of` and the `Collections` wrappers.
 * - Polymorphic values are decoded by their Java class name as serial name: annotate the subclasses
 *   with `@SerialName("com.example.Shape\$Circle")`.
 *
 * Mapping errors throw a [SerializationException], errors of the stream a
 * [JavaSerializationException].
 */
sealed class JavaSerialization(
    override val serializersModule: SerializersModule,
) : SerialFormat {

    /**
     * The format with no serializers module.
     */
    companion object Default : JavaSerialization(EmptySerializersModule())

    /**
     * Decodes [value], something [JavaObjectInputStream.readObject] returned, with [deserializer].
     */
    fun <T> decodeFromJavaValue(deserializer: DeserializationStrategy<T>, value: Any?): T =
        deserializer.deserialize(JavaValueDecoder(this, value))

    /**
     * Decodes the first object of the serialization stream in [source], which is closed afterwards.
     *
     * @throws IOException If the stream can't be read.
     */
    fun <T> decodeFromSource(deserializer: DeserializationStrategy<T>, source: Source): T =
        JavaObjectInputStream(source).use { decodeFromJavaValue(deserializer, it.readObject()) }

    /**
     * Decodes the first object of the serialization stream in [bytes].
     *
     * @throws IOException If the stream can't be read.
     */
    fun <T> decodeFromByteArray(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T =
        decodeFromSource(deserializer, Buffer().write(bytes))

    /**
     * The settings of a [JavaSerialization] being created, starting from those of `from`.
     */
    class Builder internal constructor(from: JavaSerialization) {
        /**
         * The module the serializers of contextual and polymorphic values are looked up in.
         */
        var serializersModule: SerializersModule = from.serializersModule
    }
}

private class JavaSerializationImpl(serializersModule: SerializersModule) : JavaSerialization(serializersModule)

/**
 * Creates a [JavaSerialization] format, starting from the settings of [from].
 */
fun JavaSerialization(from: JavaSerialization = JavaSerialization, builder: JavaSerialization.Builder.() -> Unit): JavaSerialization =
    JavaSerializationImpl(JavaSerialization.Builder(from).apply(builder).serializersModule)

/**
 * Decodes [value], something [JavaObjectInputStream.readObject] returned, as [T].
 */
inline fun <reified T> JavaSerialization.decodeFromJavaValue(value: Any?): T =
    decodeFromJavaValue(serializersModule.serializer<T>(), value)

/**
 * Decodes the first object of the serialization stream in [source] as [T].
 */
inline fun <reified T> JavaSerialization.decodeFromSource(source: Source): T =
    decodeFromSource(serializersModule.serializer<T>(), source)

/**
 * Decodes the first object of the serialization stream in [bytes] as [T].
 */
inline fun <reified T> JavaSerialization.decodeFromByteArray(bytes: ByteArray): T =
    decodeFromByteArray(serializersModule.serializer<T>(), bytes)

/**
 * Reads the next object of the stream with [readObject] and decodes it with [deserializer].
 */
fun <T> JavaObjectInputStream.decodeObject(deserializer: DeserializationStrategy<T>, format: JavaSerialization = JavaSerialization): T =
    format.decodeFromJavaValue(deserializer, readObject())

/**
 * Reads the next object of the stream with [readObject] and decodes it as [T].
 */
inline fun <reified T> JavaObjectInputStream.decodeObject(format: JavaSerialization = JavaSerialization): T =
    decodeObject(format.serializersModule.serializer<T>(), format)
