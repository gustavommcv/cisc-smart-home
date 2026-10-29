package io.github.gustavommcv.ciscsmarthome.data.repository

import io.github.gustavommcv.ciscsmarthome.data.remote.BoardError
import io.github.gustavommcv.ciscsmarthome.data.remote.BoardResult
import io.github.gustavommcv.ciscsmarthome.domain.BootId
import io.github.gustavommcv.ciscsmarthome.domain.DeviceClock
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.HistoryPage
import io.github.gustavommcv.ciscsmarthome.domain.HistorySample
import io.github.gustavommcv.ciscsmarthome.domain.StoredSample
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Catching up on the history of one boot: what is asked, how it ends, and what is kept. */
class HistorySyncTest {
  private val api = FakeBoardApi()
  private val store = FakeHistoryStore()

  // The board of boot A said it had been up for 100 s at noon, so its uptime 80 000 ms is 11:59:40.
  private val deviceClock = DeviceClock().apply { observe(A, 100_000, Instant.parse("2026-10-27T12:00:00Z")) }
  private val sync = HistorySync(api, store, deviceClock)

  private fun page(boot: BootId, vararg uptimes: Long) = BoardResult.Success(HistoryPage(boot, uptimes.map { HistorySample(it, mapOf(GAS to it / 1000.0)) }))

  private fun full() = (1..360).map { it * 1_000L }.toLongArray()

  @Test
  fun `it keeps what the board has with the phone's time`() = runTest {
    api.history = { _, _ -> page(A, 80_000, 90_000) }
    sync.catchUp(A)
    assertEquals(
      listOf(
        StoredSample(A, 80_000, Instant.parse("2026-10-27T11:59:40Z"), mapOf(GAS to 80.0)),
        StoredSample(A, 90_000, Instant.parse("2026-10-27T11:59:50Z"), mapOf(GAS to 90.0)),
      ),
      store.saved,
    )
    assertEquals(listOf<Pair<Long?, Int?>>(null to 360), api.historyRequests)
  }

  @Test
  fun `the states of a sample are kept with it`() = runTest {
    api.history = { _, _ ->
      BoardResult.Success(HistoryPage(A, listOf(HistorySample(80_000, mapOf(GAS to 80.0), mapOf(EntityId("gas-state") to "emergency")))))
    }
    sync.catchUp(A)
    assertEquals(
      listOf(StoredSample(A, 80_000, Instant.parse("2026-10-27T11:59:40Z"), mapOf(GAS to 80.0), mapOf(EntityId("gas-state") to "emergency"))),
      store.saved,
    )
  }

  @Test
  fun `it asks only for what is newer than what it has`() = runTest {
    store.save(listOf(StoredSample(A, 70_000, Instant.parse("2026-10-27T11:59:00Z"), mapOf(GAS to 70.0))))
    api.history = { _, _ -> page(A, 80_000) }
    sync.catchUp(A)
    assertEquals(listOf<Pair<Long?, Int?>>(70_000L to 360), api.historyRequests)
  }

  @Test
  fun `a boot never seen is fetched from the start, whatever other boots left`() = runTest {
    store.save(listOf(StoredSample(B, 70_000, Instant.parse("2026-10-27T11:00:00Z"), mapOf(GAS to 70.0))))
    api.history = { _, _ -> page(A, 5_000) }
    sync.catchUp(A)
    assertEquals(listOf<Pair<Long?, Int?>>(null to 360), api.historyRequests)
  }

  @Test
  fun `a short page is the end`() = runTest {
    api.history = { _, _ -> page(A, *full().copyOf(359)) }
    sync.catchUp(A)
    assertEquals(1, api.historyRequests.size)
    assertEquals(359, store.saved.size)
  }

  @Test
  fun `a full page makes it ask again after the last sample, until a page is not full`() = runTest {
    api.history = { after, _ -> if (after == null) page(A, *full()) else page(A, 361_000, 362_000) }
    sync.catchUp(A)
    assertEquals(listOf<Pair<Long?, Int?>>(null to 360, 360_000L to 360), api.historyRequests)
    assertEquals(362, store.saved.size)
  }

  @Test
  fun `an empty page ends the fetching`() = runTest {
    api.history = { _, _ -> page(A) }
    sync.catchUp(A)
    assertEquals(1, api.historyRequests.size)
    assertEquals(emptyList<StoredSample>(), store.saved)
  }

  @Test
  fun `a board that keeps sending the same full page is not asked forever`() = runTest {
    api.history = { _, _ ->
      check(api.historyRequests.size <= 5) { "asked ${api.historyRequests.size} times" }
      page(A, *full())
    }
    sync.catchUp(A)
    assertEquals(2, api.historyRequests.size)
  }

  @Test
  fun `a full page that goes back in time is not followed`() = runTest {
    store.save(listOf(StoredSample(A, 500_000, Instant.parse("2026-10-27T11:59:00Z"), mapOf(GAS to 1.0))))
    api.history = { _, _ ->
      check(api.historyRequests.size <= 5) { "asked ${api.historyRequests.size} times" }
      page(A, *full())
    }
    sync.catchUp(A)
    assertEquals(1, api.historyRequests.size)
  }

  @Test
  fun `a page of another boot than the one asked about is not kept`() = runTest {
    api.history = { _, _ -> page(B, 5_000, 6_000) }
    sync.catchUp(A)
    assertEquals(emptyList<StoredSample>(), store.saved)
    assertEquals(1, api.historyRequests.size)
  }

  @Test
  fun `a history that cannot be fetched keeps nothing and does not fail`() = runTest {
    api.history = { _, _ -> BoardResult.Failure(BoardError.Unreachable(IOException("no answer"))) }
    sync.catchUp(A)
    assertEquals(emptyList<StoredSample>(), store.saved)
    assertEquals(1, api.historyRequests.size)
  }

  @Test
  fun `a failure after the first page keeps the first page`() = runTest {
    api.history = { after, _ -> if (after == null) page(A, *full()) else BoardResult.Failure(BoardError.Unreachable(IOException("no answer"))) }
    sync.catchUp(A)
    assertEquals(360, store.saved.size)
  }

  @Test
  fun `samples of a boot whose clock is unknown are not kept, for they cannot be placed in time`() = runTest {
    api.history = { _, _ -> page(B, 5_000) }
    HistorySync(api, store, deviceClock).catchUp(B)
    assertEquals(emptyList<StoredSample>(), store.saved)
  }

  private companion object {
    val A = BootId("aaaa1111")
    val B = BootId("bbbb2222")
    val GAS = EntityId("gas-level")
  }
}
