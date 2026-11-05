package ru.nsu.ccfit.zuev.osu.scoring

import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import okio.Source
import okio.buffer

actual typealias ReplaySerializable = java.io.Serializable

actual fun openReplayOutput(path: String): ReplayOutput {
    val zip = ZipOutputStream(FileOutputStream(path))
    zip.setMethod(ZipOutputStream.DEFLATED)
    zip.setLevel(Deflater.DEFAULT_COMPRESSION)
    zip.putNextEntry(ZipEntry("data"))

    val stream = ObjectOutputStream(zip)

    return object : ReplayOutput {
        override fun writeObject(value: Any?) = stream.writeObject(value)
        override fun writeLong(value: Long) = stream.writeLong(value)
        override fun writeInt(value: Int) = stream.writeInt(value)
        override fun writeShort(value: Int) = stream.writeShort(value)
        override fun writeByte(value: Int) = stream.writeByte(value)
        override fun writeFloat(value: Float) = stream.writeFloat(value)
        override fun write(bytes: ByteArray) = stream.write(bytes)

        override fun close() {
            // Closing the object stream closes the zip too, as the original try-with-resources did.
            stream.use { zip.use { } }
        }
    }
}

actual fun openReplayInput(source: Source): ReplayInput {
    val zip = ZipInputStream(source.buffer().inputStream())
    zip.nextEntry

    val stream = try {
        ObjectInputStream(zip)
    } catch (e: Throwable) {
        zip.close()
        throw e
    }

    return object : ReplayInput {
        override fun readObject(): Any? = stream.readObject()
        override fun readLong() = stream.readLong()
        override fun readInt() = stream.readInt()
        override fun readShort() = stream.readShort()
        override fun readByte() = stream.readByte()
        override fun readFloat() = stream.readFloat()
        override fun readBoolean() = stream.readBoolean()
        override fun read(bytes: ByteArray, offset: Int, length: Int) = stream.read(bytes, offset, length)

        override fun close() {
            stream.use { zip.use { } }
        }
    }
}
