package io.github.gustavommcv.ciscsmarthome.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.gustavommcv.ciscsmarthome.domain.BootId
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.HourlyReading
import io.github.gustavommcv.ciscsmarthome.domain.Reading
import io.github.gustavommcv.ciscsmarthome.domain.StoredSample
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * What the app keeps of each hour for good: the least and the greatest reading. The readings themselves are let go of after a few days, and a
 * month or a year of history is read from these.
 */
@RunWith(RobolectricTestRunner::class)
class RoomHistorySummariesTest {
  private lateinit var database: HistoryDatabase
  private lateinit var store: RoomHistoryStore

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = Room.inMemoryDatabaseBuilder(context, HistoryDatabase::class.java).build()
    store = RoomHistoryStore(database)
  }

  @After
  fun tearDown() {
    database.close()
  }

  // ---- helpers ------------------------------------------------------------------

  private fun at(hour: Int, plusMs: Long = 0) = Instant.ofEpochMilli(hour * HOUR_MS + plusMs)

  private fun sample(boot: BootId, uptimeMs: Long, at: Instant, vararg values: Pair<EntityId, Double?>) = StoredSample(boot, uptimeMs, at, mapOf(*values))

  private fun save(vararg samples: StoredSample) = runBlocking { store.save(samples.toList()) }

  private fun hourly(entity: EntityId, since: Instant = Instant.EPOCH, until: Instant = at(1_000)): List<HourlyReading> = runBlocking { store.hourly(entity, since, until).first() }

  private fun series(entity: EntityId): List<Reading> = runBlocking { store.series(entity, Instant.EPOCH).first() }

  // ---- what an hour keeps ---------------------------------------------------------

  @Test
  fun `an hour keeps its least and its greatest reading`() {
    save(sample(A, 1_000, at(0, 1_000), GAS to 5.0), sample(A, 2_000, at(0, 2_000), GAS to 9.0), sample(A, 3_000, at(0, 3_000), GAS to 7.0))
    assertEquals(listOf(HourlyReading(at(0), 5.0, 9.0)), hourly(GAS))
  }

  @Test
  fun `an hour with one reading has it as its least and its greatest`() {
    save(sample(A, 1_000, at(3, 500), GAS to 42.0))
    assertEquals(listOf(HourlyReading(at(3), 42.0, 42.0)), hourly(GAS))
  }

  @Test
  fun `hours come back oldest first, each named by the moment it starts`() {
    save(sample(A, 1_000, at(2, 10), GAS to 1.0), sample(A, 2_000, at(0, 10), GAS to 2.0), sample(A, 3_000, at(1, 10), GAS to 3.0))
    assertEquals(listOf(at(0), at(1), at(2)), hourly(GAS).map { it.hour })
  }

  @Test
  fun `the last millisecond of an hour is of that hour, and the next one starts another`() {
    save(sample(A, 1_000, at(0, HOUR_MS - 1), GAS to 1.0), sample(A, 2_000, at(1), GAS to 2.0))
    assertEquals(listOf(HourlyReading(at(0), 1.0, 1.0), HourlyReading(at(1), 2.0, 2.0)), hourly(GAS))
  }

  @Test
  fun `a reading with no valid value is not part of any hour`() {
    save(sample(A, 1_000, at(0, 1_000), GAS to null), sample(A, 2_000, at(1, 1_000), GAS to null))
    assertEquals(emptyList<HourlyReading>(), hourly(GAS))
  }

  @Test
  fun `a reading with no valid value does not change the hour it is in`() {
    save(sample(A, 1_000, at(0, 1_000), GAS to 4.0), sample(A, 2_000, at(0, 2_000), GAS to null))
    assertEquals(listOf(HourlyReading(at(0), 4.0, 4.0)), hourly(GAS))
  }

  @Test
  fun `each entity has its own hours`() {
    save(sample(A, 1_000, at(0, 1_000), GAS to 100.0, TEMPERATURE to 21.0), sample(A, 2_000, at(0, 2_000), GAS to 120.0, TEMPERATURE to 22.0))
    assertEquals(listOf(HourlyReading(at(0), 100.0, 120.0)), hourly(GAS))
    assertEquals(listOf(HourlyReading(at(0), 21.0, 22.0)), hourly(TEMPERATURE))
  }

  @Test
  fun `an entity with no readings has no hours`() {
    assertEquals(emptyList<HourlyReading>(), hourly(GAS))
  }

  // ---- the window ------------------------------------------------------------------

  @Test
  fun `the hour a window starts at is in it, and the hour it ends at is not`() {
    save(sample(A, 1_000, at(0, 10), GAS to 1.0), sample(A, 2_000, at(1, 10), GAS to 2.0), sample(A, 3_000, at(2, 10), GAS to 3.0), sample(A, 4_000, at(3, 10), GAS to 4.0))
    assertEquals(listOf(at(1), at(2)), hourly(GAS, since = at(1), until = at(3)).map { it.hour })
  }

  @Test
  fun `an empty window has no hours`() {
    save(sample(A, 1_000, at(1, 10), GAS to 1.0))
    assertEquals(emptyList<HourlyReading>(), hourly(GAS, since = at(1), until = at(1)))
  }

  // ---- how the hours grow -----------------------------------------------------------

  @Test
  fun `a later save moves the least down and the greatest up in the same hour`() {
    save(sample(A, 1_000, at(0, 1_000), GAS to 10.0))
    save(sample(A, 2_000, at(0, 2_000), GAS to 3.0), sample(A, 3_000, at(0, 3_000), GAS to 20.0))
    assertEquals(listOf(HourlyReading(at(0), 3.0, 20.0)), hourly(GAS))
  }

  @Test
  fun `a later save that is between the two changes nothing`() {
    save(sample(A, 1_000, at(0, 1_000), GAS to 3.0), sample(A, 2_000, at(0, 2_000), GAS to 20.0))
    save(sample(A, 3_000, at(0, 3_000), GAS to 10.0))
    assertEquals(listOf(HourlyReading(at(0), 3.0, 20.0)), hourly(GAS))
  }

  @Test
  fun `a sample that is saved again leaves the hour as it was`() {
    save(sample(A, 1_000, at(0, 1_000), GAS to 3.0), sample(A, 2_000, at(0, 2_000), GAS to 20.0))
    save(sample(A, 1_000, at(0, 1_000), GAS to 3.0), sample(A, 2_000, at(0, 2_000), GAS to 20.0))
    assertEquals(listOf(HourlyReading(at(0), 3.0, 20.0)), hourly(GAS))
  }

  @Test
  fun `an hour is of the entity and not of the boot`() {
    save(sample(A, 1_000, at(0, 1_000), GAS to 10.0), sample(B, 1_000, at(0, 2_000), GAS to 30.0))
    assertEquals(listOf(HourlyReading(at(0), 10.0, 30.0)), hourly(GAS))
  }

  @Test
  fun `a negative number is kept as it is`() {
    save(sample(A, 1_000, at(0, 1_000), TEMPERATURE to -4.5), sample(A, 2_000, at(0, 2_000), TEMPERATURE to -1.0))
    assertEquals(listOf(HourlyReading(at(0), -4.5, -1.0)), hourly(TEMPERATURE))
  }

  // ---- following the hours -----------------------------------------------------------

  @Test
  fun `hours that are being followed are sent again when a save changes them`() {
    runBlocking {
      val seen = Channel<List<HourlyReading>>(Channel.UNLIMITED)
      val following = launch(Dispatchers.Default) { store.hourly(GAS, Instant.EPOCH, at(1_000)).collect { seen.send(it) } }
      assertEquals(emptyList<HourlyReading>(), withTimeout(5.seconds) { seen.receive() })

      store.save(listOf(sample(A, 1_000, at(0, 1_000), GAS to 1.0)))
      assertEquals(listOf(HourlyReading(at(0), 1.0, 1.0)), withTimeout(5.seconds) { seen.receive() })
      following.cancel()
    }
  }

  @Test
  fun `hours that are being followed are not sent again for a save of another entity`() {
    runBlocking {
      val seen = Channel<List<HourlyReading>>(Channel.UNLIMITED)
      val following = launch(Dispatchers.Default) { store.hourly(GAS, Instant.EPOCH, at(1_000)).collect { seen.send(it) } }
      assertEquals(emptyList<HourlyReading>(), withTimeout(5.seconds) { seen.receive() })

      store.save(listOf(sample(A, 1_000, at(0, 1_000), TEMPERATURE to 21.0)))
      assertNull("sent again although the hours of the gas did not change", withTimeoutOrNull(500.milliseconds) { seen.receive() })
      following.cancel()
    }
  }

  // ---- letting go of the readings ------------------------------------------------------

  @Test
  fun `the readings before the moment are let go of, and the one at that moment is kept`() {
    save(sample(A, 1_000, at(0, 1_000), GAS to 1.0), sample(A, 2_000, at(1), GAS to 2.0), sample(A, 3_000, at(2), GAS to 3.0))
    runBlocking { store.forgetReadingsBefore(at(1)) }
    assertEquals(listOf(Reading(at(1), 2.0), Reading(at(2), 3.0)), series(GAS))
  }

  @Test
  fun `the hours stay when their readings are let go of`() {
    save(sample(A, 1_000, at(0, 1_000), GAS to 1.0), sample(A, 2_000, at(0, 2_000), GAS to 8.0))
    runBlocking { store.forgetReadingsBefore(at(5)) }
    assertEquals(emptyList<Reading>(), series(GAS))
    assertEquals(listOf(HourlyReading(at(0), 1.0, 8.0)), hourly(GAS))
  }

  @Test
  fun `letting go of readings of every entity`() {
    save(sample(A, 1_000, at(0, 1_000), GAS to 1.0, TEMPERATURE to 20.0))
    runBlocking { store.forgetReadingsBefore(at(1)) }
    assertEquals(emptyList<Reading>(), series(GAS))
    assertEquals(emptyList<Reading>(), series(TEMPERATURE))
  }

  @Test
  fun `letting go of what is not there changes nothing`() {
    save(sample(A, 1_000, at(5), GAS to 1.0))
    runBlocking { store.forgetReadingsBefore(at(1)) }
    assertEquals(listOf(Reading(at(5), 1.0)), series(GAS))
  }

  @Test
  fun `the newest readings still say where the board is, after the older ones are let go of`() {
    save(sample(A, 10_000, at(0, 1_000), GAS to 1.0), sample(A, 20_000, at(9), GAS to 2.0))
    runBlocking { store.forgetReadingsBefore(at(5)) }
    assertEquals(20_000L, runBlocking { store.latestUptime(A) })
  }

  @Test
  fun `a reading that is let go of can be saved again without changing its hour`() {
    save(sample(A, 1_000, at(0, 1_000), GAS to 1.0), sample(A, 2_000, at(0, 2_000), GAS to 8.0))
    runBlocking { store.forgetReadingsBefore(at(5)) }
    save(sample(A, 1_000, at(0, 1_000), GAS to 1.0))
    assertEquals(listOf(HourlyReading(at(0), 1.0, 8.0)), hourly(GAS))
  }

  private companion object {
    const val HOUR_MS = 3_600_000L
    val A = BootId("aaaa1111")
    val B = BootId("bbbb2222")
    val GAS = EntityId("gas-level")
    val TEMPERATURE = EntityId("temperature")
  }
}
