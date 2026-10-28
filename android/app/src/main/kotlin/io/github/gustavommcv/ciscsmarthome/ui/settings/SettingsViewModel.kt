package io.github.gustavommcv.ciscsmarthome.ui.settings

import androidx.lifecycle.ViewModel
import io.github.gustavommcv.ciscsmarthome.domain.ThemeMode
import io.github.gustavommcv.ciscsmarthome.domain.ThemeSettings
import kotlinx.coroutines.flow.StateFlow

/** What the settings tab shows and changes: for now, the theme. */
class SettingsViewModel(private val settings: ThemeSettings) : ViewModel() {
  /** The theme in force, which is the one the tab shows as chosen. */
  val themeMode: StateFlow<ThemeMode> get() = settings.mode

  fun chooseTheme(mode: ThemeMode) {
    settings.choose(mode)
  }
}
