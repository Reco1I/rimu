package game.rimu.android

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import game.rimu.management.MusicService

class RimuApplication : Application(), ServiceConnection
{


    override fun attachBaseContext(base: Context) = super.attachBaseContext(RimuContext(base))

    override fun getBaseContext() = super.getBaseContext() as RimuContext


    override fun onCreate()
    {
        super.onCreate()

        // Binding music service as application wide instance, this service should not be
        // disconnected unless the app has been closed.
        bindService(Intent(this, MusicService::class.java), this, BIND_AUTO_CREATE)
    }


    override fun onServiceConnected(name: ComponentName?, binder: IBinder?)
    {
        if (binder is MusicService.Binder)
            baseContext.musicService = binder.service
    }

    override fun onServiceDisconnected(name: ComponentName?) = Unit

}