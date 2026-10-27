package io.github.gustavommcv.ciscsmarthome.data.remote

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackoffTest {
  @Test
  fun `the first wait is the initial one and each next wait doubles`() {
    val backoff = Backoff(initial = 1.seconds, max = 60.seconds)
    assertEquals(listOf(1, 2, 4, 8, 16), (0..4).map { backoff.delayFor(it).inWholeSeconds.toInt() })
  }

  @Test
  fun `the wait never goes past the maximum`() {
    val backoff = Backoff(initial = 1.seconds, max = 10.seconds)
    assertEquals(listOf(8, 10, 10), (3..5).map { backoff.delayFor(it).inWholeSeconds.toInt() })
  }

  @Test
  fun `a very long run of failures still gives the maximum`() {
    val backoff = Backoff(initial = 1.seconds, max = 10.seconds)
    assertEquals(10.seconds, backoff.delayFor(1_000))
    assertEquals(10.seconds, backoff.delayFor(Int.MAX_VALUE))
  }

  @Test
  fun `the defaults suit a board that restarts in a few seconds`() {
    val backoff = Backoff()
    assertEquals(1.seconds, backoff.delayFor(0))
    assertEquals(10.seconds, backoff.delayFor(10))
  }

  @Test
  fun `a maximum equal to the initial wait is a fixed wait`() {
    val backoff = Backoff(initial = 250.milliseconds, max = 250.milliseconds)
    assertEquals(listOf(250L, 250L, 250L), (0..2).map { backoff.delayFor(it).inWholeMilliseconds })
  }

  @Test
  fun `a wait that is not positive, or a maximum below the initial wait, is refused`() {
    assertThrows(IllegalArgumentException::class.java) { Backoff(initial = 0.seconds, max = 1.seconds) }
    assertThrows(IllegalArgumentException::class.java) { Backoff(initial = 2.seconds, max = 1.seconds) }
  }

  @Test
  fun `a negative attempt is refused`() {
    assertThrows(IllegalArgumentException::class.java) { Backoff().delayFor(-1) }
  }
}
