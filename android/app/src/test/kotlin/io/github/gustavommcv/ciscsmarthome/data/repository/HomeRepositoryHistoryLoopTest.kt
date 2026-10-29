package io.github.gustavommcv.ciscsmarthome.data.repository

import io.github.gustavommcv.ciscsmarthome.data.remote.BoardError
import io.github.gustavommcv.ciscsmarthome.data.remote.BoardResult
import io.github.gustavommcv.ciscsmarthome.data.remote.BoardStreamUpdate
import io.github.gustavommcv.ciscsmarthome.domain.BootId
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionState
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.GasAlert
import io.github.gustavommcv.ciscsmarthome.domain.HistoryPage
import io.github.gustavommcv.ciscsmarthome.domain.HistorySample
import io.github.gustavommcv.ciscsmarthome.domain.Home
import io.github.gustavommcv.ciscsmarthome.domain.HomeEvent
import io.github.gustavommcv.ciscsmarthome.testing.FakeAlertMemory
import io.github.gustavommcv.ciscsmarthome.testing.houseOfTheMaquete
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The history while the board stays connected: the phone asks for what is new every few seconds, and not only when it connects. The board keeps
 * an hour of samples and the phone must not let any of them be overwritten before it has them. The time is virtual and the board is a fake that
 * takes a sample every ten seconds.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeRepositoryHistoryLoopTest {
  private val api = FakeBoardApi()
  private val events = FakeBoardEvents()
  private val store = FakeHistoryStore()
  private val memory = FakeAlertMemory()
  private val start = Instant.parse("2026-10-27T12:00:00Z")

  /** The virtual time at which each answer of the history was asked for. */
  private val askedAt = mutableListOf<Long>()

  /** The clock of the phone, which goes on with the virtual time of the test. */
  private class VirtualClock(private val scheduler: TestCoroutineScheduler, private val start: Instant) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = this

    override fun instant(): Instant = start.plusMillis(scheduler.currentTime)
  }

  // ---- helpers ------------------------------------------------------------------

  private fun TestScope.repository() = HomeRepositoryImpl(api, events, store, VirtualClock(testScheduler, start), memory)

  /** Starts the repository the way the service will: in the background, for as long as the test lasts. */
  private fun TestScope.begin(repository: HomeRepositoryImpl): Job {
    val job = backgroundScope.launch { repository.keepSynced() }
    runCurrent()
    return job
  }

  private fun TestScope.feed(vararg updates: BoardStreamUpdate) {
    updates.forEach { events.feed.trySend(it) }
    runCurrent()
  }

  private fun TestScope.later(ms: Long) {
    advanceTimeBy(ms)
    runCurrent()
  }

  private fun house(boot: BootId, uptimeMs: Long, gasState: String = "normal", gasAlerts: Long = 0): Home = houseOfTheMaquete(boot, uptimeMs, gasState, gasAlerts, withBedroom = true)

  private fun received(home: Home) = BoardStreamUpdate.Received(HomeEvent.HomeUpdated(home))

  private fun lost() = BoardStreamUpdate.Lost(BoardError.Unreachable(IOException("the fake board is off")))

  private fun failure() = BoardResult.Failure(BoardError.Unreachable(IOException("the fake board is off")))

  private fun page(boot: BootId, uptimes: List<Long>) = BoardResult.Success(HistoryPage(boot, uptimes.map { HistorySample(it, mapOf(GAS to it / 1000.0)) }))

  /** What the board has of [boot] at this moment: a sample every 10 s since it started, the newest [uptimeAt] ms old. */
  private fun TestScope.samplesAfter(after: Long?, limit: Int?, uptimeAt: () -> Long): List<Long> =
    (1..(uptimeAt() / SAMPLE_MS)).map { it * SAMPLE_MS }.filter { after == null || it > after }.take(limit ?: 360)

  /** A board that has been up for [uptimeAtStart] ms when the test starts and keeps sampling as the virtual time goes. */
  private fun TestScope.boardOfBoot(boot: BootId, uptimeAtStart: Long = 100_000) {
    val uptime = { uptimeAtStart + testScheduler.currentTime }
    api.home = { BoardResult.Success(house(boot, uptime())) }
    api.history = { after, limit ->
      askedAt += testScheduler.currentTime
      page(boot, samplesAfter(after, limit, uptime))
    }
  }

  private fun uptimesKept(boot: BootId = A) = store.saved.filter { it.bootId == boot }.map { it.uptimeMs }

  private fun upTo(uptimeMs: Long) = (1..(uptimeMs / SAMPLE_MS)).map { it * SAMPLE_MS }

  // ---- while it stays connected ------------------------------------------------------

  @Test
  fun `the history keeps growing while the board stays connected, with no reconnection`() = runTest {
    boardOfBoot(A)
    begin(repository())
    feed(BoardStreamUpdate.Connected)
    assertEquals(upTo(100_000), uptimesKept())
    later(HISTORY_SYNC_EVERY_MS)
    assertEquals(upTo(110_000), uptimesKept())
    later(HISTORY_SYNC_EVERY_MS)
    assertEquals(upTo(130_000), uptimesKept())
  }

  @Test
  fun `each round asks only for what is newer than the last sample it kept`() = runTest {
    boardOfBoot(A)
    begin(repository())
    feed(BoardStreamUpdate.Connected)
    later(2 * HISTORY_SYNC_EVERY_MS)
    assertEquals(listOf<Pair<Long?, Int?>>(null to 360, 100_000L to 360, 110_000L to 360), api.historyRequests)
  }

  @Test
  fun `the rounds are fifteen seconds apart`() = runTest {
    boardOfBoot(A)
    begin(repository())
    feed(BoardStreamUpdate.Connected)
    later(3 * HISTORY_SYNC_EVERY_MS)
    assertEquals(listOf(0L, 15_000L, 30_000L, 45_000L), askedAt)
    assertEquals(Duration.ofSeconds(15), HomeRepositoryImpl.HISTORY_SYNC_EVERY)
  }

  @Test
  fun `a state of the house is not a sample of the history`() = runTest {
    boardOfBoot(A)
    val repository = repository()
    begin(repository)
    feed(BoardStreamUpdate.Connected)
    val kept = store.saved.size
    feed(received(house(A, 105_000)), received(house(A, 106_000)), received(house(A, 107_000)))
    assertEquals(kept, store.saved.size)
    assertEquals(house(A, 107_000), repository.home.value)
  }

  // ---- one at a time -------------------------------------------------------------------

  @Test
  fun `the next round comes after the last one ended, so slow answers never pile up`() = runTest {
    val uptime = { 100_000 + testScheduler.currentTime }
    api.home = { BoardResult.Success(house(A, uptime())) }
    api.history = { after, limit ->
      askedAt += testScheduler.currentTime
      delay(20_000)
      page(A, samplesAfter(after, limit, uptime))
    }
    begin(repository())
    feed(BoardStreamUpdate.Connected)
    later(100_000)
    assertEquals(listOf(0L, 35_000L, 70_000L), askedAt)
    assertEquals(1, api.maxHistoryInFlight)
  }

  @Test
  fun `a new connection does not leave the rounds of the old one running`() = runTest {
    boardOfBoot(A)
    begin(repository())
    feed(BoardStreamUpdate.Connected)
    later(20_000)
    feed(lost())
    feed(BoardStreamUpdate.Connected)
    later(40_000)
    assertEquals(listOf(0L, 15_000L, 20_000L, 35_000L, 50_000L), askedAt)
    assertEquals(1, api.maxHistoryInFlight)
  }

  @Test
  fun `a connection that comes again while a round is slow takes the place of that round`() = runTest {
    val gate = CompletableDeferred<Unit>()
    val uptime = { 100_000 + testScheduler.currentTime }
    api.home = { BoardResult.Success(house(A, uptime())) }
    api.history = { after, limit ->
      askedAt += testScheduler.currentTime
      if (askedAt.size == 1) gate.await()
      page(A, samplesAfter(after, limit, uptime))
    }
    begin(repository())
    feed(BoardStreamUpdate.Connected)
    assertEquals(1, api.historyInFlight)
    // The new connection ends the round that waits, and its own round, which has no one to wait for now, is done at once.
    feed(BoardStreamUpdate.Connected)
    assertEquals(listOf(0L, 0L), askedAt)
    assertEquals(0, api.historyInFlight)
    assertEquals(1, api.maxHistoryInFlight)
    assertEquals(upTo(100_000), uptimesKept())
    later(HISTORY_SYNC_EVERY_MS)
    assertEquals("one chain of rounds, not two", listOf(0L, 0L, 15_000L), askedAt)
  }

  // ---- when the connection goes, and when the monitoring ends -------------------------------

  @Test
  fun `a lost connection stops the rounds until the board is back, and the rounds begin with a catch up`() = runTest {
    boardOfBoot(A)
    begin(repository())
    feed(BoardStreamUpdate.Connected)
    later(15_000)
    feed(lost())
    val before = askedAt.size
    later(60_000)
    assertEquals("asked while there was no connection", before, askedAt.size)
    feed(BoardStreamUpdate.Connected)
    assertEquals(before + 1, askedAt.size)
    assertEquals(upTo(175_000), uptimesKept())
  }

  @Test
  fun `a board that answers the connection but not the home has no rounds`() = runTest {
    boardOfBoot(A)
    begin(repository())
    feed(BoardStreamUpdate.Connected)
    later(15_000)
    api.home = { failure() }
    feed(BoardStreamUpdate.Connected)
    val before = askedAt.size
    later(60_000)
    assertEquals(before, askedAt.size)
  }

  @Test
  fun `ending the monitoring stops the rounds`() = runTest {
    boardOfBoot(A)
    val repository = repository()
    val job = begin(repository)
    feed(BoardStreamUpdate.Connected)
    later(15_000)
    job.cancel()
    runCurrent()
    val before = askedAt.size
    later(60_000)
    assertEquals(before, askedAt.size)
    assertEquals(ConnectionState.Connecting, repository.connection.value)
  }

  @Test
  fun `ending the monitoring while a round waits for the board does not leave it waiting`() = runTest {
    val gate = CompletableDeferred<Unit>()
    api.home = { BoardResult.Success(house(A, 100_000)) }
    api.history = { _, _ ->
      gate.await()
      page(A, listOf(10_000))
    }
    val job = begin(repository())
    feed(BoardStreamUpdate.Connected)
    assertEquals(1, api.historyInFlight)
    job.cancel()
    runCurrent()
    assertEquals(0, api.historyInFlight)
    assertTrue(store.saved.isEmpty())
  }

  @Test
  fun `ending the monitoring while the old readings are being let go of does not go on to the history`() = runTest {
    store.forgetGate = CompletableDeferred()
    boardOfBoot(A)
    val job = begin(repository())
    feed(BoardStreamUpdate.Connected)
    assertTrue("the round did not wait for the letting go", store.calls == listOf("forget"))
    job.cancel()
    runCurrent()
    assertEquals(emptyList<Pair<Long?, Int?>>(), api.historyRequests)
    assertEquals(emptyList<Long>(), uptimesKept())
  }

  // ---- when something goes wrong ----------------------------------------------------------------

  @Test
  fun `a history that cannot be fetched is tried again at the next round, and the monitoring goes on`() = runTest {
    val uptime = { 100_000 + testScheduler.currentTime }
    var failing = 2
    api.home = { BoardResult.Success(house(A, uptime())) }
    api.history = { after, limit ->
      askedAt += testScheduler.currentTime
      if (failing-- > 0) failure() else page(A, samplesAfter(after, limit, uptime))
    }
    val repository = repository()
    begin(repository)
    feed(BoardStreamUpdate.Connected)
    assertEquals(emptyList<Long>(), uptimesKept())
    later(2 * HISTORY_SYNC_EVERY_MS)
    assertEquals(listOf(0L, 15_000L, 30_000L), askedAt)
    assertEquals(upTo(130_000), uptimesKept())
    assertEquals(ConnectionState.Connected, repository.connection.value)
  }

  @Test
  fun `a failure to save does not bring the monitoring down, and the next round saves`() = runTest {
    boardOfBoot(A)
    store.failNextSave = IOException("the disk is full")
    val repository = repository()
    val job = begin(repository)
    feed(BoardStreamUpdate.Connected)
    assertTrue("the monitoring ended with the failure", job.isActive)
    assertEquals(emptyList<Long>(), uptimesKept())
    later(HISTORY_SYNC_EVERY_MS)
    assertTrue(job.isActive)
    assertEquals(upTo(110_000), uptimesKept())
    feed(received(house(A, 120_000)))
    assertEquals(house(A, 120_000), repository.home.value)
  }

  @Test
  fun `a failure to let go of the old readings does not stop the history, and is tried again`() = runTest {
    boardOfBoot(A)
    store.failNextForget = IOException("the database is busy")
    begin(repository())
    feed(BoardStreamUpdate.Connected)
    assertEquals(upTo(100_000), uptimesKept())
    assertEquals(emptyList<Instant>(), store.forgotten)
    later(HISTORY_SYNC_EVERY_MS)
    assertEquals(listOf(start.plusSeconds(15).minus(HomeRepositoryImpl.READINGS_KEPT)), store.forgotten)
    assertEquals(upTo(110_000), uptimesKept())
  }

  // ---- when the board restarts -----------------------------------------------------------------------

  @Test
  fun `after a restart of the board the rounds follow the new boot, and what the old boot left stays`() = runTest {
    boardOfBoot(A)
    begin(repository())
    feed(BoardStreamUpdate.Connected)
    later(15_000)
    val oldBoot = uptimesKept(A)
    // The board restarts at this moment and has been up for 5 s.
    val restartedAt = testScheduler.currentTime
    val uptimeB = { 5_000 + testScheduler.currentTime - restartedAt }
    api.home = { BoardResult.Success(house(B, uptimeB())) }
    api.history = { after, limit ->
      askedAt += testScheduler.currentTime
      page(B, samplesAfter(after, limit, uptimeB))
    }
    feed(lost())
    feed(BoardStreamUpdate.Connected)
    // At the first round the new boot has no sample yet (it has been up for 5 s), so the next one asks for everything again, and then for what is newer.
    later(30_000)
    assertEquals(oldBoot, uptimesKept(A))
    assertEquals(listOf(10_000L, 20_000L, 30_000L), uptimesKept(B))
    // The board of boot B said it was 5 s old at 15 s, so its 10 s is 20 s of the test.
    assertEquals(start.plusSeconds(20), store.saved.first { it.bootId == B }.at)
    assertEquals(listOf<Pair<Long?, Int?>>(null to 360, null to 360, 20_000L to 360), api.historyRequests.takeLast(3))
  }

  @Test
  fun `a restart that the stream does not announce is followed by the first state of the new boot`() = runTest {
    boardOfBoot(A)
    begin(repository())
    feed(BoardStreamUpdate.Connected)
    later(15_000)
    val oldBoot = uptimesKept(A)
    val restartedAt = testScheduler.currentTime
    val uptimeB = { 5_000 + testScheduler.currentTime - restartedAt }
    api.history = { after, limit ->
      askedAt += testScheduler.currentTime
      page(B, samplesAfter(after, limit, uptimeB))
    }
    feed(received(house(B, 5_000)))
    later(HISTORY_SYNC_EVERY_MS)
    assertEquals(oldBoot, uptimesKept(A))
    // The new boot has been up for 20 s by the next round: it has its first two samples, and nothing of the old boot was asked for.
    assertEquals(listOf(10_000L, 20_000L), uptimesKept(B))
    assertEquals(null to 360, api.historyRequests.last())
  }

  @Test
  fun `a page of the old boot is not kept as if it were of the new one`() = runTest {
    boardOfBoot(A)
    begin(repository())
    feed(BoardStreamUpdate.Connected)
    later(15_000)
    val kept = store.saved.size
    // The state of the new boot arrived, but the board still answers the history of the old one for a moment.
    feed(received(house(B, 5_000)))
    later(HISTORY_SYNC_EVERY_MS)
    assertEquals(kept, store.saved.size)
  }

  // ---- the stream does not wait for the history ----------------------------------------------------------

  @Test
  fun `the states and the alerts are handled while an answer of the history is slow`() = runTest {
    val gate = CompletableDeferred<Unit>()
    api.home = { BoardResult.Success(house(A, 100_000)) }
    api.history = { _, _ ->
      gate.await()
      page(A, listOf(10_000, 20_000))
    }
    val repository = repository()
    val alerts = mutableListOf<GasAlert>()
    backgroundScope.launch { repository.alerts.toList(alerts) }
    begin(repository)
    feed(BoardStreamUpdate.Connected)
    assertEquals("the history was not asked for", 1, api.historyInFlight)

    val emergency = house(A, 101_000, gasState = "emergency", gasAlerts = 1)
    feed(received(emergency), BoardStreamUpdate.Received(HomeEvent.GasLeak(A, KITCHEN, EntityId("gas-state"), 1, uptimeMs = 101_000)))
    assertEquals(emergency, repository.home.value)
    assertEquals(listOf(GasAlert(A, KITCHEN, 1)), alerts)
    assertEquals("the answer of the history was still waited for", 1, api.historyInFlight)
    assertTrue(store.saved.isEmpty())

    gate.complete(Unit)
    runCurrent()
    assertEquals(listOf(10_000L, 20_000L), uptimesKept())
  }

  @Test
  fun `a command is sent and answered while an answer of the history is slow`() = runTest {
    val gate = CompletableDeferred<Unit>()
    api.home = { BoardResult.Success(house(A, 100_000)) }
    api.history = { _, _ ->
      gate.await()
      page(A, listOf(10_000))
    }
    val repository = repository()
    begin(repository)
    feed(BoardStreamUpdate.Connected)
    assertEquals(1, api.historyInFlight)
    api.command = { _, _ -> BoardResult.Success(POWER) }
    var answered = false
    backgroundScope.launch {
      repository.send(POWER.id, io.github.gustavommcv.ciscsmarthome.domain.CommandValue.Flag(true))
      answered = true
    }
    runCurrent()
    assertTrue(answered)
    assertEquals(1, api.historyInFlight)
  }

  // ---- what is kept, and for how long -----------------------------------------------------------------------

  @Test
  fun `the old readings are let go of during a long connection, once an hour, and not only when it connects`() = runTest {
    api.maxHistoryRequests = 10_000
    boardOfBoot(A)
    begin(repository())
    feed(BoardStreamUpdate.Connected)
    later(3.hours.inWholeMilliseconds + 30 * 60_000)
    val expected = listOf(0L, 1L, 2L, 3L).map { hours -> start.plus(Duration.ofHours(hours)).minus(HomeRepositoryImpl.READINGS_KEPT) }
    assertEquals(expected, store.forgotten)
  }

  @Test
  fun `the history of a long connection has every sample the board took, each once`() = runTest {
    api.maxHistoryRequests = 10_000
    boardOfBoot(A)
    begin(repository())
    feed(BoardStreamUpdate.Connected)
    later(Duration.ofMinutes(90).toMillis())
    assertEquals(upTo(100_000 + Duration.ofMinutes(90).toMillis()), uptimesKept())
  }

  @Test
  fun `the letting go and the saving go together in each round, in that order`() = runTest {
    boardOfBoot(A)
    begin(repository())
    feed(BoardStreamUpdate.Connected)
    later(HISTORY_SYNC_EVERY_MS)
    assertEquals(listOf("forget", "save", "save"), store.calls)
  }

  private companion object {
    const val HISTORY_SYNC_EVERY_MS = 15_000L
    const val SAMPLE_MS = 10_000L
    val A = BootId("aaaa1111")
    val B = BootId("bbbb2222")
    val KITCHEN = io.github.gustavommcv.ciscsmarthome.domain.AreaId("kitchen")
    val GAS = EntityId("gas-level")
    val POWER = io.github.gustavommcv.ciscsmarthome.domain.Entity.Switch(EntityId("dehumidifier-power"), "Power", on = true, commandable = true)
  }
}
