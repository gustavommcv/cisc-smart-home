package io.github.gustavommcv.ciscsmarthome.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * One reading of one entity. The pair (boot, uptime) names the sample it came from, as in docs/api.md,
 * so the same reading is never kept twice. [atMs] is when it was taken, on the phone's clock.
 */
@Entity(tableName = "reading", primaryKeys = ["bootId", "uptimeMs", "entityId"], indices = [Index("entityId", "atMs")])
data class ReadingRow(val bootId: String, val uptimeMs: Long, val entityId: String, val atMs: Long, val value: Double?)

data class PointRow(val atMs: Long, val value: Double?)

@Dao
interface HistoryDao {
  @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(rows: List<ReadingRow>)

  @Query("SELECT MAX(uptimeMs) FROM reading WHERE bootId = :bootId") suspend fun latestUptime(bootId: String): Long?

  @Query("SELECT atMs, value FROM reading WHERE entityId = :entityId AND atMs >= :sinceMs ORDER BY atMs, uptimeMs")
  fun series(entityId: String, sinceMs: Long): Flow<List<PointRow>>
}

@Database(entities = [ReadingRow::class], version = 1)
abstract class HistoryDatabase : RoomDatabase() {
  abstract fun historyDao(): HistoryDao
}
