package io.github.gustavommcv.ciscsmarthome.domain

import kotlinx.coroutines.flow.StateFlow

/** The names the user gave, kept for the next time the app opens. The board keeps none: they are the phone's. */
interface NameBook {
  val names: StateFlow<Names>

  /** Names the room, or, with no name or a blank one, takes the user's name away and the default comes back. */
  fun renameArea(id: AreaId, name: String?)

  fun renameDevice(id: DeviceId, name: String?)
}
