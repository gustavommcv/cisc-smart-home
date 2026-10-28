package io.github.gustavommcv.ciscsmarthome.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.gustavommcv.ciscsmarthome.domain.BootId
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.HourlyReading
import io.github.gustavommcv.ciscsmarthome.domain.Reading
import io.github.gustavommcv.ciscsmarthome.domain.StoredSample
import java.io.File
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The history a phone already has, made by version 1 of the app, is not lost when the app learns to keep the hours: they are made from it. */
@RunWith(RobolectricTestRunner::class)
class HistoryMigrationTest {
  private lateinit var context: Context
  private lateinit var file: File
  private var database: HistoryDatabase? = null

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    file = context.getDatabasePath("migration-test.db").also { it.parentFile?.mkdirs(); it.delete() }
  }

  @After
  fun tearDown() {
    database?.close()
    file.delete()
  }

  /** A database exactly as version 1 made it (see schemas/.../1.json), with the rows given as (boot, uptime, entity, atMs, value). */
  private fun version1(vararg rows: List<Any?>) {
    val raw = SQLiteDatabase.openOrCreateDatabase(file, null)
    raw.execSQL("CREATE TABLE IF NOT EXISTS `reading` (`bootId` TEXT NOT NULL, `uptimeMs` INTEGER NOT NULL, `entityId` TEXT NOT NULL, `atMs` INTEGER NOT NULL, `value` REAL, PRIMARY KEY(`bootId`, `uptimeMs`, `entityId`))")
    raw.execSQL("CREATE INDEX IF NOT EXISTS `index_reading_entityId_atMs` ON `reading` (`entityId`, `atMs`)")
    rows.forEach { raw.execSQL("INSERT INTO reading (bootId, uptimeMs, entityId, atMs, value) VALUES (?, ?, ?, ?, ?)", it.toTypedArray()) }
    raw.version = 1
    raw.close()
  }

  private fun migrated(): RoomHistoryStore {
    database = Room.databaseBuilder(context, HistoryDatabase::class.java, file.absolutePath).addMigrations(MIGRATION_1_2).allowMainThreadQueries().build()
    return RoomHistoryStore(database!!)
  }

  private fun hourly(store: RoomHistoryStore, entity: EntityId) = runBlocking { store.hourly(entity, Instant.EPOCH, Instant.ofEpochMilli(1_000 * HOUR_MS)).first() }

  @Test
  fun `the hours are made from the readings the phone already had`() {
    version1(
      listOf("aaaa1111", 1_000, "gas-level", 1_000, 5.0),
      listOf("aaaa1111", 2_000, "gas-level", 2_000, 9.0),
      listOf("aaaa1111", 3_000, "gas-level", HOUR_MS + 10, 7.0),
    )
    assertEquals(
      listOf(HourlyReading(Instant.EPOCH, 5.0, 9.0), HourlyReading(Instant.ofEpochMilli(HOUR_MS), 7.0, 7.0)),
      hourly(migrated(), EntityId("gas-level")),
    )
  }

  @Test
  fun `each entity gets its own hours, and a reading with no value is left out`() {
    version1(
      listOf("aaaa1111", 1_000, "gas-level", 1_000, 100.0),
      listOf("aaaa1111", 1_000, "temperature", 1_000, 21.5),
      listOf("aaaa1111", 2_000, "temperature", 2_000, null),
      listOf("aaaa1111", 3_000, "humidity", 3_000, null),
    )
    val store = migrated()
    assertEquals(listOf(HourlyReading(Instant.EPOCH, 100.0, 100.0)), hourly(store, EntityId("gas-level")))
    assertEquals(listOf(HourlyReading(Instant.EPOCH, 21.5, 21.5)), hourly(store, EntityId("temperature")))
    assertEquals(emptyList<HourlyReading>(), hourly(store, EntityId("humidity")))
  }

  @Test
  fun `the readings are still there after the migration`() {
    version1(listOf("aaaa1111", 1_000, "gas-level", 1_000, 5.0), listOf("bbbb2222", 1_000, "gas-level", 2_000, 6.0))
    val store = migrated()
    assertEquals(listOf(Reading(Instant.ofEpochMilli(1_000), 5.0), Reading(Instant.ofEpochMilli(2_000), 6.0)), runBlocking { store.series(EntityId("gas-level"), Instant.EPOCH).first() })
  }

  @Test
  fun `a phone with no history migrates to no hours`() {
    version1()
    assertEquals(emptyList<HourlyReading>(), hourly(migrated(), EntityId("gas-level")))
  }

  @Test
  fun `what is saved after the migration goes on from the hours that were made`() {
    version1(listOf("aaaa1111", 1_000, "gas-level", 1_000, 5.0))
    val store = migrated()
    runBlocking {
      store.save(listOf(StoredSample(BootId("aaaa1111"), 2_000, Instant.ofEpochMilli(2_000), mapOf(EntityId("gas-level") to 12.0))))
    }
    assertEquals(listOf(HourlyReading(Instant.EPOCH, 5.0, 12.0)), hourly(store, EntityId("gas-level")))
  }

  private companion object {
    const val HOUR_MS = 3_600_000L
  }
}
