package io.github.gustavommcv.ciscsmarthome

import androidx.annotation.StringRes
import io.github.gustavommcv.ciscsmarthome.domain.AreaId

/**
 * The name of a room in Portuguese, for the two rooms of the maquete. The board names them in English;
 * a room the app does not know keeps the name the board gave it, so this says nothing about it.
 */
@StringRes
fun AreaId.nameRes(): Int? =
  when (value) {
    "kitchen" -> R.string.area_kitchen
    "bedroom" -> R.string.area_bedroom
    else -> null
  }
