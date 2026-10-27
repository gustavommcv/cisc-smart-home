package io.github.gustavommcv.ciscsmarthome.domain

import java.time.Instant
import kotlinx.coroutines.flow.Flow

/**
 * A sample the app keeps: what the board read, on which boot and at which uptime (the pair that
 * names it), and when that was on the phone's clock (see [DeviceClock]), which is what a chart needs.
 */
data class StoredSample(val bootId: BootId, val uptimeMs: Long, val at: Instant, val values: Map<EntityId, Double?>)

/** One reading of one entity; a null [value] means the entity had no valid reading. */
data class Reading(val at: Instant, val value: Double?)

/** The history of the board that the app remembers, also after the app is closed. */
interface HistoryStore {
  /** Keeps the samples. One that is already kept (same boot, same uptime) is left as it was. */
  suspend fun save(samples: List<StoredSample>)

  /** The greatest uptime kept for [bootId], or null if none: the board only needs to be asked for what is newer. */
  suspend fun latestUptime(bootId: BootId): Long?

  /** The readings of [entityId] taken at [since] or later, oldest first, sent again when they change. */
  fun series(entityId: EntityId, since: Instant): Flow<List<Reading>>
}
