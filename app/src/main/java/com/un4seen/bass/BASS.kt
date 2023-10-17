/*
	BASS 2.4 Java class
	Copyright (c) 1999-2022 Un4seen Developments Ltd.

	See the BASS.CHM file for more detailed documentation
*/

// Converted to Kotlin by Reco1l

@file:Suppress(
    "unused",
    "ClassName",
    "FunctionName",
    "NotConstructor",
    "LocalVariableName",
    "SpellCheckingInspection",
    "MemberVisibilityCanBePrivate"
)

package com.un4seen.bass

import android.content.res.AssetManager
import android.os.ParcelFileDescriptor
import java.io.IOException
import java.nio.ByteBuffer

object BASS {
    const val BASSVERSION = 0x204 // API version
    const val BASSVERSIONTEXT = "2.4"

    // Error codes returned by BASS_ErrorGetCode
    const val BASS_OK = 0 // all is OK
    const val BASS_ERROR_MEM = 1 // memory error
    const val BASS_ERROR_FILEOPEN = 2 // can't open the file
    const val BASS_ERROR_DRIVER = 3 // can't find a free/valid driver
    const val BASS_ERROR_BUFLOST = 4 // the sample buffer was lost
    const val BASS_ERROR_HANDLE = 5 // invalid handle
    const val BASS_ERROR_FORMAT = 6 // unsupported sample format
    const val BASS_ERROR_POSITION = 7 // invalid position
    const val BASS_ERROR_INIT = 8 // BASS_Init has not been successfully called
    const val BASS_ERROR_START = 9 // BASS_Start has not been successfully called
    const val BASS_ERROR_SSL = 10 // SSL/HTTPS support isn't available
    const val BASS_ERROR_REINIT = 11 // device needs to be reinitialized
    const val BASS_ERROR_ALREADY = 14 // already initialized/paused/whatever
    const val BASS_ERROR_NOTAUDIO = 17 // file does not contain audio
    const val BASS_ERROR_NOCHAN = 18 // can't get a free channel
    const val BASS_ERROR_ILLTYPE = 19 // an illegal type was specified
    const val BASS_ERROR_ILLPARAM = 20 // an illegal parameter was specified
    const val BASS_ERROR_NO3D = 21 // no 3D support
    const val BASS_ERROR_NOEAX = 22 // no EAX support
    const val BASS_ERROR_DEVICE = 23 // illegal device number
    const val BASS_ERROR_NOPLAY = 24 // not playing
    const val BASS_ERROR_FREQ = 25 // illegal sample rate
    const val BASS_ERROR_NOTFILE = 27 // the stream is not a file stream
    const val BASS_ERROR_NOHW = 29 // no hardware voices available
    const val BASS_ERROR_EMPTY = 31 // the file has no sample data
    const val BASS_ERROR_NONET = 32 // no internet connection could be opened
    const val BASS_ERROR_CREATE = 33 // couldn't create the file
    const val BASS_ERROR_NOFX = 34 // effects are not available
    const val BASS_ERROR_NOTAVAIL = 37 // requested data/action is not available
    const val BASS_ERROR_DECODE = 38 // the channel is a "decoding channel"
    const val BASS_ERROR_DX = 39 // a sufficient DirectX version is not installed
    const val BASS_ERROR_TIMEOUT = 40 // connection timedout
    const val BASS_ERROR_FILEFORM = 41 // unsupported file format
    const val BASS_ERROR_SPEAKER = 42 // unavailable speaker
    const val BASS_ERROR_VERSION = 43 // invalid BASS version (used by add-ons)
    const val BASS_ERROR_CODEC = 44 // codec is not available/supported
    const val BASS_ERROR_ENDED = 45 // the channel/file has ended
    const val BASS_ERROR_BUSY = 46 // the device is busy
    const val BASS_ERROR_UNSTREAMABLE = 47 // unstreamable file
    const val BASS_ERROR_PROTOCOL = 48 // unsupported protocol
    const val BASS_ERROR_DENIED = 49 // access denied
    const val BASS_ERROR_UNKNOWN = -1 // some other mystery problem
    const val BASS_ERROR_JAVA_CLASS = 500 // object class problem

    // BASS_SetConfig options
    const val BASS_CONFIG_BUFFER = 0
    const val BASS_CONFIG_UPDATEPERIOD = 1
    const val BASS_CONFIG_GVOL_SAMPLE = 4
    const val BASS_CONFIG_GVOL_STREAM = 5
    const val BASS_CONFIG_GVOL_MUSIC = 6
    const val BASS_CONFIG_CURVE_VOL = 7
    const val BASS_CONFIG_CURVE_PAN = 8
    const val BASS_CONFIG_FLOATDSP = 9
    const val BASS_CONFIG_3DALGORITHM = 10
    const val BASS_CONFIG_NET_TIMEOUT = 11
    const val BASS_CONFIG_NET_BUFFER = 12
    const val BASS_CONFIG_PAUSE_NOPLAY = 13
    const val BASS_CONFIG_NET_PREBUF = 15
    const val BASS_CONFIG_NET_PASSIVE = 18
    const val BASS_CONFIG_REC_BUFFER = 19
    const val BASS_CONFIG_NET_PLAYLIST = 21
    const val BASS_CONFIG_MUSIC_VIRTUAL = 22
    const val BASS_CONFIG_VERIFY = 23
    const val BASS_CONFIG_UPDATETHREADS = 24
    const val BASS_CONFIG_DEV_BUFFER = 27
    const val BASS_CONFIG_DEV_DEFAULT = 36
    const val BASS_CONFIG_NET_READTIMEOUT = 37
    const val BASS_CONFIG_HANDLES = 41
    const val BASS_CONFIG_SRC = 43
    const val BASS_CONFIG_SRC_SAMPLE = 44
    const val BASS_CONFIG_ASYNCFILE_BUFFER = 45
    const val BASS_CONFIG_OGG_PRESCAN = 47
    const val BASS_CONFIG_DEV_NONSTOP = 50
    const val BASS_CONFIG_VERIFY_NET = 52
    const val BASS_CONFIG_DEV_PERIOD = 53
    const val BASS_CONFIG_FLOAT = 54
    const val BASS_CONFIG_NET_SEEK = 56
    const val BASS_CONFIG_AM_DISABLE = 58
    const val BASS_CONFIG_NET_PLAYLIST_DEPTH = 59
    const val BASS_CONFIG_NET_PREBUF_WAIT = 60
    const val BASS_CONFIG_ANDROID_SESSIONID = 62
    const val BASS_CONFIG_ANDROID_AAUDIO = 67
    const val BASS_CONFIG_SAMPLE_ONEHANDLE = 69
    const val BASS_CONFIG_DEV_TIMEOUT = 70
    const val BASS_CONFIG_NET_META = 71
    const val BASS_CONFIG_NET_RESTRATE = 72
    const val BASS_CONFIG_REC_DEFAULT = 73
    const val BASS_CONFIG_NORAMP = 74

    // BASS_SetConfigPtr options
    const val BASS_CONFIG_NET_AGENT = 16
    const val BASS_CONFIG_NET_PROXY = 17
    const val BASS_CONFIG_LIBSSL = 64
    const val BASS_CONFIG_FILENAME = 75
    const val BASS_CONFIG_THREAD = 0x40000000 // flag: thread-specific setting

    // BASS_Init flags
    const val BASS_DEVICE_8BITS = 1 // unused
    const val BASS_DEVICE_MONO = 2 // mono
    const val BASS_DEVICE_3D = 4 // unused
    const val BASS_DEVICE_16BITS = 8 // limit output to 16-bit
    const val BASS_DEVICE_REINIT = 128 // reinitialize
    const val BASS_DEVICE_LATENCY = 0x100 // unused
    const val BASS_DEVICE_SPEAKERS = 0x800 // force enabling of speaker assignment
    const val BASS_DEVICE_NOSPEAKER = 0x1000 // ignore speaker arrangement
    const val BASS_DEVICE_FREQ = 0x4000 // set device sample rate
    const val BASS_DEVICE_AUDIOTRACK = 0x20000 // use AudioTrack output
    const val BASS_DEVICE_SOFTWARE = 0x80000 // disable hardware/fastpath output

    // BASS_DEVICEINFO flags
    const val BASS_DEVICE_ENABLED = 1
    const val BASS_DEVICE_DEFAULT = 2
    const val BASS_DEVICE_INIT = 4
    const val BASS_SAMPLE_8BITS = 1 // 8 bit
    const val BASS_SAMPLE_FLOAT = 256 // 32-bit floating-point
    const val BASS_SAMPLE_MONO = 2 // mono
    const val BASS_SAMPLE_LOOP = 4 // looped
    const val BASS_SAMPLE_3D = 8 // 3D functionality
    const val BASS_SAMPLE_SOFTWARE = 16 // unused
    const val BASS_SAMPLE_MUTEMAX = 32 // mute at max distance (3D only)
    const val BASS_SAMPLE_VAM = 64 // unused
    const val BASS_SAMPLE_FX = 128 // unused
    const val BASS_SAMPLE_OVER_VOL = 0x10000 // override lowest volume
    const val BASS_SAMPLE_OVER_POS = 0x20000 // override longest playing
    const val BASS_SAMPLE_OVER_DIST = 0x30000 // override furthest from listener (3D only)
    const val BASS_STREAM_PRESCAN = 0x20000 // scan file for accurate seeking and length
    const val BASS_STREAM_AUTOFREE = 0x40000 // automatically free the stream when it stops/ends
    const val BASS_STREAM_RESTRATE = 0x80000 // restrict the download rate of internet file streams
    const val BASS_STREAM_BLOCK = 0x100000 // download/play internet file stream in small blocks
    const val BASS_STREAM_DECODE =
        0x200000 // don't play the stream, only decode (BASS_ChannelGetData)
    const val BASS_STREAM_STATUS =
        0x800000 // give server status info (HTTP/ICY tags) in DOWNLOADPROC
    const val BASS_MP3_IGNOREDELAY = 0x200 // ignore LAME/Xing/VBRI/iTunes delay & padding info
    const val BASS_MP3_SETPOS = BASS_STREAM_PRESCAN
    const val BASS_MUSIC_FLOAT = BASS_SAMPLE_FLOAT
    const val BASS_MUSIC_MONO = BASS_SAMPLE_MONO
    const val BASS_MUSIC_LOOP = BASS_SAMPLE_LOOP
    const val BASS_MUSIC_3D = BASS_SAMPLE_3D
    const val BASS_MUSIC_FX = BASS_SAMPLE_FX
    const val BASS_MUSIC_AUTOFREE = BASS_STREAM_AUTOFREE
    const val BASS_MUSIC_DECODE = BASS_STREAM_DECODE
    const val BASS_MUSIC_PRESCAN = BASS_STREAM_PRESCAN // calculate playback length
    const val BASS_MUSIC_CALCLEN = BASS_MUSIC_PRESCAN
    const val BASS_MUSIC_RAMP = 0x200 // normal ramping
    const val BASS_MUSIC_RAMPS = 0x400 // sensitive ramping
    const val BASS_MUSIC_SURROUND = 0x800 // surround sound
    const val BASS_MUSIC_SURROUND2 = 0x1000 // surround sound (mode 2)
    const val BASS_MUSIC_FT2PAN = 0x2000 // apply FastTracker 2 panning to XM files
    const val BASS_MUSIC_FT2MOD = 0x2000 // play .MOD as FastTracker 2 does
    const val BASS_MUSIC_PT1MOD = 0x4000 // play .MOD as ProTracker 1 does
    const val BASS_MUSIC_NONINTER = 0x10000 // non-interpolated sample mixing
    const val BASS_MUSIC_SINCINTER = 0x800000 // sinc interpolated sample mixing
    const val BASS_MUSIC_POSRESET = 0x8000 // stop all notes when moving position
    const val BASS_MUSIC_POSRESETEX =
        0x400000 // stop all notes and reset bmp/etc when moving position
    const val BASS_MUSIC_STOPBACK = 0x80000 // stop the music on a backwards jump effect
    const val BASS_MUSIC_NOSAMPLE = 0x100000 // don't load the samples

    // Speaker assignment flags
    const val BASS_SPEAKER_FRONT = 0x1000000 // front speakers
    const val BASS_SPEAKER_REAR = 0x2000000 // rear speakers
    const val BASS_SPEAKER_CENLFE = 0x3000000 // center & LFE speakers (5.1)
    const val BASS_SPEAKER_SIDE = 0x4000000 // side speakers (7.1)
    fun BASS_SPEAKER_N(n: Int): Int {
        return n shl 24
    } // n'th pair of speakers (max 15)

    const val BASS_SPEAKER_LEFT = 0x10000000 // modifier: left
    const val BASS_SPEAKER_RIGHT = 0x20000000 // modifier: right
    const val BASS_SPEAKER_FRONTLEFT = BASS_SPEAKER_FRONT or BASS_SPEAKER_LEFT
    const val BASS_SPEAKER_FRONTRIGHT = BASS_SPEAKER_FRONT or BASS_SPEAKER_RIGHT
    const val BASS_SPEAKER_REARLEFT = BASS_SPEAKER_REAR or BASS_SPEAKER_LEFT
    const val BASS_SPEAKER_REARRIGHT = BASS_SPEAKER_REAR or BASS_SPEAKER_RIGHT
    const val BASS_SPEAKER_CENTER = BASS_SPEAKER_CENLFE or BASS_SPEAKER_LEFT
    const val BASS_SPEAKER_LFE = BASS_SPEAKER_CENLFE or BASS_SPEAKER_RIGHT
    const val BASS_SPEAKER_SIDELEFT = BASS_SPEAKER_SIDE or BASS_SPEAKER_LEFT
    const val BASS_SPEAKER_SIDERIGHT = BASS_SPEAKER_SIDE or BASS_SPEAKER_RIGHT
    const val BASS_SPEAKER_REAR2 = BASS_SPEAKER_SIDE
    const val BASS_SPEAKER_REAR2LEFT = BASS_SPEAKER_SIDELEFT
    const val BASS_SPEAKER_REAR2RIGHT = BASS_SPEAKER_SIDERIGHT
    const val BASS_ASYNCFILE = 0x40000000 // read file asynchronously
    const val BASS_RECORD_PAUSE = 0x8000 // start recording paused
    const val BASS_ORIGRES_FLOAT = 0x10000

    // BASS_CHANNELINFO types
    const val BASS_CTYPE_SAMPLE = 1
    const val BASS_CTYPE_RECORD = 2
    const val BASS_CTYPE_STREAM = 0x10000
    const val BASS_CTYPE_STREAM_VORBIS = 0x10002
    const val BASS_CTYPE_STREAM_OGG = 0x10002
    const val BASS_CTYPE_STREAM_MP1 = 0x10003
    const val BASS_CTYPE_STREAM_MP2 = 0x10004
    const val BASS_CTYPE_STREAM_MP3 = 0x10005
    const val BASS_CTYPE_STREAM_AIFF = 0x10006
    const val BASS_CTYPE_STREAM_CA = 0x10007
    const val BASS_CTYPE_STREAM_MF = 0x10008
    const val BASS_CTYPE_STREAM_AM = 0x10009
    const val BASS_CTYPE_STREAM_SAMPLE = 0x1000a
    const val BASS_CTYPE_STREAM_DUMMY = 0x18000
    const val BASS_CTYPE_STREAM_DEVICE = 0x18001
    const val BASS_CTYPE_STREAM_WAV = 0x40000 // WAVE flag (LOWORD=codec)
    const val BASS_CTYPE_STREAM_WAV_PCM = 0x50001
    const val BASS_CTYPE_STREAM_WAV_FLOAT = 0x50003
    const val BASS_CTYPE_MUSIC_MOD = 0x20000
    const val BASS_CTYPE_MUSIC_MTM = 0x20001
    const val BASS_CTYPE_MUSIC_S3M = 0x20002
    const val BASS_CTYPE_MUSIC_XM = 0x20003
    const val BASS_CTYPE_MUSIC_IT = 0x20004
    const val BASS_CTYPE_MUSIC_MO3 = 0x00100 // MO3 flag

    // 3D channel modes
    const val BASS_3DMODE_NORMAL = 0 // normal 3D processing
    const val BASS_3DMODE_RELATIVE = 1 // position is relative to the listener
    const val BASS_3DMODE_OFF = 2 // no 3D processing

    // software 3D mixing algorithms (used with BASS_CONFIG_3DALGORITHM)
    const val BASS_3DALG_DEFAULT = 0
    const val BASS_3DALG_OFF = 1
    const val BASS_3DALG_FULL = 2
    const val BASS_3DALG_LIGHT = 3

    // BASS_SampleGetChannel flags
    const val BASS_SAMCHAN_NEW = 1 // get a new playback channel
    const val BASS_SAMCHAN_STREAM = 2 // create a stream
    const val BASS_STREAMPROC_END = -0x80000000 // end of user stream flag

    // Special STREAMPROCs
    const val STREAMPROC_DUMMY = 0 // "dummy" stream
    const val STREAMPROC_PUSH = -1 // push stream
    const val STREAMPROC_DEVICE = -2 // device mix stream
    const val STREAMPROC_DEVICE_3D = -3 // device 3D mix stream

    // BASS_StreamCreateFileUser file systems
    const val STREAMFILE_NOBUFFER = 0
    const val STREAMFILE_BUFFER = 1
    const val STREAMFILE_BUFFERPUSH = 2

    // BASS_StreamPutFileData options
    const val BASS_FILEDATA_END = 0 // end & close the file

    // BASS_StreamGetFilePosition modes
    const val BASS_FILEPOS_CURRENT = 0
    const val BASS_FILEPOS_DECODE = BASS_FILEPOS_CURRENT
    const val BASS_FILEPOS_DOWNLOAD = 1
    const val BASS_FILEPOS_END = 2
    const val BASS_FILEPOS_START = 3
    const val BASS_FILEPOS_CONNECTED = 4
    const val BASS_FILEPOS_BUFFER = 5
    const val BASS_FILEPOS_SOCKET = 6
    const val BASS_FILEPOS_ASYNCBUF = 7
    const val BASS_FILEPOS_SIZE = 8
    const val BASS_FILEPOS_BUFFERING = 9
    const val BASS_FILEPOS_AVAILABLE = 10

    // BASS_ChannelSetSync types
    const val BASS_SYNC_POS = 0
    const val BASS_SYNC_END = 2
    const val BASS_SYNC_META = 4
    const val BASS_SYNC_SLIDE = 5
    const val BASS_SYNC_STALL = 6
    const val BASS_SYNC_DOWNLOAD = 7
    const val BASS_SYNC_FREE = 8
    const val BASS_SYNC_SETPOS = 11
    const val BASS_SYNC_MUSICPOS = 10
    const val BASS_SYNC_MUSICINST = 1
    const val BASS_SYNC_MUSICFX = 3
    const val BASS_SYNC_OGG_CHANGE = 12
    const val BASS_SYNC_DEV_FAIL = 14
    const val BASS_SYNC_DEV_FORMAT = 15
    const val BASS_SYNC_THREAD = 0x20000000 // flag: call sync in other thread
    const val BASS_SYNC_MIXTIME = 0x40000000 // flag: sync at mixtime, else at playtime
    const val BASS_SYNC_ONETIME = -0x80000000 // flag: sync only once, else continuously

    // BASS_ChannelIsActive return values
    const val BASS_ACTIVE_STOPPED = 0
    const val BASS_ACTIVE_PLAYING = 1
    const val BASS_ACTIVE_STALLED = 2
    const val BASS_ACTIVE_PAUSED = 3
    const val BASS_ACTIVE_PAUSED_DEVICE = 4

    // Channel attributes
    const val BASS_ATTRIB_FREQ = 1
    const val BASS_ATTRIB_VOL = 2
    const val BASS_ATTRIB_PAN = 3
    const val BASS_ATTRIB_EAXMIX = 4
    const val BASS_ATTRIB_NOBUFFER = 5
    const val BASS_ATTRIB_VBR = 6
    const val BASS_ATTRIB_CPU = 7
    const val BASS_ATTRIB_SRC = 8
    const val BASS_ATTRIB_NET_RESUME = 9
    const val BASS_ATTRIB_SCANINFO = 10
    const val BASS_ATTRIB_NORAMP = 11
    const val BASS_ATTRIB_BITRATE = 12
    const val BASS_ATTRIB_BUFFER = 13
    const val BASS_ATTRIB_GRANULE = 14
    const val BASS_ATTRIB_USER = 15
    const val BASS_ATTRIB_TAIL = 16
    const val BASS_ATTRIB_PUSH_LIMIT = 17
    const val BASS_ATTRIB_DOWNLOADPROC = 18
    const val BASS_ATTRIB_VOLDSP = 19
    const val BASS_ATTRIB_VOLDSP_PRIORITY = 20
    const val BASS_ATTRIB_MUSIC_AMPLIFY = 0x100
    const val BASS_ATTRIB_MUSIC_PANSEP = 0x101
    const val BASS_ATTRIB_MUSIC_PSCALER = 0x102
    const val BASS_ATTRIB_MUSIC_BPM = 0x103
    const val BASS_ATTRIB_MUSIC_SPEED = 0x104
    const val BASS_ATTRIB_MUSIC_VOL_GLOBAL = 0x105
    const val BASS_ATTRIB_MUSIC_VOL_CHAN = 0x200 // + channel #
    const val BASS_ATTRIB_MUSIC_VOL_INST = 0x300 // + instrument #

    // BASS_ChannelSlideAttribute flags
    const val BASS_SLIDE_LOG = 0x1000000

    // BASS_ChannelGetData flags
    const val BASS_DATA_AVAILABLE = 0 // query how much data is buffered
    const val BASS_DATA_NOREMOVE = 0x10000000 // flag: don't remove data from recording buffer
    const val BASS_DATA_FIXED = 0x20000000 // unused
    const val BASS_DATA_FLOAT = 0x40000000 // flag: return floating-point sample data
    const val BASS_DATA_FFT256 = -0x80000000 // 256 sample FFT
    const val BASS_DATA_FFT512 = -0x7fffffff // 512 FFT
    const val BASS_DATA_FFT1024 = -0x7ffffffe // 1024 FFT
    const val BASS_DATA_FFT2048 = -0x7ffffffd // 2048 FFT
    const val BASS_DATA_FFT4096 = -0x7ffffffc // 4096 FFT
    const val BASS_DATA_FFT8192 = -0x7ffffffb // 8192 FFT
    const val BASS_DATA_FFT16384 = -0x7ffffffa // 16384 FFT
    const val BASS_DATA_FFT32768 = -0x7ffffff9 // 32768 FFT
    const val BASS_DATA_FFT_INDIVIDUAL = 0x10 // FFT flag: FFT for each channel, else all combined
    const val BASS_DATA_FFT_NOWINDOW = 0x20 // FFT flag: no Hanning window
    const val BASS_DATA_FFT_REMOVEDC = 0x40 // FFT flag: pre-remove DC bias
    const val BASS_DATA_FFT_COMPLEX = 0x80 // FFT flag: return complex data
    const val BASS_DATA_FFT_NYQUIST = 0x100 // FFT flag: return extra Nyquist value

    // BASS_ChannelGetLevelEx flags
    const val BASS_LEVEL_MONO = 1 // get mono level
    const val BASS_LEVEL_STEREO = 2 // get stereo level
    const val BASS_LEVEL_RMS = 4 // get RMS levels
    const val BASS_LEVEL_VOLPAN = 8 // apply VOL/PAN attributes to the levels
    const val BASS_LEVEL_NOREMOVE = 16 // don't remove data from recording buffer

    // BASS_ChannelGetTags types : what's returned
    const val BASS_TAG_ID3 = 0 // ID3v1 tags : TAG_ID3
    const val BASS_TAG_ID3V2 = 1 // ID3v2 tags : ByteBuffer
    const val BASS_TAG_OGG = 2 // OGG comments : String array
    const val BASS_TAG_HTTP = 3 // HTTP headers : String array
    const val BASS_TAG_ICY = 4 // ICY headers : String array
    const val BASS_TAG_META = 5 // ICY metadata : String
    const val BASS_TAG_APE = 6 // APE tags : String array
    const val BASS_TAG_MP4 = 7 // MP4/iTunes metadata : String array
    const val BASS_TAG_VENDOR = 9 // OGG encoder : String
    const val BASS_TAG_LYRICS3 = 10 // Lyric3v2 tag : String
    const val BASS_TAG_WAVEFORMAT =
        14 // WAVE format : ByteBuffer containing WAVEFORMATEEX structure
    const val BASS_TAG_AM_NAME = 16 // Android Media codec name : String
    const val BASS_TAG_ID3V2_2 = 17 // ID3v2 tags (2nd block) : ByteBuffer
    const val BASS_TAG_AM_MIME = 18 // Android Media MIME type : String
    const val BASS_TAG_LOCATION = 19 // redirected URL : String
    const val BASS_TAG_RIFF_INFO = 0x100 // RIFF "INFO" tags : String array
    const val BASS_TAG_RIFF_BEXT = 0x101 // RIFF/BWF "bext" tags : TAG_BEXT
    const val BASS_TAG_RIFF_CART = 0x102 // RIFF/BWF "cart" tags : TAG_CART
    const val BASS_TAG_RIFF_DISP = 0x103 // RIFF "DISP" text tag : String
    const val BASS_TAG_RIFF_CUE = 0x104 // RIFF "cue " chunk : TAG_CUE structure
    const val BASS_TAG_RIFF_SMPL = 0x105 // RIFF "smpl" chunk : TAG_SMPL structure
    const val BASS_TAG_APE_BINARY = 0x1000 // + index #, binary APE tag : TAG_APE_BINARY
    const val BASS_TAG_MUSIC_NAME = 0x10000 // MOD music name : String
    const val BASS_TAG_MUSIC_MESSAGE = 0x10001 // MOD message : String
    const val BASS_TAG_MUSIC_ORDERS = 0x10002 // MOD order list : ByteBuffer
    const val BASS_TAG_MUSIC_AUTH = 0x10003 // MOD author : UTF-8 string
    const val BASS_TAG_MUSIC_INST = 0x10100 // + instrument #, MOD instrument name : String
    const val BASS_TAG_MUSIC_CHAN = 0x10200 // + channel #, MOD channel name : String
    const val BASS_TAG_MUSIC_SAMPLE = 0x10300 // + sample #, MOD sample name : String
    const val BASS_TAG_BYTEBUFFER =
        0x10000000 // flag: return a ByteBuffer instead of a String or TAG_ID3

    // BASS_ChannelGetLength/GetPosition/SetPosition modes
    const val BASS_POS_BYTE = 0 // byte position
    const val BASS_POS_MUSIC_ORDER = 1 // order.row position, MAKELONG(order,row)
    const val BASS_POS_OGG = 3 // OGG bitstream number
    const val BASS_POS_END = 0x10 // trimmed end position
    const val BASS_POS_LOOP = 0x11 // loop start positiom
    const val BASS_POS_FLUSH = 0x1000000 // flag: flush decoder/FX buffers
    const val BASS_POS_RESET = 0x2000000 // flag: reset user file buffers
    const val BASS_POS_RELATIVE = 0x4000000 // flag: seek relative to the current position
    const val BASS_POS_INEXACT = 0x8000000 // flag: allow seeking to inexact position
    const val BASS_POS_DECODE = 0x10000000 // flag: get the decoding (not playing) position
    const val BASS_POS_DECODETO = 0x20000000 // flag: decode to the position instead of seeking
    const val BASS_POS_SCAN = 0x40000000 // flag: scan to the position

    // BASS_ChannelSetDevice/GetDevice option
    const val BASS_NODEVICE = 0x20000

    // DX8 effect types, use with BASS_ChannelSetFX
    const val BASS_FX_DX8_CHORUS = 0
    const val BASS_FX_DX8_COMPRESSOR = 1
    const val BASS_FX_DX8_DISTORTION = 2
    const val BASS_FX_DX8_ECHO = 3
    const val BASS_FX_DX8_FLANGER = 4
    const val BASS_FX_DX8_GARGLE = 5
    const val BASS_FX_DX8_I3DL2REVERB = 6
    const val BASS_FX_DX8_PARAMEQ = 7
    const val BASS_FX_DX8_REVERB = 8
    const val BASS_FX_VOLUME = 9
    const val BASS_DX8_PHASE_NEG_180 = 0
    const val BASS_DX8_PHASE_NEG_90 = 1
    const val BASS_DX8_PHASE_ZERO = 2
    const val BASS_DX8_PHASE_90 = 3
    const val BASS_DX8_PHASE_180 = 4
    external fun BASS_SetConfig(option: Int, value: Int): Boolean
    external fun BASS_GetConfig(option: Int): Int
    external fun BASS_SetConfigPtr(option: Int, value: Any?): Boolean
    external fun BASS_GetConfigPtr(option: Int): Any?
    external fun BASS_GetVersion(): Int
    external fun BASS_ErrorGetCode(): Int
    external fun BASS_GetDeviceInfo(device: Int, info: BASS_DEVICEINFO?): Boolean
    external fun BASS_Init(device: Int, freq: Int, flags: Int): Boolean
    external fun BASS_Free(): Boolean
    external fun BASS_SetDevice(device: Int): Boolean
    external fun BASS_GetDevice(): Int
    external fun BASS_GetInfo(info: BASS_INFO?): Boolean
    external fun BASS_Start(): Boolean
    external fun BASS_Stop(): Boolean
    external fun BASS_Pause(): Boolean
    external fun BASS_IsStarted(): Int
    external fun BASS_Update(length: Int): Boolean
    external fun BASS_GetCPU(): Float
    external fun BASS_SetVolume(volume: Float): Boolean
    external fun BASS_GetVolume(): Float
    external fun BASS_Set3DFactors(distf: Float, rollf: Float, doppf: Float): Boolean
    external fun BASS_Get3DFactors(
        distf: FloatValue?,
        rollf: FloatValue?,
        doppf: FloatValue?
    ): Boolean

    external fun BASS_Set3DPosition(
        pos: BASS_3DVECTOR?,
        vel: BASS_3DVECTOR?,
        front: BASS_3DVECTOR?,
        top: BASS_3DVECTOR?
    ): Boolean

    external fun BASS_Get3DPosition(
        pos: BASS_3DVECTOR?,
        vel: BASS_3DVECTOR?,
        front: BASS_3DVECTOR?,
        top: BASS_3DVECTOR?
    ): Boolean

    external fun BASS_Apply3D()
    external fun BASS_PluginLoad(file: String?, flags: Int): Int
    external fun BASS_PluginFree(handle: Int): Boolean
    external fun BASS_PluginEnable(handle: Int, enable: Boolean): Boolean
    external fun BASS_PluginGetInfo(handle: Int): BASS_PLUGININFO?
    external fun BASS_SampleLoad(
        file: String?,
        offset: Long,
        length: Int,
        max: Int,
        flags: Int
    ): Int

    external fun BASS_SampleLoad(
        file: ByteBuffer?,
        offset: Long,
        length: Int,
        max: Int,
        flags: Int
    ): Int

    external fun BASS_SampleLoad(file: Asset?, offset: Long, length: Int, max: Int, flags: Int): Int
    external fun BASS_SampleLoad(
        file: ParcelFileDescriptor?,
        offset: Long,
        length: Int,
        max: Int,
        flags: Int
    ): Int

    external fun BASS_SampleCreate(length: Int, freq: Int, chans: Int, max: Int, flags: Int): Int
    external fun BASS_SampleFree(handle: Int): Boolean
    external fun BASS_SampleSetData(handle: Int, buffer: ByteBuffer?): Boolean
    external fun BASS_SampleGetData(handle: Int, buffer: ByteBuffer?): Boolean
    external fun BASS_SampleGetInfo(handle: Int, info: BASS_SAMPLE?): Boolean
    external fun BASS_SampleSetInfo(handle: Int, info: BASS_SAMPLE?): Boolean
    external fun BASS_SampleGetChannel(handle: Int, onlynew: Boolean): Int
    external fun BASS_SampleGetChannels(handle: Int, channels: IntArray?): Int
    external fun BASS_SampleStop(handle: Int): Boolean
    external fun BASS_StreamCreate(
        freq: Int,
        chans: Int,
        flags: Int,
        proc: STREAMPROC?,
        user: Any?
    ): Int

    external fun BASS_StreamCreateFile(file: String?, offset: Long, length: Long, flags: Int): Int
    external fun BASS_StreamCreateFile(
        file: ByteBuffer?,
        offset: Long,
        length: Long,
        flags: Int
    ): Int

    external fun BASS_StreamCreateFile(
        file: ParcelFileDescriptor?,
        offset: Long,
        length: Long,
        flags: Int
    ): Int

    external fun BASS_StreamCreateFile(asset: Asset?, offset: Long, length: Long, flags: Int): Int
    external fun BASS_StreamCreateURL(
        url: String?,
        offset: Int,
        flags: Int,
        proc: DOWNLOADPROC?,
        user: Any?
    ): Int

    external fun BASS_StreamCreateFileUser(
        system: Int,
        flags: Int,
        procs: BASS_FILEPROCS?,
        user: Any?
    ): Int

    external fun BASS_StreamFree(handle: Int): Boolean
    external fun BASS_StreamGetFilePosition(handle: Int, mode: Int): Long
    external fun BASS_StreamPutData(handle: Int, buffer: ByteBuffer?, length: Int): Int
    external fun BASS_StreamPutFileData(handle: Int, buffer: ByteBuffer?, length: Int): Int
    external fun BASS_MusicLoad(
        file: String?,
        offset: Long,
        length: Int,
        flags: Int,
        freq: Int
    ): Int

    external fun BASS_MusicLoad(
        file: ByteBuffer?,
        offset: Long,
        length: Int,
        flags: Int,
        freq: Int
    ): Int

    external fun BASS_MusicLoad(
        asset: Asset?,
        offset: Long,
        length: Int,
        flags: Int,
        freq: Int
    ): Int

    external fun BASS_MusicLoad(
        asset: ParcelFileDescriptor?,
        offset: Long,
        length: Int,
        flags: Int,
        freq: Int
    ): Int

    external fun BASS_MusicFree(handle: Int): Boolean
    external fun BASS_RecordGetDeviceInfo(device: Int, info: BASS_DEVICEINFO?): Boolean
    external fun BASS_RecordInit(device: Int): Boolean
    external fun BASS_RecordFree(): Boolean
    external fun BASS_RecordSetDevice(device: Int): Boolean
    external fun BASS_RecordGetDevice(): Int
    external fun BASS_RecordGetInfo(info: BASS_RECORDINFO?): Boolean
    external fun BASS_RecordGetInputName(input: Int): String?
    external fun BASS_RecordSetInput(input: Int, flags: Int, volume: Float): Boolean
    external fun BASS_RecordGetInput(input: Int, volume: FloatValue?): Int
    external fun BASS_RecordStart(
        freq: Int,
        chans: Int,
        flags: Int,
        proc: RECORDPROC?,
        user: Any?
    ): Int

    external fun BASS_ChannelBytes2Seconds(handle: Int, pos: Long): Double
    external fun BASS_ChannelSeconds2Bytes(handle: Int, pos: Double): Long
    external fun BASS_ChannelGetDevice(handle: Int): Int
    external fun BASS_ChannelSetDevice(handle: Int, device: Int): Boolean
    external fun BASS_ChannelIsActive(handle: Int): Int
    external fun BASS_ChannelGetInfo(handle: Int, info: BASS_CHANNELINFO?): Boolean
    external fun BASS_ChannelGetTags(handle: Int, tags: Int): Any?
    external fun BASS_ChannelFlags(handle: Int, flags: Int, mask: Int): Long
    external fun BASS_ChannelLock(handle: Int, lock: Boolean): Boolean
    external fun BASS_ChannelFree(handle: Int): Boolean
    external fun BASS_ChannelPlay(handle: Int, restart: Boolean): Boolean
    external fun BASS_ChannelStart(handle: Int): Boolean
    external fun BASS_ChannelStop(handle: Int): Boolean
    external fun BASS_ChannelPause(handle: Int): Boolean
    external fun BASS_ChannelUpdate(handle: Int, length: Int): Boolean
    external fun BASS_ChannelSetAttribute(handle: Int, attrib: Int, value: Float): Boolean
    external fun BASS_ChannelGetAttribute(handle: Int, attrib: Int, value: FloatValue?): Boolean
    external fun BASS_ChannelSlideAttribute(
        handle: Int,
        attrib: Int,
        value: Float,
        time: Int
    ): Boolean

    external fun BASS_ChannelIsSliding(handle: Int, attrib: Int): Boolean
    external fun BASS_ChannelSetAttributeEx(
        handle: Int,
        attrib: Int,
        value: ByteBuffer?,
        size: Int
    ): Boolean

    external fun BASS_ChannelSetAttributeDOWNLOADPROC(
        handle: Int,
        proc: DOWNLOADPROC?,
        user: Any?
    ): Boolean

    external fun BASS_ChannelGetAttributeEx(
        handle: Int,
        attrib: Int,
        value: ByteBuffer?,
        size: Int
    ): Int

    external fun BASS_ChannelSet3DAttributes(
        handle: Int,
        mode: Int,
        min: Float,
        max: Float,
        iangle: Int,
        oangle: Int,
        outvol: Float
    ): Boolean

    external fun BASS_ChannelGet3DAttributes(
        handle: Int,
        mode: Int?,
        min: FloatValue?,
        max: FloatValue?,
        iangle: Int?,
        oangle: Int?,
        outvol: FloatValue?
    ): Boolean

    external fun BASS_ChannelSet3DPosition(
        handle: Int,
        pos: BASS_3DVECTOR?,
        orient: BASS_3DVECTOR?,
        vel: BASS_3DVECTOR?
    ): Boolean

    external fun BASS_ChannelGet3DPosition(
        handle: Int,
        pos: BASS_3DVECTOR?,
        orient: BASS_3DVECTOR?,
        vel: BASS_3DVECTOR?
    ): Boolean

    external fun BASS_ChannelGetLength(handle: Int, mode: Int): Long
    external fun BASS_ChannelSetPosition(handle: Int, pos: Long, mode: Int): Boolean
    external fun BASS_ChannelGetPosition(handle: Int, mode: Int): Long
    external fun BASS_ChannelGetLevel(handle: Int): Int
    external fun BASS_ChannelGetLevelEx(
        handle: Int,
        levels: FloatArray?,
        length: Float,
        flags: Int
    ): Boolean

    external fun BASS_ChannelGetData(handle: Int, buffer: ByteBuffer?, length: Int): Int
    external fun BASS_ChannelSetSync(
        handle: Int,
        type: Int,
        param: Long,
        proc: SYNCPROC?,
        user: Any?
    ): Int

    external fun BASS_ChannelRemoveSync(handle: Int, sync: Int): Boolean
    external fun BASS_ChannelSetLink(handle: Int, chan: Int): Boolean
    external fun BASS_ChannelRemoveLink(handle: Int, chan: Int): Boolean
    external fun BASS_ChannelSetDSP(handle: Int, proc: DSPPROC?, user: Any?, priority: Int): Int
    external fun BASS_ChannelRemoveDSP(handle: Int, dsp: Int): Boolean
    external fun BASS_ChannelSetFX(handle: Int, type: Int, priority: Int): Int
    external fun BASS_ChannelRemoveFX(handle: Int, fx: Int): Boolean
    external fun BASS_FXSetParameters(handle: Int, params: Any?): Boolean
    external fun BASS_FXGetParameters(handle: Int, params: Any?): Boolean
    external fun BASS_FXSetPriority(handle: Int, priority: Int): Boolean
    external fun BASS_FXReset(handle: Int): Boolean
    external fun BASS_StreamCreateConst(
        freq: Int,
        chans: Int,
        flags: Int,
        proc: Int,
        user: Any?
    ): Int

    fun BASS_StreamCreate(freq: Int, chans: Int, flags: Int, proc: Int, user: Any?): Int {
        return BASS_StreamCreateConst(freq, chans, flags, proc, user)
    }

    init {
        System.loadLibrary("bass")
    }

    // Device info structure
    class BASS_DEVICEINFO {
        var name: String? = null // description
        var driver: String? = null // driver
        var flags = 0
    }

    class BASS_INFO {
        var flags = 0 // device capabilities (DSCAPS_xxx flags)
        var hwsize = 0 // unused
        var hwfree = 0 // unused
        var freesam = 0 // unused
        var free3d = 0 // unused
        var minrate = 0 // unused
        var maxrate = 0 // unused
        var eax = 0 // unused
        var minbuf = 0 // recommended minimum buffer length in ms
        var dsver = 0 // DirectSound version
        var latency = 0 // average delay (in ms) before start of playback
        var initflags = 0 // BASS_Init "flags" parameter
        var speakers = 0 // number of speakers available
        var freq = 0 // current output rate
    }

    // Recording device info structure
    class BASS_RECORDINFO {
        var flags = 0 // device capabilities (DSCCAPS_xxx flags)
        var formats = 0 // supported standard formats (WAVE_FORMAT_xxx flags)
        var inputs = 0 // number of inputs
        var singlein = false // TRUE = only 1 input can be set at a time
        var freq = 0 // current input rate
    }

    // Sample info structure
    class BASS_SAMPLE {
        var freq = 0 // default playback rate
        var volume = 0f // default volume (0-1)
        var pan = 0f // default pan (-1=left, 0=middle, 1=right)
        var flags = 0 // BASS_SAMPLE_xxx flags
        var length = 0 // length (in bytes)
        var max = 0 // maximum simultaneous playbacks
        var origres = 0 // original resolution bits
        var chans = 0 // number of channels
        var mingap = 0 // minimum gap (ms) between creating channels
        var mode3d = 0 // BASS_3DMODE_xxx mode
        var mindist = 0f // minimum distance
        var maxdist = 0f // maximum distance
        var iangle = 0 // angle of inside projection cone
        var oangle = 0 // angle of outside projection cone
        var outvol = 0f // delta-volume outside the projection cone
        var vam = 0 // unused
        var priority = 0 // unused
    }

    // Channel info structure
    class BASS_CHANNELINFO {
        var freq = 0 // default playback rate
        var chans = 0 // channels
        var flags = 0
        var ctype = 0 // type of channel
        var origres = 0 // original resolution
        var plugin = 0
        var sample = 0
        var filename: String? = null
    }

    class BASS_PLUGINFORM {
        var ctype = 0 // channel type
        var name: String? = null // format description
        var exts: String? = null // file extension filter (*.ext1;*.ext2;etc...)
    }

    class BASS_PLUGININFO {
        var version = 0 // version (same form as BASS_GetVersion)
        var formatc = 0 // number of formats
        lateinit var formats : Array<BASS_PLUGINFORM> // the array of formats

    }

    // 3D vector (for 3D positions/velocities/orientations)
    class BASS_3DVECTOR {
        constructor()
        constructor(_x: Float, _y: Float, _z: Float) {
            x = _x
            y = _y
            z = _z
        }

        var x = 0f // +=right, -=left
        var y = 0f // +=up, -=down
        var z = 0f // +=front, -=behind
    }

    fun interface STREAMPROC {
        fun STREAMPROC(handle: Int, buffer: ByteBuffer?, length: Int, user: Any?) /* User stream callback function.
		handle : The stream that needs writing
		buffer : Buffer to write the samples in
		length : Number of bytes to write
		user   : The 'user' parameter value given when calling BASS_StreamCreate
		RETURN : Number of bytes written. Set the BASS_STREAMPROC_END flag to end
				 the stream. */
    }

    interface BASS_FILEPROCS {
        // User file stream callback functions
        fun FILECLOSEPROC(user: Any?)

        @Throws(IOException::class)
        fun FILELENPROC(user: Any?): Long
        fun FILEREADPROC(buffer: ByteBuffer?, length: Int, user: Any?): Int
        fun FILESEEKPROC(offset: Long, user: Any?): Boolean
    }

    fun interface DOWNLOADPROC {
        fun DOWNLOADPROC(buffer: ByteBuffer?, length: Int, user: Any?) /* Internet stream download callback function.
		buffer : Buffer containing the downloaded data... NULL=end of download
		length : Number of bytes in the buffer
		user   : The 'user' parameter value given when calling BASS_StreamCreateURL */
    }

    fun interface SYNCPROC {
        fun SYNCPROC(handle: Int, channel: Int, data: Int, user: Any?) /* Sync callback function.
		handle : The sync that has occured
		channel: Channel that the sync occured in
		data   : Additional data associated with the sync's occurance
		user   : The 'user' parameter given when calling BASS_ChannelSetSync */
    }

    fun interface DSPPROC {
        fun DSPPROC(handle: Int, channel: Int, buffer: ByteBuffer?, length: Int, user: Any?) /* DSP callback function.
		handle : The DSP handle
		channel: Channel that the DSP is being applied to
		buffer : Buffer to apply the DSP to
		length : Number of bytes in the buffer
		user   : The 'user' parameter given when calling BASS_ChannelSetDSP */
    }

    fun interface RECORDPROC {
        fun RECORDPROC(handle: Int, buffer: ByteBuffer?, length: Int, user: Any?) /* Recording callback function.
		handle : The recording handle
		buffer : Buffer containing the recorded sample data
		length : Number of bytes
		user   : The 'user' parameter value given when calling BASS_RecordStart
		RETURN : true = continue recording, false = stop */
    }

    // ID3v1 tag structure
    class TAG_ID3 {
        var id: String? = null
        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var year: String? = null
        var comment: String? = null
        var genre: Byte = 0
        var track: Byte = 0
    }

    // Binary APE tag structure
    class TAG_APE_BINARY {
        var key: String? = null
        var data: ByteBuffer? = null
        var length = 0
    }

    class BASS_DX8_CHORUS {
        var fWetDryMix = 0f
        var fDepth = 0f
        var fFeedback = 0f
        var fFrequency = 0f
        var lWaveform = 0 // 0=triangle, 1=sine
        var fDelay = 0f
        var lPhase = 0 // BASS_DX8_PHASE_xxx
    }

    class BASS_DX8_DISTORTION {
        var fGain = 0f
        var fEdge = 0f
        var fPostEQCenterFrequency = 0f
        var fPostEQBandwidth = 0f
        var fPreLowpassCutoff = 0f
    }

    class BASS_DX8_ECHO {
        var fWetDryMix = 0f
        var fFeedback = 0f
        var fLeftDelay = 0f
        var fRightDelay = 0f
        var lPanDelay = false
    }

    class BASS_DX8_FLANGER {
        var fWetDryMix = 0f
        var fDepth = 0f
        var fFeedback = 0f
        var fFrequency = 0f
        var lWaveform = 0 // 0=triangle, 1=sine
        var fDelay = 0f
        var lPhase = 0 // BASS_DX8_PHASE_xxx
    }

    class BASS_DX8_PARAMEQ {
        var fCenter = 0f
        var fBandwidth = 0f
        var fGain = 0f
    }

    class BASS_DX8_REVERB {
        var fInGain = 0f
        var fReverbMix = 0f
        var fReverbTime = 0f
        var fHighFreqRTRatio = 0f
    }

    class BASS_FX_VOLUME_PARAM {
        var fTarget = 0f
        var fCurrent = 0f
        var fTime = 0f
        var lCurve = 0
    }

    class Asset {
        constructor()
        constructor(m: AssetManager?, f: String?) {
            manager = m
            file = f
        }

        var manager: AssetManager? = null
        var file: String? = null
    }

    class FloatValue {
        var value = 0f
    }

    object Utils {
        fun LOBYTE(n: Int): Int {
            return n and 0xff
        }

        fun HIBYTE(n: Int): Int {
            return n shr 8 and 0xff
        }

        fun LOWORD(n: Int): Int {
            return n and 0xffff
        }

        fun HIWORD(n: Int): Int {
            return n shr 16 and 0xffff
        }

        fun MAKEWORD(a: Int, b: Int): Int {
            return a and 0xff or (b and 0xff shl 8)
        }

        fun MAKELONG(a: Int, b: Int): Int {
            return a and 0xffff or (b shl 16)
        }
    }
}