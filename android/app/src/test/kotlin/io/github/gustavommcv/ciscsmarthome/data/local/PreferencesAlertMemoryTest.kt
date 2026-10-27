package io.github.gustavommcv.ciscsmarthome.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.gustavommcv.ciscsmarthome.domain.BootId
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PreferencesAlertMemoryTest {
  private val context: Context = ApplicationProvider.getApplicationContext()
  private val file = "alert-memory-test"

  private fun memory() = PreferencesAlertMemory(context.getSharedPreferences(file, Context.MODE_PRIVATE))

  @Before
  fun clean() {
    context.getSharedPreferences(file, Context.MODE_PRIVATE).edit().clear().commit()
  }

  @Test
  fun `nothing is remembered at first`() {
    assertEquals(emptyMap<BootId, Long>(), memory().load())
  }

  @Test
  fun `what was saved comes back, for every boot`() {
    val announced = mapOf(BootId("aaaa1111") to 3L, BootId("bbbb2222") to 1L)
    memory().save(announced)
    assertEquals(announced, memory().load())
  }

  @Test
  fun `it is remembered after the app restarts`() {
    memory().save(mapOf(BootId("aaaa1111") to 3L))
    // A new object over the same file is what the next run of the app gets.
    assertEquals(mapOf(BootId("aaaa1111") to 3L), memory().load())
  }

  @Test
  fun `saving replaces what was there`() {
    val memory = memory()
    memory.save(mapOf(BootId("aaaa1111") to 1L, BootId("bbbb2222") to 2L))
    memory.save(mapOf(BootId("aaaa1111") to 5L))
    assertEquals(mapOf(BootId("aaaa1111") to 5L), memory.load())
  }

  @Test
  fun `a count past what fits in an Int is kept whole`() {
    val big = Int.MAX_VALUE.toLong() + 10
    memory().save(mapOf(BootId("aaaa1111") to big))
    assertEquals(big, memory().load().getValue(BootId("aaaa1111")))
  }
}
