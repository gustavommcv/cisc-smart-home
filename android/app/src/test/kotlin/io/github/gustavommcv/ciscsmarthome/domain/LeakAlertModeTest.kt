package io.github.gustavommcv.ciscsmarthome.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** How the phone warns of a gas leak, and which way a room has unless the user chose another. */
class LeakAlertModeTest {
  @Test
  fun `a room warns with the alarm unless the user chose another way`() {
    assertEquals(LeakAlertMode.Alarm, LeakAlertMode.Default)
    assertEquals(LeakAlertMode.Alarm, LeakAlertModes().of(AreaId("kitchen")))
  }

  @Test
  fun `the ways are the alarm, a notification and none, in that order`() {
    assertEquals(listOf(LeakAlertMode.Alarm, LeakAlertMode.Notice, LeakAlertMode.Off), LeakAlertMode.entries)
  }

  @Test
  fun `the keys are the ones kept in the files of the users, so they never change`() {
    assertEquals(listOf("alarm", "notice", "off"), LeakAlertMode.entries.map { it.key })
  }

  @Test
  fun `each key finds its way again`() {
    LeakAlertMode.entries.forEach { assertEquals(it, LeakAlertMode.fromKey(it.key)) }
  }

  @Test
  fun `a key the app does not know is no way`() {
    assertNull(LeakAlertMode.fromKey("siren"))
    assertNull(LeakAlertMode.fromKey(""))
  }

  @Test
  fun `a room has the way the user chose for it, and the others keep the default`() {
    val modes = LeakAlertModes(mapOf(AreaId("kitchen") to LeakAlertMode.Off))
    assertEquals(LeakAlertMode.Off, modes.of(AreaId("kitchen")))
    assertEquals(LeakAlertMode.Alarm, modes.of(AreaId("bedroom")))
  }
}
