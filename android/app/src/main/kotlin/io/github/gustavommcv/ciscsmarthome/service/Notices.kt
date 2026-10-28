package io.github.gustavommcv.ciscsmarthome.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionState
import io.github.gustavommcv.ciscsmarthome.messageRes

/** The channels and the notifications of the service: one that stays while it works, and the one that warns of a leak. */
internal object Notices {
  const val MONITORING_CHANNEL = "monitoring"
  // A channel keeps the sound it was made with, whatever the app asks later, so a new sound is a new channel; the first one used the alarm ringtone of the phone.
  const val GAS_ALERT_CHANNEL = "gas_alarm"
  private const val OLD_GAS_ALERT_CHANNEL = "gas_alert"
  const val CONNECTION_LOST_CHANNEL = "connection_lost"
  const val MONITORING_ID = 1
  const val CONNECTION_LOST_ID = 2

  /** Creates the channels, which Android does once: what the user later changes in them is theirs. */
  fun createChannels(context: Context) {
    val manager = context.getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(NotificationChannel(MONITORING_CHANNEL, context.getString(R.string.channel_monitoring), NotificationManager.IMPORTANCE_LOW))
    val alarm = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
    val gasAlert =
      NotificationChannel(GAS_ALERT_CHANNEL, context.getString(R.string.channel_gas_alert), NotificationManager.IMPORTANCE_HIGH).apply {
        // A leak is a warning to wake someone up, so it uses the alarm volume, and a sound of its own: the alarm ringtone of the phone is the sound of the alarm
        // clock, and a leak must not be taken for it. Three strong pulses, like the three beeps.
        setSound("android.resource://${context.packageName}/${R.raw.gas_alarm}".toUri(), alarm)
        // Giving a pattern turns the vibration on.
        vibrationPattern = GAS_VIBRATION
        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
      }
    manager.deleteNotificationChannel(OLD_GAS_ALERT_CHANNEL)
    manager.createNotificationChannel(gasAlert)
    // Loud, so it is noticed, but not the alarm: a house nobody watches is serious, and it is not gas.
    manager.createNotificationChannel(NotificationChannel(CONNECTION_LOST_CHANNEL, context.getString(R.string.channel_connection_lost), NotificationManager.IMPORTANCE_HIGH))
  }

  private val GAS_VIBRATION = longArrayOf(0, 400, 150, 400, 150, 400, 600)

  /** The notice that stays while the service works: it says whether the board is being heard. */
  fun monitoring(context: Context, connection: ConnectionState): Notification =
    NotificationCompat.Builder(context, MONITORING_CHANNEL)
      .setSmallIcon(R.drawable.ic_notification)
      .setContentTitle(context.getString(R.string.app_name))
      .setContentText(context.getString(connection.messageRes()))
      .setCategory(NotificationCompat.CATEGORY_SERVICE)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setContentIntent(openApp(context))
      .addAction(0, context.getString(R.string.notice_stop_monitoring), stopMonitoring(context))
      .build()

  /** The warning of a leak in [areaName]. */
  fun gasLeak(context: Context, areaName: String): Notification =
    NotificationCompat.Builder(context, GAS_ALERT_CHANNEL)
      .setSmallIcon(R.drawable.ic_notification)
      .setContentTitle(context.getString(R.string.alert_gas_title))
      .setContentText(context.getString(R.string.alert_gas_text, areaName))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_ALARM)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setAutoCancel(true)
      .setContentIntent(openApp(context))
      .build()
      // The sound goes on until the warning is opened or cancelled, which the board's own rearming does: a leak
      // is not a thing to miss because the sound played once while someone slept. The builder has no setter for it.
      .apply { flags = flags or Notification.FLAG_INSISTENT }

  /** The warning that the board has been out of reach for a while, so nothing is watching the house. */
  fun connectionLost(context: Context, connection: ConnectionState.Disconnected): Notification =
    NotificationCompat.Builder(context, CONNECTION_LOST_CHANNEL)
      .setSmallIcon(R.drawable.ic_notification)
      .setContentTitle(context.getString(R.string.alert_lost_title))
      .setContentText(context.getString(connection.messageRes()))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_ERROR)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setAutoCancel(true)
      .setContentIntent(openApp(context))
      .build()

  // The button asks the service itself, which the app owns, so no other app can send it.
  private fun stopMonitoring(context: Context): PendingIntent =
    PendingIntent.getService(context, 1, Intent(context, HomeService::class.java).setAction(HomeService.ACTION_STOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

  // The service may not import the screen, so the app is opened by what Android says opens it.
  private fun openApp(context: Context): PendingIntent {
    val launch = requireNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName)) { "the app has no screen to open" }
    return PendingIntent.getActivity(context, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
  }
}
