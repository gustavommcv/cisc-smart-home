package io.github.gustavommcv.ciscsmarthome.service

import io.github.gustavommcv.ciscsmarthome.domain.ConnectionProblem
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionState
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Saying what the connection is, and warning when the board stays out of reach, against a notifier that writes things down. */
@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionWatchTest {
  private val notifier = FakeNotifier()
  private val connection = MutableStateFlow<ConnectionState>(ConnectionState.Connecting)
  private val unreachable = ConnectionState.Disconnected(ConnectionProblem.Unreachable)
  private val crowded = ConnectionState.Disconnected(ConnectionProblem.TooManyClients)

  /** Starts watching the way the service does: in the background, for as long as the test lasts. */
  private fun TestScope.watch(): Job {
    val job = backgroundScope.launch { ConnectionWatch(notifier).watch(connection) }
    runCurrent()
    return job
  }

  private fun TestScope.pass(time: kotlin.time.Duration) {
    advanceTimeBy(time)
    runCurrent()
  }

  @Test
  fun `it says what the connection is, and again each time it changes`() = runTest {
    watch()
    connection.value = ConnectionState.Connected
    runCurrent()
    connection.value = unreachable
    runCurrent()
    assertEquals(listOf(ConnectionState.Connecting, ConnectionState.Connected, unreachable), notifier.connections)
  }

  @Test
  fun `a board that stays out of reach for a minute is a house nobody is watching`() = runTest {
    watch()
    connection.value = unreachable
    runCurrent()
    pass(59_999.milliseconds)
    assertEquals(emptyList<ConnectionState.Disconnected>(), notifier.boardLost)
    pass(1.milliseconds)
    assertEquals(listOf(unreachable), notifier.boardLost)
  }

  @Test
  fun `the warning is given once, however long the board stays away`() = runTest {
    watch()
    connection.value = unreachable
    runCurrent()
    pass(10.minutes)
    assertEquals(1, notifier.boardLost.size)
  }

  @Test
  fun `a board that comes back in time is not warned about`() = runTest {
    watch()
    connection.value = unreachable
    runCurrent()
    pass(40.seconds)
    connection.value = ConnectionState.Connected
    runCurrent()
    pass(5.minutes)
    assertEquals(emptyList<ConnectionState.Disconnected>(), notifier.boardLost)
  }

  @Test
  fun `a board that goes away again starts the minute again`() = runTest {
    watch()
    connection.value = unreachable
    runCurrent()
    pass(40.seconds)
    connection.value = ConnectionState.Connected
    runCurrent()
    pass(5.seconds)
    connection.value = unreachable
    runCurrent()
    // 70 s since the first time it went away, but only 25 s since the last time it did.
    pass(25.seconds)
    assertEquals(emptyList<ConnectionState.Disconnected>(), notifier.boardLost)
    pass(35.seconds)
    assertEquals(1, notifier.boardLost.size)
  }

  @Test
  fun `a new reason starts the minute again, and the warning gives the latest one`() = runTest {
    watch()
    connection.value = unreachable
    runCurrent()
    pass(40.seconds)
    connection.value = crowded
    runCurrent()
    // 61 s since it went away, but only 21 s since the reason changed.
    pass(21.seconds)
    assertEquals(emptyList<ConnectionState.Disconnected>(), notifier.boardLost)
    pass(39.seconds)
    assertEquals(listOf(crowded), notifier.boardLost)
  }

  @Test
  fun `the warning is taken down when the board is heard again`() = runTest {
    watch()
    val clearedAtStart = notifier.boardLostCleared
    connection.value = unreachable
    runCurrent()
    pass(61.seconds)
    assertEquals(clearedAtStart, notifier.boardLostCleared)
    connection.value = ConnectionState.Connected
    runCurrent()
    assertEquals(clearedAtStart + 1, notifier.boardLostCleared)
  }

  @Test
  fun `a connection that is not lost takes the warning down, whatever else it is`() = runTest {
    watch()
    val clearedAtStart = notifier.boardLostCleared
    connection.value = ConnectionState.Connected
    runCurrent()
    assertEquals(clearedAtStart + 1, notifier.boardLostCleared)
  }

  @Test
  fun `stopping the watch takes the countdown with it`() = runTest {
    val watching = watch()
    connection.value = unreachable
    runCurrent()
    pass(10.seconds)
    watching.cancel()
    pass(5.minutes)
    assertEquals(emptyList<ConnectionState.Disconnected>(), notifier.boardLost)
  }

  @Test
  fun `a board is lost after one minute, the time the service has always allowed`() {
    assertEquals(1.minutes, ConnectionWatch.BOARD_LOST_AFTER)
  }
}
