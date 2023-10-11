package game.rimu.management

import android.app.Service
import android.content.Intent
import com.reco1l.basskt.BassDevice

/**
 * The music service used to operate the main BASS stream.
 */
class MusicService : Service()
{

    /**
     * The BASS device.
     */
    lateinit var device: BassDevice


    // Overrides

    override fun onBind(intent: Intent) = Binder()

    override fun onTaskRemoved(rootIntent: Intent) = stopSelf()


    // Lifecycle

    override fun onCreate()
    {
        super.onCreate()
        device = BassDevice()
    }

    override fun onDestroy()
    {
        device.free()
        super.onDestroy()
    }


    // Binder

    /**
     * Binder for [MusicService] instance wrapper.
     */
    inner class Binder : android.os.Binder()
    {
        /**
         * The [MusicService] current instance.
         */
        val service = this@MusicService
    }
}
