/*===========================================================================
 BASS_FX 2.4 - Copyright (c) 2002-2019 (: JOBnik! :) [Arthur Aminov, ISRAEL]
                                                     [http://www.jobnik.org]

      bugs/suggestions/questions:
        forum  : http://www.un4seen.com/forum/?board=1
                 http://www.jobnik.org/forums
        e-mail : bass_fx@jobnik.org
     --------------------------------------------------

 NOTE: This header will work only with BASS_FX version 2.4.12
       Check www.un4seen.com or www.jobnik.org for any later versions.

 * Requires BASS 2.4 (available at http://www.un4seen.com)
===========================================================================*/

// File converted to Kotlin by Reco1l

@file:Suppress(
    "unused",
    "ClassName",
    "FunctionName",
    "NotConstructor",
    "LocalVariableName",
    "SpellCheckingInspection"
)

package com.un4seen.bass

import kotlin.math.log10
import kotlin.math.pow

object BASS_FX {
    // BASS_CHANNELINFO types
    const val BASS_CTYPE_STREAM_TEMPO = 0x1f200
    const val BASS_CTYPE_STREAM_REVERSE = 0x1f201

    // Tempo / Reverse / BPM / Beat flag
    const val BASS_FX_FREESOURCE = 0x10000 // Free the source handle as well?

    // BASS_FX Version
    external fun BASS_FX_GetVersion(): Int

    /*===========================================================================
		DSP (Digital Signal Processing)
	===========================================================================*/
    /*
		Multi-channel order of each channel is as follows:
		 3 channels       left-front, right-front, center.
		 4 channels       left-front, right-front, left-rear/side, right-rear/side.
		 5 channels       left-front, right-front, center, left-rear/side, right-rear/side.
		 6 channels (5.1) left-front, right-front, center, LFE, left-rear/side, right-rear/side.
		 8 channels (7.1) left-front, right-front, center, LFE, left-rear/side, right-rear/side, left-rear center, right-rear center.
	*/
    // DSP channels flags
    const val BASS_BFX_CHANALL = -1 // all channels at once (as by default)
    const val BASS_BFX_CHANNONE = 0 // disable an effect for all channels
    const val BASS_BFX_CHAN1 = 1 // left-front channel
    const val BASS_BFX_CHAN2 = 2 // right-front channel
    const val BASS_BFX_CHAN3 = 4 // see above info
    const val BASS_BFX_CHAN4 = 8 // see above info
    const val BASS_BFX_CHAN5 = 16 // see above info
    const val BASS_BFX_CHAN6 = 32 // see above info
    const val BASS_BFX_CHAN7 = 64 // see above info
    const val BASS_BFX_CHAN8 = 128 // see above info

    // if you have more than 8 channels (7.1), use this function
    fun BASS_BFX_CHANNEL_N(n: Int): Int {
        return 1 shl n - 1
    }

    // DSP effects
    const val BASS_FX_BFX_ROTATE = 0x10000 // A channels volume ping-pong	/ multi channel
    const val BASS_FX_BFX_ECHO = 0x10001 // Echo							/ 2 channels max	(deprecated)
    const val BASS_FX_BFX_FLANGER = 0x10002 // Flanger						/ multi channel		(deprecated)
    const val BASS_FX_BFX_VOLUME = 0x10003 // Volume						/ multi channel
    const val BASS_FX_BFX_PEAKEQ = 0x10004 // Peaking Equalizer			/ multi channel
    const val BASS_FX_BFX_REVERB = 0x10005 // Reverb						/ 2 channels max	(deprecated)
    const val BASS_FX_BFX_LPF = 0x10006 // Low Pass Filter 24dB			/ multi channel		(deprecated)
    const val BASS_FX_BFX_MIX = 0x10007 // Swap, remap and mix channels	/ multi channel
    const val BASS_FX_BFX_DAMP = 0x10008 // Dynamic Amplification		/ multi channel
    const val BASS_FX_BFX_AUTOWAH = 0x10009 // Auto Wah						/ multi channel
    const val BASS_FX_BFX_ECHO2 = 0x1000a // Echo 2						/ multi channel		(deprecated)
    const val BASS_FX_BFX_PHASER = 0x1000b // Phaser						/ multi channel
    const val BASS_FX_BFX_ECHO3 = 0x1000c // Echo 3						/ multi channel		(deprecated)
    const val BASS_FX_BFX_CHORUS = 0x1000d // Chorus/Flanger				/ multi channel
    const val BASS_FX_BFX_APF = 0x1000e // All Pass Filter				/ multi channel		(deprecated)
    const val BASS_FX_BFX_COMPRESSOR = 0x1000f // Compressor					/ multi channel		(deprecated)
    const val BASS_FX_BFX_DISTORTION = 0x10010 // Distortion					/ multi channel
    const val BASS_FX_BFX_COMPRESSOR2 = 0x10011 // Compressor 2					/ multi channel
    const val BASS_FX_BFX_VOLUME_ENV = 0x10012 // Volume envelope				/ multi channel
    const val BASS_FX_BFX_BQF = 0x10013 // BiQuad filters				/ multi channel
    const val BASS_FX_BFX_ECHO4 = 0x10014 // Echo 4						/ multi channel
    const val BASS_FX_BFX_PITCHSHIFT =
        0x10015 // Pitch shift using FFT		/ multi channel		(not available on mobile)
    const val BASS_FX_BFX_FREEVERB = 0x10016 // Reverb using "Freeverb" algo	/ multi channel

    // BiQuad Filters
    const val BASS_BFX_BQF_LOWPASS = 0
    const val BASS_BFX_BQF_HIGHPASS = 1
    const val BASS_BFX_BQF_BANDPASS = 2 // constant 0 dB peak gain
    const val BASS_BFX_BQF_BANDPASS_Q = 3 // constant skirt gain, peak gain = Q
    const val BASS_BFX_BQF_NOTCH = 4
    const val BASS_BFX_BQF_ALLPASS = 5
    const val BASS_BFX_BQF_PEAKINGEQ = 6
    const val BASS_BFX_BQF_LOWSHELF = 7
    const val BASS_BFX_BQF_HIGHSHELF = 8

    // Freeverb
    const val BASS_BFX_FREEVERB_MODE_FREEZE = 1

    /*===========================================================================
		set dsp fx			- BASS_ChannelSetFX
		remove dsp fx		- BASS_ChannelRemoveFX
		set parameters		- BASS_FXSetParameters
		retrieve parameters - BASS_FXGetParameters
		reset the state		- BASS_FXReset
	===========================================================================*/
    /*===========================================================================
		Tempo, Pitch scaling and Sample rate changers
	===========================================================================*/
    // NOTE: Enable Tempo supported flags in BASS_FX_TempoCreate and the others to source handle.
    // tempo attributes (BASS_ChannelSet/GetAttribute)
    const val BASS_ATTRIB_TEMPO = 0x10000
    const val BASS_ATTRIB_TEMPO_PITCH = 0x10001
    const val BASS_ATTRIB_TEMPO_FREQ = 0x10002

    // tempo attributes options
    const val BASS_ATTRIB_TEMPO_OPTION_USE_AA_FILTER =
        0x10010 // TRUE (default) / FALSE (default for multi-channel on mobile devices for lower CPU usage)
    const val BASS_ATTRIB_TEMPO_OPTION_AA_FILTER_LENGTH = 0x10011 // 32 default (8 .. 128 taps)
    const val BASS_ATTRIB_TEMPO_OPTION_USE_QUICKALGO =
        0x10012 // TRUE (default on mobile devices for lower CPU usage) / FALSE (default)
    const val BASS_ATTRIB_TEMPO_OPTION_SEQUENCE_MS = 0x10013 // 82 default, 0 = automatic
    const val BASS_ATTRIB_TEMPO_OPTION_SEEKWINDOW_MS = 0x10014 // 28 default, 0 = automatic
    const val BASS_ATTRIB_TEMPO_OPTION_OVERLAP_MS = 0x10015 // 8  default
    const val BASS_ATTRIB_TEMPO_OPTION_PREVENT_CLICK = 0x10016 // TRUE / FALSE (default)

    // tempo algorithm flags
    const val BASS_FX_TEMPO_ALGO_LINEAR = 0x200
    const val BASS_FX_TEMPO_ALGO_CUBIC = 0x400 // default
    const val BASS_FX_TEMPO_ALGO_SHANNON = 0x800
    external fun BASS_FX_TempoCreate(chan: Int, flags: Int): Int
    external fun BASS_FX_TempoGetSource(chan: Int): Int
    external fun BASS_FX_TempoGetRateRatio(chan: Int): Float

    /*===========================================================================
		Reverse playback
	===========================================================================*/
    // NOTES: 1. MODs won't load without BASS_MUSIC_PRESCAN flag.
    //		  2. Enable Reverse supported flags in BASS_FX_ReverseCreate and the others to source handle.
    // reverse attribute (BASS_ChannelSet/GetAttribute)
    const val BASS_ATTRIB_REVERSE_DIR = 0x11000

    // playback directions
    const val BASS_FX_RVS_REVERSE = -1
    const val BASS_FX_RVS_FORWARD = 1
    external fun BASS_FX_ReverseCreate(chan: Int, dec_block: Float, flags: Int): Int
    external fun BASS_FX_ReverseGetSource(chan: Int): Int

    /*===========================================================================
		BPM (Beats Per Minute)
	===========================================================================*/
    // bpm flags
    const val BASS_FX_BPM_BKGRND =
        1 // if in use, then you can do other processing while detection's in progress. Available only in Windows platforms (BPM/Beat)
    const val BASS_FX_BPM_MULT2 =
        2 // if in use, then will auto multiply bpm by 2 (if BPM < minBPM*2)

    // translation options (deprecated)
    const val BASS_FX_BPM_TRAN_X2 =
        0 // multiply the original BPM value by 2 (may be called only once & will change the original BPM as well!)
    const val BASS_FX_BPM_TRAN_2FREQ = 1 // BPM value to Frequency
    const val BASS_FX_BPM_TRAN_FREQ2 = 2 // Frequency to BPM value
    const val BASS_FX_BPM_TRAN_2PERCENT = 3 // BPM value to Percents
    const val BASS_FX_BPM_TRAN_PERCENT2 = 4 // Percents to BPM value
    external fun BASS_FX_BPM_DecodeGet(
        chan: Int,
        startSec: Double,
        endSec: Double,
        minMaxBPM: Int,
        flags: Int,
        proc: Any?,
        user: Any?
    ): Float

    external fun BASS_FX_BPM_CallbackSet(
        handle: Int,
        proc: BPMPROC?,
        period: Double,
        minMaxBPM: Int,
        flags: Int,
        user: Any?
    ): Boolean

    external fun BASS_FX_BPM_CallbackReset(handle: Int): Boolean
    external fun BASS_FX_BPM_Translate(
        handle: Int,
        val2tran: Float,
        trans: Int
    ): Float // deprecated

    external fun BASS_FX_BPM_Free(handle: Int): Boolean
    external fun BASS_FX_BPM_BeatCallbackSet(handle: Int, proc: BPMBEATPROC?, user: Any?): Boolean
    external fun BASS_FX_BPM_BeatCallbackReset(handle: Int): Boolean
    external fun BASS_FX_BPM_BeatDecodeGet(
        chan: Int,
        startSec: Double,
        endSec: Double,
        flags: Int,
        proc: BPMBEATPROC?,
        user: Any?
    ): Boolean

    external fun BASS_FX_BPM_BeatSetParameters(
        handle: Int,
        bandwidth: Float,
        centerfreq: Float,
        beat_rtime: Float
    ): Boolean

    external fun BASS_FX_BPM_BeatGetParameters(
        handle: Int,
        bandwidth: Float?,
        centerfreq: Float?,
        beat_rtime: Float?
    ): Boolean

    external fun BASS_FX_BPM_BeatFree(handle: Int): Boolean

    /*===========================================================================
		Macros
	===========================================================================*/
    // translate linear level to logarithmic dB
    fun BASS_BFX_Linear2dB(level: Double) = 20 * log10(level)

    // translate logarithmic dB level to linear
    fun BASS_BFX_dB2Linear(dB: Double) = (10.0).pow(dB / 20)

    init {
        System.loadLibrary("bass_fx")
    }

    /*
	    Deprecated effects in 2.4.10 version:
		------------------------------------
		BASS_FX_BFX_ECHO		-> use BASS_FX_BFX_ECHO4
		BASS_FX_BFX_ECHO2		-> use BASS_FX_BFX_ECHO4
		BASS_FX_BFX_ECHO3		-> use BASS_FX_BFX_ECHO4
		BASS_FX_BFX_REVERB		-> use BASS_FX_BFX_FREEVERB
		BASS_FX_BFX_FLANGER		-> use BASS_FX_BFX_CHORUS
		BASS_FX_BFX_COMPRESSOR	-> use BASS_FX_BFX_COMPRESSOR2
		BASS_FX_BFX_APF			-> use BASS_FX_BFX_BQF with BASS_BFX_BQF_ALLPASS filter
		BASS_FX_BFX_LPF			-> use 2x BASS_FX_BFX_BQF with BASS_BFX_BQF_LOWPASS filter and appropriate fQ values
	*/
    // Rotate
    class BASS_BFX_ROTATE {
        var fRate =
            0f // rotation rate/speed in Hz (A negative rate can be used for reverse direction)
        var lChannel = 0 // BASS_BFX_CHANxxx flag/s (supported only even number of channels)
    }

    // Echo (deprecated)
    class BASS_BFX_ECHO {
        var fLevel = 0f // [0....1....n] linear
        var lDelay = 0 // [1200..30000]
    }

    // Flanger (deprecated)
    class BASS_BFX_FLANGER {
        var fWetDry = 0f // [0....1....n] linear
        var fSpeed = 0f // [0......0.09]
        var lChannel = 0 // BASS_BFX_CHANxxx flag/s
    }

    // Volume
    class BASS_BFX_VOLUME {
        var lChannel = 0 // BASS_BFX_CHANxxx flag/s or 0 for global volume control
        var fVolume = 0f // [0....1....n] linear
    }

    // Peaking Equalizer
    class BASS_BFX_PEAKEQ {
        var lBand = 0 // [0...............n] more bands means more memory & cpu usage
        var fBandwidth =
            0f // [0.1...........<10] in octaves - fQ is not in use (Bandwidth has a priority over fQ)
        var fQ =
            0f // [0...............1] the EE kinda definition (linear) (if Bandwidth is not in use)
        var fCenter = 0f // [1Hz..<info.freq/2] in Hz
        var fGain = 0f // [-15dB...0...+15dB] in dB (can be above/below these limits)
        var lChannel = 0 // BASS_BFX_CHANxxx flag/s
    }

    // Reverb (deprecated)
    class BASS_BFX_REVERB {
        var fLevel = 0f // [0....1....n] linear
        var lDelay = 0 // [1200..10000]
    }

    // Low Pass Filter (deprecated)
    class BASS_BFX_LPF {
        var fResonance = 0f // [0.01...........10]
        var fCutOffFreq = 0f // [1Hz...info.freq/2] cutoff frequency
        var lChannel = 0 // BASS_BFX_CHANxxx flag/s
    }

    // Swap, remap and mix
    class BASS_BFX_MIX {
        lateinit var lChannel: IntArray // an array of channels to mix using BASS_BFX_CHANxxx flag/s (lChannel[0] is left channel...)
    }

    // Dynamic Amplification
    class BASS_BFX_DAMP {
        var fTarget = 0f // target volume level						[0<......1] linear
        var fQuiet = 0f // quiet  volume level						[0.......1] linear
        var fRate = 0f // amp adjustment rate						[0.......1] linear
        var fGain = 0f // amplification level						[0...1...n] linear
        var fDelay = 0f // delay in seconds before increasing level	[0.......n] linear
        var lChannel = 0 // BASS_BFX_CHANxxx flag/s
    }

    // Auto Wah
    class BASS_BFX_AUTOWAH {
        var fDryMix = 0f // dry (unaffected) signal mix				[-2......2]
        var fWetMix = 0f // wet (affected) signal mix				[-2......2]
        var fFeedback = 0f // output signal to feed back into input	[-1......1]
        var fRate = 0f // rate of sweep in cycles per second		[0<....<10]
        var fRange = 0f // sweep range in octaves					[0<....<10]
        var fFreq = 0f // base frequency of sweep Hz				[0<...1000]
        var lChannel = 0 // BASS_BFX_CHANxxx flag/s
    }

    // Echo 2 (deprecated)
    class BASS_BFX_ECHO2 {
        var fDryMix = 0f // dry (unaffected) signal mix				[-2......2]
        var fWetMix = 0f // wet (affected) signal mix				[-2......2]
        var fFeedback = 0f // output signal to feed back into input	[-1......1]
        var fDelay = 0f // delay sec								[0<......n]
        var lChannel = 0 // BASS_BFX_CHANxxx flag/s
    }

    // Phaser
    class BASS_BFX_PHASER {
        var fDryMix = 0f // dry (unaffected) signal mix				[-2......2]
        var fWetMix = 0f // wet (affected) signal mix				[-2......2]
        var fFeedback = 0f // output signal to feed back into input	[-1......1]
        var fRate = 0f // rate of sweep in cycles per second		[0<....<10]
        var fRange = 0f // sweep range in octaves					[0<....<10]
        var fFreq = 0f // base frequency of sweep					[0<...1000]
        var lChannel = 0 // BASS_BFX_CHANxxx flag/s
    }

    // Echo 3 (deprecated)
    class BASS_BFX_ECHO3 {
        var fDryMix = 0f // dry (unaffected) signal mix				[-2......2]
        var fWetMix = 0f // wet (affected) signal mix				[-2......2]
        var fDelay = 0f // delay sec								[0<......n]
        var lChannel = 0 // BASS_BFX_CHANxxx flag/s
    }

    // Chorus/Flanger
    class BASS_BFX_CHORUS {
        var fDryMix = 0f // dry (unaffected) signal mix				[-2......2]
        var fWetMix = 0f // wet (affected) signal mix				[-2......2]
        var fFeedback = 0f // output signal to feed back into input	[-1......1]
        var fMinSweep = 0f // minimal delay ms							[0<...6000]
        var fMaxSweep = 0f // maximum delay ms							[0<...6000]
        var fRate = 0f // rate ms/s								[0<...1000]
        var lChannel = 0 // BASS_BFX_CHANxxx flag/s
    }

    // All Pass Filter (deprecated)
    class BASS_BFX_APF {
        var fGain = 0f // reverberation time						[-1=<..<=1]
        var fDelay = 0f // delay sec								[0<....<=n]
        var lChannel = 0 // BASS_BFX_CHANxxx flag/s
    }

    // Compressor (deprecated)
    class BASS_BFX_COMPRESSOR {
        var fThreshold = 0f // compressor threshold						[0<=...<=1]
        var fAttacktime = 0f // attack time ms							[0<.<=1000]
        var fReleasetime = 0f // release time ms							[0<.<=5000]
        var lChannel = 0 // BASS_BFX_CHANxxx flag/s
    }

    // Distortion
    class BASS_BFX_DISTORTION {
        var fDrive = 0f // distortion drive							[0<=...<=5]
        var fDryMix = 0f // dry (unaffected) signal mix				[-5<=..<=5]
        var fWetMix = 0f // wet (affected) signal mix				[-5<=..<=5]
        var fFeedback = 0f // output signal to feed back into input	[-1<=..<=1]
        var fVolume = 0f // distortion volume						[0=<...<=2]
        var lChannel = 0 // BASS_BFX_CHANxxx flag/s
    }

    // Compressor 2
    class BASS_BFX_COMPRESSOR2 {
        var fGain = 0f // output gain of signal after compression	[-60....60] in dB
        var fThreshold = 0f // point at which compression begins		[-60.....0] in dB
        var fRatio = 0f // compression ratio						[1.......n]
        var fAttack = 0f // attack time in ms						[0.01.1000]
        var fRelease = 0f // release time in ms						[0.01.5000]
        var lChannel = 0 // BASS_BFX_CHANxxx flag/s
    }

    // Volume envelope
    class BASS_BFX_VOLUME_ENV {
        var lChannel = 0 // BASS_BFX_CHANxxx flag/s
        var lNodeCount = 0 // number of nodes
        lateinit var pNodes: Array<BASS_BFX_ENV_NODE> // the nodes
        var bFollow = false // follow source position
    }

    class BASS_BFX_ENV_NODE {
        var pos = 0.0 // node position in seconds (1st envelope node must be at position 0)
        var `val` = 0f // node value
    }

    class BASS_BFX_BQF {
        var lFilter = 0 // BASS_BFX_BQF_xxx filter types
        var fCenter = 0f // [1Hz..<info.freq/2] Cutoff (central) frequency in Hz
        var fGain =
            0f // [-15dB...0...+15dB] Used only for PEAKINGEQ and Shelving filters in dB (can be above/below these limits)
        var fBandwidth =
            0f // [0.1...........<10] Bandwidth in octaves (fQ is not in use (fBandwidth has a priority over fQ))

        // 						(between -3 dB frequencies for BANDPASS and NOTCH or between midpoint
        // 						(fGgain/2) gain frequencies for PEAKINGEQ)
        var fQ =
            0f // [0.1.....1.......n] The EE kinda definition (linear) (if fBandwidth is not in use)
        var fS =
            0f // [0.1.....1.......n] A "shelf slope" parameter (linear) (used only with Shelving filters)

        // 						when fS = 1, the shelf slope is as steep as you can get it and remain monotonically
        // 						increasing or decreasing gain with frequency.
        var lChannel = 0 // BASS_BFX_CHANxxx flag/s
    }

    // Echo 4
    class BASS_BFX_ECHO4 {
        var fDryMix = 0f // dry (unaffected) signal mix				[-2.......2]
        var fWetMix = 0f // wet (affected) signal mix				[-2.......2]
        var fFeedback = 0f // output signal to feed back into input	[-1.......1]
        var fDelay = 0f // delay sec								[0<.......n]
        var bStereo = false // echo adjoining channels to each other	[TRUE/FALSE]
        var lChannel = 0 // BASS_BFX_CHANxxx flag/s
    }

    // Pitch shift (not available on mobile)
    class BASS_BFX_PITCHSHIFT {
        var fPitchShift =
            0f // A factor value which is between 0.5 (one octave down) and 2 (one octave up) (1 won't change the pitch) [1 default]

        // (fSemitones is not in use, fPitchShift has a priority over fSemitones)
        var fSemitones = 0f // Semitones (0 won't change the pitch) [0 default]
        var lFFTsize =
            0 // Defines the FFT frame size used for the processing. Typical values are 1024, 2048 and 4096 [2048 default]

        // It may be any value <= 8192 but it MUST be a power of 2
        var lOsamp =
            0 // Is the STFT oversampling factor which also determines the overlap between adjacent STFT frames [8 default]

        // It should at least be 4 for moderate scaling ratios. A value of 32 is recommended for best quality (better quality = higher CPU usage)
        var lChannel = 0 // BASS_BFX_CHANxxx flag/s
    }

    class BASS_BFX_FREEVERB {
        var fDryMix = 0f // dry (unaffected) signal mix				[0........1], def. 0
        var fWetMix = 0f // wet (affected) signal mix				[0........3], def. 1.0f
        var fRoomSize = 0f // room size								[0........1], def. 0.5f
        var fDamp = 0f // damping									[0........1], def. 0.5f
        var fWidth = 0f // stereo width								[0........1], def. 1
        var lMode = 0 // 0 or BASS_BFX_FREEVERB_MODE_FREEZE, def. 0 (no freeze)
        var lChannel = 0 // BASS_BFX_CHANxxx flag/s
    }

    fun interface BPMPROC {
        fun BPMPROC(chan: Int, bpm: Float, user: Any?)
    }

    fun interface BPMPROGRESSPROC {
        fun BPMPROGRESSPROC(chan: Int, percent: Float, user: Any?)
    }

    // back-compatibility
    fun interface BPMPROCESSPROC {
        fun BPMPROCESSPROC(chan: Int, percent: Float, user: Any?)
    }

    /*===========================================================================
		Beat position trigger
	===========================================================================*/
    fun interface BPMBEATPROC {
        fun BPMBEATPROC(chan: Int, beatpos: Double, user: Any?)
    }
}