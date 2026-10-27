package io.github.gustavommcv.ciscsmarthome.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionState
import io.github.gustavommcv.ciscsmarthome.messageRes

/** The channels and the notifications of the service: one that stays while it works, and the one that warns of a leak. */
internal object Notices {
  const val MONITORING_CHANNEL = "monitoring"
  const val GAS_ALERT_CHANNEL = "gas_alert"
  const val MONITORING_ID = 1

  /** Creates the channels, which Android does once: what the user later changes in them is theirs. */
  fun createChannels(context: Context) {
    val manager = context.getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(NotificationChannel(MONITORING_CHANNEL, context.getString(R.string.channel_monitoring), NotificationManager.IMPORTANCE_LOW))
    val alarm = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
    val gasAlert =
      NotificationChannel(GAS_ALERT_CHANNEL, context.getString(R.string.channel_gas_alert), NotificationManager.IMPORTANCE_HIGH).apply {
        // A leak is a warning to wake someone up, so it uses the alarm sound and the alarm volume.
        setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), alarm)
        enableVibration(true)
        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
      }
    manager.createNotificationChannel(gasAlert)
  }

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

  // The service may not import the screen, so the app is opened by what Android says opens it.
  private fun openApp(context: Context): PendingIntent {
    val launch = requireNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName)) { "the app has no screen to open" }
    return PendingIntent.getActivity(context, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
  }
}
