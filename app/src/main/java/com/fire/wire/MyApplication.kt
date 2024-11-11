package com.fire.wire

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.fire.wire.utils.Prefs
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject


val prefs: Prefs by lazy {
    MyApplication.prefs!!
}

@HiltAndroidApp
class MyApplication : Application() , Configuration.Provider{

    @Inject lateinit var workerFactory: HiltWorkerFactory

    companion object {
        var prefs: Prefs? = null
        lateinit var instance: MyApplication
            private set
    }


    override fun onCreate() {
        super.onCreate()

        instance = this
        prefs = Prefs(applicationContext)


    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder().setWorkerFactory(workerFactory).build()
    }
}

