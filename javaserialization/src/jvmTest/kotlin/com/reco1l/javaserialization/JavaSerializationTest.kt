package com.reco1l.javaserialization

import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.util.ArrayDeque
import java.util.Collections
import java.util.EnumMap
import java.util.EnumSet
import java.util.LinkedList
import java.util.PriorityQueue
import java.util.TreeMap
import java.util.TreeSet
import java.util.Vector
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import okio.Buffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class JavaSerializationTest {

    // The Java side: what's written.

    enum class JavaColor { RED, GREEN, BLUE }

    open class JavaShape : java.io.Serializable {
        @JvmField var name = "shape"
    }

    class JavaCircle(@JvmField var radius: Double) : JavaShape()

    class JavaSquare(@JvmField var side: Int) : JavaShape()

    class JavaPoint(@JvmField var x: Int, @JvmField var y: Float) : java.io.Serializable {
        @JvmField var label: String? = "p"
        @JvmField var color = JavaColor.GREEN
        @JvmField var next: JavaPoint? = null
        @JvmField var tags: ArrayList<String> = arrayListOf("a", "b")
        @JvmField var counts: HashMap<String, Int> = hashMapOf("one" to 1)
        @JvmField var values = intArrayOf(1, 2)
        @JvmField var boxed: Long? = 5L
        @JvmField var ignored = "not in the Kotlin class"
    }

    class JavaHolder(@JvmField var shape: JavaShape?, @JvmField var shapes: List<JavaShape>) : java.io.Serializable


    // The Kotlin side: what's decoded.

    @Serializable
    enum class Color { RED, GREEN, BLUE }

    @Serializable
    class Point(
        val x: Int,
        val y: Float,
        val label: String?,
        val color: Color,
        val next: Point? = null,
        val tags: List<String>,
        val counts: Map<String, Int>,
        val values: IntArray,
        val boxed: Long?,
        val missing: String = "default",
    )

    @Serializable
    sealed class Shape {
        abstract val name: String
    }

    @Serializable
    @SerialName("com.reco1l.javaserialization.JavaSerializationTest\$JavaCircle")
    class Circle(override val name: String, val radius: Double) : Shape()

    @Serializable
    @SerialName("com.reco1l.javaserialization.JavaSerializationTest\$JavaSquare")
    class Square(override val name: String, val side: Int) : Shape()

    @Serializable
    class Holder(val shape: Shape?, val shapes: List<Shape>)

    @Serializable
    class Renamed(@SerialName("x") val horizontal: Int)

    @Serializable
    class Required(val x: Int, val absent: Int)


    private fun bytes(vararg objects: Any?): ByteArray {
        val bytes = ByteArrayOutputStream()
        ObjectOutputStream(bytes).use { stream -> objects.forEach { stream.writeObject(it) } }
        return bytes.toByteArray()
    }

    private inline fun <reified T> decode(value: Any?): T = JavaSerialization.decodeFromByteArray<T>(bytes(value))


    @Test
    fun testClasses() {
        val point = decode<Point>(JavaPoint(3, 4.5f).apply { next = JavaPoint(-1, 0f).apply { label = null; boxed = null } })

        assertEquals(3, point.x)
        assertEquals(4.5f, point.y)
        assertEquals("p", point.label)
        assertEquals(Color.GREEN, point.color)
        assertEquals(listOf("a", "b"), point.tags)
        assertEquals(mapOf("one" to 1), point.counts)
        assertArrayEquals(intArrayOf(1, 2), point.values)
        assertEquals(5L, point.boxed)
        assertEquals("default", point.missing)

        val next = point.next!!
        assertEquals(-1, next.x)
        assertNull(next.label)
        assertNull(next.boxed)
        assertNull(next.next)
    }

    @Test
    fun testRenamedAndMissingProperties() {
        assertEquals(3, decode<Renamed>(JavaPoint(3, 0f)).horizontal)
        assertThrows(SerializationException::class.java) { decode<Required>(JavaPoint(3, 0f)) }
    }

    @Test
    fun testTypeMismatches() {
        assertThrows(SerializationException::class.java) { decode<Int>("text") }
        assertThrows(SerializationException::class.java) { decode<Long>(1) }
        assertThrows(SerializationException::class.java) { decode<List<String>>("text") }
        assertThrows(SerializationException::class.java) { decode<Color>("PURPLE") }
    }

    @Test
    fun testPrimitivesAndBoxes() {
        assertEquals(42, decode<Int>(42))
        assertEquals(-7L, decode<Long>(-7L))
        assertEquals(true, decode<Boolean>(true))
        assertEquals('z', decode<Char>('z'))
        assertEquals(1.5, decode<Double>(1.5), 0.0)
        assertEquals("text", decode<String>("text"))
        assertNull(decode<String?>(null))
    }

    @Test
    fun testEnums() {
        assertEquals(Color.BLUE, decode<Color>(JavaColor.BLUE))
        // A constant decodes as its name too.
        assertEquals("BLUE", decode<String>(JavaColor.BLUE))
        assertEquals(setOf(Color.RED, Color.BLUE), decode<Set<Color>>(EnumSet.of(JavaColor.RED, JavaColor.BLUE)))
    }

    @Test
    fun testCollections() {
        val expected = listOf("b", "a", "c")

        assertEquals(expected, decode<List<String>>(arrayOf("b", "a", "c")))
        assertEquals(expected, decode<List<String>>(arrayListOf("b", "a", "c")))
        assertEquals(expected, decode<List<String>>(LinkedList(expected)))
        assertEquals(expected, decode<List<String>>(ArrayDeque(expected)))
        assertEquals(expected, decode<List<String>>(Vector(expected)))
        assertEquals(expected, decode<List<String>>(java.util.Arrays.asList("b", "a", "c")))
        assertEquals(expected, decode<List<String>>(java.util.List.of("b", "a", "c")))
        assertEquals(expected, decode<List<String>>(Collections.unmodifiableList(ArrayList(expected))))
        assertEquals(expected, decode<List<String>>(Collections.synchronizedList(ArrayList(expected))))
        assertEquals(listOf("a"), decode<List<String>>(Collections.singletonList("a")))
        assertEquals(emptyList<String>(), decode<List<String>>(Collections.emptyList<String>()))
        assertEquals(listOf("a", "b", "c"), decode<List<String>>(TreeSet(expected)))
        assertEquals(expected.toSet(), decode<Set<String>>(hashSetOf("b", "a", "c")))
        assertEquals(expected.toSet(), decode<Set<String>>(linkedSetOf("b", "a", "c")))
        assertEquals(expected.toSet(), decode<Set<String>>(PriorityQueue(expected)).toSet())
        assertEquals(setOf(1, 2), decode<Set<Int>>(hashSetOf(1, 2)))
        assertEquals(listOf(listOf(1), emptyList()), decode<List<List<Int>>>(arrayListOf(arrayListOf(1), arrayListOf<Int>())))
        assertArrayEquals(longArrayOf(1, -2), decode<LongArray>(longArrayOf(1, -2)))
    }

    @Test
    fun testMaps() {
        val expected = mapOf("a" to 1, "b" to 2)

        assertEquals(expected, decode<Map<String, Int>>(HashMap(expected)))
        assertEquals(expected, decode<Map<String, Int>>(LinkedHashMap(expected)))
        assertEquals(expected, decode<Map<String, Int>>(TreeMap(expected)))
        assertEquals(expected, decode<Map<String, Int>>(java.util.Hashtable(expected)))
        assertEquals(expected, decode<Map<String, Int>>(ConcurrentHashMap(expected)))
        assertEquals(expected, decode<Map<String, Int>>(java.util.Map.of("a", 1, "b", 2)))
        assertEquals(expected, decode<Map<String, Int>>(Collections.unmodifiableMap(HashMap(expected))))
        assertEquals(mapOf("k" to 1), decode<Map<String, Int>>(Collections.singletonMap("k", 1)))
        assertEquals(
            mapOf(Color.RED to "r"),
            decode<Map<Color, String>>(EnumMap<JavaColor, String>(JavaColor::class.java).apply { put(JavaColor.RED, "r") }),
        )
    }

    @Test
    fun testPolymorphism() {
        val holder = decode<Holder>(JavaHolder(JavaCircle(2.5), arrayListOf(JavaSquare(3), JavaCircle(1.0))))

        val circle = holder.shape as Circle
        assertEquals(2.5, circle.radius, 0.0)
        assertEquals("shape", circle.name)
        assertEquals(3, (holder.shapes[0] as Square).side)
        assertEquals(1.0, (holder.shapes[1] as Circle).radius, 0.0)

        // A class without a serializer.
        assertThrows(SerializationException::class.java) { decode<Shape>(JavaShape()) }
    }

    @Test
    fun testSerializersModule() {
        val format = JavaSerialization {
            serializersModule = SerializersModule {
                polymorphic(Any::class) {
                    subclass(Circle::class)
                }
            }
        }

        // An open polymorphic value, its subclasses registered in the module.
        val shape = format.decodeFromByteArray(PolymorphicSerializer(Any::class), bytes(JavaCircle(4.0)))
        assertEquals(4.0, (shape as Circle).radius, 0.0)
    }

    @Test
    fun testDecodeObjectsFromAStream() {
        val bytes = ByteArrayOutputStream()
        ObjectOutputStream(bytes).use {
            it.writeInt(7)
            it.writeObject(arrayListOf("x"))
            it.writeLong(8)
            it.writeObject(JavaColor.RED)
        }

        val input = JavaObjectInputStream(Buffer().write(bytes.toByteArray()))
        assertEquals(7, input.readInt())
        assertEquals(listOf("x"), input.decodeObject<List<String>>())
        assertEquals(8L, input.readLong())
        assertEquals(Color.RED, input.decodeObject<Color>())
    }
}
