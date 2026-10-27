package io.github.gustavommcv.ciscsmarthome.data.repository

import io.github.gustavommcv.ciscsmarthome.data.remote.BoardApi
import io.github.gustavommcv.ciscsmarthome.data.remote.BoardError
import io.github.gustavommcv.ciscsmarthome.data.remote.BoardEvents
import io.github.gustavommcv.ciscsmarthome.data.remote.BoardResult
import io.github.gustavommcv.ciscsmarthome.data.remote.BoardStreamUpdate
import io.github.gustavommcv.ciscsmarthome.domain.BootId
import io.github.gustavommcv.ciscsmarthome.domain.CommandValue
import io.github.gustavommcv.ciscsmarthome.domain.Entity
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.HistoryPage
import io.github.gustavommcv.ciscsmarthome.domain.HistoryStore
import io.github.gustavommcv.ciscsmarthome.domain.Home
import io.github.gustavommcv.ciscsmarthome.domain.Reading
import io.github.gustavommcv.ciscsmarthome.domain.StoredSample
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow

private fun unreachable(): BoardResult.Failure = BoardResult.Failure(BoardError.Unreachable(IOException("the fake board is off")))

/** The REST side of the board, answering what each test says. */
internal class FakeBoardApi : BoardApi {
  var home: () -> BoardResult<Home> = { unreachable() }
  var history: (after: Long?, limit: Int?) -> BoardResult<HistoryPage> = { _, _ -> unreachable() }
  var command: (EntityId, CommandValue) -> BoardResult<Entity> = { _, _ -> unreachable() }

  val historyRequests = mutableListOf<Pair<Long?, Int?>>()
  val commands = mutableListOf<Pair<EntityId, CommandValue>>()

  override suspend fun getHome(): BoardResult<Home> = home()

  override suspend fun getHistory(after: Long?, limit: Int?): BoardResult<HistoryPage> {
    historyRequests += after to limit
    return history(after, limit)
  }

  override suspend fun sendCommand(entityId: EntityId, value: CommandValue): BoardResult<Entity> {
    commands += entityId to value
    return command(entityId, value)
  }
}

/** The event stream, saying what each test sends to [feed]. */
internal class FakeBoardEvents : BoardEvents {
  val feed = Channel<BoardStreamUpdate>(Channel.UNLIMITED)

  override fun updates(): Flow<BoardStreamUpdate> = feed.receiveAsFlow()
}

/** The history in memory, with the same rule as the real one: a sample that is kept is left as it was. */
internal class FakeHistoryStore : HistoryStore {
  private val kept = linkedMapOf<Pair<BootId, Long>, StoredSample>()
  private val changes = MutableStateFlow(0)

  val saved: List<StoredSample> get() = kept.values.toList()

  override suspend fun save(samples: List<StoredSample>) {
    samples.forEach { kept.putIfAbsent(it.bootId to it.uptimeMs, it) }
    changes.value += 1
  }

  override suspend fun latestUptime(bootId: BootId): Long? = kept.keys.filter { it.first == bootId }.maxOfOrNull { it.second }

  override fun series(entityId: EntityId, since: Instant): Flow<List<Reading>> =
    changes.map {
      kept.values.filter { sample -> sample.at >= since && entityId in sample.values }.sortedBy { it.at }.map { Reading(it.at, it.values[entityId]) }
    }
}

/** A clock the test moves by hand. */
internal class TestClock(var now: Instant) : Clock() {
  override fun getZone(): ZoneId = ZoneOffset.UTC

  override fun withZone(zone: ZoneId): Clock = this

  override fun instant(): Instant = now
}
