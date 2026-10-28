package io.github.gustavommcv.ciscsmarthome.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.gustavommcv.ciscsmarthome.domain.AreaIcons
import io.github.gustavommcv.ciscsmarthome.domain.AreaId
import io.github.gustavommcv.ciscsmarthome.domain.DeviceId
import io.github.gustavommcv.ciscsmarthome.domain.Names
import io.github.gustavommcv.ciscsmarthome.domain.RoomIcon
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PreferencesNameBookTest {
  private val context: Context = ApplicationProvider.getApplicationContext()
  private val file = "user-names-test"
  private val preferences get() = context.getSharedPreferences(file, Context.MODE_PRIVATE)
  private val kitchen = AreaId("kitchen")
  private val gasMonitor = DeviceId("gas-monitor")

  private fun book() = PreferencesNameBook(preferences)

  @Before
  fun clean() {
    preferences.edit().clear().commit()
  }

  @Test
  fun `nothing is named at first`() {
    assertEquals(Names(), book().names.value)
  }

  @Test
  fun `a room that is named has the name at once`() {
    val book = book()
    book.renameArea(kitchen, "Cozinha da vó")
    assertEquals(mapOf(kitchen to "Cozinha da vó"), book.names.value.areas)
  }

  @Test
  fun `a device that is named has the name at once`() {
    val book = book()
    book.renameDevice(gasMonitor, "Detector da pia")
    assertEquals(mapOf(gasMonitor to "Detector da pia"), book.names.value.devices)
  }

  @Test
  fun `the names come back after the app restarts`() {
    book().apply {
      renameArea(kitchen, "Cozinha da vó")
      renameDevice(gasMonitor, "Detector da pia")
    }
    // A new object over the same file is what the next run of the app gets.
    assertEquals(Names(areas = mapOf(kitchen to "Cozinha da vó"), devices = mapOf(gasMonitor to "Detector da pia")), book().names.value)
  }

  @Test
  fun `a later name replaces the earlier one`() {
    val book = book()
    book.renameArea(kitchen, "Cozinha da vó")
    book.renameArea(kitchen, "Copa")
    assertEquals("Copa", book().names.value.areas.getValue(kitchen))
  }

  @Test
  fun `a name is cleaned before it is kept`() {
    val book = book()
    book.renameArea(kitchen, "  Copa  ")
    book.renameDevice(gasMonitor, "d".repeat(Names.MAX_LENGTH + 10))
    assertEquals("Copa", book().names.value.areas.getValue(kitchen))
    assertEquals("d".repeat(Names.MAX_LENGTH), book().names.value.devices.getValue(gasMonitor))
  }

  @Test
  fun `no name, or a blank one, takes the name of the user away and the default comes back`() {
    val book = book()
    book.renameArea(kitchen, "Copa")
    book.renameDevice(gasMonitor, "Detector")
    book.renameArea(kitchen, null)
    book.renameDevice(gasMonitor, "   ")
    assertEquals(Names(), book.names.value)
    assertEquals(Names(), book().names.value)
  }

  @Test
  fun `taking away a name of one room leaves the names of the others`() {
    val book = book()
    book.renameArea(kitchen, "Copa")
    book.renameArea(AreaId("bedroom"), "Quarto do Pedro")
    book.renameArea(kitchen, null)
    assertEquals(mapOf(AreaId("bedroom") to "Quarto do Pedro"), book().names.value.areas)
  }

  @Test
  fun `a room and a device with the same id keep each its own name`() {
    val book = book()
    book.renameArea(AreaId("x"), "A sala")
    book.renameDevice(DeviceId("x"), "O aparelho")
    assertEquals(Names(areas = mapOf(AreaId("x") to "A sala"), devices = mapOf(DeviceId("x") to "O aparelho")), book().names.value)
  }

  @Test
  fun `the names are kept under the kind and the id, so a file from an earlier version still reads`() {
    val book = book()
    book.renameArea(kitchen, "Copa")
    book.renameDevice(gasMonitor, "Detector")
    assertEquals("Copa", preferences.getString("area:kitchen", null))
    assertEquals("Detector", preferences.getString("device:gas-monitor", null))
  }

  @Test
  fun `what the file has that is not a name is ignored`() {
    preferences.edit().putInt("area:garage", 3).putString("device:doorbell", "    ").putString("other:thing", "x").putString("area:", "no id").commit()
    assertEquals(Names(), book().names.value)
  }

  // ---- the icons of the rooms ------------------------------------------------------

  @Test
  fun `no room has an icon of the user's at first`() {
    assertEquals(AreaIcons(), book().icons.value)
  }

  @Test
  fun `a room that is given an icon has it at once`() {
    val book = book()
    book.chooseAreaIcon(kitchen, RoomIcon.Dining)
    assertEquals(mapOf(kitchen to RoomIcon.Dining), book.icons.value.areas)
  }

  @Test
  fun `the icons come back after the app restarts`() {
    book().apply {
      chooseAreaIcon(kitchen, RoomIcon.Dining)
      chooseAreaIcon(AreaId("bedroom"), RoomIcon.Nursery)
    }
    assertEquals(AreaIcons(mapOf(kitchen to RoomIcon.Dining, AreaId("bedroom") to RoomIcon.Nursery)), book().icons.value)
  }

  @Test
  fun `a later icon replaces the earlier one`() {
    val book = book()
    book.chooseAreaIcon(kitchen, RoomIcon.Dining)
    book.chooseAreaIcon(kitchen, RoomIcon.Yard)
    assertEquals(RoomIcon.Yard, book().icons.value.areas.getValue(kitchen))
  }

  @Test
  fun `no icon takes the icon of the user away and the default comes back, in the file too`() {
    val book = book()
    book.chooseAreaIcon(kitchen, RoomIcon.Dining)
    book.chooseAreaIcon(AreaId("bedroom"), RoomIcon.Nursery)
    book.chooseAreaIcon(kitchen, null)
    assertEquals(mapOf(AreaId("bedroom") to RoomIcon.Nursery), book.icons.value.areas)
    assertEquals(mapOf(AreaId("bedroom") to RoomIcon.Nursery), book().icons.value.areas)
  }

  @Test
  fun `the icons are kept under the room and the key of the icon, so a file from an earlier version still reads`() {
    book().chooseAreaIcon(kitchen, RoomIcon.Laundry)
    assertEquals("local_laundry_service", preferences.getString("area-icon:kitchen", null))
  }

  @Test
  fun `a name and an icon of the same room do not take each other away`() {
    val book = book()
    book.renameArea(kitchen, "Copa")
    book.chooseAreaIcon(kitchen, RoomIcon.Dining)
    book.renameArea(kitchen, null)
    assertEquals(Names(), book().names.value)
    assertEquals(RoomIcon.Dining, book().icons.value.areas.getValue(kitchen))
    book.renameArea(kitchen, "Copa")
    book.chooseAreaIcon(kitchen, null)
    assertEquals("Copa", book().names.value.areas.getValue(kitchen))
    assertEquals(AreaIcons(), book().icons.value)
  }

  @Test
  fun `an icon is not read as a name, whatever its room is called`() {
    book().chooseAreaIcon(AreaId("kitchen"), RoomIcon.Dining)
    assertEquals(Names(), book().names.value)
  }

  @Test
  fun `what the file has that is not an icon is ignored`() {
    preferences.edit().putInt("area-icon:garage", 3).putString("area-icon:attic", "spaceship").putString("area-icon:", "bed").putString("device-icon:doorbell", "bed").commit()
    assertEquals(AreaIcons(), book().icons.value)
  }

  @Test
  fun `a name that is too long in the file is cut when read`() {
    preferences.edit().putString("area:kitchen", "k".repeat(Names.MAX_LENGTH + 15)).commit()
    assertEquals("k".repeat(Names.MAX_LENGTH), book().names.value.areas.getValue(kitchen))
  }
}
