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
import io.github.gustavommcv.ciscsmarthome.domain.HourlyReading
import io.github.gustavommcv.ciscsmarthome.domain.Home
import io.github.gustavommcv.ciscsmarthome.domain.Reading
import io.github.gustavommcv.ciscsmarthome.domain.StoredSample
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow

private fun unreachable(): BoardResult.Failure = BoardResult.Failure(BoardError.Unreachable(IOException("the fake board is off")))

/** The REST side of the board, answering what each test says. */
internal class FakeBoardApi : BoardApi {
  var home: () -> BoardResult<Home> = { unreachable() }
  // It may suspend, to be a board that answers slowly.
  var history: suspend (after: Long?, limit: Int?) -> BoardResult<HistoryPage> = { _, _ -> unreachable() }
  var command: (EntityId, CommandValue) -> BoardResult<Entity> = { _, _ -> unreachable() }

  val historyRequests = mutableListOf<Pair<Long?, Int?>>()

  /** How many answers of the history are being waited for now, and the most there ever were at once. */
  var historyInFlight = 0
    private set
  var maxHistoryInFlight = 0
    private set

  /** A test that lets time pass asks for the history many times, and says how many it takes before the fake gives up. */
  var maxHistoryRequests = 50
  val commands = mutableListOf<Pair<EntityId, CommandValue>>()

  override suspend fun getHome(): BoardResult<Home> = home()

  override suspend fun getHistory(after: Long?, limit: Int?): BoardResult<HistoryPage> {
    // A loop that never ends would spin the test forever, and fill the memory, instead of failing it.
    check(historyRequests.size < maxHistoryRequests) { "the history was asked for $maxHistoryRequests times, and it is meant to end" }
    historyRequests += after to limit
    historyInFlight += 1
    maxHistoryInFlight = maxOf(maxHistoryInFlight, historyInFlight)
    try {
      return history(after, limit)
    } finally {
      historyInFlight -= 1
    }
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

  /** What the repository asked for, in order: "save" and "forget". */
  val calls = mutableListOf<String>()

  /** The moments the readings before were let go of, in order. */
  val forgotten = mutableListOf<Instant>()

  /** What the next save, or the next letting go, fails with (once): a disk that is full, a database that is busy. */
  var failNextSave: Throwable? = null
  var failNextForget: Throwable? = null

  /** When set, the letting go waits for it: a database that takes its time. */
  var forgetGate: CompletableDeferred<Unit>? = null

  override suspend fun save(samples: List<StoredSample>) {
    calls += "save"
    failNextSave?.let { failure -> failNextSave = null; throw failure }
    samples.forEach { kept.putIfAbsent(it.bootId to it.uptimeMs, it) }
    changes.value += 1
  }

  override fun firstHour(): Flow<Instant?> =
    changes.map { kept.values.filter { sample -> sample.values.values.any { it != null } }.minOfOrNull { Instant.ofEpochSecond(it.at.epochSecond / 3_600 * 3_600) } }

  override suspend fun forgetReadingsBefore(before: Instant) {
    calls += "forget"
    failNextForget?.let { failure -> failNextForget = null; throw failure }
    forgetGate?.await()
    forgotten += before
  }

  override fun hourly(entityId: EntityId, since: Instant, until: Instant): Flow<List<HourlyReading>> =
    changes.map {
      kept.values
        .mapNotNull { sample -> sample.values[entityId]?.let { sample.at to it } }
        .groupBy { (at, _) -> Instant.ofEpochSecond(at.epochSecond / 3_600 * 3_600) }
        .filterKeys { hour -> hour >= since && hour < until }
        .toSortedMap()
        .map { (hour, readings) -> HourlyReading(hour, readings.minOf { it.second }, readings.maxOf { it.second }) }
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
