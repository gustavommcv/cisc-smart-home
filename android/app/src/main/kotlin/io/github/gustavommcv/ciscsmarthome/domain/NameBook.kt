package io.github.gustavommcv.ciscsmarthome.domain

import kotlinx.coroutines.flow.StateFlow

/** The names the user gave to rooms and devices, and the icons they gave to rooms, kept for the next time the app opens. The board keeps none: they are the phone's. */
interface NameBook {
  val names: StateFlow<Names>

  val icons: StateFlow<AreaIcons>

  /** Gives the room [icon], or, with none, takes the user's icon away and the default comes back. */
  fun chooseAreaIcon(id: AreaId, icon: RoomIcon?)

  /** Names the room, or, with no name or a blank one, takes the user's name away and the default comes back. */
  fun renameArea(id: AreaId, name: String?)

  fun renameDevice(id: DeviceId, name: String?)
}
