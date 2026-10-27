package io.github.gustavommcv.ciscsmarthome.data.repository

import io.github.gustavommcv.ciscsmarthome.data.remote.BoardApi
import io.github.gustavommcv.ciscsmarthome.data.remote.BoardError
import io.github.gustavommcv.ciscsmarthome.data.remote.BoardEvents
import io.github.gustavommcv.ciscsmarthome.data.remote.BoardResult
import io.github.gustavommcv.ciscsmarthome.data.remote.BoardStreamUpdate
import io.github.gustavommcv.ciscsmarthome.data.remote.ErrorCode
import io.github.gustavommcv.ciscsmarthome.domain.AlertTracker
import io.github.gustavommcv.ciscsmarthome.domain.BootId
import io.github.gustavommcv.ciscsmarthome.domain.CommandResult
import io.github.gustavommcv.ciscsmarthome.domain.CommandValue
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionProblem
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionState
import io.github.gustavommcv.ciscsmarthome.domain.DeviceClock
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.GasAlert
import io.github.gustavommcv.ciscsmarthome.domain.HistoryStore
import io.github.gustavommcv.ciscsmarthome.domain.Home
import io.github.gustavommcv.ciscsmarthome.domain.HomeEvent
import io.github.gustavommcv.ciscsmarthome.domain.HomeRepository
import io.github.gustavommcv.ciscsmarthome.domain.Reading
import io.github.gustavommcv.ciscsmarthome.domain.StoredSample
import io.github.gustavommcv.ciscsmarthome.domain.gasLeakStatus
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

/**
 * Joins what the app has: the REST client, the events of the board, the history kept on the phone
 * and the rules of the domain, into what the screens and the service read.
 *
 * Everything that changes the state of the sync (the clock of the board, what was announced) is
 * touched only by [keepSynced], one event at a time, so none of it needs a lock. [send] only changes
 * [home], which is safe to change from anywhere.
 */
class HomeRepositoryImpl(
  private val api: BoardApi,
  private val events: BoardEvents,
  private val store: HistoryStore,
  private val clock: Clock,
  private val alertTracker: AlertTracker,
  private val deviceClock: DeviceClock = DeviceClock(),
) : HomeRepository {
  private val homeState = MutableStateFlow<Home?>(null)
  private val connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Connecting)
  private val news = Channel<GasAlert>(Channel.UNLIMITED)

  override val home: StateFlow<Home?> = homeState.asStateFlow()
  override val connection: StateFlow<ConnectionState> = connectionState.asStateFlow()

  // A leak that nobody collected yet waits here, so it is not lost between the board saying it and the service listening.
  override val alerts: Flow<GasAlert> = news.receiveAsFlow()

  override fun series(entityId: EntityId, since: Instant): Flow<List<Reading>> = store.series(entityId, since)

  override suspend fun send(entityId: EntityId, value: CommandValue): CommandResult =
    when (val result = api.sendCommand(entityId, value)) {
      is BoardResult.Success -> {
        homeState.update { it?.withEntity(result.value) }
        CommandResult.Applied(result.value)
      }
      is BoardResult.Failure ->
        when (val error = result.error) {
          is BoardError.Rejected -> CommandResult.Refused
          else -> CommandResult.Failed(error.toProblem())
        }
    }

  override suspend fun keepSynced(): Nothing {
    // The stream connects again by itself when it is lost, so this only has to react to what it says.
    events.updates().collect { update ->
      when (update) {
        BoardStreamUpdate.Connected -> onConnected()
        is BoardStreamUpdate.Received -> {
          // Whatever the board sends proves the connection is alive.
          connectionState.value = ConnectionState.Connected
          when (val event = update.event) {
            is HomeEvent.HomeUpdated -> accept(event.home)
            is HomeEvent.GasLeak -> if (alertTracker.onAlertEvent(event.bootId, event.count)) news.trySend(GasAlert(event.bootId, event.areaId, event.count))
          }
        }
        is BoardStreamUpdate.Lost -> connectionState.value = ConnectionState.Disconnected(update.error.toProblem())
      }
    }
    throw IllegalStateException("the event stream ended, and it is meant to go on")
  }

  /** The board accepted the connection: ask for the whole house (this also tells if the board can be used), then catch up on the history. */
  private suspend fun onConnected() {
    when (val result = api.getHome()) {
      is BoardResult.Failure -> connectionState.value = ConnectionState.Disconnected(result.error.toProblem())
      is BoardResult.Success -> {
        accept(result.value)
        connectionState.value = ConnectionState.Connected
        syncHistory(result.value.bootId)
      }
    }
  }

  private fun accept(incoming: Home) {
    val current = homeState.value
    // The REST answer and the stream can cross: the older one must not take the place of the newer.
    if (current != null && current.bootId == incoming.bootId && incoming.uptimeMs < current.uptimeMs) return
    deviceClock.observe(incoming.bootId, incoming.uptimeMs, clock.instant())
    homeState.value = incoming
    val leaks = incoming.gasLeakStatus() ?: return
    if (alertTracker.onHome(incoming.bootId, leaks.alertCount, leaks.active)) news.trySend(GasAlert(incoming.bootId, leaks.areaId, leaks.alertCount))
  }

  /** Fetches what the board has that the phone does not, a page at a time, from where the phone's history of this boot ends. */
  private suspend fun syncHistory(bootId: BootId) {
    var after = store.latestUptime(bootId)
    while (true) {
      val page = (api.getHistory(after, HISTORY_PAGE_SIZE) as? BoardResult.Success)?.value ?: return
      // The board restarted in between: the connection drops with it, and the next one catches up on the new boot.
      if (page.bootId != bootId || page.samples.isEmpty()) return
      store.save(page.samples.mapNotNull { sample -> deviceClock.toInstant(bootId, sample.uptimeMs)?.let { StoredSample(bootId, sample.uptimeMs, it, sample.values) } })
      val last = page.samples.last().uptimeMs
      // A short page is the end. So is a page that does not go past where we are, which a board that works never sends.
      if (page.samples.size < HISTORY_PAGE_SIZE || last <= (after ?: Long.MIN_VALUE)) return
      after = last
    }
  }

  private fun BoardError.toProblem(): ConnectionProblem =
    when (this) {
      is BoardError.Unreachable -> ConnectionProblem.Unreachable
      is BoardError.Rejected -> if (code == ErrorCode.TooManyClients) ConnectionProblem.TooManyClients else ConnectionProblem.Incompatible
      is BoardError.Unexpected -> ConnectionProblem.Incompatible
    }

  private companion object {
    // The most the contract lets one request ask for, and what the board keeps: a board that has all of it answers in one page.
    const val HISTORY_PAGE_SIZE = 360
  }
}
