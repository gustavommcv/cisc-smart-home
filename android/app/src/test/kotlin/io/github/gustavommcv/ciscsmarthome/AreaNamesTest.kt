package io.github.gustavommcv.ciscsmarthome

import io.github.gustavommcv.ciscsmarthome.domain.AreaId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AreaNamesTest {
  @Test
  fun `the two rooms of the maquete have their names in Portuguese`() {
    assertEquals(R.string.area_kitchen, AreaId("kitchen").nameRes())
    assertEquals(R.string.area_bedroom, AreaId("bedroom").nameRes())
  }

  @Test
  fun `a room the app does not know has no name of its own`() {
    assertNull(AreaId("garage").nameRes())
    assertNull(AreaId("").nameRes())
  }
}
