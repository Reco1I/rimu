package ru.nsu.ccfit.zuev.osu.scoring

import okio.Source

// Replays are a zip with a single "data" entry, written with Java's object serialization. These read
// and write it exactly as java.io.ObjectOutputStream and ObjectInputStream do, so replays recorded on
// any platform stay compatible.

/**
 * Marks classes written into replays with Java's object serialization, `java.io.Serializable` on the
 * JVM.
 */
expect interface ReplaySerializable

/**
 * Writes the values of a replay, as `java.io.ObjectOutputStream` does.
 */
interface ReplayOutput : AutoCloseable {
    fun writeObject(value: Any?)
    fun writeLong(value: Long)
    fun writeInt(value: Int)
    fun writeShort(value: Int)
    fun writeByte(value: Int)
    fun writeFloat(value: Float)
    fun write(bytes: ByteArray)
}

/**
 * Reads the values of a replay, as `java.io.ObjectInputStream` does.
 */
interface ReplayInput : AutoCloseable {
    fun readObject(): Any?
    fun readLong(): Long
    fun readInt(): Int
    fun readShort(): Short
    fun readByte(): Byte
    fun readFloat(): Float
    fun readBoolean(): Boolean
    fun read(bytes: ByteArray, offset: Int, length: Int): Int
}

/**
 * Creates the replay file at [path] and opens its data for writing.
 */
expect fun openReplayOutput(path: String): ReplayOutput

/**
 * Opens the data of the replay in [source] for reading.
 */
expect fun openReplayInput(source: Source): ReplayInput

/**
 * Whether [value], read from a replay, is a set of [ru.nsu.ccfit.zuev.osu.game.mods.GameMod]s, which
 * replays older than version 7 contain.
 */
@Suppress("UNCHECKED_CAST", "DEPRECATION")
internal fun asGameModSet(value: Any?) = value as Set<ru.nsu.ccfit.zuev.osu.game.mods.GameMod>
