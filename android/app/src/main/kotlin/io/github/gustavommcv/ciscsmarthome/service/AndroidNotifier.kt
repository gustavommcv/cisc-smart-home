package io.github.gustavommcv.ciscsmarthome.service

import android.app.NotificationManager
import android.content.Context
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionState
import io.github.gustavommcv.ciscsmarthome.domain.GasAlert
import io.github.gustavommcv.ciscsmarthome.domain.Home
import io.github.gustavommcv.ciscsmarthome.domain.LeakAlertMode
import io.github.gustavommcv.ciscsmarthome.domain.NameBook
import io.github.gustavommcv.ciscsmarthome.nameRes

/** The [Notifier] of the phone: each thing the service says is a notification, built by [Notices]. */
internal class AndroidNotifier(private val context: Context, private val nameBook: NameBook) : Notifier {
  private val manager = context.getSystemService(NotificationManager::class.java)

  override fun showConnection(connection: ConnectionState) = manager.notify(Notices.MONITORING_ID, Notices.monitoring(context, connection))

  override fun showBoardLost(connection: ConnectionState.Disconnected) = manager.notify(Notices.CONNECTION_LOST_ID, Notices.connectionLost(context, connection))

  override fun clearBoardLost() = manager.cancel(Notices.CONNECTION_LOST_ID)

  // One notification per leak: the boot and the count name it, so a new leak warns again instead of replacing the last.
  override fun showGasLeak(alert: GasAlert, home: Home?, mode: LeakAlertMode) =
    manager.notify(alert.bootId.value, alert.count.toInt(), Notices.gasLeak(context, areaName(alert, home), mode))

  override fun clearGasLeak(alert: GasAlert) = manager.cancel(alert.bootId.value, alert.count.toInt())

  // What the user named the room, which is the name they know it by, and in a leak it is the one that must be said; else the names the board gives,
  // which are in English, and the two rooms of the maquete have theirs in Portuguese. Read when the warning is made, so a name given a moment ago counts.
  private fun areaName(alert: GasAlert, home: Home?): String =
    nameBook.names.value.nameOfArea(
      alert.areaId,
      portuguese = alert.areaId.nameRes()?.let { context.getString(it) },
      board = home?.areas?.firstOrNull { it.id == alert.areaId }?.name ?: alert.areaId.value,
    )
}
