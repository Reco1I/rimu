package com.reco1l.bassbinding.stream

import androidx.annotation.FloatRange
import com.reco1l.bassbinding.AudioChannel
import com.reco1l.bassbinding.AudioChannel.BOTH
import com.reco1l.bassbinding.AudioChannel.LEFT
import com.reco1l.bassbinding.AudioChannel.RIGHT
import com.reco1l.bassbinding.AudioState
import com.reco1l.bassbinding.exceptions.InvalidBassDevice
import com.reco1l.bassbinding.exceptions.InvalidBassStream
import com.un4seen.bass.BASS.BASS_ACTIVE_PAUSED
import com.un4seen.bass.BASS.BASS_ACTIVE_PLAYING
import com.un4seen.bass.BASS.BASS_ACTIVE_STOPPED
import com.un4seen.bass.BASS.BASS_ATTRIB_VOL
import com.un4seen.bass.BASS.BASS_CHANNELINFO
import com.un4seen.bass.BASS.BASS_ChannelBytes2Seconds
import com.un4seen.bass.BASS.BASS_ChannelGetAttribute
import com.un4seen.bass.BASS.BASS_ChannelGetData
import com.un4seen.bass.BASS.BASS_ChannelGetInfo
import com.un4seen.bass.BASS.BASS_ChannelGetLength
import com.un4seen.bass.BASS.BASS_ChannelGetLevel
import com.un4seen.bass.BASS.BASS_ChannelGetPosition
import com.un4seen.bass.BASS.BASS_ChannelIsActive
import com.un4seen.bass.BASS.BASS_ChannelPause
import com.un4seen.bass.BASS.BASS_ChannelPlay
import com.un4seen.bass.BASS.BASS_ChannelSeconds2Bytes
import com.un4seen.bass.BASS.BASS_ChannelSetAttribute
import com.un4seen.bass.BASS.BASS_ChannelSetPosition
import com.un4seen.bass.BASS.BASS_ChannelSetSync
import com.un4seen.bass.BASS.BASS_ChannelStop
import com.un4seen.bass.BASS.BASS_FXSetParameters
import com.un4seen.bass.BASS.BASS_GetDevice
import com.un4seen.bass.BASS.BASS_POS_BYTE
import com.un4seen.bass.BASS.BASS_POS_DECODE
import com.un4seen.bass.BASS.BASS_SYNC_END
import com.un4seen.bass.BASS.BASS_Stop
import com.un4seen.bass.BASS.BASS_StreamFree
import com.un4seen.bass.BASS.FloatValue
import com.un4seen.bass.BASS_FX.BASS_ATTRIB_TEMPO
import com.un4seen.bass.BASS_FX.BASS_ATTRIB_TEMPO_PITCH
import com.un4seen.bass.BASS_FX.BASS_BFX_BQF
import com.un4seen.bass.BASS_FX.BASS_BFX_BQF_HIGHPASS
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
            BASS_ChannelGetInfo(id, field)
            return field
        }

    /**
     * Returns the channel current [AudioState].
     */
    val state: AudioState
        get() = when (BASS_ChannelIsActive(id))
        {
            BASS_ACTIVE_STOPPED -> AudioState.STOPPED
            BASS_ACTIVE_PAUSED -> AudioState.PAUSED
            BASS_ACTIVE_PLAYING -> AudioState.PLAYING
            else -> AudioState.STOPPED
        }

    /**
     * The current audio length in milliseconds.
     */
    val length
        get() = BASS_ChannelBytes2Seconds(
            id,
            BASS_ChannelGetLength(id, BASS_POS_BYTE)
        ) * 1000


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
            BASS_ChannelSetPosition(
                id,
                BASS_ChannelSeconds2Bytes(id, value / 1000.0),
                BASS_POS_DECODE
            )
        }
        get() = (BASS_ChannelBytes2Seconds(
            id,
            BASS_ChannelGetPosition(id, BASS_POS_BYTE)
        ) * 1000).toLong()

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
            setFxParameters(muffleEqualizer.apply { fGain = -12 * value })
            field = value
        }


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
    fun stop() = BASS_ChannelStop(id)

    /**
     * Binding for [BASS_ChannelPause]
     */
    fun pause() = BASS_ChannelPause(id)

    /**
     * Binding for [BASS_ChannelPlay]
     */
    fun play(restart: Boolean = false) = BASS_ChannelPlay(id, restart)

    /**
     * Binding for [BASS_StreamFree]
     */
    open fun free(): Boolean
    {
        BASS_Stop()
        return BASS_StreamFree(id).also { id = 0 }
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
        val left = (level and 0xFFFF) / Short.MAX_VALUE
        val right = (level shr 16 and 0xFFFF) / Short.MAX_VALUE

        return when (channel)
        {
            LEFT -> left.toFloat()
            RIGHT -> right.toFloat()
            BOTH -> (left + right) / 2f
        }
    }

    /**
     * Binding for [BASS_ChannelGetAttribute].
     */
    fun getAttribute(attribute: Int): Float
    {
        // Not sure about the reason of this class but it's required by the BASS JNI
        val float = FloatValue()
        BASS_ChannelGetAttribute(id, attribute, float)
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
        return BASS_ChannelSetAttribute(id, attribute, value)
    }

    /**
     * Binding for [BASS_FXSetParameters]
     */
    fun setFxParameters(params: Any): Boolean
    {
        return BASS_FXSetParameters(id, params)
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