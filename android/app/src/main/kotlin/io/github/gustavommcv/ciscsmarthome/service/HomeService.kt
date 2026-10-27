package io.github.gustavommcv.ciscsmarthome.service

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.ServiceCompat
import io.github.gustavommcv.ciscsmarthome.AppDependencies
import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.GasAlert
import io.github.gustavommcv.ciscsmarthome.domain.Home
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Keeps the app in touch with the board when the screen is gone, and warns of a gas leak. It is a
 * foreground service of the "connected device" kind, because Android cuts the network of an app in the
 * background, and a leak is exactly what must not be missed with the screen off. The notice that stays
 * says whether the board is being heard.
 *
 * It runs on the main thread: everything it waits for suspends, and what it does is a notification.
 */
class HomeService : Service() {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var syncing: Job? = null

  override fun onCreate() {
    super.onCreate()
    Notices.createChannels(this)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val repository = (application as AppDependencies).homeRepository
    // Android wants this soon after the service is started, before anything else is done.
    ServiceCompat.startForeground(
      this,
      Notices.MONITORING_ID,
      Notices.monitoring(this, repository.connection.value),
      ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
    )
    if (syncing == null) {
      val manager = getSystemService(NotificationManager::class.java)
      syncing =
        scope.launch {
          launch { repository.keepSynced() }
          launch { repository.connection.collect { manager.notify(Notices.MONITORING_ID, Notices.monitoring(this@HomeService, it)) } }
          launch { repository.alerts.collect { warn(manager, it, repository.home.value) } }
        }
    }
    // If Android stops the service for memory, it is started again: nobody is watching the board without it.
    return START_STICKY
  }

  override fun onDestroy() {
    scope.cancel()
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  private fun warn(manager: NotificationManager, alert: GasAlert, home: Home?) {
    // One notification per leak: the boot and the count name it, so a new leak warns again instead of replacing the last.
    manager.notify(alert.bootId.value, alert.count.toInt(), Notices.gasLeak(this, areaName(alert, home)))
  }

  // The names the board gives are in English; the two rooms of the maquete have theirs in Portuguese.
  private fun areaName(alert: GasAlert, home: Home?): String =
    when (alert.areaId.value) {
      "kitchen" -> getString(R.string.area_kitchen)
      "bedroom" -> getString(R.string.area_bedroom)
      else -> home?.areas?.firstOrNull { it.id == alert.areaId }?.name ?: alert.areaId.value
    }
}
