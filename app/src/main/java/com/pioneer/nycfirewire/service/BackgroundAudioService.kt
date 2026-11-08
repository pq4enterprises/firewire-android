package com.pioneer.nycfirewire.service
import android.app.Notification
import android.app.Notification.EXTRA_NOTIFICATION_ID
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.receiver.MyAudioBroadCastReceiver
import com.pioneer.nycfirewire.utils.Constants.ACTION_STOP
import com.pioneer.nycfirewire.utils.IntentUtils.AUDIO_URL

/**
 * Foreground playback service. Holds exactly one ExoPlayer, identified by the
 * stream URL it was built for. The previous build kept a map of players keyed
 * by the feed's adapter position: a position is not a stable identity (it
 * changes when areas are added/removed or the server reorders), so a re-start
 * for the same stream at a new position leaked the old player and could play
 * two streams at once.
 */
class BackgroundAudioService : Service() {

    private val pauseAction = "ACTION_PAUSE"
    private var url: String?=null
    private var player: ExoPlayer? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "player_channel",
                "Audio Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Audio playback notifications"
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }



    private fun buildNotification(): Notification {

        val playIntent = Intent(this, MyAudioBroadCastReceiver::class.java).apply {
            action = pauseAction
            putExtra(EXTRA_NOTIFICATION_ID, 0)
            putExtra(AUDIO_URL,url)
        }

        // Create a PendingIntent for the snooze action
        val playPausePendingIntent: PendingIntent =
            PendingIntent.getBroadcast(this, 0, playIntent, PendingIntent.FLAG_IMMUTABLE)


        val notificationBuilder = NotificationCompat.Builder(this, "player_channel")
            .setContentTitle("FireWire")
            .setContentText("Playing audio")
            .setSmallIcon(R.drawable.ic_play) // Add a valid icon
            .addAction(R.drawable.ic_pause, "Stop", playPausePendingIntent) // Change to play/pause icon
            //.addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
            .setOngoing(true)

        return notificationBuilder.build()
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            // Service restarted by system without intent
            return START_STICKY // Or START_NOT_STICKY if you want it to stop
        }
        val requestedUrl = intent.getStringExtra("url") ?: return START_NOT_STICKY
        var actionOfNotification= intent.getStringExtra("action").toString()

        if(actionOfNotification!=pauseAction) {
            // Same stream already loaded (adapter re-bind after a list refresh):
            // keep playing. Different stream: swap the player. This is keyed by
            // URL, never by list position.
            if (player == null || requestedUrl != url) {
                releasePlayer()
                url = requestedUrl
                player = ExoPlayer.Builder(this).build().apply {
                    setMediaItem(MediaItem.fromUri(requestedUrl))
                    prepare()
                    playWhenReady = true
                }
            }

            startForeground(1, buildNotification())
        }


        return when (actionOfNotification) {
            pauseAction -> {
                //togglePlayback()
                stopPlayback()
                START_NOT_STICKY
            }
            ACTION_STOP -> {
                stopPlayback()  // Handle stop action
                START_NOT_STICKY // Stop service after action is completed
            }
            else -> {
                // If no action, return START_STICKY to keep service running in the background
                START_STICKY
            }
        }

        println("MusicService:"+"onStartCommand called with action: ${intent.action}")


    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    private fun releasePlayer() {
        player?.release()
        player = null
        url = null
    }


    companion object {
        fun startService(context: Context, url: String, pauseAction: String) {
            val intent = Intent(context, BackgroundAudioService::class.java).apply {
                putExtra("url", url)
                putExtra("action", pauseAction)
            }
            context.startService(intent)
        }

        fun stopService(context: Context) {
            context.stopService(Intent(context, BackgroundAudioService::class.java))
        }
    }

     fun togglePlayback() {
        if (player?.isPlaying == true) {
            player?.pause()
        } else {
            player?.play()
        }
        // Update the notification after toggling the playback
        updateNotification()
    }

    private fun stopPlayback() {
        player?.stop()
        stopForeground(true) // Stop the service and remove the notification
        stopSelf()
    }
    private fun updateNotification() {

        val notification = buildNotification()
        startForeground(1, notification)
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }




}



