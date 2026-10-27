package io.github.gustavommcv.ciscsmarthome.testing

import io.github.gustavommcv.ciscsmarthome.domain.CommandResult
import io.github.gustavommcv.ciscsmarthome.domain.CommandValue
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionState
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.GasAlert
import io.github.gustavommcv.ciscsmarthome.domain.Home
import io.github.gustavommcv.ciscsmarthome.domain.HomeRepository
import io.github.gustavommcv.ciscsmarthome.domain.Reading
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.receiveAsFlow

/** A repository the tests of the screens and of the service move by hand. */
class FakeHomeRepository : HomeRepository {
  override val home = MutableStateFlow<Home?>(null)
  override val connection = MutableStateFlow<ConnectionState>(ConnectionState.Connecting)

  private val news = Channel<GasAlert>(Channel.UNLIMITED)
  override val alerts: Flow<GasAlert> = news.receiveAsFlow()

  /** How many times the sync was started, and whether it was stopped by being cancelled. */
  var syncsStarted = 0
    private set
  var syncCancelled = false
    private set

  fun announce(alert: GasAlert) {
    news.trySend(alert)
  }

  override fun series(entityId: EntityId, since: Instant): Flow<List<Reading>> = emptyFlow()

  override suspend fun send(entityId: EntityId, value: CommandValue): CommandResult = CommandResult.Refused

  override suspend fun keepSynced(): Nothing {
    syncsStarted++
    try {
      awaitCancellation()
    } catch (e: CancellationException) {
      syncCancelled = true
      throw e
    }
  }
}
