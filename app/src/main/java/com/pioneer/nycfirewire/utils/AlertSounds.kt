package com.pioneer.nycfirewire.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * GLOBAL ALERT SOUND (design screen 14) — device-local catalog of the alert
 * sounds ported from the iOS bundle (NotificationSoundsViewModel), plus a
 * DEFAULT row that keeps the system notification sound.
 *
 * Android O+ locks a channel's sound at creation time, so each sound gets its
 * own channel id ("fw_alerts_<key>_v1"). Switching sounds creates the new
 * channel and deletes the stale fw_alerts_* ones; incoming OneSignal pushes
 * are redirected onto the current channel by [com.pioneer.nycfirewire.notification.NotificationServiceExtension].
 */
object AlertSounds {

    // NOTE: two raw files in this lineage are spelled with a single "t"
    // (batalion_ticket, engine_ladder_batalion_ticket). The catalog below uses
    // the ACTUAL filenames — getIdentifier() returns 0 for a mismatch, which
    // would silently play nothing.

    const val DEFAULT_KEY = "default"
    private const val CHANNEL_PREFIX = "fw_alerts_"
    // bump if a channel's locked attributes (importance, vibration…) change
    private const val CHANNEL_VERSION = "v1"

    data class Sound(
        val key: String,          // stable id persisted in Prefs / channel id
        val displayName: String,  // uppercase row label, mirrors iOS names
        val rawName: String?      // res/raw resource, null = system default
    )

    /** Row order mirrors design screen 14 (alphabetical, DEFAULT inline). */
    val all = listOf(
        Sound("acting_engine", "ACTING ENGINE", "acting_engine_ticket"),
        Sound("battalion", "BATTALION", "batalion_ticket"),
        Sound(DEFAULT_KEY, "DEFAULT", null),
        Sound("division", "DIVISION", "division_ticket"),
        Sound("engine_ladder_ticket", "ENGINE LADDER TICKET", "engine_ladder_ticket"),
        Sound("engine_ticket", "ENGINE TICKET", "engine_ticket"),
        Sound("engine_ladder_battalion", "ENGINE, LADDER, BATTALION", "engine_ladder_batalion_ticket"),
        Sound("ladder_ticket", "LADDER TICKET", "ladder_ticket"),
        Sound("mdt_ring", "MDT RING", "mdt_ring"),
        Sound("special_unit", "SPECIAL UNIT", "special_unit"),
        Sound("standby_for_message", "STANDBY FOR MESSAGE", "standby_for_message"),
        Sound("tones_only", "TONES ONLY", "tones_only")
    )

    fun byKey(key: String?): Sound =
        all.firstOrNull { it.key == key } ?: all.first { it.key == DEFAULT_KEY }

    fun current(prefs: Prefs): Sound = byKey(prefs.alertSound)

    fun rawResId(context: Context, sound: Sound): Int =
        if (sound.rawName == null) 0
        else context.resources.getIdentifier(sound.rawName, "raw", context.packageName)

    /** null = keep the system default notification sound. */
    fun soundUri(context: Context, sound: Sound): Uri? =
        sound.rawName?.let { Uri.parse("android.resource://" + context.packageName + "/raw/" + it) }

    fun channelIdFor(sound: Sound): String = CHANNEL_PREFIX + sound.key + "_" + CHANNEL_VERSION

    /** Persists the choice and rebuilds the notification channel. */
    fun apply(context: Context, prefs: Prefs, key: String) {
        prefs.alertSound = key
        ensureChannel(context, prefs)
    }

    /**
     * Creates the channel for the currently selected sound (no-op if it already
     * exists) and deletes any stale fw_alerts_* channels, so at most one app
     * alert channel is visible in system settings. Returns the active channel id.
     */
    fun ensureChannel(context: Context, prefs: Prefs): String {
        val sound = current(prefs)
        val channelId = channelIdFor(sound)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return channelId

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(channelId) == null) {
            val channel = NotificationChannel(
                channelId, "Alerts", NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Incident alerts"
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val uri = soundUri(context, sound) ?: Settings.System.DEFAULT_NOTIFICATION_URI
            channel.setSound(uri, attributes)
            channel.enableVibration(true)
            nm.createNotificationChannel(channel)
        }
        // remove superseded sound channels (old sound choices / old versions)
        nm.notificationChannels.forEach {
            if (it.id.startsWith(CHANNEL_PREFIX) && it.id != channelId) {
                nm.deleteNotificationChannel(it.id)
            }
        }
        return channelId
    }
}
