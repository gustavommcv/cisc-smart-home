package io.github.gustavommcv.ciscsmarthome.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class LinkKindTest {
  @Test
  fun `with nothing chosen the app joins the network of the board by itself`() {
    assertEquals(LinkKind.BoardWifi, LinkKind.Default)
  }

  @Test
  fun `there are the two ways the app has today`() {
    assertEquals(listOf(LinkKind.BoardWifi, LinkKind.CurrentNetwork), LinkKind.entries)
  }
}
