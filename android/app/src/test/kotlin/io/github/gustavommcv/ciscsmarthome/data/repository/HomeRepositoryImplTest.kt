package io.github.gustavommcv.ciscsmarthome.data.repository

import io.github.gustavommcv.ciscsmarthome.data.remote.BoardError
import io.github.gustavommcv.ciscsmarthome.data.remote.BoardResult
import io.github.gustavommcv.ciscsmarthome.data.remote.BoardStreamUpdate
import io.github.gustavommcv.ciscsmarthome.data.remote.ErrorCode
import io.github.gustavommcv.ciscsmarthome.domain.AlertTracker
import io.github.gustavommcv.ciscsmarthome.domain.Area
import io.github.gustavommcv.ciscsmarthome.domain.AreaId
import io.github.gustavommcv.ciscsmarthome.domain.BootId
import io.github.gustavommcv.ciscsmarthome.domain.CommandResult
import io.github.gustavommcv.ciscsmarthome.domain.CommandValue
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionProblem
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionState
import io.github.gustavommcv.ciscsmarthome.domain.Device
import io.github.gustavommcv.ciscsmarthome.domain.DeviceId
import io.github.gustavommcv.ciscsmarthome.domain.Entity
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.GasAlert
import io.github.gustavommcv.ciscsmarthome.domain.HistoryPage
import io.github.gustavommcv.ciscsmarthome.domain.HistorySample
import io.github.gustavommcv.ciscsmarthome.domain.Home
import io.github.gustavommcv.ciscsmarthome.domain.HomeEvent
import io.github.gustavommcv.ciscsmarthome.domain.Reading
import io.github.gustavommcv.ciscsmarthome.domain.SensorValue
import io.github.gustavommcv.ciscsmarthome.domain.StoredSample
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The repository against fakes of the board, of its events, of the stored history and of the clock. */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeRepositoryImplTest {
  private val api = FakeBoardApi()
  private val events = FakeBoardEvents()
  private val store = FakeHistoryStore()
  private val clock = TestClock(Instant.parse("2026-10-27T12:00:00Z"))

  // ---- helpers ------------------------------------------------------------------

  private fun repository(tracker: AlertTracker = AlertTracker()) = HomeRepositoryImpl(api, events, store, clock, tracker)

  /** Starts the repository the way the service will: in the background, for as long as the test lasts. */
  private fun TestScope.start(repository: HomeRepositoryImpl) {
    backgroundScope.launch { repository.keepSynced() }
    runCurrent()
  }

  private fun TestScope.feed(vararg updates: BoardStreamUpdate) {
    updates.forEach { events.feed.trySend(it) }
    runCurrent()
  }

  private fun TestScope.collectAlerts(repository: HomeRepositoryImpl): List<GasAlert> {
    val alerts = mutableListOf<GasAlert>()
    backgroundScope.launch { repository.alerts.toList(alerts) }
    runCurrent()
    return alerts
  }

  private fun house(boot: BootId = A, uptimeMs: Long = 100_000, state: String = "normal", alerts: Long = 0) =
    Home(
      boot,
      uptimeMs,
      listOf(
        Area(
          KITCHEN,
          "Kitchen",
          listOf(
            Device(
              DeviceId("gas-sensor"),
              "Gas sensor",
              listOf(
                Entity.Sensor(GAS_STATE, "Gas state", SensorValue.Text(state)),
                Entity.Sensor(EntityId("gas-alerts"), "Gas alerts", SensorValue.Number(alerts.toDouble())),
              ),
            ),
          ),
        ),
        Area(BEDROOM, "Bedroom", listOf(Device(DeviceId("dehumidifier"), "Dehumidifier", listOf(POWER)))),
      ),
    )

  private fun received(home: Home) = BoardStreamUpdate.Received(HomeEvent.HomeUpdated(home))

  private fun leak(count: Long, boot: BootId = A) = BoardStreamUpdate.Received(HomeEvent.GasLeak(boot, KITCHEN, GAS_STATE, count, uptimeMs = 120_000))

  private fun lost(error: BoardError) = BoardStreamUpdate.Lost(error)

  private fun unreachable() = BoardError.Unreachable(IOException("no answer"))

  private fun page(boot: BootId, vararg uptimes: Long) = BoardResult.Success(HistoryPage(boot, uptimes.map { HistorySample(it, mapOf(GAS to it / 1000.0)) }))

  private fun boardHas(home: Home) {
    api.home = { BoardResult.Success(home) }
  }

  // ---- the state of the connection -------------------------------------------------

  @Test
  fun `before anything happens it is connecting and has no home`() = runTest {
    val repository = repository()
    start(repository)
    assertEquals(ConnectionState.Connecting, repository.connection.value)
    assertNull(repository.home.value)
  }

  @Test
  fun `when the board accepts the connection it asks for the home and is connected`() = runTest {
    boardHas(house())
    val repository = repository()
    start(repository)
    feed(BoardStreamUpdate.Connected)
    assertEquals(ConnectionState.Connected, repository.connection.value)
    assertEquals(house(), repository.home.value)
  }

  @Test
  fun `a home that arrives on the stream becomes the home`() = runTest {
    boardHas(house())
    val repository = repository()
    start(repository)
    feed(BoardStreamUpdate.Connected, received(house(uptimeMs = 101_000, state = "emergency", alerts = 1)))
    assertEquals(house(uptimeMs = 101_000, state = "emergency", alerts = 1), repository.home.value)
  }

  @Test
  fun `an older home of the same boot is ignored`() = runTest {
    boardHas(house())
    val repository = repository()
    start(repository)
    feed(BoardStreamUpdate.Connected, received(house(uptimeMs = 105_000)), received(house(uptimeMs = 102_000, state = "emergency")))
    assertEquals(house(uptimeMs = 105_000), repository.home.value)
  }

  @Test
  fun `a home of another boot is taken even if its uptime is smaller`() = runTest {
    boardHas(house())
    val repository = repository()
    start(repository)
    feed(BoardStreamUpdate.Connected, received(house(boot = A, uptimeMs = 105_000)), received(house(boot = B, uptimeMs = 2_000)))
    assertEquals(house(boot = B, uptimeMs = 2_000), repository.home.value)
  }

  @Test
  fun `when the connection is lost the last home is kept and the reason is known`() = runTest {
    boardHas(house())
    val repository = repository()
    start(repository)
    feed(BoardStreamUpdate.Connected, lost(unreachable()))
    assertEquals(ConnectionState.Disconnected(ConnectionProblem.Unreachable), repository.connection.value)
    assertEquals(house(), repository.home.value)
  }

  @Test
  fun `why the connection was lost is told to whoever shows it`() = runTest {
    val reasons =
      listOf(
        unreachable() to ConnectionProblem.Unreachable,
        BoardError.Rejected(503, ErrorCode.TooManyClients, "busy") to ConnectionProblem.TooManyClients,
        BoardError.Rejected(406, ErrorCode.NotAcceptable, "no") to ConnectionProblem.Incompatible,
        BoardError.Rejected(404, ErrorCode.Unknown, "?") to ConnectionProblem.Incompatible,
        BoardError.Unexpected("not an event stream") to ConnectionProblem.Incompatible,
      )
    val repository = repository()
    start(repository)
    for ((error, problem) in reasons) {
      feed(lost(error))
      assertEquals("for $error", ConnectionState.Disconnected(problem), repository.connection.value)
    }
  }

  @Test
  fun `it is connected again when the board accepts again`() = runTest {
    boardHas(house())
    val repository = repository()
    start(repository)
    feed(BoardStreamUpdate.Connected, lost(unreachable()), BoardStreamUpdate.Connected)
    assertEquals(ConnectionState.Connected, repository.connection.value)
  }

  @Test
  fun `a home that cannot be fetched after connecting is a problem, not a connection`() = runTest {
    api.home = { BoardResult.Failure(unreachable()) }
    val repository = repository()
    start(repository)
    feed(BoardStreamUpdate.Connected)
    assertEquals(ConnectionState.Disconnected(ConnectionProblem.Unreachable), repository.connection.value)
  }

  @Test
  fun `a home the app cannot understand is an incompatible board`() = runTest {
    api.home = { BoardResult.Failure(BoardError.Unexpected("the board speaks version 2 of the API")) }
    val repository = repository()
    start(repository)
    feed(BoardStreamUpdate.Connected)
    assertEquals(ConnectionState.Disconnected(ConnectionProblem.Incompatible), repository.connection.value)
  }

  @Test
  fun `events that keep coming prove the connection even if the home request failed`() = runTest {
    api.home = { BoardResult.Failure(unreachable()) }
    val repository = repository()
    start(repository)
    feed(BoardStreamUpdate.Connected)
    feed(received(house()))
    assertEquals(ConnectionState.Connected, repository.connection.value)
    assertEquals(house(), repository.home.value)
  }

  // ---- the history ------------------------------------------------------------------

  @Test
  fun `after connecting it fetches the history and keeps it with the phone's time`() = runTest {
    boardHas(house(uptimeMs = 100_000))
    api.history = { _, _ -> page(A, 80_000, 90_000) }
    start(repository())
    feed(BoardStreamUpdate.Connected)
    assertEquals(
      listOf(
        StoredSample(A, 80_000, Instant.parse("2026-10-27T11:59:40Z"), mapOf(GAS to 80.0)),
        StoredSample(A, 90_000, Instant.parse("2026-10-27T11:59:50Z"), mapOf(GAS to 90.0)),
      ),
      store.saved,
    )
    assertEquals(listOf<Pair<Long?, Int?>>(null to 360), api.historyRequests)
  }

  @Test
  fun `it asks only for what is newer than what it has`() = runTest {
    boardHas(house())
    store.save(listOf(StoredSample(A, 70_000, Instant.parse("2026-10-27T11:59:00Z"), mapOf(GAS to 70.0))))
    api.history = { _, _ -> page(A, 80_000) }
    start(repository())
    feed(BoardStreamUpdate.Connected)
    assertEquals(listOf<Pair<Long?, Int?>>(70_000L to 360), api.historyRequests)
  }

  @Test
  fun `a boot never seen is fetched from the start, whatever other boots left`() = runTest {
    boardHas(house(boot = A))
    store.save(listOf(StoredSample(B, 70_000, Instant.parse("2026-10-27T11:00:00Z"), mapOf(GAS to 70.0))))
    api.history = { _, _ -> page(A, 5_000) }
    start(repository())
    feed(BoardStreamUpdate.Connected)
    assertEquals(listOf<Pair<Long?, Int?>>(null to 360), api.historyRequests)
  }

  @Test
  fun `a full page makes it ask again after the last sample, until a page is not full`() = runTest {
    boardHas(house(uptimeMs = 500_000))
    val full = (1..360).map { it * 1_000L }.toLongArray()
    api.history = { after, _ -> if (after == null) page(A, *full) else page(A, 361_000, 362_000) }
    start(repository())
    feed(BoardStreamUpdate.Connected)
    assertEquals(listOf<Pair<Long?, Int?>>(null to 360, 360_000L to 360), api.historyRequests)
    assertEquals(362, store.saved.size)
  }

  @Test
  fun `an empty page ends the fetching`() = runTest {
    boardHas(house())
    api.history = { _, _ -> page(A) }
    start(repository())
    feed(BoardStreamUpdate.Connected)
    assertEquals(1, api.historyRequests.size)
    assertEquals(emptyList<StoredSample>(), store.saved)
  }

  @Test
  fun `a board that keeps sending the same full page is not asked forever`() = runTest {
    boardHas(house(uptimeMs = 500_000))
    val full = (1..360).map { it * 1_000L }.toLongArray()
    api.history = { _, _ ->
      check(api.historyRequests.size <= 5) { "asked ${api.historyRequests.size} times" }
      page(A, *full)
    }
    start(repository())
    feed(BoardStreamUpdate.Connected)
    assertEquals(2, api.historyRequests.size)
  }

  @Test
  fun `a page of another boot than the one asked about is not kept`() = runTest {
    boardHas(house(boot = A))
    api.history = { _, _ -> page(B, 5_000, 6_000) }
    start(repository())
    feed(BoardStreamUpdate.Connected)
    assertEquals(emptyList<StoredSample>(), store.saved)
  }

  @Test
  fun `a history that cannot be fetched does not take the home away`() = runTest {
    boardHas(house())
    api.history = { _, _ -> BoardResult.Failure(unreachable()) }
    val repository = repository()
    start(repository)
    feed(BoardStreamUpdate.Connected)
    assertEquals(ConnectionState.Connected, repository.connection.value)
    assertEquals(house(), repository.home.value)
    assertEquals(emptyList<StoredSample>(), store.saved)
  }

  @Test
  fun `the series come from the stored history`() = runTest {
    val sample = StoredSample(A, 70_000, Instant.parse("2026-10-27T11:59:00Z"), mapOf(GAS to 70.0))
    store.save(listOf(sample))
    val series = repository().series(GAS, Instant.EPOCH).first()
    assertEquals(listOf(Reading(sample.at, 70.0)), series)
  }

  // ---- gas leaks ------------------------------------------------------------------------

  @Test
  fun `a gas leak event is news the first time`() = runTest {
    boardHas(house())
    val repository = repository()
    val alerts = collectAlerts(repository)
    start(repository)
    feed(BoardStreamUpdate.Connected, leak(count = 1))
    assertEquals(listOf(GasAlert(A, KITCHEN, 1)), alerts)
  }

  @Test
  fun `the same leak is not news again when the home says it too`() = runTest {
    boardHas(house())
    val repository = repository()
    val alerts = collectAlerts(repository)
    start(repository)
    feed(BoardStreamUpdate.Connected, leak(count = 1), received(house(uptimeMs = 120_500, state = "emergency", alerts = 1)))
    assertEquals(1, alerts.size)
  }

  @Test
  fun `a leak that is active in the home is news even if no event came`() = runTest {
    boardHas(house())
    val repository = repository()
    val alerts = collectAlerts(repository)
    start(repository)
    feed(BoardStreamUpdate.Connected, received(house(uptimeMs = 120_000, state = "emergency", alerts = 1)))
    assertEquals(listOf(GasAlert(A, KITCHEN, 1)), alerts)
  }

  @Test
  fun `a leak that is active in the first home after connecting is news`() = runTest {
    boardHas(house(state = "emergency", alerts = 2))
    val repository = repository()
    val alerts = collectAlerts(repository)
    start(repository)
    feed(BoardStreamUpdate.Connected)
    assertEquals(listOf(GasAlert(A, KITCHEN, 2)), alerts)
  }

  @Test
  fun `leaks that ended while the app was away are not news`() = runTest {
    boardHas(house(state = "normal", alerts = 4))
    val repository = repository()
    val alerts = collectAlerts(repository)
    start(repository)
    feed(BoardStreamUpdate.Connected, leak(count = 4))
    assertEquals(emptyList<GasAlert>(), alerts)
  }

  @Test
  fun `a second leak is news of its own`() = runTest {
    boardHas(house())
    val repository = repository()
    val alerts = collectAlerts(repository)
    start(repository)
    feed(BoardStreamUpdate.Connected, leak(count = 1), leak(count = 2))
    assertEquals(listOf(GasAlert(A, KITCHEN, 1), GasAlert(A, KITCHEN, 2)), alerts)
  }

  @Test
  fun `a leak that was already announced stays quiet after the app restarts`() = runTest {
    boardHas(house(state = "emergency", alerts = 1))
    val repository = repository(AlertTracker(mapOf(A to 1L)))
    val alerts = collectAlerts(repository)
    start(repository)
    feed(BoardStreamUpdate.Connected)
    assertEquals(emptyList<GasAlert>(), alerts)
  }

  @Test
  fun `news waits for whoever collects it`() = runTest {
    boardHas(house())
    val repository = repository()
    start(repository)
    feed(BoardStreamUpdate.Connected, leak(count = 1))
    val alerts = collectAlerts(repository)
    assertEquals(listOf(GasAlert(A, KITCHEN, 1)), alerts)
  }

  // ---- commands ---------------------------------------------------------------------------

  @Test
  fun `a command goes to the board and the answer replaces the entity in the home`() = runTest {
    boardHas(house())
    val on = POWER.copy(on = true)
    api.command = { _, _ -> BoardResult.Success(on) }
    val repository = repository()
    start(repository)
    feed(BoardStreamUpdate.Connected)

    val result = repository.send(POWER.id, CommandValue.Flag(true))

    assertEquals(CommandResult.Applied(on), result)
    assertEquals(listOf(POWER.id to CommandValue.Flag(true)), api.commands)
    assertEquals(on, repository.home.value?.findEntity(POWER.id))
  }

  @Test
  fun `a command the board refuses is refused, and the home stays as it was`() = runTest {
    boardHas(house())
    api.command = { _, _ -> BoardResult.Failure(BoardError.Rejected(403, ErrorCode.NotCommandable, "read only")) }
    val repository = repository()
    start(repository)
    feed(BoardStreamUpdate.Connected)

    assertEquals(CommandResult.Refused, repository.send(EntityId("valve-open"), CommandValue.Flag(true)))
    assertEquals(house(), repository.home.value)
  }

  @Test
  fun `a command that cannot reach the board fails with the reason`() = runTest {
    api.command = { _, _ -> BoardResult.Failure(unreachable()) }
    assertEquals(CommandResult.Failed(ConnectionProblem.Unreachable), repository().send(POWER.id, CommandValue.Flag(true)))
  }

  @Test
  fun `an answer to a command that the app cannot understand is an incompatible board`() = runTest {
    api.command = { _, _ -> BoardResult.Failure(BoardError.Unexpected("odd")) }
    assertEquals(CommandResult.Failed(ConnectionProblem.Incompatible), repository().send(POWER.id, CommandValue.Flag(true)))
  }

  @Test
  fun `a command before there is a home does not invent one`() = runTest {
    api.command = { _, _ -> BoardResult.Success(POWER.copy(on = true)) }
    val repository = repository()
    repository.send(POWER.id, CommandValue.Flag(true))
    assertNull(repository.home.value)
  }

  private companion object {
    val A = BootId("aaaa1111")
    val B = BootId("bbbb2222")
    val KITCHEN = AreaId("kitchen")
    val BEDROOM = AreaId("bedroom")
    val GAS_STATE = EntityId("gas-state")
    val GAS = EntityId("gas-level")
    val POWER = Entity.Switch(EntityId("dehumidifier-power"), "Power", on = false, commandable = true)
  }
}
