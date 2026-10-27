package io.github.gustavommcv.ciscsmarthome.data.remote

import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * How long to wait before trying again: [initial] after the first failure, twice as long after each
 * one that follows, and never more than [max]. There is one board and one phone, so nothing is gained
 * by spreading the attempts out at random.
 */
class Backoff(val initial: Duration = 1.seconds, val max: Duration = 10.seconds) {
  init {
    require(initial.isPositive()) { "the first wait must be positive" }
    require(max >= initial) { "the longest wait cannot be shorter than the first" }
  }

  /** The wait after [attempt] failures in a row, counting from 0 for the first one. */
  fun delayFor(attempt: Int): Duration {
    require(attempt >= 0) { "an attempt cannot be negative" }
    return minOf(max, initial * 2.0.pow(attempt.coerceAtMost(MAX_DOUBLINGS)))
  }

  private companion object {
    // Far past any maximum, and keeps the power of two from overflowing.
    const val MAX_DOUBLINGS = 30
  }
}
