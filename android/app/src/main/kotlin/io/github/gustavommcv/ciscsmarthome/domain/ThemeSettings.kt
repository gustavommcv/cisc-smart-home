package io.github.gustavommcv.ciscsmarthome.domain

import kotlinx.coroutines.flow.StateFlow

/** The theme the user chose, kept for the next time the app opens. */
interface ThemeSettings {
  /** The theme in force: the user's choice, or [ThemeMode.Default] before there is one. */
  val mode: StateFlow<ThemeMode>

  /** Keeps [mode] as the user's choice, and makes it the one in force at once. */
  fun choose(mode: ThemeMode)
}
