package io.github.gustavommcv.ciscsmarthome.data.link

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The link that does nothing: the phone is already where the board can be reached. */
@OptIn(ExperimentalCoroutinesApi::class)
class DirectLinkTest {
  private val address = "http://192.168.4.1/".toHttpUrl()
  private val http = OkHttpClient()
  private val link = DirectLink(address, http)

  @Test
  fun `the route is open on the address from the start`() {
    assertEquals(BoardRoute.Open(address, http), link.route.value)
  }

  @Test
  fun `it keeps nothing up, and waits until it is stopped`() = runTest {
    val job = backgroundScope.launch { link.keepUp() }
    runCurrent()
    assertTrue(job.isActive)
    job.cancel()
    runCurrent()
    assertTrue(job.isCancelled)
  }
}
