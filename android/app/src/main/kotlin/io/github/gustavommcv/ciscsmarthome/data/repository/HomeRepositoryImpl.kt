package io.github.gustavommcv.ciscsmarthome.data.repository

import io.github.gustavommcv.ciscsmarthome.data.remote.BoardApi
import io.github.gustavommcv.ciscsmarthome.data.remote.BoardError
import io.github.gustavommcv.ciscsmarthome.data.remote.BoardEvents
import io.github.gustavommcv.ciscsmarthome.data.remote.BoardResult
import io.github.gustavommcv.ciscsmarthome.data.remote.BoardStreamUpdate
import io.github.gustavommcv.ciscsmarthome.data.remote.ErrorCode
import io.github.gustavommcv.ciscsmarthome.domain.AlertMemory
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
import io.github.gustavommcv.ciscsmarthome.domain.HourlyReading
import io.github.gustavommcv.ciscsmarthome.domain.LeakAnnouncer
import io.github.gustavommcv.ciscsmarthome.domain.Reading
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Joins what the app has: the REST client, the events of the board, the history kept on the phone
 * and the rules of the domain, into what the screens and the service read. It decides what to do
 * with each thing the board says, and leaves the doing to [LeakAnnouncer] and [HistorySync].
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
  alertMemory: AlertMemory,
  private val deviceClock: DeviceClock = DeviceClock(),
) : HomeRepository {
  private val homeState = MutableStateFlow<Home?>(null)
  private val connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Connecting)
  private val leaks = LeakAnnouncer(alertMemory)
  private val history = HistorySync(api, store, deviceClock)
  private var readingsLetGoOfAt: Instant? = null

  override val home: StateFlow<Home?> = homeState.asStateFlow()
  override val connection: StateFlow<ConnectionState> = connectionState.asStateFlow()
  override val alerts: Flow<GasAlert> = leaks.alerts

  override fun series(entityId: EntityId, since: Instant): Flow<List<Reading>> = store.series(entityId, since)

  override fun hourly(entityId: EntityId, since: Instant, until: Instant): Flow<List<HourlyReading>> = store.hourly(entityId, since, until)

  override fun firstHour(): Flow<Instant?> = store.firstHour()

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
    try {
      // The stream connects again by itself when it is lost, so this only has to react to what it says.
      events.updates().collect { update ->
        when (update) {
          BoardStreamUpdate.Connected -> onConnected()
          is BoardStreamUpdate.Received -> {
            // Whatever the board sends proves the connection is alive.
            connectionState.value = ConnectionState.Connected
            when (val event = update.event) {
              is HomeEvent.HomeUpdated -> accept(event.home)
              is HomeEvent.GasLeak -> leaks.onLeakEvent(event)
            }
          }
          is BoardStreamUpdate.Lost -> connectionState.value = ConnectionState.Disconnected(update.error.toProblem())
        }
      }
      throw IllegalStateException("the event stream ended, and it is meant to go on")
    } finally {
      // Nobody is listening to the board now, so nothing is known about it; the last home stays, as it was.
      connectionState.value = ConnectionState.Connecting
    }
  }

  /** The board accepted the connection: ask for the whole house (this also tells if the board can be used), then catch up on the history. */
  private suspend fun onConnected() {
    when (val result = api.getHome()) {
      is BoardResult.Failure -> connectionState.value = ConnectionState.Disconnected(result.error.toProblem())
      is BoardResult.Success -> {
        accept(result.value)
        connectionState.value = ConnectionState.Connected
        letGoOfOldReadings()
        history.catchUp(result.value.bootId)
      }
    }
  }

  /**
   * The readings are kept for [READINGS_KEPT], and what was read in each hour stays as its least and its greatest. This is done when the board
   * connects, and at most once in [LET_GO_EVERY], for a board that keeps dropping would otherwise make the database look through everything each time.
   */
  private suspend fun letGoOfOldReadings() {
    val now = clock.instant()
    val last = readingsLetGoOfAt
    if (last != null && Duration.between(last, now) < LET_GO_EVERY) return
    store.forgetReadingsBefore(now.minus(READINGS_KEPT))
    readingsLetGoOfAt = now
  }

  private fun accept(incoming: Home) {
    val current = homeState.value
    // The REST answer and the stream can cross: the older one must not take the place of the newer.
    if (current != null && current.bootId == incoming.bootId && incoming.uptimeMs < current.uptimeMs) return
    deviceClock.observe(incoming.bootId, incoming.uptimeMs, clock.instant())
    homeState.value = incoming
    leaks.onHome(incoming)
  }

  private fun BoardError.toProblem(): ConnectionProblem =
    when (this) {
      is BoardError.Unreachable -> ConnectionProblem.Unreachable
      is BoardError.Rejected -> if (code == ErrorCode.TooManyClients) ConnectionProblem.TooManyClients else ConnectionProblem.Incompatible
      is BoardError.Unexpected -> ConnectionProblem.Incompatible
      is BoardError.NoRoute -> problem
    }

  internal companion object {
    /** How long the readings are kept. A day of them is what the charts of hours show; a week leaves room to go back a few days. After that, only the hours stay. */
    val READINGS_KEPT: Duration = Duration.ofDays(7)

    /** How often at most the old readings are let go of. */
    val LET_GO_EVERY: Duration = Duration.ofHours(1)
  }
}
