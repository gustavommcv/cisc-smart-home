package io.github.gustavommcv.ciscsmarthome.domain

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Turns what the board says about gas leaks into news, once for each leak, and remembers what was
 * announced so a restart of the app does not announce it again (see [AlertTracker]).
 *
 * It starts from what [memory] kept, and is told of each leak event and each state of the house, one
 * at a time: it keeps no lock, so it must be called from one place only.
 */
class LeakAnnouncer(private val memory: AlertMemory) {
  private val tracker = AlertTracker(memory.load())
  private var remembered = tracker.announced()
  private val news = Channel<GasAlert>(Channel.UNLIMITED)

  // A leak that nobody collected yet waits here, so it is not lost between the board saying it and the service listening.
  val alerts: Flow<GasAlert> = news.receiveAsFlow()

  /** The board said a leak started. */
  fun onLeakEvent(event: HomeEvent.GasLeak) {
    if (tracker.onAlertEvent(event.bootId, event.count)) news.trySend(GasAlert(event.bootId, event.areaId, event.count))
    remember()
  }

  /** The board sent the state of the house, which tells of a leak that is happening and of those that ended. */
  fun onHome(home: Home) {
    val leaks = home.gasLeakStatus() ?: return
    if (tracker.onHome(home.bootId, leaks.alertCount, leaks.active)) news.trySend(GasAlert(home.bootId, leaks.areaId, leaks.alertCount))
    remember()
  }

  /** Keeps what was announced, after it was handed to whoever shows it, and only when it changed. */
  private fun remember() {
    val now = tracker.announced()
    if (now == remembered) return
    memory.save(now)
    remembered = now
  }
}
