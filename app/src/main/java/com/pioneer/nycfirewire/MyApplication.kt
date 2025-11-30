package com.pioneer.nycfirewire

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.pioneer.nycfirewire.utils.Prefs
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import com.onesignal.notifications.INotificationClickEvent
import com.onesignal.notifications.INotificationClickListener
import com.pioneer.nycfirewire.activity.WireDetailActivity
import com.pioneer.nycfirewire.service.BackgroundAudioService
import com.pioneer.nycfirewire.utils.IntentUtils.BUN_WIRE_DETAILS
import com.pioneer.nycfirewire.utils.IntentUtils.BUN_WIRE_LIST_DETAIL
import com.pioneer.nycfirewire.utils.IntentUtils.INCIDENT_ID
import dagger.hilt.android.HiltAndroidApp
import org.json.JSONObject
import javax.inject.Inject



val prefs = MyApplication.prefs


@HiltAndroidApp
class MyApplication : Application() {
    //, Configuration.Provider

    @Inject lateinit var workerFactory: HiltWorkerFactory

    val ONESIGNAL_APP_ID = "8721de76-7494-4ec0-a4c1-a85f0c995cf5"
    private var activityCount = 0

    companion object {
        lateinit var prefs: Prefs
        lateinit var instance: MyApplication
            private set
    }


    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = Prefs(applicationContext)
        FacebookSdk.sdkInitialize(applicationContext)
        AppEventsLogger.activateApp(this)


        OneSignal.Debug.logLevel = LogLevel.VERBOSE

        // OneSignal Initialization
        OneSignal.initWithContext(this, ONESIGNAL_APP_ID)

        OneSignal.Notifications.addClickListener(object : INotificationClickListener {
            override fun onClick(event: INotificationClickEvent) {
                val data: JSONObject? = event.notification.additionalData
                val incidentId = data?.optString("incidentId")

                if (!incidentId.isNullOrEmpty()) {
                     val bundle= Bundle()
               bundle.putString(INCIDENT_ID,incidentId)
                val intent = Intent(instance, WireDetailActivity::class.java)
                intent.putExtra(BUN_WIRE_DETAILS, bundle)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    instance.startActivity(intent)
                }
            }
        })


// Registering the activity lifecycle callback
        registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(p0: Activity, p1: Bundle?) {}
            override fun onActivityStarted(p0: Activity) {
                activityCount++
            }

            override fun onActivityResumed(p0: Activity) {}

            override fun onActivityPaused(p0: Activity) {}

            override fun onActivityStopped(p0: Activity) {
                activityCount--
            }

            override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {}
            override fun onActivityDestroyed(p0: Activity) {
                if (activityCount == 0) {
                    BackgroundAudioService.stopService(this@MyApplication)
                }

            }
        })

    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        when (applicationContext.resources?.configuration?.uiMode?.and(android.content.res.Configuration.UI_MODE_NIGHT_MASK)) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> {
                com.pioneer.nycfirewire.prefs.isDarkMode=true
                AppCompatDelegate
                    .setDefaultNightMode(
                        AppCompatDelegate
                            .MODE_NIGHT_YES);

            }
            android.content.res.Configuration.UI_MODE_NIGHT_NO -> {prefs.isDarkMode=false
                AppCompatDelegate
                    .setDefaultNightMode(
                        AppCompatDelegate
                            .MODE_NIGHT_NO);

            }
            android.content.res.Configuration.UI_MODE_NIGHT_UNDEFINED -> {prefs.isDarkMode=false}
        }
    }



   /* override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder().setWorkerFactory(workerFactory).build()
    }*/





}



