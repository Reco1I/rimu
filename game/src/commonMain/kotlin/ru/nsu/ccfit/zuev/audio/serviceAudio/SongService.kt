package ru.nsu.ccfit.zuev.audio.serviceAudio

import com.osudroid.OsuPlatform
import com.osudroid.utils.SystemFileSystem
import com.reco1l.verktex.Logger

import kotlin.jvm.JvmStatic
import okio.Path.Companion.toPath

import com.osudroid.audio.SongPlayer
import com.reco1l.verktex.Platform

import ru.nsu.ccfit.zuev.audio.Status


/**
 * Plays the songs. On Android it lives in a bound service, which calls [onBind], [onUnbind] and
 * sets [onExit].
 */
open class SongService {
    private var audioFunc: SongPlayer? = null
    open var isGaming = false
        set(isGaming) {
            audioFunc!!.setGaming(isGaming)
            Logger.w("Gaming Mode", "In Gamming mode :$isGaming")
            field = isGaming
        }
    // private boolean isSettingMenu = false;

    /**
     * Called when the platform stops the service, such as Android's `Service.stopSelf`.
     */
    var onExit: (() -> Unit)? = null

    fun onBind() {
        if (audioFunc == null) {
            audioFunc = SongPlayer()
        }
    }

    fun onUnbind() {
        println("Service unbind")
        exit()
    }

    fun preLoad(filePath: String?): Boolean {
        return preLoad(filePath, 1f, false, false)
    }

    fun preLoad(filePath: String?, speed: Float, adjustPitch: Boolean): Boolean {
        return preLoad(filePath, speed, adjustPitch, false)
    }

    open fun preLoad(filePath: String?, speed: Float, adjustPitch: Boolean, isLoop: Boolean): Boolean {
        if (checkFileExist(filePath)) {
            if (audioFunc == null) {
                return false
            }

            return audioFunc!!.preLoad(filePath!!, speed, adjustPitch, isLoop)
        }
        return false
    }

    open fun play() {
        if (audioFunc == null) return
        audioFunc!!.play()
    }

    open fun pause() {
        if (audioFunc == null) return
        audioFunc!!.pause()
    }

    open fun stop(): Boolean {
        if (audioFunc == null) return false
        return audioFunc!!.stop()
    }

    open fun exit(): Boolean {
        Logger.w("SongService", "Hei Service is on EXIT()")
        if (audioFunc == null) return false
        audioFunc!!.stop()
        Platform.audioDriver.dispose()
        onExit?.invoke()
        return true
    }

    open fun seekTo(time: Int) {
        if (audioFunc == null) return
        Logger.i("SongService", "Seeking to " + time + " ms: " + (if (audioFunc!!.jump(time)) "Success" else "Failed"))
    }

    /*
    public boolean isSettingMenu() {
        return isSettingMenu;
    }

    public void setIsSettingMenu(boolean isSettingMenu) {
        this.isSettingMenu = isSettingMenu;
    } */

    open val status: Status
        get() {
            if (audioFunc != null) {
                return audioFunc!!.getStatus()
            }
            return Status.STOPPED
        }

    val position: Int
        get() = positionPrecise.toInt()

    open val positionPrecise: Double
        get() {
            if (audioFunc != null) {
                return audioFunc!!.getPosition()
            }
            return 0.0
        }

    open val length: Int
        get() {
            if (audioFunc != null) {
                return audioFunc!!.getLength()
            }
            return 0
        }

    open val spectrum: FloatArray?
        get() {
            if (audioFunc != null) {
                return audioFunc!!.getSpectrum()
            }
            return FloatArray(0)
        }

    open fun getAudioFFT(resolution: Int): FloatArray? {
        if (audioFunc != null) {
            return audioFunc!!.getAudioFFT(resolution)
        }
        return null
    }

    open var volume: Float
        get() {
            if (audioFunc != null) {
                return audioFunc!!.getVolume()
            }
            return 0f
        }
        set(volume) {
            if (audioFunc != null) {
                audioFunc!!.setVolume(volume)
            }
        }

    open var speed: Float
        get() {
            if (audioFunc != null) {
                return audioFunc!!.getSpeed()
            }
            return 0f
        }
        set(speed) {
            if (audioFunc != null) {
                audioFunc!!.setSpeed(speed)
            }
        }

    open fun setAdjustPitch(adjustPitch: Boolean) {
        if (audioFunc != null) {
            audioFunc!!.setAdjustPitch(adjustPitch)
        }
    }

    open fun setPitchRate(pitchRate: Float) {
        if (audioFunc != null) {
            audioFunc!!.setPitchRate(pitchRate)
        }
    }

    open fun setFrequencyForcefully(frequency: Float) {
        if (audioFunc != null) {
            audioFunc!!.setFrequencyForcefully(frequency)
        }
    }

    open val frequency: Float
        get() {
            if (audioFunc != null) {
                return audioFunc!!.getFrequency()
            }
            return 0f
        }

    open fun checkFileExist(path: String?): Boolean {
        if (path == null) return false
        if (path.trim().isEmpty()) return false
        else {
            val songFile = path.toPath()
            if (!SystemFileSystem.exists(songFile)) return false
        }
        return true
    }

    open val isRunningForeground: Boolean
        get() = OsuPlatform.implementation.isInForeground

    companion object {
        const val defaultFrequency = 44100

        /**
         * Starts the audio driver of the platform.
         */
        @JvmStatic
        fun initAudio() {
            Platform.audioDriver.enumerateDevices()
        }
    }

}
