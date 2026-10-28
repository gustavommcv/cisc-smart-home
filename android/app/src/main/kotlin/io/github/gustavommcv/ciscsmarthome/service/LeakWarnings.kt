package io.github.gustavommcv.ciscsmarthome.service

import io.github.gustavommcv.ciscsmarthome.domain.GasAlert
import io.github.gustavommcv.ciscsmarthome.domain.Home
import io.github.gustavommcv.ciscsmarthome.domain.isOverIn

/**
 * Warns of each gas leak, and takes the warning down when the board says the leak is over. It keeps what it
 * warned of, so it must be called from one thread (the service's).
 */
internal class LeakWarnings(private val notifier: Notifier) {
  // The warnings that are up, so the ones the board says are over can be taken down.
  private val warned = mutableListOf<GasAlert>()

  fun warn(alert: GasAlert, home: Home?) {
    warned += alert
    notifier.showGasLeak(alert, home)
  }

  /**
   * Takes down the warnings of the leaks the board says are over. That happens when someone presses the
   * button of the maquete after the gas is gone, which is the only way the board goes back to normal, so
   * the alarm stops without anyone having to touch the phone. A leak is over only when the board says that
   * one is: a late home from before it does not silence it.
   */
  fun silenceEnded(home: Home) {
    val ended = warned.filter { it.isOverIn(home) }
    ended.forEach { notifier.clearGasLeak(it) }
    warned.removeAll(ended)
  }
}
