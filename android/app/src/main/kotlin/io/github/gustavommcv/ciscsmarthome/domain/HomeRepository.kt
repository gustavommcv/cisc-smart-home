package io.github.gustavommcv.ciscsmarthome.domain

import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Where the screens and the service get the house from, however it reaches the phone. */
interface HomeRepository {
  /** The house as it was last heard from the board, or null before the first time. It is kept when the connection is lost. */
  val home: StateFlow<Home?>

  val connection: StateFlow<ConnectionState>

  /** Gas leaks that are news, each once. They wait for whoever collects them, so none is lost in between. */
  val alerts: Flow<GasAlert>

  /** The readings of [entityId] taken at [since] or later, oldest first, sent again when they change. */
  fun series(entityId: EntityId, since: Instant): Flow<List<Reading>>

  /** The least and the greatest reading of each hour of [entityId] that starts at [since] or later and before [until], oldest first, sent again when they change. */
  fun hourly(entityId: EntityId, since: Instant, until: Instant): Flow<List<HourlyReading>>

  /** Asks an entity to become [value]. */
  suspend fun send(entityId: EntityId, value: CommandValue): CommandResult

  /** Keeps the repository in step with the board, reconnecting as needed, until the coroutine is cancelled. */
  suspend fun keepSynced(): Nothing
}

sealed interface ConnectionState {
  /** Nothing is known yet: the first attempt is under way. */
  data object Connecting : ConnectionState

  data object Connected : ConnectionState

  /** The board cannot be used now, and the repository is trying again. */
  data class Disconnected(val problem: ConnectionProblem) : ConnectionState
}

enum class ConnectionProblem {
  /** The board did not answer: the phone is on another network, the board is off, or it is too slow. */
  Unreachable,

  /** The board answered that it has as many connections as it takes. */
  TooManyClients,

  /** The board answered with something the app cannot use, such as another version of the API. */
  Incompatible,

  /** The app is to join the Wi-Fi of the board and does not have its password yet. */
  NetworkNotSet,

  /** The phone could not join the Wi-Fi of the board, or lost it: the board is off or far, the password is wrong, or the user refused. */
  NetworkNotJoined,
}

sealed interface CommandResult {
  /** The board did it, and this is the entity as it ended up. */
  data class Applied(val entity: Entity) : CommandResult

  /** The board understood and said no: the entity does not accept this. */
  data object Refused : CommandResult

  /** The command could not be sent or its answer could not be used. */
  data class Failed(val problem: ConnectionProblem) : CommandResult
}

/** A gas leak that is news: the boot of the board and the number of the leak name it (see [AlertTracker]). */
data class GasAlert(val bootId: BootId, val areaId: AreaId, val count: Long)
