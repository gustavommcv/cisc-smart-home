package io.github.gustavommcv.ciscsmarthome.data.local

import android.content.SharedPreferences
import androidx.core.content.edit
import io.github.gustavommcv.ciscsmarthome.domain.ThemeMode
import io.github.gustavommcv.ciscsmarthome.domain.ThemeSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The choice of theme in a small preferences file, one word: the name of the mode. A word the app does not know
 * (a file from a later version, a damaged one) reads as no choice, so it never keeps the app from opening.
 */
class PreferencesThemeSettings(private val preferences: SharedPreferences) : ThemeSettings {
  private val state = MutableStateFlow(load())

  override val mode: StateFlow<ThemeMode> = state.asStateFlow()

  override fun choose(mode: ThemeMode) {
    preferences.edit { putString(KEY, mode.name) }
    state.value = mode
  }

  private fun load(): ThemeMode {
    val name = runCatching { preferences.getString(KEY, null) }.getOrNull()
    return ThemeMode.entries.firstOrNull { it.name == name } ?: ThemeMode.Default
  }

  private companion object {
    const val KEY = "theme"
  }
}
