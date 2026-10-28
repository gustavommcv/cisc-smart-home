package io.github.gustavommcv.ciscsmarthome.ui.home

import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.LeakAlertMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The words of each way of being warned of a leak, and which choice asks first. */
class LeakAlertLabelsTest {
  @Test
  fun `each way has its own word`() {
    assertEquals(listOf(R.string.leak_mode_alarm, R.string.leak_mode_notice, R.string.leak_mode_off), LeakAlertMode.entries.map { it.labelRes() })
  }

  @Test
  fun `each way has a line that says what it does`() {
    assertEquals(listOf(R.string.leak_mode_alarm_hint, R.string.leak_mode_notice_hint, R.string.leak_mode_off_hint), LeakAlertMode.entries.map { it.hintRes() })
  }

  @Test
  fun `the words are all different`() {
    assertEquals(LeakAlertMode.entries.size, LeakAlertMode.entries.map { it.labelRes() }.toSet().size)
    assertEquals(LeakAlertMode.entries.size, LeakAlertMode.entries.map { it.hintRes() }.toSet().size)
  }

  @Test
  fun `only turning the warning off asks first, for it leaves the house without a warning on the phone`() {
    assertTrue(LeakAlertMode.Off.needsConfirmation)
    assertFalse(LeakAlertMode.Alarm.needsConfirmation)
    assertFalse(LeakAlertMode.Notice.needsConfirmation)
  }
}
