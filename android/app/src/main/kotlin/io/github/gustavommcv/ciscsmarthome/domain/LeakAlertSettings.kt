package io.github.gustavommcv.ciscsmarthome.domain

import kotlinx.coroutines.flow.StateFlow

/** How the user chose to be warned of a leak in each room, kept for the next time the app opens. */
interface LeakAlertSettings {
  val modes: StateFlow<LeakAlertModes>

  /** Keeps [mode] as the way of warning for the room, and makes it the one in force at once. Choosing [LeakAlertMode.Default] keeps nothing: the room goes back to following the default. */
  fun choose(areaId: AreaId, mode: LeakAlertMode)
}
