package io.github.gustavommcv.ciscsmarthome.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.gustavommcv.ciscsmarthome.R

/** The places of the bar at the bottom, in the order it shows them. */
internal enum class Tab(@StringRes val labelRes: Int, @DrawableRes val iconRes: Int) {
  Home(R.string.tab_home, R.drawable.ic_home),
  History(R.string.tab_history, R.drawable.ic_history),
  Settings(R.string.tab_settings, R.drawable.ic_settings);

  /** Where the back button goes from here, or null when it is not for the bar to say: from home it is the screen's own, or the system's, which closes the app. */
  fun back(): Tab? = if (this == Home) null else Home

  companion object {
    /** The tab the app opens on. */
    val Start = Home
  }
}
