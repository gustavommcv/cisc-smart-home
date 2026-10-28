package io.github.gustavommcv.ciscsmarthome.data.link

import io.github.gustavommcv.ciscsmarthome.domain.ConnectionProblem
import io.github.gustavommcv.ciscsmarthome.domain.LinkKind
import io.github.gustavommcv.ciscsmarthome.testing.FakeLinkSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The link that is the one the user chose, and changes to the next one the moment the user chooses it. */
@OptIn(ExperimentalCoroutinesApi::class)
class ChosenLinkTest {
  private val settings = FakeLinkSettings(LinkKind.BoardWifi)
  private val wifi = FakeBoardLink(BoardRoute.Closed(ConnectionProblem.NetworkNotSet))
  private val current = FakeBoardLink(BoardRoute.Open("http://192.168.4.1/".toHttpUrl(), OkHttpClient()))
  private val built = mutableListOf<LinkKind>()
  private val chosen =
    ChosenLink(settings) { kind ->
      built += kind
      when (kind) {
        LinkKind.BoardWifi -> wifi
        LinkKind.CurrentNetwork -> current
      }
    }

  private fun TestScope.keepUp(): Job {
    val job = backgroundScope.launch { chosen.keepUp() }
    runCurrent()
    return job
  }

  @Test
  fun `before it is kept up the route waits`() {
    assertEquals(BoardRoute.Joining, chosen.route.value)
  }

  @Test
  fun `it keeps up the link the user chose, and says its route`() = runTest {
    keepUp()
    assertEquals(listOf(LinkKind.BoardWifi), built)
    assertTrue(wifi.running)
    assertFalse(current.running)
    assertEquals(BoardRoute.Closed(ConnectionProblem.NetworkNotSet), chosen.route.value)
  }

  @Test
  fun `when the link changes its route, so does this one`() = runTest {
    keepUp()
    wifi.go(BoardRoute.Joining)
    runCurrent()
    assertEquals(BoardRoute.Joining, chosen.route.value)
  }

  @Test
  fun `choosing another link stops the old one and keeps up the new one`() = runTest {
    keepUp()
    settings.choose(LinkKind.CurrentNetwork)
    runCurrent()
    assertFalse(wifi.running)
    assertTrue(current.running)
    assertEquals(listOf(LinkKind.BoardWifi, LinkKind.CurrentNetwork), built)
  }

  @Test
  fun `after a change the route is the new link's, not the old one's`() = runTest {
    keepUp()
    settings.choose(LinkKind.CurrentNetwork)
    runCurrent()
    assertEquals(current.route.value, chosen.route.value)
    wifi.go(BoardRoute.Joining)
    runCurrent()
    assertEquals(current.route.value, chosen.route.value)
  }

  @Test
  fun `a change makes the route wait, until the new link says its own`() = runTest {
    val seen = mutableListOf<BoardRoute>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { chosen.route.toList(seen) }
    keepUp()
    seen.clear()
    settings.choose(LinkKind.CurrentNetwork)
    runCurrent()
    assertEquals(listOf(BoardRoute.Joining, current.route.value), seen)
  }

  @Test
  fun `going back builds the link again, and keeps it up`() = runTest {
    keepUp()
    settings.choose(LinkKind.CurrentNetwork)
    runCurrent()
    settings.choose(LinkKind.BoardWifi)
    runCurrent()
    assertTrue(wifi.running)
    assertFalse(current.running)
    assertEquals(2, wifi.started)
  }

  @Test
  fun `stopping it stops the link in use`() = runTest {
    val job = keepUp()
    job.cancel()
    runCurrent()
    assertFalse(wifi.running)
  }
}
