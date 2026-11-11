package com.reco1l.verktex.desktop.video

import com.reco1l.verktex.Logger
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.graphics.GraphicsTexture
import com.reco1l.verktex.video.VideoDecoder
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.PointerByReference
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.min

/**
 * A [VideoDecoder] over FFmpeg's libraries, see [FFmpeg]. A thread of its own decodes the frames in
 * time with the video's clock, which [play], [pause], [setSpeed] and [seekTo] drive, and converts the
 * ones it shows to RGBA. The texture uploads the newest of them when it's next drawn.
 */
class FFmpegVideoDecoder(private val path: String) : VideoDecoder {

    @Volatile
    override var width = 0
        private set

    @Volatile
    override var height = 0
        private set

    @Volatile
    override var region: GraphicsTexture.Region? = null
        private set

    private var texture: GraphicsTexture? = null
    private val onSizeKnown = AtomicReference<(() -> Unit)?>()

    // The video's clock and what the thread is asked to do, guarded by the lock.
    private val lock = Object()
    private var isPlaying = false
    private var speed = 1f
    private var anchorSeconds = 0.0
    private var anchorNanos = System.nanoTime()
    private var pendingSeekSeconds: Double? = null
    private var isReleased = false

    // The frame shown and the one written next, swapped under frameLock.
    private val frameLock = Any()
    private var shownFrame: ByteBuffer? = null
    private var nextFrame: ByteBuffer? = null


    init {
        Thread(::run, "FFmpegVideoDecoder").apply {
            isDaemon = true
            start()
        }
    }


    override fun setOnSizeKnown(callback: () -> Unit) {
        onSizeKnown.set(callback)

        if (region != null) {
            onSizeKnown.getAndSet(null)?.invoke()
        }
    }

    override fun play() = synchronized(lock) {
        if (!isPlaying) {
            anchorNanos = System.nanoTime()
            isPlaying = true
            lock.notifyAll()
        }
    }

    override fun pause() = synchronized(lock) {
        if (isPlaying) {
            anchorSeconds = videoSeconds()
            isPlaying = false
        }
    }

    override fun seekTo(milliseconds: Long) = synchronized(lock) {
        anchorSeconds = milliseconds / 1000.0
        anchorNanos = System.nanoTime()
        pendingSeekSeconds = anchorSeconds
        lock.notifyAll()
    }

    override fun setSpeed(speed: Float) = synchronized(lock) {
        anchorSeconds = videoSeconds()
        anchorNanos = System.nanoTime()
        this.speed = speed
    }

    override fun release() = synchronized(lock) {
        isReleased = true
        lock.notifyAll()
    }

    // Where the video's clock is, in seconds. Called with the lock held.
    private fun videoSeconds(): Double {
        return if (isPlaying) anchorSeconds + (System.nanoTime() - anchorNanos) / 1e9 * speed else anchorSeconds
    }


    private fun run() {
        val ffmpeg = FFmpeg.libraries ?: return

        try {
            Decoding(ffmpeg).use { it.run() }
        } catch (e: Throwable) {
            Logger.e("FFmpegVideoDecoder", "Failed to play $path", e)
        }

        val texture = texture ?: return
        Verktex.scheduleOnDraw {
            texture.dispose()
            Verktex.graphics.unregisterResource(texture)
        }
    }


    /**
     * The FFmpeg objects of the decoding thread, freed when it ends.
     */
    private inner class Decoding(private val ffmpeg: FFmpeg.Libraries) : AutoCloseable {

        private val formatReference = PointerByReference()
        private var format: Pointer? = null
        private var codecContext: Pointer? = null
        private var packet: Pointer? = null
        private var frame: Pointer? = null
        private var scaler: Pointer? = null

        private var streamIndex = -1
        private var timeBase = 0.0
        private var isDraining = false

        private val destinations = Memory(8L * Native.POINTER_SIZE)
        private val destinationStrides = Memory(8L * 4)


        fun run() {
            if (!open()) {
                return
            }

            var hasShownFrame = false
            var isAtEnd = false

            while (true) {
                val seekSeconds: Double?

                synchronized(lock) {
                    // Paused with a frame shown, or at the end, nothing to do until asked.
                    while (!isReleased && pendingSeekSeconds == null && hasShownFrame && (!isPlaying || isAtEnd)) {
                        lock.wait()
                    }
                    if (isReleased) {
                        return
                    }
                    seekSeconds = pendingSeekSeconds
                    pendingSeekSeconds = null
                }

                if (seekSeconds != null) {
                    seek(seekSeconds)
                    isAtEnd = false
                    hasShownFrame = showFrameAt(seekSeconds) || hasShownFrame
                    continue
                }

                if (!decodeFrame()) {
                    isAtEnd = true
                    hasShownFrame = true
                    continue
                }

                val frameSeconds = frameSeconds()

                if (!hasShownFrame) {
                    // The first frame is shown right away, even when paused.
                    showFrame()
                    hasShownFrame = true
                    continue
                }

                if (!waitUntil(frameSeconds)) {
                    continue
                }

                // Too late to be worth converting, the next frames catch up.
                val lateness = synchronized(lock) { videoSeconds() } - frameSeconds
                if (lateness < MAX_LATENESS_SECONDS) {
                    showFrame()
                }
            }
        }

        private fun open(): Boolean {
            val bytes = path.toByteArray(Charsets.UTF_8)
            val url = Memory(bytes.size + 1L).apply {
                write(0, bytes, 0, bytes.size)
                setByte(bytes.size.toLong(), 0)
            }

            if (ffmpeg.avformat.avformat_open_input(formatReference, url, null, null) < 0) {
                Logger.w("FFmpegVideoDecoder", "Couldn't open $path")
                return false
            }
            val format = formatReference.value
            this.format = format

            if (ffmpeg.avformat.avformat_find_stream_info(format, null) < 0) {
                return false
            }

            val codecReference = PointerByReference()
            streamIndex = ffmpeg.avformat.av_find_best_stream(format, FFmpeg.AVMEDIA_TYPE_VIDEO, -1, -1, codecReference, 0)
            if (streamIndex < 0) {
                Logger.w("FFmpegVideoDecoder", "No video stream in $path")
                return false
            }

            val stream = format.getPointer(FFmpeg.FORMAT_STREAMS).getPointer(streamIndex.toLong() * Native.POINTER_SIZE)
            val parameters = stream.getPointer(FFmpeg.STREAM_CODECPAR)
            timeBase = stream.getInt(FFmpeg.STREAM_TIME_BASE).toDouble() / stream.getInt(FFmpeg.STREAM_TIME_BASE + 4)

            val codecContext = ffmpeg.avcodec.avcodec_alloc_context3(codecReference.value) ?: return false
            this.codecContext = codecContext

            if (ffmpeg.avcodec.avcodec_parameters_to_context(codecContext, parameters) < 0) {
                return false
            }

            // As many threads as the CPU has.
            codecContext.setInt(FFmpeg.CODEC_CONTEXT_THREAD_COUNT, 0)

            if (ffmpeg.avcodec.avcodec_open2(codecContext, codecReference.value, null) < 0) {
                Logger.w("FFmpegVideoDecoder", "Couldn't open the decoder of $path")
                return false
            }

            packet = ffmpeg.avcodec.av_packet_alloc() ?: return false
            frame = ffmpeg.avutil.av_frame_alloc() ?: return false

            val width = parameters.getInt(FFmpeg.PARAMETERS_WIDTH)
            val height = parameters.getInt(FFmpeg.PARAMETERS_HEIGHT)
            if (width <= 0 || height <= 0) {
                return false
            }

            synchronized(frameLock) {
                shownFrame = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder())
                nextFrame = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder())
            }

            val texture = Verktex.graphics.createTexture(width.toFloat(), height.toFloat())
            texture.regionPadding = 0
            this@FFmpegVideoDecoder.texture = texture

            this@FFmpegVideoDecoder.width = width
            this@FFmpegVideoDecoder.height = height
            region = texture.addSource(FrameSource(width, height))

            onSizeKnown.getAndSet(null)?.invoke()
            return true
        }

        /**
         * Decodes the next frame of the video stream into [frame], false at its end.
         */
        private fun decodeFrame(): Boolean {
            val codecContext = codecContext!!
            val frame = frame!!
            val packet = packet!!

            while (true) {
                val received = ffmpeg.avcodec.avcodec_receive_frame(codecContext, frame)
                if (received == 0) {
                    return true
                }
                if (received != FFmpeg.AVERROR_EAGAIN || isDraining) {
                    return false
                }

                if (ffmpeg.avformat.av_read_frame(format!!, packet) < 0) {
                    // The end of the file, the decoder gives the frames it still holds.
                    ffmpeg.avcodec.avcodec_send_packet(codecContext, null)
                    isDraining = true
                    continue
                }

                if (packet.getInt(FFmpeg.PACKET_STREAM_INDEX) == streamIndex) {
                    ffmpeg.avcodec.avcodec_send_packet(codecContext, packet)
                }
                ffmpeg.avcodec.av_packet_unref(packet)
            }
        }

        private fun frameSeconds(): Double {
            val frame = frame!!
            var timestamp = frame.getLong(FFmpeg.FRAME_BEST_EFFORT_TIMESTAMP)
            if (timestamp == FFmpeg.AV_NOPTS_VALUE) {
                timestamp = frame.getLong(FFmpeg.FRAME_PTS)
            }
            return if (timestamp == FFmpeg.AV_NOPTS_VALUE) 0.0 else timestamp * timeBase
        }

        private fun seek(seconds: Double) {
            val timestamp = (seconds / timeBase).toLong()
            ffmpeg.avformat.av_seek_frame(format!!, streamIndex, timestamp, FFmpeg.AVSEEK_FLAG_BACKWARD)
            ffmpeg.avcodec.avcodec_flush_buffers(codecContext!!)
            isDraining = false
        }

        /**
         * Decodes up to the frame at [seconds] and shows it, false if the video ends before.
         */
        private fun showFrameAt(seconds: Double): Boolean {
            while (decodeFrame()) {
                // A frame lasting past the target time is the one showing at it.
                if (frameSeconds() >= seconds - SEEK_TOLERANCE_SECONDS) {
                    showFrame()
                    return true
                }
            }
            return false
        }

        /**
         * Waits until the video's clock reaches [seconds], false if a seek or a release comes first.
         */
        private fun waitUntil(seconds: Double): Boolean {
            synchronized(lock) {
                while (true) {
                    if (isReleased || pendingSeekSeconds != null) {
                        return false
                    }

                    if (!isPlaying) {
                        lock.wait()
                        continue
                    }

                    val remaining = (seconds - videoSeconds()) / speed
                    if (remaining <= 0) {
                        return true
                    }
                    lock.wait(min(50L, (remaining * 1000).toLong().coerceAtLeast(1)))
                }
            }
        }

        /**
         * Converts [frame] to RGBA and makes it the shown one.
         */
        private fun showFrame() {
            val frame = frame!!
            val next = nextFrame ?: return

            val frameWidth = frame.getInt(FFmpeg.FRAME_WIDTH)
            val frameHeight = frame.getInt(FFmpeg.FRAME_HEIGHT)
            val frameFormat = frame.getInt(FFmpeg.FRAME_FORMAT)

            scaler = ffmpeg.swscale.sws_getCachedContext(
                scaler, frameWidth, frameHeight, frameFormat, width, height, FFmpeg.AV_PIX_FMT_RGBA,
                FFmpeg.SWS_BILINEAR, null, null, null
            ) ?: return

            destinations.setPointer(0, Native.getDirectBufferPointer(next))
            destinationStrides.setInt(0, width * 4)

            ffmpeg.swscale.sws_scale(
                scaler!!, frame.share(FFmpeg.FRAME_DATA), frame.share(FFmpeg.FRAME_LINESIZE), 0, frameHeight,
                destinations, destinationStrides
            )

            synchronized(frameLock) {
                nextFrame = shownFrame
                shownFrame = next
            }

            val texture = texture ?: return
            val region = region ?: return
            texture.invalidateRegion(region)
        }

        override fun close() {
            ffmpeg.swscale.sws_freeContext(scaler)
            frame?.let { ffmpeg.avutil.av_frame_free(PointerByReference(it)) }
            packet?.let { ffmpeg.avcodec.av_packet_free(PointerByReference(it)) }
            codecContext?.let { ffmpeg.avcodec.avcodec_free_context(PointerByReference(it)) }
            if (format != null) {
                ffmpeg.avformat.avformat_close_input(formatReference)
            }
        }
    }

    /**
     * The shown frame, as the source of the texture's region.
     */
    private inner class FrameSource(private val frameWidth: Int, private val frameHeight: Int) : GraphicsTexture.Source {

        override val width = frameWidth.toFloat()
        override val height = frameHeight.toFloat()

        override fun writePixels(target: GraphicsTexture.PixelTarget, x: Int, y: Int) {
            synchronized(frameLock) {
                val pixels = shownFrame ?: return
                pixels.position(0).limit(frameWidth * frameHeight * 4)
                target.write(x, y, frameWidth, frameHeight, pixels)
            }
        }
    }


    private companion object {
        // A frame shown later than this behind the video's clock is skipped.
        const val MAX_LATENESS_SECONDS = 0.1

        // Timestamps rounded by the stream's time base still land on the frame asked for.
        const val SEEK_TOLERANCE_SECONDS = 0.001
    }
}
