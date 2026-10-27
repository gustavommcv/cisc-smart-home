package io.github.gustavommcv.ciscsmarthome.data.remote

import io.github.gustavommcv.ciscsmarthome.domain.Area
import io.github.gustavommcv.ciscsmarthome.domain.AreaId
import io.github.gustavommcv.ciscsmarthome.domain.BootId
import io.github.gustavommcv.ciscsmarthome.domain.Device
import io.github.gustavommcv.ciscsmarthome.domain.DeviceId
import io.github.gustavommcv.ciscsmarthome.domain.Entity
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.Home
import io.github.gustavommcv.ciscsmarthome.domain.HomeEvent
import io.github.gustavommcv.ciscsmarthome.domain.SensorValue
import java.net.SocketTimeoutException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import mockwebserver3.MockResponse
import mockwebserver3.MockResponseBody
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okio.BufferedSink
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** The event stream against a fake of the board that sends what, and when, each test says. */
class BoardEventStreamTest {
  private val server = MockWebServer()
  private val http = OkHttpClient()
  private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
  private val streams = mutableListOf<LiveStream>()
  private val watches = mutableListOf<Watch>()

  @Before
  fun setUp() {
    server.start()
  }

  @After
  fun tearDown() {
    watches.forEach { it.stop() }
    streams.forEach { it.end() }
    server.close()
    scope.cancel()
  }

  // ---- helpers ------------------------------------------------------------------

  /** A response whose body the test writes by hand, for as long as it likes. */
  private class LiveStream : MockResponseBody {
    private val outgoing = LinkedBlockingQueue<String>()

    override val contentLength: Long get() = -1L

    override fun writeTo(sink: BufferedSink) {
      while (true) {
        val text = outgoing.take()
        if (text === END) return
        sink.writeUtf8(text)
        sink.flush()
      }
    }

    fun send(text: String) = outgoing.put(text)

    /** The board closes the stream. */
    fun end() = outgoing.put(END)

    private companion object {
      val END = String()
    }
  }

  private fun stream(contentType: String = "text/event-stream"): LiveStream {
    val live = LiveStream()
    streams += live
    // Like the board's, the body has no length: it ends when the board closes the connection.
    server.enqueue(MockResponse.Builder().addHeader("Content-Type", contentType).body(live).onResponseEnd(SocketEffect.CloseSocket()).build())
    return live
  }

  private fun reject(status: Int, code: String) {
    server.enqueue(
      MockResponse.Builder()
        .code(status)
        .addHeader("Content-Type", "application/json")
        .body("""{"error":{"code":"$code","message":"a message from the board"}}""")
        .build(),
    )
  }

  /** Collects the updates of a stream in the background; the test takes them one by one. */
  private inner class Watch(stream: BoardEventStream) {
    private val updates = Channel<BoardStreamUpdate>(Channel.UNLIMITED)
    private val job = scope.launch { stream.updates().collect { updates.send(it) } }

    init {
      watches += this
    }

    fun next(): BoardStreamUpdate {
      val update = runBlocking { withTimeoutOrNull(5.seconds) { updates.receive() } }
      return update ?: throw AssertionError("no update arrived in 5 s")
    }

    /** True when nothing arrives for [duration]. */
    fun quietFor(duration: Duration): Boolean = runBlocking { withTimeoutOrNull(duration) { updates.receive() } } == null

    fun stop() = runBlocking { job.cancelAndJoin() }
  }

  private fun watch(
    baseUrl: HttpUrl = server.url("/"),
    client: OkHttpClient = http,
    silenceLimit: Duration = 20.seconds,
    backoff: Backoff = Backoff(),
    pause: suspend (Duration) -> Unit = { delay(10.milliseconds) },
  ) = Watch(BoardEventStream(baseUrl, client, silenceLimit, backoff, pause))

  private fun home(boot: String = BOOT, apiVersion: Int = 1, alerts: Long = 0) =
    """{"apiVersion":$apiVersion,"bootId":"$boot","uptimeMs":183450,"areas":[{"id":"kitchen","name":"Kitchen","devices":[""" +
      """{"id":"gas-monitor","name":"Gas monitor","entities":[{"id":"gas-state","kind":"sensor","name":"Gas state","value":"normal"},""" +
      """{"id":"gas-alerts","kind":"sensor","name":"Gas alerts since boot","value":$alerts}]}]}]}"""

  private fun homeEvent(boot: String = BOOT, apiVersion: Int = 1, alerts: Long = 0) = "event: home\ndata: ${home(boot, apiVersion, alerts)}\n\n"

  private fun alertEvent(count: Long = 1, type: String = "gas-leak") =
    "event: alert\ndata: {\"type\":\"$type\",\"areaId\":\"kitchen\",\"entityId\":\"gas-state\",\"count\":$count,\"uptimeMs\":183450}\n\n"

  private fun expectedHome(boot: String = BOOT, alerts: Long = 0) =
    Home(
      BootId(boot),
      183450,
      listOf(
        Area(
          AreaId("kitchen"),
          "Kitchen",
          listOf(
            Device(
              DeviceId("gas-monitor"),
              "Gas monitor",
              listOf(
                Entity.Sensor(EntityId("gas-state"), "Gas state", SensorValue.Text("normal")),
                Entity.Sensor(EntityId("gas-alerts"), "Gas alerts since boot", SensorValue.Number(alerts.toDouble())),
              ),
            ),
          ),
        ),
      ),
    )

  private fun received(event: HomeEvent) = BoardStreamUpdate.Received(event)

  private fun BoardStreamUpdate.lost(): BoardError {
    assertTrue("expected the stream to be lost but it was $this", this is BoardStreamUpdate.Lost)
    return (this as BoardStreamUpdate.Lost).error
  }

  // ---- the request ---------------------------------------------------------------

  @Test
  fun `it asks for the event stream the way the contract says`() {
    stream()
    watch().next()
    val request = server.takeRequest()
    assertEquals("GET", request.method)
    assertEquals("/api/v1/events", request.url.encodedPath)
    // The library sends it; without it the board answers 406, so a library that stopped would show here.
    assertEquals("text/event-stream", request.headers["Accept"])
  }

  // ---- what the board sends ------------------------------------------------------

  @Test
  fun `it says it is connected and then gives the home as the board sent it`() {
    val board = stream()
    val events = watch()
    board.send(homeEvent())
    assertEquals(BoardStreamUpdate.Connected, events.next())
    assertEquals(received(HomeEvent.HomeUpdated(expectedHome())), events.next())
  }

  @Test
  fun `a gas leak carries the boot of the connection it came on`() {
    val board = stream()
    val events = watch()
    board.send(homeEvent())
    board.send(alertEvent(count = 3))
    events.next()
    events.next()
    assertEquals(
      received(HomeEvent.GasLeak(BootId(BOOT), AreaId("kitchen"), EntityId("gas-state"), count = 3, uptimeMs = 183450)),
      events.next(),
    )
  }

  @Test
  fun `the alert comes before the home of the same moment, as the board sent them`() {
    val board = stream()
    val events = watch()
    board.send(homeEvent())
    board.send(alertEvent(count = 1) + homeEvent(alerts = 1))
    events.next()
    events.next()
    assertTrue(events.next().isGasLeak())
    assertEquals(received(HomeEvent.HomeUpdated(expectedHome(alerts = 1))), events.next())
  }

  @Test
  fun `an alert before any home is skipped, because it cannot be told which boot it belongs to`() {
    val board = stream()
    val events = watch()
    board.send(alertEvent())
    board.send(homeEvent())
    events.next()
    assertEquals(received(HomeEvent.HomeUpdated(expectedHome())), events.next())
  }

  @Test
  fun `an alert of a kind the app does not know is skipped`() {
    val board = stream()
    val events = watch()
    board.send(homeEvent())
    board.send(alertEvent(type = "smoke") + homeEvent(alerts = 5))
    events.next()
    events.next()
    assertEquals(received(HomeEvent.HomeUpdated(expectedHome(alerts = 5))), events.next())
  }

  @Test
  fun `an alert that is missing a field, or is not JSON, is skipped`() {
    val board = stream()
    val events = watch()
    board.send(homeEvent())
    board.send("event: alert\ndata: {\"type\":\"gas-leak\",\"count\":1}\n\n")
    board.send("event: alert\ndata: not json\n\n")
    board.send(homeEvent(alerts = 7))
    events.next()
    events.next()
    assertEquals(received(HomeEvent.HomeUpdated(expectedHome(alerts = 7))), events.next())
  }

  @Test
  fun `pings, comments and events it does not know are ignored`() {
    val board = stream()
    val events = watch()
    board.send("event: ping\ndata: \n\n")
    board.send(": a comment\n\n")
    board.send("event: from-the-future\ndata: {}\n\n")
    board.send("data: no event name\n\n")
    board.send(homeEvent())
    assertEquals(BoardStreamUpdate.Connected, events.next())
    assertEquals(received(HomeEvent.HomeUpdated(expectedHome())), events.next())
    assertTrue(events.quietFor(200.milliseconds))
  }

  @Test
  fun `a home it cannot read is skipped and the stream goes on`() {
    val board = stream()
    val events = watch()
    board.send("event: home\ndata: not json\n\n")
    board.send(homeEvent(apiVersion = 2))
    board.send("event: home\ndata: {\"apiVersion\":1}\n\n")
    board.send(homeEvent(alerts = 9))
    assertEquals(BoardStreamUpdate.Connected, events.next())
    assertEquals(received(HomeEvent.HomeUpdated(expectedHome(alerts = 9))), events.next())
    assertTrue(events.quietFor(200.milliseconds))
  }

  @Test
  fun `an event that arrives in pieces is put together`() {
    val board = stream()
    val events = watch()
    val whole = homeEvent()
    board.send(whole.substring(0, 30))
    Thread.sleep(100)
    board.send(whole.substring(30, 90))
    Thread.sleep(100)
    board.send(whole.substring(90))
    assertEquals(BoardStreamUpdate.Connected, events.next())
    assertEquals(received(HomeEvent.HomeUpdated(expectedHome())), events.next())
  }

  // ---- when the board says no ----------------------------------------------------

  @Test
  fun `a board that refuses the connection is a rejection, and the stream tries again`() {
    reject(503, "too-many-clients")
    val board = stream()
    val events = watch()
    val rejection = events.next().lost() as BoardError.Rejected
    assertEquals(503, rejection.status)
    assertEquals(ErrorCode.TooManyClients, rejection.code)
    board.send(homeEvent())
    assertEquals(BoardStreamUpdate.Connected, events.next())
    assertEquals(received(HomeEvent.HomeUpdated(expectedHome())), events.next())
  }

  @Test
  fun `a refusal that is not an error of the contract is unexpected`() {
    server.enqueue(MockResponse.Builder().code(500).body("oops").build())
    assertTrue(watch().next().lost() is BoardError.Unexpected)
  }

  @Test
  fun `an answer that is not an event stream is unexpected`() {
    stream(contentType = "application/json")
    assertTrue(watch().next().lost() is BoardError.Unexpected)
  }

  // ---- when the connection is lost -----------------------------------------------

  @Test
  fun `a board that closes the stream is a loss, and the stream connects again`() {
    val first = stream()
    val second = stream()
    val events = watch()
    first.send(homeEvent())
    first.end()
    events.next()
    events.next()
    assertTrue(events.next().lost() is BoardError.Unreachable)
    second.send(homeEvent(alerts = 2))
    assertEquals(BoardStreamUpdate.Connected, events.next())
    assertEquals(received(HomeEvent.HomeUpdated(expectedHome(alerts = 2))), events.next())
  }

  @Test
  fun `a board that cannot be reached is a loss, again and again`() {
    val gone = MockWebServer().apply { start() }
    val url = gone.url("/")
    gone.close()
    val events = watch(baseUrl = url)
    assertTrue(events.next().lost() is BoardError.Unreachable)
    assertTrue(events.next().lost() is BoardError.Unreachable)
    assertTrue(events.next().lost() is BoardError.Unreachable)
  }

  @Test
  fun `a board that goes silent for longer than the limit is a loss, and the stream connects again`() {
    val first = stream()
    val second = stream()
    val events = watch(silenceLimit = 300.milliseconds)
    first.send(homeEvent())
    events.next()
    events.next()
    val loss = events.next().lost() as BoardError.Unreachable
    assertTrue("expected a timeout but was ${loss.cause}", loss.cause is SocketTimeoutException)
    second.send(homeEvent(alerts = 1))
    assertEquals(BoardStreamUpdate.Connected, events.next())
    assertEquals(received(HomeEvent.HomeUpdated(expectedHome(alerts = 1))), events.next())
  }

  @Test
  fun `pings keep a stream alive for longer than the limit`() {
    val board = stream()
    val events = watch(silenceLimit = 400.milliseconds)
    board.send(homeEvent())
    events.next()
    events.next()
    repeat(8) {
      Thread.sleep(150)
      board.send("event: ping\ndata: \n\n")
    }
    assertTrue("the stream was lost although pings kept coming", events.quietFor(100.milliseconds))
    board.send(alertEvent())
    assertTrue(events.next().isGasLeak())
  }

  @Test
  fun `the call timeout of the client it was given does not cut the stream`() {
    // The library lifts the call timeout once the stream opens; a stream that dies at that timeout would show here.
    val board = stream()
    val events = watch(client = OkHttpClient.Builder().callTimeout(300, TimeUnit.MILLISECONDS).build())
    board.send(homeEvent())
    events.next()
    events.next()
    Thread.sleep(800)
    board.send(alertEvent())
    assertTrue(events.next().isGasLeak())
  }

  @Test
  fun `a new connection is a new boot, and its alerts carry the new boot`() {
    val first = stream()
    val second = stream()
    val events = watch()
    first.send(homeEvent(boot = "aaaa1111"))
    first.end()
    second.send(homeEvent(boot = "bbbb2222") + alertEvent())
    repeat(4) { events.next() } // connected, home, lost, connected
    events.next()
    val leak = events.next() as BoardStreamUpdate.Received
    assertEquals(BootId("bbbb2222"), (leak.event as HomeEvent.GasLeak).bootId)
  }

  // ---- when it tries again -------------------------------------------------------

  @Test
  fun `the waits between attempts grow, and start over after a connection that worked`() {
    val pauses = Channel<Duration>(Channel.UNLIMITED)
    repeat(4) { reject(503, "too-many-clients") }
    val worked = stream()
    val events = watch(backoff = Backoff(initial = 1.seconds, max = 4.seconds), pause = { pauses.send(it) })
    repeat(4) { events.next().lost() }
    assertEquals(BoardStreamUpdate.Connected, events.next())
    worked.end()
    events.next().lost()
    val waited = runBlocking { List(5) { withTimeoutOrNull(5.seconds) { pauses.receive() } } }
    assertEquals(listOf(1.seconds, 2.seconds, 4.seconds, 4.seconds, 1.seconds), waited)
  }

  // ---- when the collector goes away ----------------------------------------------

  @Test
  fun `stopping the collection cancels the call and does not connect again`() {
    val board = stream()
    val events = watch()
    board.send(homeEvent())
    events.next()
    events.next()
    events.stop()
    val deadline = System.nanoTime() + 5_000_000_000L
    while (http.dispatcher.runningCallsCount() > 0 && System.nanoTime() < deadline) Thread.sleep(10)
    assertEquals("the call to the board is still running", 0, http.dispatcher.runningCallsCount())
    val requests = server.requestCount
    Thread.sleep(200)
    assertEquals(requests, server.requestCount)
  }

  @Test
  fun `no update is lost when the collector is slower than the board`() {
    val board = stream()
    val gate = CompletableDeferred<Unit>()
    val stream = BoardEventStream(server.url("/"), http, 20.seconds, Backoff(), { delay(10.milliseconds) })
    val collected = scope.async { stream.updates().onEach { gate.await() }.take(202).toList() }
    board.send(homeEvent())
    repeat(200) { board.send(alertEvent(count = it + 1L)) }
    Thread.sleep(500) // the collector is held at the first update while the rest piles up
    gate.complete(Unit)
    val updates = runBlocking { withTimeoutOrNull(5.seconds) { collected.await() } } ?: throw AssertionError("the updates did not all arrive")
    val counts = updates.drop(2).map { ((it as BoardStreamUpdate.Received).event as HomeEvent.GasLeak).count }
    assertEquals((1L..200L).toList(), counts)
  }

  private fun BoardStreamUpdate.isGasLeak() = this is BoardStreamUpdate.Received && event is HomeEvent.GasLeak

  private companion object {
    const val BOOT = "5f3a9c21"
  }
}
