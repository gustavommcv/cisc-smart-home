package io.github.gustavommcv.ciscsmarthome.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.gustavommcv.ciscsmarthome.domain.BootId
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
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

/** The history the app keeps, against a real database (in memory) that Robolectric runs on the computer. */
@RunWith(RobolectricTestRunner::class)
class RoomHistoryStoreTest {
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

  private fun sample(boot: BootId, uptimeMs: Long, atMs: Long, vararg values: Pair<EntityId, Double?>) =
    StoredSample(boot, uptimeMs, Instant.ofEpochMilli(atMs), mapOf(*values))

  private fun save(vararg samples: StoredSample) = runBlocking { store.save(samples.toList()) }

  private fun series(entity: EntityId, since: Instant = Instant.EPOCH): List<Reading> = runBlocking { store.series(entity, since).first() }

  private fun reading(atMs: Long, value: Double?) = Reading(Instant.ofEpochMilli(atMs), value)

  private fun latestUptime(boot: BootId): Long? = runBlocking { store.latestUptime(boot) }

  // ---- reading the history back ---------------------------------------------------

  @Test
  fun `readings come back oldest first, with the time they were taken`() {
    save(
      sample(A, 30_000, 3_000, GAS to 30.0),
      sample(A, 10_000, 1_000, GAS to 10.0),
      sample(A, 20_000, 2_000, GAS to 20.0),
    )
    assertEquals(listOf(reading(1_000, 10.0), reading(2_000, 20.0), reading(3_000, 30.0)), series(GAS))
  }

  @Test
  fun `a reading without a valid value comes back as null`() {
    save(sample(A, 10_000, 1_000, TEMPERATURE to null))
    assertEquals(listOf(reading(1_000, null)), series(TEMPERATURE))
  }

  @Test
  fun `only the entity that was asked for comes back`() {
    save(sample(A, 10_000, 1_000, GAS to 142.0, TEMPERATURE to 21.4, HUMIDITY to 74.1))
    assertEquals(listOf(reading(1_000, 142.0)), series(GAS))
    assertEquals(listOf(reading(1_000, 21.4)), series(TEMPERATURE))
    assertEquals(listOf(reading(1_000, 74.1)), series(HUMIDITY))
  }

  @Test
  fun `an entity with no readings has an empty series`() {
    save(sample(A, 10_000, 1_000, GAS to 142.0))
    assertEquals(emptyList<Reading>(), series(HUMIDITY))
  }

  @Test
  fun `readings before the moment asked are left out, and the one at that moment is kept`() {
    save(
      sample(A, 10_000, 1_000, GAS to 1.0),
      sample(A, 20_000, 2_000, GAS to 2.0),
      sample(A, 30_000, 3_000, GAS to 3.0),
    )
    assertEquals(listOf(reading(2_000, 2.0), reading(3_000, 3.0)), series(GAS, since = Instant.ofEpochMilli(2_000)))
  }

  @Test
  fun `readings of different boots are in the order of the time they were taken`() {
    save(
      sample(A, 10_000, 1_000, GAS to 1.0),
      sample(A, 20_000, 3_000, GAS to 3.0),
      sample(B, 5_000, 2_000, GAS to 2.0),
    )
    assertEquals(listOf(reading(1_000, 1.0), reading(2_000, 2.0), reading(3_000, 3.0)), series(GAS))
  }

  // ---- what is already kept -------------------------------------------------------

  @Test
  fun `a sample that is already kept is left as it was`() {
    save(sample(A, 10_000, 1_000, GAS to 1.0))
    save(sample(A, 10_000, 1_500, GAS to 9.0))
    assertEquals(listOf(reading(1_000, 1.0)), series(GAS))
  }

  @Test
  fun `the same uptime on another boot is another sample`() {
    save(sample(A, 10_000, 1_000, GAS to 1.0))
    save(sample(B, 10_000, 2_000, GAS to 2.0))
    assertEquals(listOf(reading(1_000, 1.0), reading(2_000, 2.0)), series(GAS))
  }

  @Test
  fun `saving a page that overlaps what is kept adds only what is new`() {
    save(sample(A, 10_000, 1_000, GAS to 1.0), sample(A, 20_000, 2_000, GAS to 2.0))
    save(sample(A, 20_000, 2_000, GAS to 2.0), sample(A, 30_000, 3_000, GAS to 3.0))
    assertEquals(listOf(reading(1_000, 1.0), reading(2_000, 2.0), reading(3_000, 3.0)), series(GAS))
  }

  @Test
  fun `saving nothing changes nothing`() {
    runBlocking { store.save(emptyList()) }
    assertEquals(emptyList<Reading>(), series(GAS))
  }

  @Test
  fun `a whole page of the board is saved in one go`() {
    val page = (1..360).map { sample(A, it * 10_000L, it * 1_000L, GAS to it.toDouble(), TEMPERATURE to 20.0, HUMIDITY to null) }
    runBlocking { store.save(page) }
    assertEquals(360, series(GAS).size)
    assertEquals(360.0, series(GAS).last().value!!, 0.0)
    assertEquals(360, series(HUMIDITY).size)
  }

  // ---- how far the history goes ---------------------------------------------------

  @Test
  fun `nothing is known about a boot that was never saved`() {
    assertNull(latestUptime(A))
  }

  @Test
  fun `the latest uptime is the greatest one kept for that boot`() {
    save(sample(A, 20_000, 2_000, GAS to 2.0), sample(A, 30_000, 3_000, GAS to 3.0), sample(A, 10_000, 1_000, GAS to 1.0))
    assertEquals(30_000L, latestUptime(A))
  }

  @Test
  fun `the latest uptime of a boot does not look at the other boots`() {
    save(sample(A, 30_000, 3_000, GAS to 3.0), sample(B, 99_000, 4_000, GAS to 4.0))
    assertEquals(30_000L, latestUptime(A))
    assertEquals(99_000L, latestUptime(B))
  }

  @Test
  fun `a sample with no values leaves no trace`() {
    save(sample(A, 10_000, 1_000))
    assertNull(latestUptime(A))
  }

  // ---- following the history live -------------------------------------------------

  @Test
  fun `a series that is being followed is sent again when new samples are saved`() {
    runBlocking {
      val seen = Channel<List<Reading>>(Channel.UNLIMITED)
      val following = launch(Dispatchers.Default) { store.series(GAS, Instant.EPOCH).collect { seen.send(it) } }
      assertEquals(emptyList<Reading>(), withTimeout(5.seconds) { seen.receive() })

      store.save(listOf(sample(A, 10_000, 1_000, GAS to 1.0)))
      assertEquals(listOf(reading(1_000, 1.0)), withTimeout(5.seconds) { seen.receive() })

      store.save(listOf(sample(A, 20_000, 2_000, GAS to 2.0)))
      assertEquals(listOf(reading(1_000, 1.0), reading(2_000, 2.0)), withTimeout(5.seconds) { seen.receive() })
      following.cancel()
    }
  }

  @Test
  fun `a series that is being followed is not sent again for a save that does not change it`() {
    runBlocking {
      val seen = Channel<List<Reading>>(Channel.UNLIMITED)
      val following = launch(Dispatchers.Default) { store.series(GAS, Instant.EPOCH).collect { seen.send(it) } }
      assertEquals(emptyList<Reading>(), withTimeout(5.seconds) { seen.receive() })

      store.save(listOf(sample(A, 10_000, 1_000, TEMPERATURE to 21.4)))
      assertNull("sent again although the gas series did not change", withTimeoutOrNull(500.milliseconds) { seen.receive() })
      following.cancel()
    }
  }

  private companion object {
    val A = BootId("aaaa1111")
    val B = BootId("bbbb2222")
    val GAS = EntityId("gas-level")
    val TEMPERATURE = EntityId("temperature")
    val HUMIDITY = EntityId("humidity")
  }
}
