package com.fire.wire.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.fire.wire.R
import com.fire.wire.activity.FeedsActivity

/**
 * Foreground service that owns the scanner-stream MediaPlayer so live audio
 * keeps playing when the app is backgrounded, with a notification the user
 * can stop playback from.
 *
 * Uses the platform MediaPlayer only — this project's AGP/Gradle (7.0.2) is
 * frozen, so no new dependencies (i.e. no ExoPlayer/media3).
 *
 * Feeds are live Icecast-style streams (verified: audio/mpeg over HTTP), so
 * there is no seeking/duration — transport is play/stop.
 *
 * State flow:
 *   IDLE -> BUFFERING (setDataSource + prepareAsync)
 *        -> PLAYING   (onPrepared + audio focus granted + start)
 *   any  -> IDLE      (stop action, error, or permanent audio-focus loss)
 *
 * The current state is mirrored in the companion object so FeedsActivity can
 * re-sync its instrument-panel console after recreation, and a [Listener]
 * (registered by FeedsActivity while visible) receives live updates on the
 * main thread.
 */
class ScannerPlaybackService : Service() {

    enum class State { IDLE, BUFFERING, PLAYING }

    interface Listener {
        /** Called on the main thread whenever playback state changes. */
        fun onPlaybackStateChanged(state: State)

        /** Called on the main thread when a stream fails (unreachable/unplayable). */
        fun onPlaybackError(feedName: String?)
    }

    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false
    private var resumeOnFocusGain = false

    private val playbackAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Permanent loss (another app took over playback): stop fully.
                stopEverything()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                val mp = mediaPlayer ?: return@OnAudioFocusChangeListener
                try {
                    if (mp.isPlaying) {
                        mp.pause()
                        resumeOnFocusGain = true
                    }
                } catch (ignored: IllegalStateException) {
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                try {
                    mediaPlayer?.setVolume(DUCK_VOLUME, DUCK_VOLUME)
                } catch (ignored: IllegalStateException) {
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                try {
                    mediaPlayer?.setVolume(1f, 1f)
                    if (resumeOnFocusGain) {
                        mediaPlayer?.start()
                        setState(State.PLAYING)
                    }
                } catch (ignored: IllegalStateException) {
                }
                resumeOnFocusGain = false
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val name = intent.getStringExtra(EXTRA_FEED_NAME) ?: ""
                val url = intent.getStringExtra(EXTRA_FEED_URL) ?: ""
                val region = intent.getStringExtra(EXTRA_REGION) ?: ""
                // We may have been started with startForegroundService(): promote
                // immediately, before any async work.
                goForeground(name)
                if (url.isEmpty()) {
                    stopEverything()
                } else {
                    startPlayback(name, url, region)
                }
            }
            ACTION_STOP -> stopEverything()
            else -> if (state == State.IDLE) stopEverything()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        releasePlayer()
        abandonFocus()
        setCurrent(null, null, null)
        setState(State.IDLE)
        super.onDestroy()
    }

    // ---------------------------------------------------------------- playback

    private fun startPlayback(name: String, url: String, region: String) {
        releasePlayer() // switching feeds: stop the old stream first
        setCurrent(name, url, region)
        setState(State.BUFFERING)
        updateNotification(name)

        val player = MediaPlayer()
        mediaPlayer = player
        try {
            player.setAudioAttributes(playbackAttributes)
            // Keep the CPU alive while streaming in the background.
            player.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            player.setDataSource(url)
            player.setOnPreparedListener { mp -> onPrepared(mp) }
            player.setOnErrorListener { mp, _, _ ->
                if (mp == mediaPlayer) onStreamFailed()
                true
            }
            player.setOnInfoListener { mp, what, _ ->
                if (mp == mediaPlayer) {
                    when (what) {
                        MediaPlayer.MEDIA_INFO_BUFFERING_START -> setState(State.BUFFERING)
                        MediaPlayer.MEDIA_INFO_BUFFERING_END -> setState(State.PLAYING)
                    }
                }
                false
            }
            player.prepareAsync() // async: don't block the UI thread while connecting
        } catch (e: Exception) {
            // Bad URL / unreachable host surfaced synchronously.
            onStreamFailed()
        }
    }

    private fun onPrepared(player: MediaPlayer) {
        if (player != mediaPlayer) return // stale callback from a replaced player
        if (requestFocus()) {
            try {
                player.start()
                setState(State.PLAYING)
                updateNotification(currentFeedName ?: "")
            } catch (e: IllegalStateException) {
                onStreamFailed()
            }
        } else {
            onStreamFailed()
        }
    }

    /** Stream unreachable or unplayable: alert the UI and reset to STANDBY. */
    private fun onStreamFailed() {
        val failedFeed = currentFeedName
        releasePlayer()
        abandonFocus()
        setCurrent(null, null, null)
        setState(State.IDLE)
        listener?.onPlaybackError(failedFeed)
        stopForegroundCompat()
        stopSelf()
    }

    private fun stopEverything() {
        releasePlayer()
        abandonFocus()
        setCurrent(null, null, null)
        setState(State.IDLE)
        stopForegroundCompat()
        stopSelf()
    }

    private fun releasePlayer() {
        resumeOnFocusGain = false
        mediaPlayer?.let { mp ->
            try {
                if (mp.isPlaying) mp.stop()
            } catch (ignored: IllegalStateException) {
            }
            mp.reset()
            mp.release()
        }
        mediaPlayer = null
    }

    // ------------------------------------------------------------- audio focus

    private fun requestFocus(): Boolean {
        val am = audioManager ?: return false
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build()
            focusRequest = request
            am.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return hasAudioFocus
    }

    private fun abandonFocus() {
        val am = audioManager ?: return
        if (!hasAudioFocus) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { am.abandonAudioFocusRequest(it) }
            focusRequest = null
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(focusChangeListener)
        }
        hasAudioFocus = false
    }

    // ------------------------------------------------------------ notification

    private fun goForeground(feedName: String) {
        val notification = buildNotification(feedName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(feedName: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(feedName))
    }

    private fun buildNotification(feedName: String): android.app.Notification {
        ensureChannel()

        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        // Tapping the notification reopens the scanner screen.
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, FeedsActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            pendingFlags
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, ScannerPlaybackService::class.java).setAction(ACTION_STOP),
            pendingFlags
        )

        val statusText = if (state == State.BUFFERING) {
            getString(R.string.scanner_buffering)
        } else {
            getString(R.string.scanner_notification_listening)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            // small icons render as alpha masks: the play-triangle silhouette reads cleanly
            .setSmallIcon(R.drawable.fw_ic_play)
            .setContentTitle(if (feedName.isNotEmpty()) feedName else getString(R.string.scanner_title))
            .setContentText(statusText)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                R.drawable.fw_ic_pause,
                getString(R.string.scanner_notification_stop),
                stopIntent
            )
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.scanner_playback_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            channel.setShowBadge(false)
            nm.createNotificationChannel(channel)
        }
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    // -------------------------------------------------------------- companion

    companion object {
        const val ACTION_PLAY = "com.fire.wire.action.SCANNER_PLAY"
        const val ACTION_STOP = "com.fire.wire.action.SCANNER_STOP"
        private const val EXTRA_FEED_NAME = "extra_feed_name"
        private const val EXTRA_FEED_URL = "extra_feed_url"
        private const val EXTRA_REGION = "extra_region"

        private const val CHANNEL_ID = "scanner_playback"
        private const val NOTIFICATION_ID = 4114
        private const val DUCK_VOLUME = 0.2f

        /** Mirrored playback state so the UI can re-sync after recreation. */
        @Volatile
        var state: State = State.IDLE
            private set

        @Volatile
        var currentFeedName: String? = null
            private set

        @Volatile
        var currentFeedUrl: String? = null
            private set

        @Volatile
        var currentRegion: String? = null
            private set

        /** Set by FeedsActivity while visible; callbacks arrive on the main thread. */
        var listener: Listener? = null

        fun play(context: Context, feedName: String, feedUrl: String, region: String) {
            val intent = Intent(context, ScannerPlaybackService::class.java)
                .setAction(ACTION_PLAY)
                .putExtra(EXTRA_FEED_NAME, feedName)
                .putExtra(EXTRA_FEED_URL, feedUrl)
                .putExtra(EXTRA_REGION, region)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, ScannerPlaybackService::class.java).setAction(ACTION_STOP)
            )
        }

        /** True when [url] is the feed currently buffering or playing. */
        fun isActiveFeed(url: String?): Boolean =
            state != State.IDLE && url != null && url == currentFeedUrl
    }

    private fun setState(newState: State) {
        Companion.state = newState
        listener?.onPlaybackStateChanged(newState)
    }

    private fun setCurrent(name: String?, url: String?, region: String?) {
        currentFeedName = name
        currentFeedUrl = url
        currentRegion = region
    }
}
