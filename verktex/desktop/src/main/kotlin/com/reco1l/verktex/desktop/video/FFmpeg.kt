package com.reco1l.verktex.desktop.video

import com.reco1l.verktex.Logger
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import com.sun.jna.Platform
import com.sun.jna.Pointer
import com.sun.jna.ptr.PointerByReference
import java.io.File

/**
 * JNA bindings to the parts of FFmpeg that [FFmpegVideoDecoder] uses.
 *
 * The libraries are loaded from `<verktex.ffmpeg.path>/<platform>` when that system property is set
 * (the build downloads them there, see `prepareFFmpeg` in verktex/desktop/build.gradle.kts), from the
 * system otherwise. Structs are read at the offsets of [AVUTIL_MAJOR], [AVCODEC_MAJOR] and
 * [AVFORMAT_MAJOR], so other versions are refused.
 */
internal object FFmpeg {

    const val AVUTIL_MAJOR = 60
    const val SWRESAMPLE_MAJOR = 6
    const val SWSCALE_MAJOR = 9
    const val AVCODEC_MAJOR = 62
    const val AVFORMAT_MAJOR = 62

    // Offsets in the structs of these versions, found with offsetof on their headers.
    const val FORMAT_STREAMS = 48L
    const val STREAM_CODECPAR = 16L
    const val STREAM_TIME_BASE = 32L
    const val PARAMETERS_WIDTH = 72L
    const val PARAMETERS_HEIGHT = 76L
    const val FRAME_DATA = 0L
    const val FRAME_LINESIZE = 64L
    const val FRAME_WIDTH = 104L
    const val FRAME_HEIGHT = 108L
    const val FRAME_FORMAT = 116L
    const val FRAME_PTS = 136L
    const val FRAME_BEST_EFFORT_TIMESTAMP = 304L
    const val PACKET_STREAM_INDEX = 36L
    const val CODEC_CONTEXT_THREAD_COUNT = 656L

    const val AVMEDIA_TYPE_VIDEO = 0
    const val AV_PIX_FMT_RGBA = 26
    const val AVSEEK_FLAG_BACKWARD = 1
    const val SWS_BILINEAR = 2
    const val AV_NOPTS_VALUE = Long.MIN_VALUE

    // AVERROR(EAGAIN) is -EAGAIN, 11 on Windows and Linux, 35 on macOS.
    val AVERROR_EAGAIN = if (Platform.isMac()) -35 else -11
    const val AVERROR_EOF = -541478725


    @Suppress("FunctionName")
    interface AVUtil : Library {
        fun avutil_version(): Int
        fun av_frame_alloc(): Pointer?
        fun av_frame_free(frame: PointerByReference)
    }

    @Suppress("FunctionName")
    interface SWScale : Library {
        fun sws_getCachedContext(
            context: Pointer?, sourceWidth: Int, sourceHeight: Int, sourceFormat: Int,
            destinationWidth: Int, destinationHeight: Int, destinationFormat: Int,
            flags: Int, sourceFilter: Pointer?, destinationFilter: Pointer?, parameters: Pointer?
        ): Pointer?

        fun sws_scale(
            context: Pointer, sourceSlices: Pointer, sourceStrides: Pointer, sliceY: Int, sliceHeight: Int,
            destinations: Pointer, destinationStrides: Pointer
        ): Int

        fun sws_freeContext(context: Pointer?)
    }

    @Suppress("FunctionName")
    interface AVCodec : Library {
        fun avcodec_version(): Int
        fun avcodec_alloc_context3(codec: Pointer?): Pointer?
        fun avcodec_parameters_to_context(context: Pointer, parameters: Pointer): Int
        fun avcodec_open2(context: Pointer, codec: Pointer?, options: Pointer?): Int
        fun avcodec_send_packet(context: Pointer, packet: Pointer?): Int
        fun avcodec_receive_frame(context: Pointer, frame: Pointer): Int
        fun avcodec_flush_buffers(context: Pointer)
        fun avcodec_free_context(context: PointerByReference)
        fun av_packet_alloc(): Pointer?
        fun av_packet_unref(packet: Pointer)
        fun av_packet_free(packet: PointerByReference)
    }

    @Suppress("FunctionName")
    interface AVFormat : Library {
        fun avformat_version(): Int
        fun avformat_open_input(context: PointerByReference, url: Pointer, format: Pointer?, options: Pointer?): Int
        fun avformat_find_stream_info(context: Pointer, options: Pointer?): Int
        fun av_find_best_stream(context: Pointer, type: Int, wanted: Int, related: Int, decoder: PointerByReference?, flags: Int): Int
        fun av_read_frame(context: Pointer, packet: Pointer): Int
        fun av_seek_frame(context: Pointer, streamIndex: Int, timestamp: Long, flags: Int): Int
        fun avformat_close_input(context: PointerByReference)
    }

    class Libraries(val avutil: AVUtil, val swscale: SWScale, val avcodec: AVCodec, val avformat: AVFormat)


    /**
     * The loaded libraries, null when they couldn't be loaded or are of other versions.
     */
    val libraries: Libraries? by lazy {
        try {
            load()
        } catch (e: Throwable) {
            Logger.w("FFmpeg", "FFmpeg couldn't be loaded, videos won't play: ${e.message}")
            null
        }
    }

    private fun load(): Libraries? {
        val directory = System.getProperty("verktex.ffmpeg.path")
            ?.let { File(it, Platform.RESOURCE_PREFIX) }
            ?.takeIf { it.isDirectory }

        // A library by its full path when bundled, by its versioned name from the system otherwise.
        fun name(library: String, major: Int): String {
            val file = when {
                Platform.isWindows() -> "$library-$major.dll"
                Platform.isMac() -> "lib$library.$major.dylib"
                else -> "lib$library.so.$major"
            }
            return directory?.resolve(file)?.absolutePath ?: file
        }

        // Each is loaded after the ones it needs, which the system then finds already loaded.
        val avutil = Native.load(name("avutil", AVUTIL_MAJOR), AVUtil::class.java)
        NativeLibrary.getInstance(name("swresample", SWRESAMPLE_MAJOR))
        val swscale = Native.load(name("swscale", SWSCALE_MAJOR), SWScale::class.java)
        val avcodec = Native.load(name("avcodec", AVCODEC_MAJOR), AVCodec::class.java)
        val avformat = Native.load(name("avformat", AVFORMAT_MAJOR), AVFormat::class.java)

        val versions = listOf(
            Triple("avutil", avutil.avutil_version() ushr 16, AVUTIL_MAJOR),
            Triple("avcodec", avcodec.avcodec_version() ushr 16, AVCODEC_MAJOR),
            Triple("avformat", avformat.avformat_version() ushr 16, AVFORMAT_MAJOR),
        )

        for ((library, major, expected) in versions) {
            if (major != expected) {
                Logger.w("FFmpeg", "$library is version $major, videos need $expected.")
                return null
            }
        }

        Logger.i("FFmpeg", "Loaded FFmpeg from ${directory ?: "the system"}")
        return Libraries(avutil, swscale, avcodec, avformat)
    }
}
