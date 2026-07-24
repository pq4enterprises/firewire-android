package com.fire.wire

import android.app.Application
import android.widget.Toast
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.fire.wire.utils.AlertSounds
import com.fire.wire.utils.Prefs
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.onesignal.notifications.INotificationClickEvent
import com.onesignal.notifications.INotificationClickListener


val prefs: Prefs by lazy {
    MyApplication.prefs!!
}

@HiltAndroidApp
class MyApplication : Application() , Configuration.Provider{

    @Inject lateinit var workerFactory: HiltWorkerFactory

    val ONESIGNAL_APP_ID = "8721de76-7494-4ec0-a4c1-a85f0c995cf5"

    companion object {
        var prefs: Prefs? = null
        lateinit var instance: MyApplication
            private set
    }


    override fun onCreate() {
        super.onCreate()

        instance = this
        prefs = Prefs(applicationContext)

        // make sure the alert-sound notification channel exists before the
        // first push arrives (and migrate stale fw_alerts_* channels)
        AlertSounds.ensureChannel(this, prefs!!)
        FacebookSdk.sdkInitialize(applicationContext)
        AppEventsLogger.activateApp(this)

        OneSignal.Debug.logLevel = LogLevel.VERBOSE

        // OneSignal Initialization
        OneSignal.initWithContext(this, ONESIGNAL_APP_ID)

    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder().setWorkerFactory(workerFactory).build()
    }




}

