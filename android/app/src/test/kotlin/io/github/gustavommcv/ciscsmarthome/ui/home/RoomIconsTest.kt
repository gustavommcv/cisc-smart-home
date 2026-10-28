package io.github.gustavommcv.ciscsmarthome.ui.home

import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.AreaId
import io.github.gustavommcv.ciscsmarthome.domain.RoomIcon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The picture and the words of each icon a room can have. */
class RoomIconsTest {
  @Test
  fun `the kitchen is a kitchen, not a flame`() {
    assertEquals(R.drawable.ic_kitchen, RoomIcon.Kitchen.iconRes())
  }

  @Test
  fun `the bedroom is a bed, and the default is a house`() {
    assertEquals(R.drawable.ic_bed, RoomIcon.Bedroom.iconRes())
    assertEquals(R.drawable.ic_home, RoomIcon.Home.iconRes())
  }

  @Test
  fun `each icon has the picture its name says`() {
    assertEquals(
      listOf(
        R.drawable.ic_home,
        R.drawable.ic_kitchen,
        R.drawable.ic_bed,
        R.drawable.ic_weekend,
        R.drawable.ic_dining,
        R.drawable.ic_bathtub,
        R.drawable.ic_local_laundry_service,
        R.drawable.ic_desk,
        R.drawable.ic_child_care,
        R.drawable.ic_door_front,
        R.drawable.ic_balcony,
        R.drawable.ic_yard,
        R.drawable.ic_garage,
      ),
      RoomIcon.entries.map { it.iconRes() },
    )
  }

  @Test
  fun `the icon a room has by default is no choice, so the room goes on following the default`() {
    assertNull(RoomIcon.Kitchen.asChoiceFor(AreaId("kitchen")))
    assertNull(RoomIcon.Bedroom.asChoiceFor(AreaId("bedroom")))
    assertNull(RoomIcon.Home.asChoiceFor(AreaId("garage")))
  }

  @Test
  fun `any other icon is the choice of the user`() {
    assertEquals(RoomIcon.Yard, RoomIcon.Yard.asChoiceFor(AreaId("kitchen")))
    assertEquals(RoomIcon.Kitchen, RoomIcon.Kitchen.asChoiceFor(AreaId("bedroom")))
    assertEquals(RoomIcon.Home, RoomIcon.Home.asChoiceFor(AreaId("kitchen")))
  }

  @Test
  fun `no two icons are the same picture`() {
    assertEquals(RoomIcon.entries.size, RoomIcon.entries.map { it.iconRes() }.toSet().size)
  }

  @Test
  fun `each icon has the word that says it, for a screen reader and for the user`() {
    assertEquals(
      listOf(
        R.string.room_icon_home,
        R.string.room_icon_kitchen,
        R.string.room_icon_bedroom,
        R.string.room_icon_living,
        R.string.room_icon_dining,
        R.string.room_icon_bathroom,
        R.string.room_icon_laundry,
        R.string.room_icon_office,
        R.string.room_icon_nursery,
        R.string.room_icon_entrance,
        R.string.room_icon_balcony,
        R.string.room_icon_yard,
        R.string.room_icon_garage,
      ),
      RoomIcon.entries.map { it.labelRes() },
    )
  }
}
