package io.github.gustavommcv.ciscsmarthome.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertTrackerTest {
  private val boot = BootId("5f3a9c21")
  private val other = BootId("deadbeef")

  // ---- alert events ----------------------------------------------------------

  @Test
  fun `the first alert of a boot is announced`() {
    assertTrue(AlertTracker().onAlertEvent(boot, count = 1))
  }

  @Test
  fun `the same alert is not announced twice, even if it arrives again after a reconnection`() {
    val tracker = AlertTracker()
    assertTrue(tracker.onAlertEvent(boot, count = 1))
    assertFalse(tracker.onAlertEvent(boot, count = 1))
  }

  @Test
  fun `a later alert is announced`() {
    val tracker = AlertTracker()
    tracker.onAlertEvent(boot, count = 1)
    assertTrue(tracker.onAlertEvent(boot, count = 2))
  }

  @Test
  fun `an older alert arriving late is not announced`() {
    val tracker = AlertTracker()
    tracker.onAlertEvent(boot, count = 3)
    assertFalse(tracker.onAlertEvent(boot, count = 2))
  }

  @Test
  fun `each boot counts from its own start`() {
    val tracker = AlertTracker()
    tracker.onAlertEvent(boot, count = 5)
    assertTrue(tracker.onAlertEvent(other, count = 1))
  }

  // ---- the state of the house -------------------------------------------------

  @Test
  fun `connecting in the middle of a leak announces it`() {
    assertTrue(AlertTracker().onHome(boot, alertCount = 1, leakActive = true))
  }

  @Test
  fun `a leak already announced by its event is not announced again by the state`() {
    val tracker = AlertTracker()
    tracker.onAlertEvent(boot, count = 2)
    assertFalse(tracker.onHome(boot, alertCount = 2, leakActive = true))
  }

  @Test
  fun `a leak seen first in the state is not announced again by its late event`() {
    val tracker = AlertTracker()
    assertTrue(tracker.onHome(boot, alertCount = 2, leakActive = true))
    assertFalse(tracker.onAlertEvent(boot, count = 2))
  }

  @Test
  fun `a state with no leak announces nothing, however many leaks the board counted`() {
    assertFalse(AlertTracker().onHome(boot, alertCount = 7, leakActive = false))
  }

  @Test
  fun `leaks that ended while the app was away are the baseline, so only the next one is announced`() {
    val tracker = AlertTracker()
    tracker.onHome(boot, alertCount = 3, leakActive = false)
    assertFalse(tracker.onAlertEvent(boot, count = 3))
    assertTrue(tracker.onAlertEvent(boot, count = 4))
  }

  @Test
  fun `a state with the leak still active after a quiet one announces the new leak`() {
    val tracker = AlertTracker()
    tracker.onHome(boot, alertCount = 3, leakActive = false)
    assertTrue(tracker.onHome(boot, alertCount = 4, leakActive = true))
  }

  // ---- remembering across restarts ----------------------------------------------

  @Test
  fun `what was already announced before the app restarted is not announced again`() {
    val tracker = AlertTracker(announced = mapOf(boot to 2L))
    assertFalse(tracker.onHome(boot, alertCount = 2, leakActive = true))
    assertTrue(tracker.onAlertEvent(boot, count = 3))
  }

  @Test
  fun `the tracker can say what it knows, to be kept`() {
    val tracker = AlertTracker()
    tracker.onAlertEvent(boot, count = 2)
    tracker.onHome(other, alertCount = 5, leakActive = false)
    assertEquals(mapOf(boot to 2L, other to 5L), tracker.announced())
  }
}
