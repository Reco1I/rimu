package com.reco1l.javaserialization

import okio.ByteString
import okio.IOException

/**
 * Thrown when a stream isn't a valid Java serialization stream, or uses a part of the protocol
 * [JavaObjectInputStream] doesn't read.
 */
class JavaSerializationException(message: String, cause: Throwable? = null) : IOException(message, cause)

/**
 * The description of a class, as a stream writes it before the first instance of the class (or the
 * class itself, an enum constant or an array of it).
 */
class JavaClassDescription internal constructor(
    /**
     * The name of the class, as `Class.getName` returns it (`java.util.HashSet`, `[I`,
     * `com.example.Outer$Inner`).
     */
    val name: String,
    /**
     * The `serialVersionUID` of the class.
     */
    val serialVersionUID: Long,
) {
    /**
     * The `SC_*` flags of the class, see [isEnum], [hasWriteMethod] and [isExternalizable].
     */
    var flags: Int = 0
        internal set

    /**
     * The serializable fields of the class, in the order their values are written: the primitive
     * ones, then the objects, each sorted by name.
     */
    var fields: List<JavaField> = emptyList()
        internal set

    /**
     * What the class's `annotateClass` wrote, usually nothing.
     */
    var annotations: List<Any?> = emptyList()
        internal set

    /**
     * The description of the closest serializable superclass, or `null` if there's none.
     */
    var superclass: JavaClassDescription? = null
        internal set

    /**
     * Whether the class is an enum.
     */
    val isEnum get() = flags and StreamConstants.SC_ENUM != 0

    /**
     * Whether the class has its own `writeObject` method, which writes what [JavaObject.annotations]
     * hold after the fields.
     */
    val hasWriteMethod get() = flags and StreamConstants.SC_WRITE_METHOD != 0

    /**
     * Whether the class is `java.io.Externalizable`, writing all its data itself.
     */
    val isExternalizable get() = flags and StreamConstants.SC_EXTERNALIZABLE != 0

    /**
     * This class and its serializable superclasses, from the topmost superclass down to this class,
     * the order their data is written in.
     */
    val hierarchy: List<JavaClassDescription>
        get() = generateSequence(this) { it.superclass }.toList().asReversed()

    override fun toString() = name
}

/**
 * A serializable field of a class.
 */
class JavaField internal constructor(
    /**
     * The name of the field.
     */
    val name: String,
    /**
     * The JVM type code of the field: `B`, `C`, `D`, `F`, `I`, `J`, `S` or `Z` for a primitive, `L` for
     * an object and `[` for an array.
     */
    val typeCode: Char,
    /**
     * The JVM descriptor of the type of an object or array field (`Ljava/lang/String;`, `[I`), or
     * `null` for a primitive one.
     */
    val typeName: String?,
) {
    /**
     * Whether the field is of a primitive type.
     */
    val isPrimitive get() = typeCode != 'L' && typeCode != '['

    override fun toString() = "$name: ${typeName ?: typeCode}"
}

/**
 * An instance of a serializable class read from a stream.
 */
class JavaObject internal constructor(
    /**
     * The class of the object.
     */
    val description: JavaClassDescription,
) {
    internal val mutableFields = LinkedHashMap<String, Any?>()
    internal val mutableAnnotations = LinkedHashMap<String, List<Any?>>()

    /**
     * The name of the class of the object.
     */
    val className get() = description.name

    /**
     * The values of the fields of the object, by name. Primitives are their Kotlin types (`Int`,
     * `Long`, `Char`…), objects what [JavaObjectInputStream.readObject] reads. A field a superclass
     * declares with the same name as one of a subclass is only kept as the subclass's: read it from
     * [fieldsOf] instead.
     */
    val fields: Map<String, Any?> get() = mutableFields

    private val fieldsByClass = LinkedHashMap<String, Map<String, Any?>>()

    /**
     * What each class of the object's hierarchy wrote after its fields with its own `writeObject`
     * method (or all of an `Externalizable` object's data), by class name: [ByteString]s for the
     * primitive data and the objects between them, in the order they were written.
     */
    val annotations: Map<String, List<Any?>> get() = mutableAnnotations

    /**
     * The values of the fields the class named [className] of the object's hierarchy declares.
     */
    fun fieldsOf(className: String): Map<String, Any?> = fieldsByClass[className] ?: emptyMap()

    internal fun putFields(className: String, values: Map<String, Any?>) {
        fieldsByClass[className] = values
        mutableFields.putAll(values)
    }

    override fun toString() = "$className$fields"
}

/**
 * An enum constant read from a stream.
 */
class JavaEnum internal constructor(
    /**
     * The enum class of the constant.
     */
    val description: JavaClassDescription,
    /**
     * The name of the constant.
     */
    val constant: String,
) {
    /**
     * The name of the enum class of the constant.
     */
    val className get() = description.name

    override fun toString() = "$className.$constant"
}

/**
 * An array read from a stream.
 */
class JavaArray internal constructor(
    /**
     * The array class (`[I`, `[Ljava.lang.String;`).
     */
    val description: JavaClassDescription,
    /**
     * The elements of the array: boxed primitives for a primitive array, what
     * [JavaObjectInputStream.readObject] reads for an array of objects.
     */
    val elements: List<Any?>,
) {
    /**
     * The name of the array class.
     */
    val className get() = description.name

    override fun toString() = "$className$elements"
}

/**
 * A `java.lang.Class` read from a stream.
 */
class JavaClass internal constructor(
    /**
     * The class it is.
     */
    val description: JavaClassDescription,
) {
    /**
     * The name of the class.
     */
    val name get() = description.name

    override fun toString() = "class $name"
}
