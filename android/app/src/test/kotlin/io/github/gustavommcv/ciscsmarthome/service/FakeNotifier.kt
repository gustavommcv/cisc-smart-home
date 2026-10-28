package io.github.gustavommcv.ciscsmarthome.service

import io.github.gustavommcv.ciscsmarthome.domain.ConnectionState
import io.github.gustavommcv.ciscsmarthome.domain.GasAlert
import io.github.gustavommcv.ciscsmarthome.domain.Home
import io.github.gustavommcv.ciscsmarthome.domain.LeakAlertMode

/** What the service would have shown the user, written down in the order it happened. */
internal class FakeNotifier : Notifier {
  val connections = mutableListOf<ConnectionState>()
  val boardLost = mutableListOf<ConnectionState.Disconnected>()
  var boardLostCleared = 0
    private set
  val gasLeaks = mutableListOf<Pair<GasAlert, Home?>>()
  val gasLeakModes = mutableListOf<LeakAlertMode>()
  val gasLeaksCleared = mutableListOf<GasAlert>()

  override fun showConnection(connection: ConnectionState) {
    connections += connection
  }

  override fun showBoardLost(connection: ConnectionState.Disconnected) {
    boardLost += connection
  }

  override fun clearBoardLost() {
    boardLostCleared++
  }

  override fun showGasLeak(alert: GasAlert, home: Home?, mode: LeakAlertMode) {
    gasLeaks += alert to home
    gasLeakModes += mode
  }

  override fun clearGasLeak(alert: GasAlert) {
    gasLeaksCleared += alert
  }
}
