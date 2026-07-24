package com.fire.wire.notification

import android.os.Build
import androidx.core.app.NotificationCompat
import com.fire.wire.utils.AlertSounds
import com.fire.wire.utils.Prefs
import com.onesignal.notifications.INotificationReceivedEvent
import com.onesignal.notifications.INotificationServiceExtension

/**
 * OneSignal notification service extension (registered in AndroidManifest via
 * the "com.onesignal.NotificationServiceExtension" meta-data key).
 *
 * OneSignal 5.x builds each notification on its own fallback channel, whose
 * sound is locked to the system default. This extender runs before the
 * notification is displayed and re-targets it onto the app-managed
 * "fw_alerts_*" channel carrying the user's GLOBAL ALERT SOUND choice
 * (see AlertSounds). Pre-O devices have no channels, so the sound is set
 * directly on the builder instead.
 */
class NotificationServiceExtension : INotificationServiceExtension {

    override fun onNotificationReceived(event: INotificationReceivedEvent) {
        val context = event.context
        // read prefs directly: this may run before/without MyApplication's lazy global
        val prefs = Prefs(context.applicationContext)
        event.notification.setExtender(NotificationCompat.Extender { builder ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setChannelId(AlertSounds.ensureChannel(context, prefs))
            } else {
                AlertSounds.soundUri(context, AlertSounds.current(prefs))?.let {
                    builder.setSound(it)
                }
            }
            builder
        })
    }
}
