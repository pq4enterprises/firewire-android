package com.pioneer.nycfirewire.receiver

import android.app.Notification.EXTRA_NOTIFICATION_ID
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.pioneer.nycfirewire.prefs
import com.pioneer.nycfirewire.service.BackgroundAudioService
import com.pioneer.nycfirewire.utils.Constants.AUDIO_BROADCAST

class MyAudioBroadCastReceiver: BroadcastReceiver() {

    private var context: Context?=null
    private val pauseAction = "ACTION_PAUSE"

    override fun onReceive(context: Context?, intent: Intent?) {
        this.context= context
        if (intent?.action == pauseAction) {
            // Notification "Stop": clear the persisted playing feed first so a
            // scanner screen that reopens later restores nothing.
            prefs.clearPlayingFeed()

            val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
            if (notificationId != -1) {
                BackgroundAudioService.stopService(context!!)

                // Tell an open FeedsActivity to drop its "playing" row state.
                LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(AUDIO_BROADCAST))
            }
        }
    }



}