package io.github.gustavommcv.ciscsmarthome.domain

import java.time.Instant
import kotlinx.coroutines.flow.Flow

/**
 * A sample the app keeps: what the board read, on which boot and at which uptime (the pair that
 * names it), and when that was on the phone's clock (see [DeviceClock]), which is what a chart needs.
 */
data class StoredSample(
  val bootId: BootId,
  val uptimeMs: Long,
  val at: Instant,
  val values: Map<EntityId, Double?>,
  val states: Map<EntityId, String> = emptyMap(),
)

/** The state an entity was in at one moment, as text: "emergency" for the gas, "true" for a dehumidifier that was on. */
data class StateReading(val at: Instant, val state: String)

/** One reading of one entity; a null [value] means the entity had no valid reading. */
data class Reading(val at: Instant, val value: Double?)

/**
 * The least and the greatest reading of one hour of one entity, which is what the app keeps for good once the readings themselves are let go of.
 * [hour] is the moment the hour starts, on the phone's clock.
 */
data class HourlyReading(val hour: Instant, val lowest: Double, val highest: Double)

/** The history of the board that the app remembers, also after the app is closed. */
interface HistoryStore {
  /** Keeps the samples. One that is already kept (same boot, same uptime) is left as it was. */
  suspend fun save(samples: List<StoredSample>)

  /** The greatest uptime kept for [bootId], or null if none: the board only needs to be asked for what is newer. */
  suspend fun latestUptime(bootId: BootId): Long?

  /** The readings of [entityId] taken at [since] or later, oldest first, sent again when they change. */
  fun series(entityId: EntityId, since: Instant): Flow<List<Reading>>

  /** The states of [entityId] at [since] or later, oldest first, sent again when they change. It is what says *when* the dehumidifier was on. */
  fun states(entityId: EntityId, since: Instant): Flow<List<StateReading>>

  /** The hours of [entityId] that start at [since] or later and before [until], oldest first, sent again when they change. */
  fun hourly(entityId: EntityId, since: Instant, until: Instant): Flow<List<HourlyReading>>

  /** The start of the earliest hour kept, of any entity, or null when there is none; sent again when it changes. It says how far back the history goes. */
  fun firstHour(): Flow<Instant?>

  /** Lets go of the readings, and the states, taken before [before]. What was kept of each hour stays: it is what a month or a year is read from. */
  suspend fun forgetReadingsBefore(before: Instant)
}
