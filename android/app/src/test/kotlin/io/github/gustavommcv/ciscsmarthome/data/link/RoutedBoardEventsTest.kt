package io.github.gustavommcv.ciscsmarthome.data.link

import io.github.gustavommcv.ciscsmarthome.data.remote.BoardError
import io.github.gustavommcv.ciscsmarthome.data.remote.BoardStreamUpdate
import io.github.gustavommcv.ciscsmarthome.data.repository.FakeBoardEvents
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionProblem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The events of the board by whatever route the link says: they flow when there is a way, and the lack of one is told as a loss. */
@OptIn(ExperimentalCoroutinesApi::class)
class RoutedBoardEventsTest {
  private val link = FakeBoardLink()
  private val streams = mutableMapOf<BoardRoute.Open, FakeBoardEvents>()
  private val events = RoutedBoardEvents(link) { route -> streams.getOrPut(route) { FakeBoardEvents() } }

  private val first = BoardRoute.Open("http://192.168.4.1/".toHttpUrl(), OkHttpClient())
  private val second = BoardRoute.Open("http://10.0.0.5/".toHttpUrl(), OkHttpClient())
  private val updates = mutableListOf<BoardStreamUpdate>()

  private fun TestScope.collect(): Job {
    val job = backgroundScope.launch { events.updates().collect { updates += it } }
    runCurrent()
    return job
  }

  private fun lostFor(problem: ConnectionProblem) = BoardStreamUpdate.Lost(BoardError.NoRoute(problem))

  @Test
  fun `while it is collected the link is kept up`() = runTest {
    val job = collect()
    assertTrue(link.running)
    job.cancel()
    runCurrent()
    assertFalse(link.running)
  }

  @Test
  fun `while the link is joining there is nothing to tell`() = runTest {
    collect()
    assertEquals(emptyList<BoardStreamUpdate>(), updates)
  }

  @Test
  fun `what the stream of an open route tells is passed on`() = runTest {
    link.go(first)
    collect()
    streams.getValue(first).feed.trySend(BoardStreamUpdate.Connected)
    runCurrent()
    assertEquals(listOf<BoardStreamUpdate>(BoardStreamUpdate.Connected), updates)
  }

  @Test
  fun `a closed route is told as a loss, with its reason`() = runTest {
    link.go(BoardRoute.Closed(ConnectionProblem.NetworkNotSet))
    collect()
    assertEquals(listOf<BoardStreamUpdate>(lostFor(ConnectionProblem.NetworkNotSet)), updates)
  }

  @Test
  fun `a route that closes after it was open is a loss from then on`() = runTest {
    link.go(first)
    collect()
    streams.getValue(first).feed.trySend(BoardStreamUpdate.Connected)
    runCurrent()
    link.go(BoardRoute.Closed(ConnectionProblem.NetworkNotJoined))
    runCurrent()
    assertEquals(listOf(BoardStreamUpdate.Connected, lostFor(ConnectionProblem.NetworkNotJoined)), updates)
  }

  @Test
  fun `a route that opens after it was closed brings the stream of the board`() = runTest {
    link.go(BoardRoute.Closed(ConnectionProblem.NetworkNotJoined))
    collect()
    link.go(first)
    runCurrent()
    streams.getValue(first).feed.trySend(BoardStreamUpdate.Connected)
    runCurrent()
    assertEquals(listOf(lostFor(ConnectionProblem.NetworkNotJoined), BoardStreamUpdate.Connected), updates)
  }

  @Test
  fun `a route that changes to another open one leaves the old stream and follows the new`() = runTest {
    link.go(first)
    collect()
    val old = streams.getValue(first)
    link.go(second)
    runCurrent()
    old.feed.trySend(BoardStreamUpdate.Connected)
    streams.getValue(second).feed.trySend(BoardStreamUpdate.Connected)
    runCurrent()
    assertEquals(listOf<BoardStreamUpdate>(BoardStreamUpdate.Connected), updates)
  }

  @Test
  fun `the same route again does not open the stream again`() = runTest {
    link.go(first)
    collect()
    link.go(first)
    runCurrent()
    assertEquals(1, streams.size)
  }
}
