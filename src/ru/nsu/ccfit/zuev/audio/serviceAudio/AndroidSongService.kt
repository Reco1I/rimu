package ru.nsu.ccfit.zuev.audio.serviceAudio

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

/**
 * The Android service that keeps the [SongService] playing.
 */
class AndroidSongService : Service() {

    private val songService = SongService().apply {
        onExit = { stopSelf() }
    }

    override fun onBind(intent: Intent?): IBinder {
        songService.onBind()
        return ReturnBindObject()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        songService.onUnbind()
        return super.onUnbind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
        println("onReBind")
    }

    inner class ReturnBindObject : Binder() {
        fun getObject(): SongService {
            return songService
        }
    }
}
