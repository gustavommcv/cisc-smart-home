package io.github.gustavommcv.ciscsmarthome.ui.navigation

import io.github.gustavommcv.ciscsmarthome.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The tabs of the bar at the bottom: which there are, what each is called and drawn as, and where the back button goes. */
class TabTest {
  @Test
  fun `the bar has home, history and settings, in that order`() {
    assertEquals(listOf(Tab.Home, Tab.History, Tab.Settings), Tab.entries)
  }

  @Test
  fun `the app opens on home`() {
    assertEquals(Tab.Home, Tab.Start)
  }

  @Test
  fun `each tab has its own word`() {
    assertEquals(R.string.tab_home, Tab.Home.labelRes)
    assertEquals(R.string.tab_history, Tab.History.labelRes)
    assertEquals(R.string.tab_settings, Tab.Settings.labelRes)
  }

  @Test
  fun `each tab has its own icon`() {
    assertEquals(R.drawable.ic_home, Tab.Home.iconRes)
    assertEquals(R.drawable.ic_history, Tab.History.iconRes)
    assertEquals(R.drawable.ic_settings, Tab.Settings.iconRes)
  }

  @Test
  fun `back from history or settings goes home`() {
    assertEquals(Tab.Home, Tab.History.back())
    assertEquals(Tab.Home, Tab.Settings.back())
  }

  @Test
  fun `back from home is left to the system, which closes the app`() {
    assertNull(Tab.Home.back())
  }
}
