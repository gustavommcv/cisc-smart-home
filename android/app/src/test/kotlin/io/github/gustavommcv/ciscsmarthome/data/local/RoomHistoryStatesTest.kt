package io.github.gustavommcv.ciscsmarthome.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.gustavommcv.ciscsmarthome.domain.BootId
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.StateReading
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

/** The states the board says each sample was in (the gas state, the dehumidifier on or off), kept next to the readings. */
@RunWith(RobolectricTestRunner::class)
class RoomHistoryStatesTest {
  private lateinit var database: HistoryDatabase
  private lateinit var store: RoomHistoryStore

  @Before
  fun setUp() {
    database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), HistoryDatabase::class.java).build()
    store = RoomHistoryStore(database)
  }

  @After
  fun tearDown() {
    database.close()
  }

  private fun sample(boot: BootId, uptimeMs: Long, atMs: Long, vararg states: Pair<EntityId, String>) =
    StoredSample(boot, uptimeMs, Instant.ofEpochMilli(atMs), mapOf(GAS to 1.0), mapOf(*states))

  private fun save(vararg samples: StoredSample) = runBlocking { store.save(samples.toList()) }

  private fun states(entity: EntityId, since: Instant = Instant.EPOCH): List<StateReading> = runBlocking { store.states(entity, since).first() }

  private fun at(atMs: Long, state: String) = StateReading(Instant.ofEpochMilli(atMs), state)

  @Test
  fun `states come back oldest first, with the time of the sample`() {
    save(
      sample(A, 30_000, 3_000, POWER to "false"),
      sample(A, 10_000, 1_000, POWER to "true"),
      sample(A, 20_000, 2_000, POWER to "true"),
    )
    assertEquals(listOf(at(1_000, "true"), at(2_000, "true"), at(3_000, "false")), states(POWER))
  }

  @Test
  fun `only the entity that was asked for comes back`() {
    save(sample(A, 10_000, 1_000, POWER to "true", GAS_STATE to "normal"))
    assertEquals(listOf(at(1_000, "normal")), states(GAS_STATE))
    assertEquals(listOf(at(1_000, "true")), states(POWER))
  }

  @Test
  fun `a sample without states adds none, and its readings are kept all the same`() {
    save(sample(A, 10_000, 1_000))
    assertEquals(emptyList<StateReading>(), states(POWER))
    assertEquals(1, runBlocking { store.series(GAS, Instant.EPOCH).first() }.size)
  }

  @Test
  fun `states before the moment asked are left out, and the one at that moment is kept`() {
    save(sample(A, 10_000, 1_000, POWER to "true"), sample(A, 20_000, 2_000, POWER to "false"))
    assertEquals(listOf(at(2_000, "false")), states(POWER, Instant.ofEpochMilli(2_000)))
  }

  @Test
  fun `a sample that is kept is left as it was, states too`() {
    save(sample(A, 10_000, 1_000, POWER to "true"))
    save(sample(A, 10_000, 1_000, POWER to "false"))
    assertEquals(listOf(at(1_000, "true")), states(POWER))
  }

  @Test
  fun `the same uptime on another boot is another state`() {
    save(sample(A, 10_000, 1_000, POWER to "true"), sample(B, 10_000, 5_000, POWER to "false"))
    assertEquals(listOf(at(1_000, "true"), at(5_000, "false")), states(POWER))
  }

  @Test
  fun `letting go of the readings lets go of the states of the same time`() {
    save(sample(A, 10_000, 1_000, POWER to "true"), sample(A, 20_000, 2_000, POWER to "false"))
    runBlocking { store.forgetReadingsBefore(Instant.ofEpochMilli(2_000)) }
    assertEquals(listOf(at(2_000, "false")), states(POWER))
  }

  @Test
  fun `a sample that has only states is kept`() {
    save(StoredSample(A, 10_000, Instant.ofEpochMilli(1_000), emptyMap(), mapOf(POWER to "true")))
    assertEquals(listOf(at(1_000, "true")), states(POWER))
  }

  @Test
  fun `the states being followed are sent again when they change, and not for a save that does not change them`() {
    runBlocking {
      val seen = Channel<List<StateReading>>(Channel.UNLIMITED)
      val following = launch(Dispatchers.Default) { store.states(POWER, Instant.EPOCH).collect { seen.send(it) } }
      assertEquals(emptyList<StateReading>(), withTimeout(5.seconds) { seen.receive() })

      store.save(listOf(sample(A, 10_000, 1_000, GAS_STATE to "normal")))
      assertNull("sent again although the states of the dehumidifier did not change", withTimeoutOrNull(500.milliseconds) { seen.receive() })

      store.save(listOf(sample(A, 20_000, 2_000, POWER to "true")))
      assertEquals(listOf(at(2_000, "true")), withTimeout(5.seconds) { seen.receive() })
      following.cancel()
    }
  }

  private companion object {
    val A = BootId("aaaa1111")
    val B = BootId("bbbb2222")
    val GAS = EntityId("gas-level")
    val GAS_STATE = EntityId("gas-state")
    val POWER = EntityId("dehumidifier-power")
  }
}
