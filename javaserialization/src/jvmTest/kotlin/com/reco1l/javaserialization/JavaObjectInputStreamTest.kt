package com.reco1l.javaserialization

import java.io.ByteArrayOutputStream
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput
import java.io.ObjectOutputStream
import java.io.ObjectStreamConstants
import java.io.Serializable
import java.util.EnumSet
import okio.Buffer
import okio.ByteString
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class JavaObjectInputStreamTest {

    enum class Color { RED, GREEN, BLUE }

    open class Base : Serializable {
        @JvmField var id = 7L
        @JvmField var shared = "base"

        companion object {
            private const val serialVersionUID = 1L
        }
    }

    class Point(@JvmField var x: Int, @JvmField var y: Float) : Base() {
        @JvmField var label: String? = "p"
        @JvmField var flag = true
        @JvmField var letter = 'q'
        @JvmField var small: Short = -3
        @JvmField var tiny: Byte = 9
        @JvmField var precise = 0.25
        @JvmField var color = Color.GREEN
        @JvmField var next: Point? = null
        @JvmField var shared2 = "point"

        companion object {
            private const val serialVersionUID = 42L
        }
    }

    class WithWriteMethod : Serializable {
        @JvmField var value = 5

        private fun writeObject(out: ObjectOutputStream) {
            out.defaultWriteObject()
            out.writeInt(99)
            out.writeObject("extra")
        }

        companion object {
            private const val serialVersionUID = 2L
        }
    }

    class External : Externalizable {
        override fun writeExternal(out: ObjectOutput) {
            out.writeInt(1234)
            out.writeObject("inside")
        }

        override fun readExternal(input: ObjectInput) = Unit
    }


    private fun stream(protocol: Int = ObjectStreamConstants.PROTOCOL_VERSION_2, write: ObjectOutputStream.() -> Unit): JavaObjectInputStream {
        val bytes = ByteArrayOutputStream()
        ObjectOutputStream(bytes).use {
            it.useProtocolVersion(protocol)
            it.write()
        }
        return JavaObjectInputStream(Buffer().write(bytes.toByteArray()))
    }


    @Test
    fun testRejectsOtherData() {
        assertThrows(JavaSerializationException::class.java) {
            JavaObjectInputStream(Buffer().writeUtf8("{\"json\":true}"))
        }
    }

    @Test
    fun testPrimitives() {
        val input = stream {
            writeBoolean(true)
            writeByte(-2)
            writeShort(-300)
            writeChar('é'.code)
            writeInt(Int.MIN_VALUE)
            writeLong(-123_456_789_012L)
            writeFloat(1.5f)
            writeDouble(-2.25)
            writeUTF("héllo")
            write(byteArrayOf(1, 2, 3))
        }

        assertEquals(true, input.readBoolean())
        assertEquals((-2).toByte(), input.readByte())
        assertEquals((-300).toShort(), input.readShort())
        assertEquals('é', input.readChar())
        assertEquals(Int.MIN_VALUE, input.readInt())
        assertEquals(-123_456_789_012L, input.readLong())
        assertEquals(1.5f, input.readFloat())
        assertEquals(-2.25, input.readDouble(), 0.0)
        assertEquals("héllo", input.readUTF())
        assertArrayEquals(byteArrayOf(1, 2, 3), input.readFully(3))
    }

    @Test
    fun testPrimitivesAcrossBlocks() {
        // Block data records hold at most 1024 bytes, so these ints and longs span records.
        val input = stream {
            write(ByteArray(1023))
            writeInt(0x01020304)
            repeat(1000) { writeLong(it * -31L) }
            writeObject("between")
            writeInt(-1)
        }

        assertArrayEquals(ByteArray(1023), input.readFully(1023))
        assertEquals(0x01020304, input.readInt())
        repeat(1000) { assertEquals(it * -31L, input.readLong()) }
        assertEquals("between", input.readObject())
        assertEquals(-1, input.readInt())
    }

    @Test
    fun testObjectBeforePrimitivesAreRead() {
        val input = stream {
            writeInt(1)
            writeObject("after")
        }

        assertThrows(JavaSerializationException::class.java) { input.readObject() }
    }

    @Test
    fun testStrings() {
        val long = "a".repeat(70_000)
        val input = stream {
            writeObject("plain")
            writeObject("nul\u0000 ünïcödé 日本 🎵")
            writeObject(long)
            writeObject(null)
        }

        assertEquals("plain", input.readObject())
        assertEquals("nul\u0000 ünïcödé 日本 🎵", input.readObject())
        assertEquals(long, input.readObject())
        assertNull(input.readObject())
    }

    @Test
    fun testObjects() {
        val point = Point(3, 4.5f).apply { next = Point(-1, 0f) }
        val input = stream {
            writeObject(point)
            writeObject(point)
        }

        val obj = input.readObject() as JavaObject
        assertEquals(Point::class.java.name, obj.className)
        assertEquals(42L, obj.description.serialVersionUID)
        assertEquals(Base::class.java.name, obj.description.superclass!!.name)

        assertEquals(3, obj.fields["x"])
        assertEquals(4.5f, obj.fields["y"])
        assertEquals("p", obj.fields["label"])
        assertEquals(true, obj.fields["flag"])
        assertEquals('q', obj.fields["letter"])
        assertEquals((-3).toShort(), obj.fields["small"])
        assertEquals(9.toByte(), obj.fields["tiny"])
        assertEquals(0.25, obj.fields["precise"])
        assertEquals(7L, obj.fields["id"])
        assertEquals("base", obj.fieldsOf(Base::class.java.name)["shared"])
        assertEquals("point", obj.fieldsOf(Point::class.java.name)["shared2"])

        val color = obj.fields["color"] as JavaEnum
        assertEquals(Color::class.java.name, color.className)
        assertEquals("GREEN", color.constant)
        assertTrue(color.description.isEnum)

        val next = obj.fields["next"] as JavaObject
        assertEquals(-1, next.fields["x"])
        assertNull(next.fields["next"])
        // The same constant is a back-reference.
        assertSame(color, next.fields["color"])

        assertSame(obj, input.readObject())
    }

    @Test
    fun testArrays() {
        val input = stream {
            writeObject(intArrayOf(1, -2, 3))
            writeObject(arrayOf("a", null, "a"))
            writeObject(arrayOf(intArrayOf(1), intArrayOf()))
            writeObject(Color.values())
        }

        val ints = input.readObject() as JavaArray
        assertEquals("[I", ints.className)
        assertEquals(listOf(1, -2, 3), ints.elements)

        val strings = input.readObject() as JavaArray
        assertEquals(listOf("a", null, "a"), strings.elements)
        assertSame(strings.elements[0], strings.elements[2])

        val nested = input.readObject() as JavaArray
        assertEquals(listOf(1), (nested.elements[0] as JavaArray).elements)
        assertEquals(emptyList<Any?>(), (nested.elements[1] as JavaArray).elements)

        val colors = input.readObject() as JavaArray
        assertEquals(listOf("RED", "GREEN", "BLUE"), colors.elements.map { (it as JavaEnum).constant })
    }

    @Test
    fun testClasses() {
        val input = stream { writeObject(Point::class.java) }
        assertEquals(Point::class.java.name, (input.readObject() as JavaClass).name)
    }

    @Test
    fun testWriteMethodAnnotations() {
        val input = stream { writeObject(WithWriteMethod()) }

        val obj = input.readObject() as JavaObject
        assertEquals(5, obj.fields["value"])

        val annotations = obj.annotations.getValue(WithWriteMethod::class.java.name)
        assertEquals(Buffer().writeInt(99).readByteString(), annotations[0] as ByteString)
        assertEquals("extra", annotations[1])
    }

    @Test
    fun testCollections() {
        val input = stream {
            writeObject(EnumSet.of(Color.RED, Color.BLUE))
            writeObject(hashSetOf(Color.GREEN))
        }

        // An EnumSet is written as its serialization proxy, its constants in an array.
        val enumSet = input.readObject() as JavaObject
        assertEquals("java.util.EnumSet\$SerializationProxy", enumSet.className)
        assertEquals(Color::class.java.name, (enumSet.fields["elementType"] as JavaClass).name)
        assertEquals(listOf("RED", "BLUE"), (enumSet.fields["elements"] as JavaArray).elements.map { (it as JavaEnum).constant })

        // A HashSet writes its capacity, load factor and size, then its elements.
        val hashSet = input.readObject() as JavaObject
        val contents = hashSet.annotations.getValue("java.util.HashSet")
        assertEquals("GREEN", (contents.single { it is JavaEnum } as JavaEnum).constant)
    }

    @Test
    fun testExternalizable() {
        val obj = stream { writeObject(External()) }.readObject() as JavaObject
        assertTrue(obj.description.isExternalizable)

        val contents = obj.annotations.getValue(External::class.java.name)
        assertEquals(Buffer().writeInt(1234).readByteString(), contents[0])
        assertEquals("inside", contents[1])

        assertThrows(JavaSerializationException::class.java) {
            stream(ObjectStreamConstants.PROTOCOL_VERSION_1) { writeObject(External()) }.readObject()
        }
    }

    @Test
    fun testReset() {
        val input = stream {
            writeObject("first")
            reset()
            writeObject("second")
            writeInt(5)
        }

        assertEquals("first", input.readObject())
        assertEquals("second", input.readObject())
        assertEquals(5, input.readInt())
    }
}
