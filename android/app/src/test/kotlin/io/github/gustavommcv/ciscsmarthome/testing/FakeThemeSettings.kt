package io.github.gustavommcv.ciscsmarthome.testing

import io.github.gustavommcv.ciscsmarthome.domain.ThemeMode
import io.github.gustavommcv.ciscsmarthome.domain.ThemeSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** The choice of theme in memory, writing down what was chosen. */
class FakeThemeSettings(initial: ThemeMode = ThemeMode.System) : ThemeSettings {
  private val state = MutableStateFlow(initial)

  override val mode: StateFlow<ThemeMode> = state

  val chosen = mutableListOf<ThemeMode>()

  override fun choose(mode: ThemeMode) {
    chosen += mode
    state.value = mode
  }
}
