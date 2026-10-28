package io.github.gustavommcv.ciscsmarthome.ui.settings

import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The words and the icon of each choice of theme. */
class ThemeLabelsTest {
  @Test
  fun `each choice has its own word`() {
    assertEquals(R.string.theme_light, ThemeMode.Light.labelRes())
    assertEquals(R.string.theme_dark, ThemeMode.Dark.labelRes())
    assertEquals(R.string.theme_system, ThemeMode.System.labelRes())
  }

  @Test
  fun `each choice has its own icon`() {
    assertEquals(R.drawable.ic_light_mode, ThemeMode.Light.iconRes())
    assertEquals(R.drawable.ic_dark_mode, ThemeMode.Dark.iconRes())
    assertEquals(R.drawable.ic_brightness_auto, ThemeMode.System.iconRes())
  }

  @Test
  fun `only automatic needs a line to say what it does`() {
    assertEquals(R.string.theme_system_hint, ThemeMode.System.hintRes())
    assertNull(ThemeMode.Light.hintRes())
    assertNull(ThemeMode.Dark.hintRes())
  }
}
