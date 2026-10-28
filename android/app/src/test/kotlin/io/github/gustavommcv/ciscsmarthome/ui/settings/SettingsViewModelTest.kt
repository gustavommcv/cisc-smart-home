package io.github.gustavommcv.ciscsmarthome.ui.settings

import io.github.gustavommcv.ciscsmarthome.domain.ThemeMode
import io.github.gustavommcv.ciscsmarthome.testing.FakeThemeSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsViewModelTest {
  private val settings = FakeThemeSettings()
  private val viewModel = SettingsViewModel(settings)

  @Test
  fun `the screen starts with the theme that is chosen`() {
    assertEquals(ThemeMode.Dark, SettingsViewModel(FakeThemeSettings(ThemeMode.Dark)).themeMode.value)
  }

  @Test
  fun `choosing a theme hands the choice over`() {
    viewModel.chooseTheme(ThemeMode.Light)
    assertEquals(listOf(ThemeMode.Light), settings.chosen)
  }

  @Test
  fun `what the screen shows follows what is chosen`() {
    viewModel.chooseTheme(ThemeMode.Dark)
    assertEquals(ThemeMode.Dark, viewModel.themeMode.value)
  }

  @Test
  fun `a theme chosen somewhere else shows here too`() {
    settings.choose(ThemeMode.Light)
    assertEquals(ThemeMode.Light, viewModel.themeMode.value)
  }
}
