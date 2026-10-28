package io.github.gustavommcv.ciscsmarthome.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The icons the user can give a room, and which one a room has when the user gave none. */
class RoomIconTest {
  private val kitchen = AreaId("kitchen")

  @Test
  fun `each icon is kept under a key of its own`() {
    assertEquals(RoomIcon.entries.size, RoomIcon.entries.map { it.key }.toSet().size)
  }

  @Test
  fun `each key finds its icon again`() {
    RoomIcon.entries.forEach { assertEquals(it, RoomIcon.fromKey(it.key)) }
  }

  @Test
  fun `a key the app does not know is no icon`() {
    assertNull(RoomIcon.fromKey("spaceship"))
    assertNull(RoomIcon.fromKey(""))
  }

  @Test
  fun `the keys are the ones kept in the files of the users, so they never change`() {
    assertEquals(
      listOf("home", "kitchen", "bed", "weekend", "dining", "bathtub", "local_laundry_service", "desk", "child_care", "door_front", "balcony", "yard", "garage"),
      RoomIcon.entries.map { it.key },
    )
  }

  @Test
  fun `a kitchen is a kitchen and a bedroom a bed`() {
    assertEquals(RoomIcon.Kitchen, RoomIcon.defaultOf(AreaId("kitchen")))
    assertEquals(RoomIcon.Bedroom, RoomIcon.defaultOf(AreaId("bedroom")))
  }

  @Test
  fun `a room the app does not know is a house, so it still has an icon`() {
    assertEquals(RoomIcon.Home, RoomIcon.defaultOf(AreaId("garage")))
  }

  @Test
  fun `a room has the icon the user gave it`() {
    assertEquals(RoomIcon.Garage, AreaIcons(mapOf(kitchen to RoomIcon.Garage)).of(kitchen))
  }

  @Test
  fun `a room with no icon of the user's has the default`() {
    assertEquals(RoomIcon.Kitchen, AreaIcons().of(kitchen))
    assertEquals(RoomIcon.Kitchen, AreaIcons(mapOf(AreaId("bedroom") to RoomIcon.Yard)).of(kitchen))
  }

  @Test
  fun `the icon of the user's can be the same as the default, and stays the user's`() {
    assertEquals(RoomIcon.Kitchen, AreaIcons(mapOf(kitchen to RoomIcon.Kitchen)).of(kitchen))
  }
}
