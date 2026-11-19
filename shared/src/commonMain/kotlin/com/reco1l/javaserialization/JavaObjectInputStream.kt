package com.reco1l.javaserialization

import com.reco1l.javaserialization.StreamConstants.BASE_WIRE_HANDLE
import com.reco1l.javaserialization.StreamConstants.SC_BLOCK_DATA
import com.reco1l.javaserialization.StreamConstants.SC_SERIALIZABLE
import com.reco1l.javaserialization.StreamConstants.STREAM_MAGIC
import com.reco1l.javaserialization.StreamConstants.STREAM_VERSION
import com.reco1l.javaserialization.StreamConstants.TC_ARRAY
import com.reco1l.javaserialization.StreamConstants.TC_BLOCKDATA
import com.reco1l.javaserialization.StreamConstants.TC_BLOCKDATALONG
import com.reco1l.javaserialization.StreamConstants.TC_CLASS
import com.reco1l.javaserialization.StreamConstants.TC_CLASSDESC
import com.reco1l.javaserialization.StreamConstants.TC_ENDBLOCKDATA
import com.reco1l.javaserialization.StreamConstants.TC_ENUM
import com.reco1l.javaserialization.StreamConstants.TC_EXCEPTION
import com.reco1l.javaserialization.StreamConstants.TC_LONGSTRING
import com.reco1l.javaserialization.StreamConstants.TC_NULL
import com.reco1l.javaserialization.StreamConstants.TC_OBJECT
import com.reco1l.javaserialization.StreamConstants.TC_PROXYCLASSDESC
import com.reco1l.javaserialization.StreamConstants.TC_REFERENCE
import com.reco1l.javaserialization.StreamConstants.TC_RESET
import com.reco1l.javaserialization.StreamConstants.TC_STRING
import okio.BufferedSource
import okio.Closeable
import okio.EOFException
import okio.Source
import okio.buffer

/**
 * Reads what a `java.io.ObjectOutputStream` wrote, without the JVM: the classes of the objects
 * don't have to exist, objects are read as their data ([JavaObject], [JavaEnum], [JavaArray],
 * [JavaClass], and [String]s as themselves), which the caller interprets.
 *
 * Primitives written with `writeInt`, `writeLong`… are read with the functions of the same name,
 * and objects with [readObject], in the order they were written, as with `java.io.ObjectInputStream`.
 *
 * Dynamic proxies, `Externalizable` objects written with the stream protocol version 1 and
 * serialized exceptions aren't supported: reading them throws a [JavaSerializationException].
 *
 * @param source The stream, from its header on. It's closed with this.
 * @throws JavaSerializationException If [source] doesn't start with a serialization stream header.
 */
class JavaObjectInputStream(source: Source) : Closeable {

    private val source: BufferedSource = source as? BufferedSource ?: source.buffer()

    private val handles = ArrayList<Any?>()

    // The bytes left in the current block data record, which the primitives are read from.
    private var blockRemaining = 0L

    init {
        val magic = this.source.readShort()
        val version = this.source.readShort()

        if (magic != STREAM_MAGIC || version != STREAM_VERSION) {
            throw JavaSerializationException("Not a Java serialization stream (header ${magic.hex()} ${version.hex()})")
        }
    }

    /**
     * Reads the next object: `null`, a [String], a [JavaObject], a [JavaEnum], a [JavaArray], a
     * [JavaClass] or a [JavaClassDescription]. An object written more than once is the same
     * instance each time.
     *
     * @throws JavaSerializationException If primitive data is next instead.
     */
    fun readObject(): Any? {
        if (blockRemaining > 0) {
            throw JavaSerializationException("Expected an object, $blockRemaining bytes of primitive data are left")
        }

        return readContent()
    }

    fun readBoolean() = readBlockByte() != 0.toByte()

    fun readByte() = readBlockByte()

    fun readUnsignedByte() = readBlockByte().toInt() and 0xFF

    fun readShort() = readBlockBits(2).toShort()

    fun readUnsignedShort() = readBlockBits(2).toInt() and 0xFFFF

    fun readChar() = readBlockBits(2).toInt().toChar()

    fun readInt() = readBlockBits(4).toInt()

    fun readLong() = readBlockBits(8)

    fun readFloat() = Float.fromBits(readInt())

    fun readDouble() = Double.fromBits(readLong())

    /**
     * Reads a string written with `writeUTF`.
     */
    fun readUTF() = decodeModifiedUtf8(readFully(readUnsignedShort()))

    /**
     * Reads the next [byteCount] bytes of primitive data, written with `write`.
     */
    fun readFully(byteCount: Int): ByteArray {
        val bytes = ByteArray(byteCount)
        readFully(bytes, 0, byteCount)
        return bytes
    }

    /**
     * Reads the next [length] bytes of primitive data into [bytes], from [offset].
     */
    fun readFully(bytes: ByteArray, offset: Int, length: Int) {
        var read = 0

        while (read < length) {
            ensureBlock()
            val count = source.read(bytes, offset + read, minOf((length - read).toLong(), blockRemaining).toInt())

            if (count < 0) {
                throw EOFException("The stream ended with ${length - read} bytes left to read")
            }

            read += count
            blockRemaining -= count
        }
    }

    override fun close() = source.close()


    // Primitive data

    private fun ensureBlock() {
        while (blockRemaining == 0L) {
            when (val tag = source.readByte()) {
                TC_BLOCKDATA -> blockRemaining = source.readByte().toLong() and 0xFF
                TC_BLOCKDATALONG -> blockRemaining = source.readInt().toLong() and 0xFFFFFFFFL
                TC_RESET -> handles.clear()
                else -> throw JavaSerializationException("Expected primitive data, found ${tag.hex()}")
            }
        }
    }

    private fun readBlockByte(): Byte {
        ensureBlock()
        blockRemaining--
        return source.readByte()
    }

    // Reads a big-endian value of byteCount bytes, which may span several block data records.
    private fun readBlockBits(byteCount: Int): Long {
        ensureBlock()

        if (blockRemaining >= byteCount) {
            blockRemaining -= byteCount
            return when (byteCount) {
                2 -> source.readShort().toLong()
                4 -> source.readInt().toLong()
                else -> source.readLong()
            }
        }

        var value = 0L
        repeat(byteCount) { value = (value shl 8) or (readBlockByte().toLong() and 0xFF) }

        // Sign-extends as reading the value at once does.
        val shift = 64 - byteCount * 8
        return (value shl shift) shr shift
    }


    // Objects

    private fun newHandle(value: Any?): Int {
        handles += value
        return handles.size - 1
    }

    private fun readContent() = readContent(source.readByte())

    private fun readContent(tag: Byte): Any? = when (tag) {
        TC_NULL -> null
        TC_REFERENCE -> readReference()
        TC_STRING -> readUtf().also { newHandle(it) }
        TC_LONGSTRING -> readLongString()
        TC_CLASSDESC -> readNewClassDescription()
        TC_CLASS -> JavaClass(readClassDescription() ?: throw JavaSerializationException("A class without description")).also { newHandle(it) }
        TC_ARRAY -> readArray()
        TC_ENUM -> readEnum()
        TC_OBJECT -> readNewObject()

        TC_RESET -> {
            handles.clear()
            readContent()
        }

        TC_PROXYCLASSDESC -> throw JavaSerializationException("Dynamic proxy classes aren't supported")
        TC_EXCEPTION -> throw JavaSerializationException("The stream contains an exception thrown while it was written")
        TC_BLOCKDATA, TC_BLOCKDATALONG -> throw JavaSerializationException("Expected an object, found primitive data")
        else -> throw JavaSerializationException("Unknown type code ${tag.hex()}")
    }

    private fun readReference(): Any? {
        val index = source.readInt() - BASE_WIRE_HANDLE

        if (index !in handles.indices) {
            throw JavaSerializationException("Invalid handle ${(index + BASE_WIRE_HANDLE).toString(16)}")
        }

        return handles[index]
    }

    private fun readLongString(): String {
        val length = source.readLong()

        if (length !in 0..Int.MAX_VALUE) {
            throw JavaSerializationException("String too long: $length bytes")
        }

        val string = decodeModifiedUtf8(source.readByteArray(length))
        newHandle(string)
        return string
    }

    private fun readString(): String = when (val value = readContent()) {
        is String -> value
        else -> throw JavaSerializationException("Expected a string, found $value")
    }

    private fun readUtf() = decodeModifiedUtf8(source.readByteArray(source.readShort().toLong() and 0xFFFF))

    private fun readClassDescription(): JavaClassDescription? = when (val tag = source.readByte()) {
        TC_NULL -> null
        TC_CLASSDESC -> readNewClassDescription()
        TC_REFERENCE -> readReference() as? JavaClassDescription ?: throw JavaSerializationException("Expected a class description")
        TC_PROXYCLASSDESC -> throw JavaSerializationException("Dynamic proxy classes aren't supported")
        else -> throw JavaSerializationException("Expected a class description, found ${tag.hex()}")
    }

    private fun readNewClassDescription(): JavaClassDescription {
        val description = JavaClassDescription(readUtf(), source.readLong())
        newHandle(description)

        description.flags = source.readByte().toInt() and 0xFF
        description.fields = List(source.readShort().toInt() and 0xFFFF) {
            val typeCode = source.readByte().toInt().toChar()
            val name = readUtf()

            when (typeCode) {
                'L', '[' -> JavaField(name, typeCode, readString())
                'B', 'C', 'D', 'F', 'I', 'J', 'S', 'Z' -> JavaField(name, typeCode, null)
                else -> throw JavaSerializationException("Unknown type code '$typeCode' of field ${description.name}.$name")
            }
        }
        description.annotations = readAnnotations()
        description.superclass = readClassDescription()

        return description
    }

    // What a class's annotateClass or an object's writeObject wrote, up to its end.
    private fun readAnnotations(): List<Any?> {
        val contents = ArrayList<Any?>()

        while (true) {
            when (val tag = source.readByte()) {
                TC_ENDBLOCKDATA -> return contents
                TC_BLOCKDATA -> contents += source.readByteString(source.readByte().toLong() and 0xFF)
                TC_BLOCKDATALONG -> contents += source.readByteString(source.readInt().toLong() and 0xFFFFFFFFL)
                else -> contents += readContent(tag)
            }
        }
    }

    private fun readArray(): JavaArray {
        val description = readClassDescription() ?: throw JavaSerializationException("An array without description")
        val elements = ArrayList<Any?>()
        val array = JavaArray(description, elements)
        newHandle(array)

        val size = source.readInt()
        if (size < 0) {
            throw JavaSerializationException("Negative array size $size")
        }

        val elementType = description.name.getOrNull(1) ?: throw JavaSerializationException("Not an array class: ${description.name}")
        elements.ensureCapacity(size)
        repeat(size) { elements += readValue(elementType) }

        return array
    }

    private fun readEnum(): JavaEnum {
        val description = readClassDescription() ?: throw JavaSerializationException("An enum constant without description")
        val handle = newHandle(null)

        val constant = JavaEnum(description, readString())
        handles[handle] = constant
        return constant
    }

    private fun readNewObject(): JavaObject {
        val description = readClassDescription() ?: throw JavaSerializationException("An object without description")
        val obj = JavaObject(description)
        newHandle(obj)

        for (type in description.hierarchy) {
            if (type.isExternalizable) {
                if (type.flags and SC_BLOCK_DATA == 0) {
                    throw JavaSerializationException("${type.name} is externalizable with the stream protocol version 1, which isn't supported")
                }

                obj.mutableAnnotations[type.name] = readAnnotations()
                continue
            }

            if (type.flags and SC_SERIALIZABLE == 0) {
                continue
            }

            val values = LinkedHashMap<String, Any?>()
            for (field in type.fields) {
                values[field.name] = readValue(field.typeCode)
            }
            obj.putFields(type.name, values)

            if (type.hasWriteMethod) {
                obj.mutableAnnotations[type.name] = readAnnotations()
            }
        }

        return obj
    }

    // Reads a field or array element of the given JVM type code, outside block data.
    private fun readValue(typeCode: Char): Any? = when (typeCode) {
        'B' -> source.readByte()
        'C' -> source.readShort().toInt().toChar()
        'D' -> Double.fromBits(source.readLong())
        'F' -> Float.fromBits(source.readInt())
        'I' -> source.readInt()
        'J' -> source.readLong()
        'S' -> source.readShort()
        'Z' -> source.readByte() != 0.toByte()
        'L', '[' -> readContent()
        else -> throw JavaSerializationException("Unknown type code '$typeCode'")
    }
}

private fun Byte.hex() = (toInt() and 0xFF).toString(16).padStart(2, '0')

private fun Short.hex() = (toInt() and 0xFFFF).toString(16).padStart(4, '0')
