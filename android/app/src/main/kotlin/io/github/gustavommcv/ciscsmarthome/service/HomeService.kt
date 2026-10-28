package io.github.gustavommcv.ciscsmarthome.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.ServiceCompat
import io.github.gustavommcv.ciscsmarthome.AppDependencies
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
 * It only wires: [ConnectionWatch] and [LeakWarnings] decide what to warn of, and [AndroidNotifier] says it.
 * It runs on the main thread: everything it waits for suspends, and what it does is a notification.
 *
 * The notice that stays has a button, "stop monitoring", which is the user's way to close the app for good: it stops the sync (and with it the link to the board,
 * which gives the phone's Wi-Fi back), takes the notice and the warning of a lost board away, closes the screens, and stops the service. Android does not start it
 * again, so the app is watching again only when the user opens it. A leak that is being warned of is left on, for that is not something to silence by closing the app.
 */
class HomeService : Service() {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var syncing: Job? = null
  private var notifier: AndroidNotifier? = null

  override fun onCreate() {
    super.onCreate()
    Notices.createChannels(this)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == ACTION_STOP) {
      stopMonitoring()
      return START_NOT_STICKY
    }
    val repository = (application as AppDependencies).homeRepository
    // Android wants this soon after the service is started, before anything else is done.
    ServiceCompat.startForeground(
      this,
      Notices.MONITORING_ID,
      Notices.monitoring(this, repository.connection.value),
      ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
    )
    if (syncing == null) {
      val notifier = AndroidNotifier(this, (application as AppDependencies).nameBook).also { notifier = it }
      val connectionWatch = ConnectionWatch(notifier)
      val leakWarnings = LeakWarnings(notifier, (application as AppDependencies).leakAlertSettings)
      syncing =
        scope.launch {
          launch { repository.keepSynced() }
          launch { connectionWatch.watch(repository.connection) }
          launch { repository.alerts.collect { leakWarnings.warn(it, repository.home.value) } }
          launch { repository.home.collect { home -> if (home != null) leakWarnings.silenceEnded(home) } }
        }
    }
    // If Android stops the service for memory, it is started again: nobody is watching the board without it.
    return START_STICKY
  }

  private fun stopMonitoring() {
    scope.cancel()
    notifier?.clearBoardLost()
    (application as AppDependencies).closeApp()
    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    stopSelf()
  }

  override fun onDestroy() {
    scope.cancel()
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  companion object {
    /** What the button of the notice sends: the user wants the monitoring stopped. */
    const val ACTION_STOP = "io.github.gustavommcv.ciscsmarthome.action.STOP_MONITORING"
  }
}
