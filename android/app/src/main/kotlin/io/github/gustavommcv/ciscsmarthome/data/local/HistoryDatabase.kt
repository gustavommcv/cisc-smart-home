package io.github.gustavommcv.ciscsmarthome.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/**
 * One reading of one entity. The pair (boot, uptime) names the sample it came from, as in docs/api.md,
 * so the same reading is never kept twice. [atMs] is when it was taken, on the phone's clock.
 */
@Entity(tableName = "reading", primaryKeys = ["bootId", "uptimeMs", "entityId"], indices = [Index("entityId", "atMs")])
data class ReadingRow(val bootId: String, val uptimeMs: Long, val entityId: String, val atMs: Long, val value: Double?)

data class PointRow(val atMs: Long, val value: Double?)

/** The state of one entity in one sample, as text. It is named like the reading it came with and let go of with it. */
@Entity(tableName = "state_reading", primaryKeys = ["bootId", "uptimeMs", "entityId"], indices = [Index("entityId", "atMs")])
data class StateRow(val bootId: String, val uptimeMs: Long, val entityId: String, val atMs: Long, val state: String)

data class StatePointRow(val atMs: Long, val state: String)

/**
 * The least and the greatest reading of one hour of one entity. [hourMs] is where the hour starts, on the phone's clock. The readings are let
 * go of after a few days; these stay, a few thousand rows a year for each entity, and a month or a year of history is read from them.
 */
@Entity(tableName = "reading_hour", primaryKeys = ["entityId", "hourMs"])
data class HourRow(val entityId: String, val hourMs: Long, val lowest: Double, val highest: Double)

private const val HOUR_MS = 3_600_000L

@Dao
abstract class HistoryDao {
  @Insert(onConflict = OnConflictStrategy.IGNORE) abstract suspend fun insert(rows: List<ReadingRow>)

  @Insert(onConflict = OnConflictStrategy.IGNORE) abstract suspend fun insertStates(rows: List<StateRow>)

  /** Makes the hour take in a reading: its least goes down and its greatest goes up, never the other way, so saving a reading again changes nothing. */
  @Query(
    "INSERT INTO reading_hour (entityId, hourMs, lowest, highest) VALUES (:entityId, :hourMs, :lowest, :highest) " +
      "ON CONFLICT(entityId, hourMs) DO UPDATE SET lowest = MIN(lowest, excluded.lowest), highest = MAX(highest, excluded.highest)",
  )
  abstract suspend fun widenHour(entityId: String, hourMs: Long, lowest: Double, highest: Double)

  /** Keeps the readings and the hours they are in together: the one is never saved without the other. */
  @Transaction
  open suspend fun saveWithHours(rows: List<ReadingRow>, states: List<StateRow> = emptyList()) {
    insert(rows)
    insertStates(states)
    rows
      .filter { it.value != null }
      .groupBy { it.entityId to Math.floorDiv(it.atMs, HOUR_MS) }
      .forEach { (hour, group) ->
        val values = group.mapNotNull { it.value }
        widenHour(hour.first, hour.second * HOUR_MS, values.min(), values.max())
      }
  }

  @Query("SELECT MAX(uptimeMs) FROM reading WHERE bootId = :bootId") abstract suspend fun latestUptime(bootId: String): Long?

  @Query("SELECT atMs, value FROM reading WHERE entityId = :entityId AND atMs >= :sinceMs ORDER BY atMs, uptimeMs")
  abstract fun series(entityId: String, sinceMs: Long): Flow<List<PointRow>>

  @Query("SELECT atMs, state FROM state_reading WHERE entityId = :entityId AND atMs >= :sinceMs ORDER BY atMs, uptimeMs")
  abstract fun states(entityId: String, sinceMs: Long): Flow<List<StatePointRow>>

  @Query("SELECT * FROM reading_hour WHERE entityId = :entityId AND hourMs >= :sinceMs AND hourMs < :untilMs ORDER BY hourMs")
  abstract fun hourly(entityId: String, sinceMs: Long, untilMs: Long): Flow<List<HourRow>>

  @Query("SELECT MIN(hourMs) FROM reading_hour") abstract fun firstHour(): Flow<Long?>

  @Query("DELETE FROM reading WHERE atMs < :beforeMs") abstract suspend fun deleteReadingsBefore(beforeMs: Long)

  @Query("DELETE FROM state_reading WHERE atMs < :beforeMs") abstract suspend fun deleteStatesBefore(beforeMs: Long)

  /** The readings and the states of one time go together, so neither is left without the other. */
  @Transaction
  open suspend fun deleteBefore(beforeMs: Long) {
    deleteReadingsBefore(beforeMs)
    deleteStatesBefore(beforeMs)
  }
}

@Database(entities = [ReadingRow::class, HourRow::class, StateRow::class], version = 3)
abstract class HistoryDatabase : RoomDatabase() {
  abstract fun historyDao(): HistoryDao
}

/** Version 2 keeps the hours. A phone that has the readings of version 1 gets its hours made from them, so nothing it heard is left out of a month. */
val MIGRATION_1_2 =
  object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL("CREATE TABLE IF NOT EXISTS `reading_hour` (`entityId` TEXT NOT NULL, `hourMs` INTEGER NOT NULL, `lowest` REAL NOT NULL, `highest` REAL NOT NULL, PRIMARY KEY(`entityId`, `hourMs`))")
      db.execSQL(
        "INSERT INTO reading_hour (entityId, hourMs, lowest, highest) " +
          "SELECT entityId, atMs / $HOUR_MS * $HOUR_MS, MIN(value), MAX(value) FROM reading WHERE value IS NOT NULL GROUP BY entityId, atMs / $HOUR_MS",
      )
    }
  }

/** Version 3 keeps the states the board says each sample was in. A phone of version 2 has none for what it heard before, and the charts draw none for that time. */
val MIGRATION_2_3 =
  object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL("CREATE TABLE IF NOT EXISTS `state_reading` (`bootId` TEXT NOT NULL, `uptimeMs` INTEGER NOT NULL, `entityId` TEXT NOT NULL, `atMs` INTEGER NOT NULL, `state` TEXT NOT NULL, PRIMARY KEY(`bootId`, `uptimeMs`, `entityId`))")
      db.execSQL("CREATE INDEX IF NOT EXISTS `index_state_reading_entityId_atMs` ON `state_reading` (`entityId`, `atMs`)")
    }
  }

/** Every step from an older version of the database to the current one. The app and the tests open the database with this, so a step left out is a test that fails. */
val HISTORY_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
