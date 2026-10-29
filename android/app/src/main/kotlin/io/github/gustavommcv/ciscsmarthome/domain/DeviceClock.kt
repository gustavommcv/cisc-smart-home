package io.github.gustavommcv.ciscsmarthome.domain

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Turns the board's uptime into wall-clock time. The board keeps no clock: every response says how
 * long it has been running, and the phone notes what time it was when the response arrived. That
 * difference, kept per boot, places any later or earlier uptime on the phone's clock (see "Tempo"
 * in docs/api.md). The error is the time the response took, a fraction of a second.
 */
class DeviceClock {
  // The events write it and the job that keeps the history up to date reads it, each from its own coroutine.
  private val offsetsMs = ConcurrentHashMap<BootId, Long>()

  /** Notes that a response carrying [uptimeMs] arrived at [receivedAt]. The latest response wins. */
  fun observe(bootId: BootId, uptimeMs: Long, receivedAt: Instant) {
    offsetsMs[bootId] = receivedAt.toEpochMilli() - uptimeMs
  }

  /** When the board had been up for [uptimeMs], or null if this boot has never been heard from. */
  fun toInstant(bootId: BootId, uptimeMs: Long): Instant? =
    offsetsMs[bootId]?.let { Instant.ofEpochMilli(it + uptimeMs) }
}
