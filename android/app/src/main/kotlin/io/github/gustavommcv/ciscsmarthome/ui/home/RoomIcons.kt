package io.github.gustavommcv.ciscsmarthome.ui.home

import androidx.annotation.DrawableRes
import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.AreaId
import io.github.gustavommcv.ciscsmarthome.domain.AreaIds

/** The icon of a room: a kitchen is a kitchen and a bedroom a bed. A room the app does not know is a house, so it still has one. */
@DrawableRes
internal fun AreaId.iconRes(): Int =
  when (this) {
    AreaIds.Kitchen -> R.drawable.ic_kitchen
    AreaIds.Bedroom -> R.drawable.ic_bed
    else -> R.drawable.ic_home
  }
