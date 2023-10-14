package com.reco1l.basskt.stream

import androidx.annotation.FloatRange
import com.reco1l.basskt.AudioChannel
import com.reco1l.basskt.AudioChannel.BOTH
import com.reco1l.basskt.AudioChannel.LEFT
import com.reco1l.basskt.AudioChannel.RIGHT
import com.reco1l.basskt.AudioState
import com.reco1l.basskt.AudioState.*
import com.reco1l.basskt.BASS_ErrorGetName
import com.reco1l.basskt.BASS_GetAttributeName
import com.reco1l.basskt.InvalidBassDevice
import com.reco1l.framework.android.logE
import com.reco1l.framework.lang.isLazyInitialized
import com.reco1l.framework.lang.klass
import com.un4seen.bass.BASS.*
import com.un4seen.bass.BASS_FX.*
import java.nio.ByteBuffer

/**
 * BASS audio stream implementation.
 * [Official documentation](https://www.un4seen.com/doc/)
 */
abstract class BaseStream(source: String? = null)
{

    /**
     * The source file path.
     */
    var source: String? = source
        set(value)
        {
            if (id != 0)
                free()

            if (value != null)
            {
                id = onSourceLoad(value)

                if (id == 0)
                    throw InvalidBassStream()

                onApplyProperties()
            }
            field = value
        }

    /**
     * The stream ID, usually this shouldn't be 0.
     */
    var id = 0
        protected set


    // Initializing

    init
    {
        if (BASS_GetDevice() == -1)
            throw InvalidBassDevice()
    }

    /**
     * The source should be initialized as stream inside this function, it'll called everytime [source]
     * has been changed.
     */
    protected abstract fun onSourceLoad(path: String): Int

    /**
     * Called after [onSourceLoad] if the source was loaded successful.
     */
    protected fun onApplyProperties()
    {
        volume = volume

        if (speed != 1f)
            speed = speed

        if (pitch != 1f)
            pitch = pitch

        if (endCallback != null)
            endCallback = endCallback

        if (muffle != 1f)
            muffle = muffle
    }

    // Information

    /**
     * Return a new [BASS_CHANNELINFO] instance with this channel information in it.
     */
    val info = BASS_CHANNELINFO()
        get()
        {
            if (!BASS_ChannelGetInfo(id, field))
               klass logE "Failed to get channel information: ${BASS_ErrorGetName()}"

            return field
        }

    /**
     * Returns the channel current [AudioState].
     */
    val state: AudioState
        get() = when (BASS_ChannelIsActive(id))
        {
            BASS_ACTIVE_PAUSED -> PAUSED
            BASS_ACTIVE_PLAYING -> PLAYING
            BASS_ACTIVE_STOPPED -> STOPPED
            else -> STALLED
        }

    /**
     * The current audio length in milliseconds.
     */
    val length: Long
        get()
        {
            val length = BASS_ChannelGetLength(id, BASS_POS_BYTE)

            return BASS_ChannelBytes2Seconds(id, length).toLong() * 1000L
        }


    // Properties

    /**
     * The current audio volume in a range of 0f to 1f.
     */
    var volume: Float
        set(@FloatRange(0.0, 1.0) value)
        {
            setAttribute(BASS_ATTRIB_VOL, value)
        }
        get() = getAttribute(BASS_ATTRIB_VOL)

    /**
     * Binding for [BASS_ChannelSetPosition] and [BASS_ChannelGetPosition] with milliseconds.
     *
     * @see BASS_ChannelBytes2Seconds
     * @see BASS_ChannelSeconds2Bytes
     */
    var position: Long
        set(value)
        {
            val bytes = BASS_ChannelSeconds2Bytes(id, value / 1000.0)

            if (!BASS_ChannelSetPosition(id, bytes, BASS_POS_DECODE))
                klass logE "Failed to change position: ${BASS_ErrorGetName()}"
        }
        get()
        {
            val bytes = BASS_ChannelGetPosition(id, BASS_POS_BYTE)

            return BASS_ChannelBytes2Seconds(id, bytes).toLong() * 1000L
        }

    /**
     * Set the playback speed.
     */
    var speed: Float = 1f
        set(value)
        {
            setAttribute(BASS_ATTRIB_TEMPO, (value - 1.0f) * 100)
            field = value
        }

    /**
     * Set the audio pitch shift.
     */
    var pitch: Float = 1f
        set(value)
        {
            setAttribute(BASS_ATTRIB_TEMPO_PITCH, info.freq * value)
            field = value
        }

    /**
     * Set the audio muffle effect gain.
     */
    var muffle: Float = 1f
        set(value)
        {
            // Don't initialize the equalizer if not needed
            if (value == 1f && field == 1f && !::muffleEqualizer.isLazyInitialized)
                return

            setFxParameters(muffleEqualizer.apply { fGain = -12 * value })
            field = value
        }

    /**
     * Set the audio buffer length.
     */
    var bufferLength
        get() = getAttribute(BASS_ATTRIB_BUFFER)
        set(value) { setAttribute(BASS_ATTRIB_BUFFER, value) }


    // Callbacks

    /**
     * The callback for [BASS_SYNC_END].
     */
    var endCallback: (() -> Unit)? = null
        set(value)
        {
            // Removing in case the callback is set to null
            if (value == null)
                setSync(BASS_SYNC_END)
            else
                setSync(BASS_SYNC_END) { value() }

            field = value
        }


    // Private

    private val muffleEqualizer by lazy {

        BASS_BFX_BQF().apply {

            lFilter = BASS_BFX_BQF_HIGHPASS
            fCenter = 8000f
            fGain = 0f
            fQ = 1f
        }
    }


    // Media control

    /**
     * Binding for [BASS_ChannelStop]
     */
    fun stop() = BASS_ChannelStop(id).also {

        if (!it)
            klass logE "Failed to stop: ${BASS_ErrorGetName()}"
    }

    /**
     * Binding for [BASS_ChannelPause]
     */
    fun pause() = BASS_ChannelPause(id).also {

        if (!it)
            klass logE "Failed to pause: ${BASS_ErrorGetName()}"
    }

    /**
     * Binding for [BASS_ChannelPlay]
     */
    fun play(restart: Boolean = false) = BASS_ChannelPlay(id, restart).also {

        if (!it)
            klass logE "Failed to play: ${BASS_ErrorGetName()}"
    }

    /**
     * Binding for [BASS_StreamFree]
     */
    open fun free(): Boolean
    {
        if (state != STOPPED || state != STALLED)
            stop()

        return BASS_StreamFree(id).also {

            if (!it)
                klass logE "Failed to free stream: ${BASS_ErrorGetName()}"

            id = 0
        }
    }


    // Getters

    /**
     * Binding for [BASS_ChannelGetLevel].
     *
     * @param channel The channel to get the level, by default [BOTH].
     */
    fun getLevel(channel: AudioChannel = BOTH): Float
    {
        val level = BASS_ChannelGetLevel(id)

        return when (channel)
        {
            // The 1f is conversion to Float
            LEFT -> (level and 0xFFFF) / Short.MAX_VALUE * 1f
            RIGHT -> (level shr 16 and 0xFFFF) / Short.MAX_VALUE * 1f

            BOTH -> (level and 0xFFFF + level shr 16 and 0xFFFF) / 2f
        }
    }

    /**
     * Binding for [BASS_ChannelGetAttribute].
     */
    fun getAttribute(attribute: Int): Float
    {
        // Not sure about the reason of this class but it's required by the BASS JNI
        val float = FloatValue()

        if (!BASS_ChannelGetAttribute(id, attribute, float))
            klass logE "Failed to get \"${BASS_GetAttributeName(attribute)}\": ${BASS_ErrorGetName()}"

        return float.value
    }

    /**
     * Binding for [BASS_ChannelGetData].
     */
    fun getData(buffer: ByteBuffer, length: Int) = BASS_ChannelGetData(id, buffer, length)


    // Setters

    /**
     * Binding for [BASS_ChannelSetAttribute]
     */
    fun setAttribute(attribute: Int, value: Float): Boolean
    {
        return BASS_ChannelSetAttribute(id, attribute, value).also {

            if (!it)
                klass logE "Failed to set ${BASS_GetAttributeName(attribute)} to \"$value\": ${BASS_ErrorGetName()}"
        }
    }

    /**
     * Binding for [BASS_FXSetParameters]
     */
    fun setFxParameters(params: Any): Boolean
    {
        return BASS_FXSetParameters(id, params).also {

            if (!it)
                klass logE "Failed to set FX parameters to \"$params\" : ${BASS_ErrorGetName()}"
        }
    }

    /**
     * Binding for [BASS_ChannelSetSync]
     */
    fun setSync(type: Int, param: Long = 0, sync: ((data: Int) -> Unit)? = null): Int
    {
        // If the sync procedure is null we remove the previous callback
        if (sync == null)
            return BASS_ChannelSetSync(id, type, param, null, null)

        // Otherwise we overwrite
        return BASS_ChannelSetSync(id, type, param, { _, _, data, _ -> sync(data) }, null)
    }


    override fun equals(other: Any?) = other === this || other is BaseStream
            && other.id == id
            && other.source == source


    // Generated
    override fun hashCode(): Int
    {
        var result = source?.hashCode() ?: 0
        result = 31 * result + id
        result = 31 * result + speed.hashCode()
        result = 31 * result + pitch.hashCode()
        result = 31 * result + muffle.hashCode()
        result = 31 * result + (endCallback?.hashCode() ?: 0)
        return result
    }
}


class InvalidBassStream : Exception("Failed to initialize BASS channel: ${BASS_ErrorGetName()}")


class AudioStream(

    source: String? = null,

    /**
     * Define the stream flags, by default [BASS_STREAM_DECODE] and [BASS_STREAM_PRESCAN].
     */
    var flags: Int = BASS_STREAM_DECODE or BASS_STREAM_PRESCAN,

    /**
     * Define the FX flags, by default [BASS_STREAM_AUTOFREE].
     */
    var fxFlags: Int = BASS_STREAM_AUTOFREE

) : BaseStream(source)
{
    override fun onSourceLoad(path: String): Int
    {
        val id = BASS_StreamCreateFile(path, 0, 0, flags)

        return BASS_FX_TempoCreate(id, fxFlags)
    }
}
