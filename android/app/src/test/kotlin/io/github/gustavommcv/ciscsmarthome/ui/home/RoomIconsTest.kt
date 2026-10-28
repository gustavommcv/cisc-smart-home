package io.github.gustavommcv.ciscsmarthome.ui.home

import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.AreaId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RoomIconsTest {
  @Test
  fun `the kitchen is a kitchen, not a flame`() {
    assertEquals(R.drawable.ic_kitchen, AreaId("kitchen").iconRes())
  }

  @Test
  fun `the bedroom is a bed`() {
    assertEquals(R.drawable.ic_bed, AreaId("bedroom").iconRes())
  }

  @Test
  fun `a room the app does not know is a house`() {
    assertEquals(R.drawable.ic_home, AreaId("garage").iconRes())
  }

  @Test
  fun `each known room has an icon of its own`() {
    assertNotEquals(AreaId("kitchen").iconRes(), AreaId("bedroom").iconRes())
  }
}
