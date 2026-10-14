package org.app.maxplayerpro

import android.app.Application
import com.psbapp.di.initKoin
import org.app.kmediaplayer.android.KMediaPlayerAndroid
import org.app.maxplayerpro.di.AppModules
import org.app.maxplayerpro.player.PlayerActivity
import org.app.maxplayerpro.player.di.configureMediaPlayer
import org.app.maxplayerpro.sharedlogic.platform.ActivityTracker
import org.koin.mp.KoinPlatform

class MaxPlayerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ActivityTracker.install(this)
        KMediaPlayerAndroid.init(this, videoActivity = PlayerActivity::class.java, musicActivity = MainActivity::class.java)
        initKoin(this, AppModules)
        KoinPlatform.getKoin().configureMediaPlayer()
    }
}
