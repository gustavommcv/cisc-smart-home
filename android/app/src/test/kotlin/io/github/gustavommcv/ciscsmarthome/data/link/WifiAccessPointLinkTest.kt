package io.github.gustavommcv.ciscsmarthome.data.link

import io.github.gustavommcv.ciscsmarthome.data.remote.Backoff
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionProblem
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/** The link that makes the phone join the Wi-Fi of the board: what it asks of the phone, and what route it says in return. */
@OptIn(ExperimentalCoroutinesApi::class)
class WifiAccessPointLinkTest {
  private val joiner = FakeNetworkJoiner()
  private val password = MutableStateFlow<String?>(null)
  private val plain = OkHttpClient()
  private val throughTheNetwork = OkHttpClient.Builder().build()
  private val backoff = Backoff(initial = 2.seconds, max = 8.seconds)
  private val link = WifiAccessPointLink(joiner, SSID, ADDRESS, plain, password, backoff)

  private val available = JoinEvent.Available { throughTheNetwork }

  private fun TestScope.keepUp(): Job {
    val job = backgroundScope.launch { link.keepUp() }
    runCurrent()
    return job
  }

  private fun TestScope.say(event: JoinEvent) {
    joiner.send(event)
    runCurrent()
  }

  private fun TestScope.pass(time: Duration) {
    advanceTimeBy(time)
    runCurrent()
  }

  @Test
  fun `without a password the route is closed for the lack of it, and the phone is asked nothing`() = runTest {
    keepUp()
    assertEquals(BoardRoute.Closed(ConnectionProblem.NetworkNotSet), link.route.value)
    assertEquals(emptyList<Pair<String, String>>(), joiner.requests)
  }

  @Test
  fun `with a password the phone is asked to join the network of the board by its name and password`() = runTest {
    password.value = "secret-pass"
    keepUp()
    assertEquals(listOf(SSID to "secret-pass"), joiner.requests)
    assertEquals(BoardRoute.Joining, link.route.value)
  }

  @Test
  fun `when the phone joins, the route opens on the address of the board, through that network`() = runTest {
    password.value = "secret-pass"
    keepUp()
    say(available)
    val route = link.route.value as BoardRoute.Open
    assertEquals(ADDRESS, route.baseUrl)
    assertSame(throughTheNetwork, route.http)
  }

  @Test
  fun `the client of the app is handed to the network, to be carried through it`() = runTest {
    password.value = "secret-pass"
    keepUp()
    var handed: OkHttpClient? = null
    say(JoinEvent.Available { client -> client.also { handed = it }; throughTheNetwork })
    assertSame(plain, handed)
  }

  @Test
  fun `a network that is lost closes the route as not joined, and the request is let go`() = runTest {
    password.value = "secret-pass"
    keepUp()
    say(available)
    say(JoinEvent.Lost)
    assertEquals(BoardRoute.Closed(ConnectionProblem.NetworkNotJoined), link.route.value)
    assertEquals(0, joiner.active)
  }

  @Test
  fun `a network that is lost is asked for again after a wait, for the phone does not bring it back by itself`() = runTest {
    password.value = "secret-pass"
    keepUp()
    say(available)
    say(JoinEvent.Lost)
    pass(1_999.milliseconds)
    assertEquals(1, joiner.requests.size)
    pass(1.milliseconds)
    assertEquals(listOf(SSID to "secret-pass", SSID to "secret-pass"), joiner.requests)
    assertEquals(1, joiner.active)
  }

  @Test
  fun `a network that comes back after it was lost opens the route again`() = runTest {
    password.value = "secret-pass"
    keepUp()
    say(available)
    say(JoinEvent.Lost)
    pass(2.seconds)
    say(available)
    assertEquals(ADDRESS, (link.route.value as BoardRoute.Open).baseUrl)
  }

  @Test
  fun `a network that keeps being lost is asked for with the waits of any other failure`() = runTest {
    password.value = "secret-pass"
    keepUp()
    say(JoinEvent.Lost)
    pass(2.seconds)
    say(JoinEvent.Lost)
    pass(3_999.milliseconds)
    assertEquals(2, joiner.requests.size)
    pass(1.milliseconds)
    assertEquals(3, joiner.requests.size)
  }

  @Test
  fun `a request the phone gives up on closes the route as not joined`() = runTest {
    password.value = "secret-pass"
    keepUp()
    say(JoinEvent.Unavailable)
    assertEquals(BoardRoute.Closed(ConnectionProblem.NetworkNotJoined), link.route.value)
  }

  @Test
  fun `a request the phone gave up on is made again after a wait, and only then`() = runTest {
    password.value = "secret-pass"
    keepUp()
    say(JoinEvent.Unavailable)
    pass(1_999.milliseconds)
    assertEquals(1, joiner.requests.size)
    pass(1.milliseconds)
    assertEquals(2, joiner.requests.size)
    assertEquals(SSID to "secret-pass", joiner.requests.last())
  }

  @Test
  fun `the route stays closed while the next request is made, for the user has nothing new to be told`() = runTest {
    password.value = "secret-pass"
    keepUp()
    say(JoinEvent.Unavailable)
    pass(2.seconds)
    assertEquals(BoardRoute.Closed(ConnectionProblem.NetworkNotJoined), link.route.value)
  }

  @Test
  fun `each failure in a row waits longer, up to the longest wait`() = runTest {
    password.value = "secret-pass"
    keepUp()
    for ((failure, wait) in listOf(1 to 2, 2 to 4, 3 to 8, 4 to 8, 5 to 8)) {
      say(JoinEvent.Unavailable)
      pass((wait * 1000 - 1).milliseconds)
      assertEquals("before the wait after failure $failure", failure, joiner.requests.size)
      pass(1.milliseconds)
      assertEquals("after the wait after failure $failure", failure + 1, joiner.requests.size)
    }
  }

  @Test
  fun `joining makes the waits start over`() = runTest {
    password.value = "secret-pass"
    keepUp()
    say(JoinEvent.Unavailable)
    pass(2.seconds)
    say(JoinEvent.Unavailable)
    pass(4.seconds)
    say(available)
    say(JoinEvent.Unavailable)
    pass(2.seconds)
    assertEquals(4, joiner.requests.size)
  }

  @Test
  fun `a request that failed is let go before the next is made, so the phone is never asked for two`() = runTest {
    password.value = "secret-pass"
    keepUp()
    say(JoinEvent.Unavailable)
    pass(2.seconds)
    say(JoinEvent.Unavailable)
    pass(4.seconds)
    assertEquals(1, joiner.mostActive)
    assertEquals(1, joiner.active)
  }

  @Test
  fun `a new password replaces the request, and the route waits for it`() = runTest {
    password.value = "first-pass"
    keepUp()
    say(available)
    password.value = "second-pass"
    runCurrent()
    assertEquals(listOf(SSID to "first-pass", SSID to "second-pass"), joiner.requests)
    assertEquals(1, joiner.active)
    assertEquals(BoardRoute.Joining, link.route.value)
  }

  @Test
  fun `a new password does not wait for the end of the wait of the old one`() = runTest {
    password.value = "first-pass"
    keepUp()
    say(JoinEvent.Unavailable)
    pass(1.seconds)
    password.value = "second-pass"
    runCurrent()
    assertEquals(listOf(SSID to "first-pass", SSID to "second-pass"), joiner.requests)
    pass(10.seconds)
    assertEquals(2, joiner.requests.size)
  }

  @Test
  fun `taking the password away lets the request go and closes the route as not set`() = runTest {
    password.value = "secret-pass"
    keepUp()
    say(available)
    password.value = null
    runCurrent()
    assertEquals(0, joiner.active)
    assertEquals(BoardRoute.Closed(ConnectionProblem.NetworkNotSet), link.route.value)
  }

  @Test
  fun `a password that comes after the start makes the request then`() = runTest {
    keepUp()
    password.value = "secret-pass"
    runCurrent()
    assertEquals(listOf(SSID to "secret-pass"), joiner.requests)
  }

  @Test
  fun `stopping the link lets the request go`() = runTest {
    password.value = "secret-pass"
    val job = keepUp()
    say(available)
    job.cancel()
    runCurrent()
    assertEquals(0, joiner.active)
  }

  @Test
  fun `stopping the link while it waits to try again ends the wait`() = runTest {
    password.value = "secret-pass"
    val job = keepUp()
    say(JoinEvent.Unavailable)
    job.cancel()
    pass(1.seconds * 60)
    assertEquals(1, joiner.requests.size)
  }

  @Test
  fun `by default it waits 5 seconds after the first failure, and 30 at most`() {
    assertEquals(5.seconds, WifiAccessPointLink.DEFAULT_BACKOFF.initial)
    assertEquals(30.seconds, WifiAccessPointLink.DEFAULT_BACKOFF.max)
  }

  private companion object {
    const val SSID = "cisc-smart-home"
    val ADDRESS = "http://192.168.4.1/".toHttpUrl()
  }
}
