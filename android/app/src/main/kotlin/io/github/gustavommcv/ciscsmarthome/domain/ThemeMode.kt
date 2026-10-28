package io.github.gustavommcv.ciscsmarthome.domain

/** How the user wants the app to look: always light, always dark, or as the phone is set. */
enum class ThemeMode {
  Light,
  Dark,
  System;

  /** Whether the app is drawn dark, given whether the phone is set to dark. */
  fun isDark(systemIsDark: Boolean): Boolean =
    when (this) {
      Light -> false
      Dark -> true
      System -> systemIsDark
    }

  companion object {
    /** Without a choice the app does what the phone does. */
    val Default = System
  }
}
