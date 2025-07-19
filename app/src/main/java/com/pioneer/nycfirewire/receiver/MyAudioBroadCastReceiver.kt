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
import com.pioneer.nycfirewire.utils.IntentUtils.AUDIO_MAIN_POSITION
import com.pioneer.nycfirewire.utils.IntentUtils.AUDIO_SUB_POSITION
import com.pioneer.nycfirewire.utils.IntentUtils.AUDIO_URL

class MyAudioBroadCastReceiver: BroadcastReceiver() {

    private var context: Context?=null
    private val pauseAction = "ACTION_PAUSE"

    override fun onReceive(context: Context?, intent: Intent?) {
        this.context= context
        if (intent?.action == pauseAction) {
            prefs.feedMainPosition=-1
            prefs.feedSubPosition= -1

            val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
            var playUrl= intent.getStringExtra(AUDIO_URL)
            var playPosition= intent.getIntExtra(AUDIO_MAIN_POSITION,-1)
            var subPosition= intent.getIntExtra(AUDIO_SUB_POSITION,-1)
            if (notificationId != -1) {
               // BackgroundAudioService.startService(context!!,playUrl!!,playPosition!!,pauseAction)
                BackgroundAudioService.stopService(context!!)

                // Send a broadcast
                val intent = Intent(AUDIO_BROADCAST)
                intent.putExtra("mainPosition", playPosition)
                intent.putExtra("position", subPosition)
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
            }
        }
    }



}