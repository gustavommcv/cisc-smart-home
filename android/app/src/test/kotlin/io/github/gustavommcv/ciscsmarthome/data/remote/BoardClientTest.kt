package io.github.gustavommcv.ciscsmarthome.data.remote

import io.github.gustavommcv.ciscsmarthome.domain.AreaId
import io.github.gustavommcv.ciscsmarthome.domain.BootId
import io.github.gustavommcv.ciscsmarthome.domain.CommandValue
import io.github.gustavommcv.ciscsmarthome.domain.DeviceId
import io.github.gustavommcv.ciscsmarthome.domain.Entity
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.Home
import io.github.gustavommcv.ciscsmarthome.domain.SensorValue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** The REST client against a fake of the board that answers as docs/api.md says. */
class BoardClientTest {
  private val server = MockWebServer()
  private val http = OkHttpClient.Builder().callTimeout(5, TimeUnit.SECONDS).build()
  private lateinit var client: BoardClient

  @Before
  fun setUp() {
    server.start()
    client = BoardClient(server.url("/"), http)
  }

  @After
  fun tearDown() {
    server.close()
  }

  // ---- helpers ------------------------------------------------------------------

  private fun reply(status: Int, body: String) {
    server.enqueue(MockResponse.Builder().code(status).addHeader("Content-Type", "application/json").body(body).build())
  }

  private fun error(status: Int, code: String) = reply(status, """{"error":{"code":"$code","message":"a message from the board"}}""")

  private fun <T> BoardResult<T>.value(): T {
    assertTrue("expected success but was $this", this is BoardResult.Success)
    return (this as BoardResult.Success).value
  }

  private fun BoardResult<*>.error(): BoardError {
    assertTrue("expected a failure but was $this", this is BoardResult.Failure)
    return (this as BoardResult.Failure).error
  }

  private fun homeWith(vararg entities: String, apiVersion: Int = 1) =
    """{"apiVersion":$apiVersion,"bootId":"b1","uptimeMs":1000,"areas":[{"id":"a","name":"A","devices":[{"id":"d","name":"D","entities":[${entities.joinToString(",")}]}]}]}"""

  private fun firstEntity(home: Home): Entity? = home.areas.single().devices.single().entities.firstOrNull()

  private fun getFirstEntity(vararg entities: String): Entity? {
    reply(200, homeWith(*entities))
    return firstEntity(runBlocking { client.getHome() }.value())
  }

  // ---- GET /api/v1/home ---------------------------------------------------------

  @Test
  fun `the home is asked for with a GET on the home route`() {
    reply(200, homeWith())
    runBlocking { client.getHome() }
    val request = server.takeRequest(1, TimeUnit.SECONDS)!!
    assertEquals("GET", request.method)
    assertEquals("/api/v1/home", request.url.encodedPath)
  }

  @Test
  fun `the whole example of the contract becomes the model of the house`() {
    reply(200, CONTRACT_HOME)
    val home = runBlocking { client.getHome() }.value()
    assertEquals(BootId("5f3a9c21"), home.bootId)
    assertEquals(183_450L, home.uptimeMs)
    assertEquals(listOf(AreaId("kitchen"), AreaId("bedroom")), home.areas.map { it.id })
    assertEquals(listOf(DeviceId("gas-sensor"), DeviceId("gas-valve")), home.areas[0].devices.map { it.id })
    assertEquals(
      Entity.Sensor(EntityId("gas-level"), "Gas level", SensorValue.Number(142.0), unit = "adc"),
      home.findEntity(EntityId("gas-level")),
    )
    assertEquals(
      Entity.Sensor(EntityId("gas-state"), "Gas state", SensorValue.Text("normal")),
      home.findEntity(EntityId("gas-state")),
    )
    assertEquals(
      Entity.BinarySensor(EntityId("valve-open"), "Valve open", value = true),
      home.findEntity(EntityId("valve-open")),
    )
    assertEquals(
      Entity.Sensor(EntityId("temperature"), "Temperature", SensorValue.Number(21.4), unit = "°C"),
      home.findEntity(EntityId("temperature")),
    )
    assertEquals(
      Entity.Switch(EntityId("dehumidifier-power"), "Power", on = false, commandable = true),
      home.findEntity(EntityId("dehumidifier-power")),
    )
    assertEquals(
      Entity.Select(EntityId("dehumidifier-mode"), "Mode", "automatic", listOf("automatic", "manual"), commandable = true),
      home.findEntity(EntityId("dehumidifier-mode")),
    )
  }

  @Test
  fun `a sensor with no valid reading is unavailable`() {
    assertEquals(
      Entity.Sensor(EntityId("t"), "T", SensorValue.Unavailable, unit = "°C"),
      getFirstEntity("""{"id":"t","kind":"sensor","name":"T","value":null,"unit":"°C"}"""),
    )
  }

  @Test
  fun `a binary sensor and a switch may have no valid reading`() {
    assertEquals(Entity.BinarySensor(EntityId("b"), "B", value = null), getFirstEntity("""{"id":"b","kind":"binary_sensor","name":"B","value":null}"""))
    assertEquals(
      Entity.Switch(EntityId("s"), "S", on = null, commandable = true),
      getFirstEntity("""{"id":"s","kind":"switch","name":"S","value":null,"commandable":true}"""),
    )
  }

  @Test
  fun `an entity that does not say it is commandable is not`() {
    val entity = getFirstEntity("""{"id":"s","kind":"switch","name":"S","value":true}""") as Entity.Switch
    assertEquals(false, entity.commandable)
  }

  @Test
  fun `an entity of a kind the app does not know is left out and the others stay`() {
    reply(
      200,
      homeWith(
        """{"id":"x","kind":"camera","name":"X","value":"?"}""",
        """{"id":"t","kind":"sensor","name":"T","value":1}""",
      ),
    )
    val entities = runBlocking { client.getHome() }.value().areas.single().devices.single().entities
    assertEquals(listOf(EntityId("t")), entities.map { it.id })
  }

  @Test
  fun `a value that does not fit the kind is no valid reading`() {
    assertEquals(
      Entity.BinarySensor(EntityId("b"), "B", value = null),
      getFirstEntity("""{"id":"b","kind":"binary_sensor","name":"B","value":"yes"}"""),
    )
  }

  @Test
  fun `fields the app does not know are ignored`() {
    reply(200, homeWith("""{"id":"t","kind":"sensor","name":"T","value":1,"colour":"red"}""").replace("\"uptimeMs\"", "\"extra\":true,\"uptimeMs\""))
    assertNotNull(runBlocking { client.getHome() }.value())
  }

  @Test
  fun `a home without a boot id is not understood`() {
    reply(200, """{"apiVersion":1,"uptimeMs":1,"areas":[]}""")
    assertTrue(runBlocking { client.getHome() }.error() is BoardError.Unexpected)
  }

  @Test
  fun `a home of another version of the contract is not understood`() {
    reply(200, homeWith(apiVersion = 2))
    assertTrue(runBlocking { client.getHome() }.error() is BoardError.Unexpected)
  }

  @Test
  fun `an answer that is not JSON is not understood`() {
    reply(200, "<html>not the board</html>")
    assertTrue(runBlocking { client.getHome() }.error() is BoardError.Unexpected)
  }

  // ---- errors ---------------------------------------------------------------------

  @Test
  fun `every error code of the contract is recognised`() {
    val expected =
      mapOf(
        "invalid-body" to ErrorCode.InvalidBody,
        "invalid-value" to ErrorCode.InvalidValue,
        "invalid-query" to ErrorCode.InvalidQuery,
        "not-commandable" to ErrorCode.NotCommandable,
        "not-found" to ErrorCode.NotFound,
        "method-not-allowed" to ErrorCode.MethodNotAllowed,
        "not-acceptable" to ErrorCode.NotAcceptable,
        "too-many-clients" to ErrorCode.TooManyClients,
      )
    for ((wire, code) in expected) {
      error(400, wire)
      val failure = runBlocking { client.getHome() }.error() as BoardError.Rejected
      assertEquals(wire, code, failure.code)
    }
  }

  @Test
  fun `a rejection carries the status and the message of the board`() {
    error(403, "not-commandable")
    val failure = runBlocking { client.getHome() }.error() as BoardError.Rejected
    assertEquals(403, failure.status)
    assertEquals("a message from the board", failure.message)
  }

  @Test
  fun `a code the app does not know is an unknown rejection, not a crash`() {
    error(409, "something-new")
    val failure = runBlocking { client.getHome() }.error() as BoardError.Rejected
    assertEquals(ErrorCode.Unknown, failure.code)
    assertEquals(409, failure.status)
  }

  @Test
  fun `an error that does not have the format of the contract is not understood`() {
    reply(500, "Internal Server Error")
    assertTrue(runBlocking { client.getHome() }.error() is BoardError.Unexpected)
  }

  @Test
  fun `a board that cannot be reached is unreachable`() {
    server.close()
    assertTrue(runBlocking { client.getHome() }.error() is BoardError.Unreachable)
  }

  @Test
  fun `a board that does not answer in time is unreachable`() {
    server.enqueue(MockResponse.Builder().code(200).body(homeWith()).headersDelay(2, TimeUnit.SECONDS).build())
    val patient = BoardClient(server.url("/"), http.newBuilder().readTimeout(300, TimeUnit.MILLISECONDS).build())
    assertTrue(runBlocking { patient.getHome() }.error() is BoardError.Unreachable)
  }

  @Test
  fun `cancelling the coroutine cancels the call to the board`() {
    server.enqueue(MockResponse.Builder().code(200).body(homeWith()).headersDelay(10, TimeUnit.SECONDS).build())
    runBlocking {
      // On another thread: this one blocks below, waiting for the server.
      val job = launch(Dispatchers.Default) { client.getHome() }
      assertNotNull("the request never reached the server", server.takeRequest(3, TimeUnit.SECONDS))
      job.cancel()
      job.join()
      val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
      while (http.dispatcher.runningCallsCount() > 0 && System.nanoTime() < deadline) Thread.sleep(20)
      assertEquals(0, http.dispatcher.runningCallsCount())
    }
  }

  // ---- PUT /api/v1/entities/{id}/value ---------------------------------------------

  @Test
  fun `turning a switch on is a PUT with the value in a JSON body`() {
    reply(200, """{"id":"dehumidifier-power","kind":"switch","name":"Power","value":true,"commandable":true}""")
    runBlocking { client.sendCommand(EntityId("dehumidifier-power"), CommandValue.Flag(true)) }
    val request = server.takeRequest(1, TimeUnit.SECONDS)!!
    assertEquals("PUT", request.method)
    assertEquals("/api/v1/entities/dehumidifier-power/value", request.url.encodedPath)
    assertEquals("""{"value":true}""", request.body!!.utf8())
    assertTrue(request.headers["Content-Type"]!!.startsWith("application/json"))
  }

  @Test
  fun `choosing an option sends the option as text`() {
    reply(200, """{"id":"dehumidifier-mode","kind":"select","name":"Mode","value":"manual","options":["automatic","manual"],"commandable":true}""")
    runBlocking { client.sendCommand(EntityId("dehumidifier-mode"), CommandValue.Choice("manual")) }
    assertEquals("""{"value":"manual"}""", server.takeRequest(1, TimeUnit.SECONDS)!!.body!!.utf8())
  }

  @Test
  fun `the answer to a command is the entity as it ended up`() {
    reply(200, """{"id":"dehumidifier-power","kind":"switch","name":"Power","value":true,"commandable":true}""")
    val entity = runBlocking { client.sendCommand(EntityId("dehumidifier-power"), CommandValue.Flag(true)) }.value()
    assertEquals(Entity.Switch(EntityId("dehumidifier-power"), "Power", on = true, commandable = true), entity)
  }

  @Test
  fun `a command the board refuses is a rejection`() {
    error(403, "not-commandable")
    val failure = runBlocking { client.sendCommand(EntityId("valve-open"), CommandValue.Flag(true)) }.error() as BoardError.Rejected
    assertEquals(ErrorCode.NotCommandable, failure.code)
  }

  @Test
  fun `an invalid value is a rejection`() {
    error(400, "invalid-value")
    val failure = runBlocking { client.sendCommand(EntityId("dehumidifier-mode"), CommandValue.Choice("turbo")) }.error() as BoardError.Rejected
    assertEquals(ErrorCode.InvalidValue, failure.code)
  }

  @Test
  fun `a command answered with an entity the app does not know is not understood`() {
    reply(200, """{"id":"x","kind":"camera","name":"X","value":1}""")
    assertTrue(runBlocking { client.sendCommand(EntityId("x"), CommandValue.Flag(true)) }.error() is BoardError.Unexpected)
  }

  @Test
  fun `an id is put in the path as one segment, whatever it contains`() {
    reply(404, """{"error":{"code":"not-found","message":"x"}}""")
    runBlocking { client.sendCommand(EntityId("a/b c"), CommandValue.Flag(true)) }
    assertEquals("/api/v1/entities/a%2Fb%20c/value", server.takeRequest(1, TimeUnit.SECONDS)!!.url.encodedPath)
  }

  // ---- GET /api/v1/history -----------------------------------------------------------

  @Test
  fun `the history without filters has no query`() {
    reply(200, """{"bootId":"b1","samples":[]}""")
    runBlocking { client.getHistory() }
    val request = server.takeRequest(1, TimeUnit.SECONDS)!!
    assertEquals("/api/v1/history", request.url.encodedPath)
    assertNull(request.url.encodedQuery)
  }

  @Test
  fun `after and limit go in the query`() {
    reply(200, """{"bootId":"b1","samples":[]}""")
    runBlocking { client.getHistory(after = 170_000, limit = 50) }
    val url = server.takeRequest(1, TimeUnit.SECONDS)!!.url
    assertEquals("170000", url.queryParameter("after"))
    assertEquals("50", url.queryParameter("limit"))
  }

  @Test
  fun `only the filter that was given goes in the query`() {
    reply(200, """{"bootId":"b1","samples":[]}""")
    runBlocking { client.getHistory(limit = 10) }
    val url = server.takeRequest(1, TimeUnit.SECONDS)!!.url
    assertNull(url.queryParameter("after"))
    assertEquals("10", url.queryParameter("limit"))
  }

  @Test
  fun `the samples keep their order and their values, with null for no reading`() {
    reply(
      200,
      """{"bootId":"5f3a9c21","samples":[
        {"uptimeMs":160000,"values":{"gas-level":142,"temperature":21.4,"humidity":74.1}},
        {"uptimeMs":170000,"values":{"gas-level":140,"temperature":null,"humidity":null}}]}""",
    )
    val page = runBlocking { client.getHistory() }.value()
    assertEquals(BootId("5f3a9c21"), page.bootId)
    assertEquals(listOf(160_000L, 170_000L), page.samples.map { it.uptimeMs })
    assertEquals(
      mapOf(EntityId("gas-level") to 142.0, EntityId("temperature") to 21.4, EntityId("humidity") to 74.1),
      page.samples[0].values,
    )
    assertEquals(mapOf(EntityId("gas-level") to 140.0, EntityId("temperature") to null, EntityId("humidity") to null), page.samples[1].values)
  }

  @Test
  fun `an empty history is an empty list`() {
    reply(200, """{"bootId":"b1","samples":[]}""")
    assertEquals(emptyList<Any>(), runBlocking { client.getHistory() }.value().samples)
  }

  @Test
  fun `a bad history query is a rejection`() {
    error(400, "invalid-query")
    val failure = runBlocking { client.getHistory(limit = 0) }.error() as BoardError.Rejected
    assertEquals(ErrorCode.InvalidQuery, failure.code)
  }

  // ---- the thread that calls ---------------------------------------------------------

  @Test
  fun `a call does not hold the thread that made it while the board takes its time to send the body`() {
    // The headers come at once and the body 600 ms later: reading it is a wait on the network.
    server.enqueue(
      MockResponse.Builder().addHeader("Content-Type", "application/json").bodyDelay(600, TimeUnit.MILLISECONDS).body(CONTRACT_HOME).build(),
    )
    val caller = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    try {
      val ticks = AtomicInteger()
      runBlocking {
        val call = async(caller) { client.getHome() }
        // Another job on the same single thread: it can only run if the call has let go of the thread.
        val ticker = launch(caller) { while (isActive) { ticks.incrementAndGet(); delay(20) } }
        call.await().value()
        ticker.cancelAndJoin()
      }
      assertTrue("the thread of the caller was held by the call ($ticks ticks in 600 ms)", ticks.get() >= 5)
    } finally {
      caller.close()
    }
  }

  private companion object {
    // The example of docs/api.md, as the board sends it.
    const val CONTRACT_HOME = """{
      "apiVersion": 1, "bootId": "5f3a9c21", "uptimeMs": 183450,
      "areas": [
        {"id": "kitchen", "name": "Kitchen", "devices": [
          {"id": "gas-sensor", "name": "Gas sensor", "entities": [
            {"id": "gas-level", "kind": "sensor", "name": "Gas level", "value": 142, "unit": "adc"},
            {"id": "gas-state", "kind": "sensor", "name": "Gas state", "value": "normal"},
            {"id": "gas-alerts", "kind": "sensor", "name": "Gas alerts since boot", "value": 0}]},
          {"id": "gas-valve", "name": "Gas valve", "entities": [
            {"id": "valve-open", "kind": "binary_sensor", "name": "Valve open", "value": true}]}]},
        {"id": "bedroom", "name": "Bedroom", "devices": [
          {"id": "closet-sensor", "name": "Closet sensor", "entities": [
            {"id": "temperature", "kind": "sensor", "name": "Temperature", "value": 21.4, "unit": "°C"},
            {"id": "humidity", "kind": "sensor", "name": "Humidity", "value": 74.1, "unit": "%"}]},
          {"id": "dehumidifier", "name": "Dehumidifier", "entities": [
            {"id": "dehumidifier-power", "kind": "switch", "name": "Power", "value": false, "commandable": true},
            {"id": "dehumidifier-mode", "kind": "select", "name": "Mode", "value": "automatic",
             "options": ["automatic", "manual"], "commandable": true}]}]}]}"""
  }
}
