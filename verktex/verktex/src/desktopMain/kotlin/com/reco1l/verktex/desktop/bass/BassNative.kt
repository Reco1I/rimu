@file:Suppress("FunctionName", "LocalVariableName")

package com.reco1l.verktex.desktop.bass

import com.sun.jna.Callback
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import com.sun.jna.Structure
import java.io.File
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * The part of BASS (bass.h) the audio driver uses, direct mapped with JNA. The Android BASS.java
 * binds the same library through JNI symbols only its Android build exports, so desktop maps the C
 * API itself.
 *
 * Types follow bass.h: DWORD and handles are [Int], QWORD is [Long], BOOL is [Boolean].
 */
object BassNative {

    init {
        BassLibraries.load()
        Native.register(BassNative::class.java, BassLibraries.searchName("bass"))
    }

    const val BASS_CONFIG_BUFFER = 0
    const val BASS_CONFIG_UPDATEPERIOD = 1
    const val BASS_CONFIG_DEV_BUFFER = 27
    const val BASS_CONFIG_UNICODE = 42
    const val BASS_CONFIG_DEV_NONSTOP = 50
    const val BASS_CONFIG_DEV_PERIOD = 53
    const val BASS_CONFIG_DEV_TIMEOUT = 70

    const val BASS_DEVICE_DEFAULT = 2
    const val BASS_DEVICE_INIT = 4

    const val BASS_SAMPLE_LOOP = 4
    const val BASS_SAMPLE_OVER_POS = 0x20000
    const val BASS_STREAM_PRESCAN = 0x20000
    const val BASS_STREAM_AUTOFREE = 0x40000
    const val BASS_STREAM_DECODE = 0x200000
    const val BASS_UNICODE = 0x80000000.toInt()

    const val BASS_SAMCHAN_STREAM = 2
    const val BASS_SYNC_END = 2

    const val BASS_ACTIVE_PLAYING = 1
    const val BASS_ACTIVE_STALLED = 2
    const val BASS_ACTIVE_PAUSED = 3

    const val BASS_ATTRIB_FREQ = 1
    const val BASS_ATTRIB_VOL = 2
    const val BASS_ATTRIB_NOBUFFER = 5
    const val BASS_ATTRIB_BUFFER = 13

    const val BASS_DATA_FFT1024 = 0x80000002.toInt()

    const val BASS_POS_BYTE = 0
    const val BASS_POS_DECODE = 0x10000000


    /**
     * BASS_DEVICEINFO, with the desktop layout (char strings, in UTF-8 with [BASS_CONFIG_UNICODE]).
     */
    @Structure.FieldOrder("name", "driver", "flags")
    class DeviceInfo : Structure() {
        @JvmField var name: Pointer? = null
        @JvmField var driver: Pointer? = null
        @JvmField var flags: Int = 0
    }

    /**
     * SYNCPROC. Instances must stay referenced while the sync exists, JNA frees what's collected.
     */
    fun interface SyncProc : Callback {
        fun invoke(handle: Int, channel: Int, data: Int, user: Pointer?)
    }

    @JvmStatic external fun BASS_SetConfig(option: Int, value: Int): Boolean
    @JvmStatic external fun BASS_GetDeviceInfo(device: Int, info: DeviceInfo): Boolean
    @JvmStatic external fun BASS_Init(device: Int, freq: Int, flags: Int, window: Pointer?, dsguid: Pointer?): Boolean
    @JvmStatic external fun BASS_Free(): Boolean
    @JvmStatic external fun BASS_ErrorGetCode(): Int

    @JvmStatic external fun BASS_SampleLoad(mem: Boolean, file: Pointer, offset: Long, length: Int, max: Int, flags: Int): Int
    @JvmStatic external fun BASS_SampleFree(handle: Int): Boolean
    @JvmStatic external fun BASS_SampleGetChannel(handle: Int, flags: Int): Int

    @JvmStatic external fun BASS_StreamCreateFile(mem: Boolean, file: Pointer, offset: Long, length: Long, flags: Int): Int
    @JvmStatic external fun BASS_StreamCreateURL(url: Pointer, offset: Int, flags: Int, proc: Pointer?, user: Pointer?): Int

    @JvmStatic external fun BASS_ChannelBytes2Seconds(handle: Int, position: Long): Double
    @JvmStatic external fun BASS_ChannelSeconds2Bytes(handle: Int, position: Double): Long
    @JvmStatic external fun BASS_ChannelIsActive(handle: Int): Int
    @JvmStatic external fun BASS_ChannelGetLength(handle: Int, mode: Int): Long
    @JvmStatic external fun BASS_ChannelGetPosition(handle: Int, mode: Int): Long
    @JvmStatic external fun BASS_ChannelSetPosition(handle: Int, position: Long, mode: Int): Boolean
    @JvmStatic external fun BASS_ChannelSetAttribute(handle: Int, attribute: Int, value: Float): Boolean
    @JvmStatic external fun BASS_ChannelGetAttribute(handle: Int, attribute: Int, value: FloatArray): Boolean
    @JvmStatic external fun BASS_ChannelFlags(handle: Int, flags: Int, mask: Int): Int
    @JvmStatic external fun BASS_ChannelPlay(handle: Int, restart: Boolean): Boolean
    @JvmStatic external fun BASS_ChannelPause(handle: Int): Boolean
    @JvmStatic external fun BASS_ChannelStop(handle: Int): Boolean
    @JvmStatic external fun BASS_ChannelFree(handle: Int): Boolean
    @JvmStatic external fun BASS_ChannelSetSync(handle: Int, type: Int, param: Long, proc: SyncProc, user: Pointer?): Int
    @JvmStatic external fun BASS_ChannelGetData(handle: Int, buffer: ByteBuffer, length: Int): Int


    private val isWindows = System.getProperty("os.name").lowercase().contains("windows")

    /**
     * A file path as BASS takes it, with the flag telling its encoding: UTF-16 with [BASS_UNICODE]
     * on Windows, UTF-8 elsewhere.
     */
    fun path(path: String): Pair<Memory, Int> {
        return if (isWindows) nullTerminated(path.toByteArray(Charsets.UTF_16LE), 2) to BASS_UNICODE
        else utf8(path) to 0
    }

    /**
     * A null terminated UTF-8 string.
     */
    fun utf8(text: String): Memory = nullTerminated(text.toByteArray(Charsets.UTF_8), 1)

    private fun nullTerminated(bytes: ByteArray, terminatorSize: Int): Memory {
        val memory = Memory((bytes.size + terminatorSize).toLong())
        memory.clear()
        memory.write(0, bytes, 0, bytes.size)
        return memory
    }
}

/**
 * The tempo part of BASS_FX (bass_fx.h).
 */
object BassFxNative {

    init {
        // bass_fx links to bass, which must be loaded first.
        BassNative.BASS_ErrorGetCode()
        Native.register(BassFxNative::class.java, BassLibraries.searchName("bass_fx"))
    }

    const val BASS_ATTRIB_TEMPO = 0x10000
    const val BASS_ATTRIB_TEMPO_FREQ = 0x10002

    @JvmStatic external fun BASS_FX_TempoCreate(channel: Int, flags: Int): Int
}

/**
 * Puts the BASS libraries of the platform, which ship as classpath resources, in a directory under
 * their own names. JNA's own extraction gives each a temporary name, and bass_fx then can't find
 * the bass it links to.
 */
internal object BassLibraries {

    private val names = listOf("bass", "bass_fx")
    private var directory: File? = null

    @Synchronized
    fun load() {
        if (directory != null) return

        val prefixes = buildList {
            add(com.sun.jna.Platform.RESOURCE_PREFIX)
            // The macOS libraries are universal binaries, for both architectures.
            if (com.sun.jna.Platform.isMac()) add("darwin")
        }

        val target = Files.createTempDirectory("verktex-bass").toFile()
        target.deleteOnExit()

        for (name in names) {
            val fileName = System.mapLibraryName(name)
            val resource = prefixes.firstNotNullOfOrNull { BassLibraries::class.java.getResourceAsStream("/$it/$fileName") }
                ?: throw UnsatisfiedLinkError("There's no BASS library for ${com.sun.jna.Platform.RESOURCE_PREFIX} ($fileName).")

            val file = File(target, fileName)
            resource.use { Files.copy(it, file.toPath(), StandardCopyOption.REPLACE_EXISTING) }
            file.deleteOnExit()

            NativeLibrary.addSearchPath(name, target.absolutePath)
        }

        directory = target
    }

    fun searchName(name: String): String {
        load()
        return name
    }
}
