package game.rimu.android

import android.app.Application
import android.content.Context

class RimuApplication : Application()
{

    override fun attachBaseContext(base: Context) = super.attachBaseContext(RimuContext(base))

    override fun getBaseContext() = super.getBaseContext() as RimuContext

}