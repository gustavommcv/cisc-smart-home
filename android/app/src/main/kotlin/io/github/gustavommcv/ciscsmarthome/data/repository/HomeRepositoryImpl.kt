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
import io.github.gustavommcv.ciscsmarthome.domain.StateReading
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Joins what the app has: the REST client, the events of the board, the history kept on the phone
 * and the rules of the domain, into what the screens and the service read. It decides what to do
 * with each thing the board says, and leaves the doing to [LeakAnnouncer] and [HistorySync].
 *
 * What the events change (the home, what was announced) is touched only by [keepSynced], one event at a
 * time, so none of it needs a lock. The history is kept up to date by a job of its own, which never makes
 * the events wait for the board or for the disk: it reads the clock of the board (which is safe to read
 * while the events write it) and owns the time of the last letting go of old readings, and only one such
 * job runs at a time, for the next one waits for the one before it to end. [send] only changes [home],
 * which is safe to change from anywhere.
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

  override fun states(entityId: EntityId, since: Instant): Flow<List<StateReading>> = store.states(entityId, since)

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

  override suspend fun keepSynced(): Nothing = coroutineScope {
    // The job that keeps the history up to date for the connection that is up, if there is one. It dies with the connection, and with this scope.
    var rounds: Job? = null
    try {
      // The stream connects again by itself when it is lost, so this only has to react to what it says.
      events.updates().collect { update ->
        when (update) {
          BoardStreamUpdate.Connected -> rounds = onConnected(rounds)
          is BoardStreamUpdate.Received -> {
            // Whatever the board sends proves the connection is alive.
            connectionState.value = ConnectionState.Connected
            when (val event = update.event) {
              is HomeEvent.HomeUpdated -> accept(event.home)
              is HomeEvent.GasLeak -> leaks.onLeakEvent(event)
            }
          }
          is BoardStreamUpdate.Lost -> {
            rounds?.cancel()
            connectionState.value = ConnectionState.Disconnected(update.error.toProblem())
          }
        }
      }
      throw IllegalStateException("the event stream ended, and it is meant to go on")
    } finally {
      // Nobody is listening to the board now, so nothing is known about it; the last home stays, as it was.
      connectionState.value = ConnectionState.Connecting
    }
  }

  /**
   * The board accepted the connection: ask for the whole house (this also tells if the board can be used), then start keeping the history up to
   * date. The job that did it for the connection before ([previous]) is ended first; the one that is returned is the new one, or null when the board
   * cannot be used.
   */
  private suspend fun CoroutineScope.onConnected(previous: Job?): Job? =
    when (val result = api.getHome()) {
      is BoardResult.Failure -> {
        previous?.cancel()
        connectionState.value = ConnectionState.Disconnected(result.error.toProblem())
        null
      }
      is BoardResult.Success -> {
        accept(result.value)
        connectionState.value = ConnectionState.Connected
        // The events do not wait for the history: it is fetched by a job of its own, which begins when the one before has ended.
        launch {
          previous?.cancelAndJoin()
          keepHistoryUpToDate()
        }
      }
    }

  /**
   * Rounds, one after another, for as long as the connection is up: each lets go of the old readings (at most once an hour) and fetches what the
   * board has that the phone does not, from where the history of the board that is up ends. The next round begins [HISTORY_SYNC_EVERY] after this one
   * ended, so a slow answer is never asked for twice. The first round is the catch up of the connection.
   */
  private suspend fun keepHistoryUpToDate() {
    while (true) {
      attempt { letGoOfOldReadings() }
      // The board that is up may not be the one the connection began with: a restart that the stream did not announce shows in the home.
      attempt { homeState.value?.bootId?.let { history.catchUp(it) } }
      delay(HISTORY_SYNC_EVERY.toMillis())
    }
  }

  // A round that fails (the board does not answer, the disk is full) ends nothing: the next one tries again. Being cancelled is not a failure.
  private suspend fun attempt(round: suspend () -> Unit) {
    try {
      round()
    } catch (cancelled: CancellationException) {
      throw cancelled
    } catch (failure: Exception) {
      // Nothing to do: the next round asks again, and what was already kept is kept.
    }
  }

  /**
   * The readings are kept for [READINGS_KEPT], and what was read in each hour stays as its least and its greatest. This is done in a round of the
   * history, so also during a connection that goes on for days, and at most once in [LET_GO_EVERY], for a board that keeps dropping would otherwise
   * make the database look through everything each time.
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

    /** How long after a round of the history ended the next one begins, while the board is connected. */
    val HISTORY_SYNC_EVERY: Duration = Duration.ofSeconds(15)
  }
}
