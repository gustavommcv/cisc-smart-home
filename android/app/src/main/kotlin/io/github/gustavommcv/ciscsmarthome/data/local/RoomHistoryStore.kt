package io.github.gustavommcv.ciscsmarthome.data.local

import io.github.gustavommcv.ciscsmarthome.domain.BootId
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.HistoryStore
import io.github.gustavommcv.ciscsmarthome.domain.HourlyReading
import io.github.gustavommcv.ciscsmarthome.domain.Reading
import io.github.gustavommcv.ciscsmarthome.domain.StateReading
import io.github.gustavommcv.ciscsmarthome.domain.StoredSample
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * The history in a database on the phone. A sample is kept as one row per entity, so a new entity needs no new column. Each hour of an entity keeps
 * its least and its greatest reading, saved together with the readings.
 */
class RoomHistoryStore(database: HistoryDatabase) : HistoryStore {
  private val dao = database.historyDao()

  override suspend fun save(samples: List<StoredSample>) {
    val rows =
      samples.flatMap { sample ->
        sample.values.map { (entity, value) -> ReadingRow(sample.bootId.value, sample.uptimeMs, entity.value, sample.at.toEpochMilli(), value) }
      }
    val states =
      samples.flatMap { sample ->
        sample.states.map { (entity, state) -> StateRow(sample.bootId.value, sample.uptimeMs, entity.value, sample.at.toEpochMilli(), state) }
      }
    if (rows.isNotEmpty() || states.isNotEmpty()) dao.saveWithHours(rows, states)
  }

  override suspend fun latestUptime(bootId: BootId): Long? = dao.latestUptime(bootId.value)

  override fun series(entityId: EntityId, since: Instant): Flow<List<Reading>> =
    dao
      .series(entityId.value, since.toEpochMilli())
      .map { rows -> rows.map { Reading(Instant.ofEpochMilli(it.atMs), it.value) } }
      // The database says "something changed" for any save, even of another entity: only a real change goes on.
      .distinctUntilChanged()

  override fun states(entityId: EntityId, since: Instant): Flow<List<StateReading>> =
    dao
      .states(entityId.value, since.toEpochMilli())
      .map { rows -> rows.map { StateReading(Instant.ofEpochMilli(it.atMs), it.state) } }
      .distinctUntilChanged()

  override fun hourly(entityId: EntityId, since: Instant, until: Instant): Flow<List<HourlyReading>> =
    dao
      .hourly(entityId.value, since.toEpochMilli(), until.toEpochMilli())
      .map { rows -> rows.map { HourlyReading(Instant.ofEpochMilli(it.hourMs), it.lowest, it.highest) } }
      .distinctUntilChanged()

  override fun firstHour(): Flow<Instant?> = dao.firstHour().map { hourMs -> hourMs?.let(Instant::ofEpochMilli) }.distinctUntilChanged()

  override suspend fun forgetReadingsBefore(before: Instant) = dao.deleteBefore(before.toEpochMilli())
}
