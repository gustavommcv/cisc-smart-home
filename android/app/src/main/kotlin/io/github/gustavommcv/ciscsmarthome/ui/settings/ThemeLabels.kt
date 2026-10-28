package io.github.gustavommcv.ciscsmarthome.ui.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.ThemeMode

@StringRes
internal fun ThemeMode.labelRes(): Int =
  when (this) {
    ThemeMode.Light -> R.string.theme_light
    ThemeMode.Dark -> R.string.theme_dark
    ThemeMode.System -> R.string.theme_system
  }

@DrawableRes
internal fun ThemeMode.iconRes(): Int =
  when (this) {
    ThemeMode.Light -> R.drawable.ic_light_mode
    ThemeMode.Dark -> R.drawable.ic_dark_mode
    ThemeMode.System -> R.drawable.ic_brightness_auto
  }

/** A line under the word, for the choice that does not explain itself: automatic. Light and dark say what they do. */
@StringRes
internal fun ThemeMode.hintRes(): Int? = if (this == ThemeMode.System) R.string.theme_system_hint else null
