package io.github.gustavommcv.ciscsmarthome.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The user's choice of theme, and what it makes of the phone's own setting. */
class ThemeModeTest {
  @Test
  fun `light is never dark, not even on a phone set to dark`() {
    assertFalse(ThemeMode.Light.isDark(systemIsDark = true))
    assertFalse(ThemeMode.Light.isDark(systemIsDark = false))
  }

  @Test
  fun `dark is always dark, not even on a phone set to light`() {
    assertTrue(ThemeMode.Dark.isDark(systemIsDark = true))
    assertTrue(ThemeMode.Dark.isDark(systemIsDark = false))
  }

  @Test
  fun `automatic follows the phone`() {
    assertTrue(ThemeMode.System.isDark(systemIsDark = true))
    assertFalse(ThemeMode.System.isDark(systemIsDark = false))
  }

  @Test
  fun `without a choice the app follows the phone`() {
    assertEquals(ThemeMode.System, ThemeMode.Default)
  }

  @Test
  fun `the three choices are light, dark and automatic, in that order`() {
    assertEquals(listOf(ThemeMode.Light, ThemeMode.Dark, ThemeMode.System), ThemeMode.entries)
  }
}
