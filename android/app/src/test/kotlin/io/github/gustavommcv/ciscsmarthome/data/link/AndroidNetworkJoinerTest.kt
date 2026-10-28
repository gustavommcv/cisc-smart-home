package io.github.gustavommcv.ciscsmarthome.data.link

import android.content.Context
import android.net.ConnectivityManager
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNetwork

/** What the phone says about the network it was asked to join, told as the link understands it. */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class AndroidNetworkJoinerTest {
  private val connectivity = ApplicationProvider.getApplicationContext<Context>().getSystemService(ConnectivityManager::class.java)
  private val joiner = AndroidNetworkJoiner(connectivity)
  private val callbacks get() = shadowOf(connectivity).networkCallbacks.toList()

  @Test
  fun `a request is made to the phone while the flow is collected, and let go when it ends`() = runTest {
    val job = backgroundScope.launch { joiner.join("cisc-smart-home", "secret-pass").collect {} }
    runCurrent()
    assertEquals(1, callbacks.size)
    job.cancel()
    runCurrent()
    assertEquals(0, callbacks.size)
  }

  @Test
  fun `what the phone says is told as it happened`() = runTest {
    val events = mutableListOf<JoinEvent>()
    backgroundScope.launch { joiner.join("cisc-smart-home", "secret-pass").collect { events += it } }
    runCurrent()
    val callback = callbacks.single()
    val network = ShadowNetwork.newInstance(1)
    callback.onAvailable(network)
    callback.onLost(network)
    callback.onUnavailable()
    runCurrent()
    assertEquals(3, events.size)
    assertTrue(events[0] is JoinEvent.Available)
    assertEquals(JoinEvent.Lost, events[1])
    assertEquals(JoinEvent.Unavailable, events[2])
  }

  @Test
  fun `the client that goes through the network is another one, whose connections are bound to it`() = runTest {
    val events = mutableListOf<JoinEvent>()
    backgroundScope.launch { joiner.join("cisc-smart-home", "secret-pass").collect { events += it } }
    runCurrent()
    callbacks.single().onAvailable(ShadowNetwork.newInstance(1))
    runCurrent()
    val plain = OkHttpClient()
    val carried = (events.single() as JoinEvent.Available).network.carry(plain)
    assertNotSame(plain, carried)
    assertNotSame(plain.socketFactory, carried.socketFactory)
    assertEquals(plain.connectTimeoutMillis, carried.connectTimeoutMillis)
  }

  @Test
  fun `the phone gives up on a request after 40 seconds`() {
    assertEquals(40, AndroidNetworkJoiner.JOIN_TIMEOUT.inWholeSeconds)
  }
}
