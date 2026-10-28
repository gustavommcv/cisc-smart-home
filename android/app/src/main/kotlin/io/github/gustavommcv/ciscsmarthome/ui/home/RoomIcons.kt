package io.github.gustavommcv.ciscsmarthome.ui.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.AreaId
import io.github.gustavommcv.ciscsmarthome.domain.RoomIcon

/** The picture of an icon a room can have. */
@DrawableRes
internal fun RoomIcon.iconRes(): Int =
  when (this) {
    RoomIcon.Home -> R.drawable.ic_home
    RoomIcon.Kitchen -> R.drawable.ic_kitchen
    RoomIcon.Bedroom -> R.drawable.ic_bed
    RoomIcon.Living -> R.drawable.ic_weekend
    RoomIcon.Dining -> R.drawable.ic_dining
    RoomIcon.Bathroom -> R.drawable.ic_bathtub
    RoomIcon.Laundry -> R.drawable.ic_local_laundry_service
    RoomIcon.Office -> R.drawable.ic_desk
    RoomIcon.Nursery -> R.drawable.ic_child_care
    RoomIcon.Entrance -> R.drawable.ic_door_front
    RoomIcon.Balcony -> R.drawable.ic_balcony
    RoomIcon.Yard -> R.drawable.ic_yard
    RoomIcon.Garage -> R.drawable.ic_garage
  }

/** What to keep when the user picks this icon for a room: nothing, if it is the one the room has by default, so the room goes on following the default. */
internal fun RoomIcon.asChoiceFor(id: AreaId): RoomIcon? = takeUnless { it == RoomIcon.defaultOf(id) }

/** What an icon is called, for the screen reader and for the user. */
@StringRes
internal fun RoomIcon.labelRes(): Int =
  when (this) {
    RoomIcon.Home -> R.string.room_icon_home
    RoomIcon.Kitchen -> R.string.room_icon_kitchen
    RoomIcon.Bedroom -> R.string.room_icon_bedroom
    RoomIcon.Living -> R.string.room_icon_living
    RoomIcon.Dining -> R.string.room_icon_dining
    RoomIcon.Bathroom -> R.string.room_icon_bathroom
    RoomIcon.Laundry -> R.string.room_icon_laundry
    RoomIcon.Office -> R.string.room_icon_office
    RoomIcon.Nursery -> R.string.room_icon_nursery
    RoomIcon.Entrance -> R.string.room_icon_entrance
    RoomIcon.Balcony -> R.string.room_icon_balcony
    RoomIcon.Yard -> R.string.room_icon_yard
    RoomIcon.Garage -> R.string.room_icon_garage
  }
