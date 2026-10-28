package io.github.gustavommcv.ciscsmarthome.service

import android.app.NotificationManager
import android.content.Context
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionState
import io.github.gustavommcv.ciscsmarthome.domain.GasAlert
import io.github.gustavommcv.ciscsmarthome.domain.Home
import io.github.gustavommcv.ciscsmarthome.nameRes

/** The [Notifier] of the phone: each thing the service says is a notification, built by [Notices]. */
internal class AndroidNotifier(private val context: Context) : Notifier {
  private val manager = context.getSystemService(NotificationManager::class.java)

  override fun showConnection(connection: ConnectionState) = manager.notify(Notices.MONITORING_ID, Notices.monitoring(context, connection))

  override fun showBoardLost(connection: ConnectionState.Disconnected) = manager.notify(Notices.CONNECTION_LOST_ID, Notices.connectionLost(context, connection))

  override fun clearBoardLost() = manager.cancel(Notices.CONNECTION_LOST_ID)

  // One notification per leak: the boot and the count name it, so a new leak warns again instead of replacing the last.
  override fun showGasLeak(alert: GasAlert, home: Home?) = manager.notify(alert.bootId.value, alert.count.toInt(), Notices.gasLeak(context, areaName(alert, home)))

  override fun clearGasLeak(alert: GasAlert) = manager.cancel(alert.bootId.value, alert.count.toInt())

  // The names the board gives are in English; the two rooms of the maquete have theirs in Portuguese.
  private fun areaName(alert: GasAlert, home: Home?): String =
    alert.areaId.nameRes()?.let { context.getString(it) } ?: home?.areas?.firstOrNull { it.id == alert.areaId }?.name ?: alert.areaId.value
}
