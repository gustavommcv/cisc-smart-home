package io.github.gustavommcv.ciscsmarthome.ui.home

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class NumberTextTest {
  private val portuguese = Locale.forLanguageTag("pt-BR")

  @Test
  fun `a whole number has no decimals`() {
    assertEquals("142", formatNumber(142.0, Locale.ROOT))
    assertEquals("0", formatNumber(0.0, Locale.ROOT))
    assertEquals("-3", formatNumber(-3.0, Locale.ROOT))
  }

  @Test
  fun `a decimal number has one decimal`() {
    assertEquals("21.4", formatNumber(21.4, Locale.ROOT))
    assertEquals("74.1", formatNumber(74.13, Locale.ROOT))
    assertEquals("74.2", formatNumber(74.16, Locale.ROOT))
  }

  @Test
  fun `the decimal separator is the one of the language`() {
    assertEquals("21,4", formatNumber(21.4, portuguese))
  }

  @Test
  fun `by default numbers are written the Portuguese way, whatever the language of the phone`() {
    val before = Locale.getDefault()
    try {
      Locale.setDefault(Locale.US)
      assertEquals("21,4", formatNumber(21.4))
      assertEquals("142", formatNumber(142.0))
    } finally {
      Locale.setDefault(before)
    }
  }

  @Test
  fun `thousands are not grouped`() {
    assertEquals("1023", formatNumber(1023.0, portuguese))
    assertEquals("12345.6", formatNumber(12345.6, Locale.ROOT))
  }
}
