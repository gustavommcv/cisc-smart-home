package io.github.gustavommcv.ciscsmarthome.service

import io.github.gustavommcv.ciscsmarthome.domain.AreaId
import io.github.gustavommcv.ciscsmarthome.domain.BootId
import io.github.gustavommcv.ciscsmarthome.domain.GasAlert
import io.github.gustavommcv.ciscsmarthome.domain.Home
import io.github.gustavommcv.ciscsmarthome.domain.LeakAlertMode
import io.github.gustavommcv.ciscsmarthome.testing.FakeLeakAlertSettings
import io.github.gustavommcv.ciscsmarthome.testing.houseOfTheMaquete
import org.junit.Assert.assertEquals
import org.junit.Test

/** Warning of each leak, and taking the warning down when the board says the leak is over. */
class LeakWarningsTest {
  private val notifier = FakeNotifier()
  private val settings = FakeLeakAlertSettings()
  private val warnings = LeakWarnings(notifier, settings)

  private fun leak(count: Long, boot: BootId = A) = GasAlert(boot, KITCHEN, count)

  private fun house(state: String, alerts: Long, boot: BootId = A) = houseOfTheMaquete(boot, uptimeMs = 1_000, gasState = state, gasAlerts = alerts)

  @Test
  fun `a leak is warned of, with the house as it was when the board said it`() {
    val home = house("emergency", alerts = 1)
    warnings.warn(leak(1), home)
    assertEquals(listOf(leak(1) to home), notifier.gasLeaks)
  }

  @Test
  fun `a leak is warned of with the alarm unless the user chose another way`() {
    warnings.warn(leak(1), null)
    assertEquals(listOf(LeakAlertMode.Alarm), notifier.gasLeakModes)
  }

  @Test
  fun `a leak is warned of in the way the user chose for that room`() {
    settings.choose(KITCHEN, LeakAlertMode.Notice)
    warnings.warn(leak(1), null)
    assertEquals(listOf(LeakAlertMode.Notice), notifier.gasLeakModes)
  }

  @Test
  fun `a room the user turned the warning off for is not warned of`() {
    settings.choose(KITCHEN, LeakAlertMode.Off)
    warnings.warn(leak(1), null)
    assertEquals(emptyList<Pair<GasAlert, Home?>>(), notifier.gasLeaks)
  }

  @Test
  fun `what is not warned of is not taken down either`() {
    settings.choose(KITCHEN, LeakAlertMode.Off)
    warnings.warn(leak(1), null)
    warnings.silenceEnded(house("normal", alerts = 1))
    assertEquals(emptyList<GasAlert>(), notifier.gasLeaksCleared)
  }

  @Test
  fun `the way chosen for one room is not the way of another`() {
    settings.choose(KITCHEN, LeakAlertMode.Off)
    warnings.warn(GasAlert(A, AreaId("bedroom"), 1), null)
    assertEquals(listOf(LeakAlertMode.Alarm), notifier.gasLeakModes)
  }

  @Test
  fun `the way is the one of the moment of the leak, so a change made a minute before counts`() {
    warnings.warn(leak(1), null)
    settings.choose(KITCHEN, LeakAlertMode.Off)
    warnings.warn(leak(2), null)
    settings.choose(KITCHEN, LeakAlertMode.Notice)
    warnings.warn(leak(3), null)
    assertEquals(listOf(LeakAlertMode.Alarm, LeakAlertMode.Notice), notifier.gasLeakModes)
  }

  @Test
  fun `a leak is warned of even before the app has heard the house`() {
    warnings.warn(leak(1), null)
    assertEquals(listOf<Pair<GasAlert, Home?>>(leak(1) to null), notifier.gasLeaks)
  }

  @Test
  fun `the warning is taken down when the board says the leak is over`() {
    warnings.warn(leak(1), null)
    warnings.silenceEnded(house("normal", alerts = 1))
    assertEquals(listOf(leak(1)), notifier.gasLeaksCleared)
  }

  @Test
  fun `the warning stays while the board still says emergency`() {
    warnings.warn(leak(1), null)
    warnings.silenceEnded(house("emergency", alerts = 1))
    assertEquals(emptyList<GasAlert>(), notifier.gasLeaksCleared)
  }

  @Test
  fun `a home from before the leak does not take its warning away`() {
    warnings.warn(leak(2), null)
    warnings.silenceEnded(house("normal", alerts = 1))
    assertEquals(emptyList<GasAlert>(), notifier.gasLeaksCleared)
  }

  @Test
  fun `a house that says nothing about gas leaves the warning alone`() {
    warnings.warn(leak(1), null)
    warnings.silenceEnded(Home(A, uptimeMs = 1_000, areas = emptyList()))
    assertEquals(emptyList<GasAlert>(), notifier.gasLeaksCleared)
  }

  @Test
  fun `a home of another boot leaves the warning alone`() {
    warnings.warn(leak(1, boot = A), null)
    warnings.silenceEnded(house("normal", alerts = 5, boot = B))
    assertEquals(emptyList<GasAlert>(), notifier.gasLeaksCleared)
  }

  @Test
  fun `the warnings of all the leaks that ended are taken down, and only those`() {
    warnings.warn(leak(1), null)
    warnings.warn(leak(2), null)
    warnings.warn(leak(3), null)
    warnings.silenceEnded(house("normal", alerts = 2))
    assertEquals(listOf(leak(1), leak(2)), notifier.gasLeaksCleared)
    warnings.silenceEnded(house("normal", alerts = 3))
    assertEquals(listOf(leak(1), leak(2), leak(3)), notifier.gasLeaksCleared)
  }

  @Test
  fun `a warning is taken down once, not again with every home after`() {
    warnings.warn(leak(1), null)
    warnings.silenceEnded(house("normal", alerts = 1))
    warnings.silenceEnded(house("normal", alerts = 1))
    assertEquals(1, notifier.gasLeaksCleared.size)
  }

  @Test
  fun `a new leak is warned of and followed after the last one ended`() {
    warnings.warn(leak(1), null)
    warnings.silenceEnded(house("normal", alerts = 1))
    warnings.warn(leak(2), null)
    assertEquals(listOf(leak(1), leak(2)), notifier.gasLeaks.map { it.first })
    warnings.silenceEnded(house("normal", alerts = 2))
    assertEquals(listOf(leak(1), leak(2)), notifier.gasLeaksCleared)
  }

  @Test
  fun `without a warning there is nothing to take down`() {
    warnings.silenceEnded(house("normal", alerts = 4))
    assertEquals(emptyList<GasAlert>(), notifier.gasLeaksCleared)
  }

  private companion object {
    val A = BootId("aaaa1111")
    val B = BootId("bbbb2222")
    val KITCHEN = AreaId("kitchen")
  }
}
