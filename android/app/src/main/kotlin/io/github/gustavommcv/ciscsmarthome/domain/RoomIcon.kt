package io.github.gustavommcv.ciscsmarthome.domain

/**
 * The icons the user can give a room. The [key] is what is kept on the phone, so it never changes once an app has shipped with it: the file of a
 * user who chose one would stop reading. The order is the one the choice is shown in.
 */
enum class RoomIcon(val key: String) {
  Home("home"),
  Kitchen("kitchen"),
  Bedroom("bed"),
  Living("weekend"),
  Dining("dining"),
  Bathroom("bathtub"),
  Laundry("local_laundry_service"),
  Office("desk"),
  Nursery("child_care"),
  Entrance("door_front"),
  Balcony("balcony"),
  Yard("yard"),
  Garage("garage");

  companion object {
    fun fromKey(key: String): RoomIcon? = entries.firstOrNull { it.key == key }

    /** The icon of a room the user gave none: a kitchen is a kitchen and a bedroom a bed, and a room the app does not know is a house, so it still has one. */
    fun defaultOf(id: AreaId): RoomIcon =
      when (id) {
        AreaIds.Kitchen -> Kitchen
        AreaIds.Bedroom -> Bedroom
        else -> Home
      }
  }
}

/** The icons the user gave to rooms, by id. A room with none has the default one. */
data class AreaIcons(val areas: Map<AreaId, RoomIcon> = emptyMap()) {
  fun of(id: AreaId): RoomIcon = areas[id] ?: RoomIcon.defaultOf(id)
}
