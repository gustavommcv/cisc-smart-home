package io.github.gustavommcv.ciscsmarthome.ui.settings

import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.LinkKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** The words and the icon of each way of reaching the board. */
class LinkLabelsTest {
  @Test
  fun `each link has its own word`() {
    assertEquals(R.string.link_board_wifi, LinkKind.BoardWifi.labelRes())
    assertEquals(R.string.link_current_network, LinkKind.CurrentNetwork.labelRes())
  }

  @Test
  fun `each link has a line to say what it does`() {
    assertEquals(R.string.link_board_wifi_hint, LinkKind.BoardWifi.hintRes())
    assertEquals(R.string.link_current_network_hint, LinkKind.CurrentNetwork.hintRes())
  }

  @Test
  fun `each link has its own icon`() {
    assertEquals(R.drawable.ic_sensors, LinkKind.BoardWifi.iconRes())
    assertEquals(R.drawable.ic_home, LinkKind.CurrentNetwork.iconRes())
    assertNotEquals(LinkKind.BoardWifi.iconRes(), LinkKind.CurrentNetwork.iconRes())
  }
}
