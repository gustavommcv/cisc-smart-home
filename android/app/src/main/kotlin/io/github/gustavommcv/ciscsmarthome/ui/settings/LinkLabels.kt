package io.github.gustavommcv.ciscsmarthome.ui.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.LinkKind

@StringRes
internal fun LinkKind.labelRes(): Int =
  when (this) {
    LinkKind.BoardWifi -> R.string.link_board_wifi
    LinkKind.CurrentNetwork -> R.string.link_current_network
  }

/** A line under the word, for both choices: neither says by itself what it does to the Wi-Fi of the phone. */
@StringRes
internal fun LinkKind.hintRes(): Int =
  when (this) {
    LinkKind.BoardWifi -> R.string.link_board_wifi_hint
    LinkKind.CurrentNetwork -> R.string.link_current_network_hint
  }

@DrawableRes
internal fun LinkKind.iconRes(): Int =
  when (this) {
    LinkKind.BoardWifi -> R.drawable.ic_sensors
    LinkKind.CurrentNetwork -> R.drawable.ic_home
  }
