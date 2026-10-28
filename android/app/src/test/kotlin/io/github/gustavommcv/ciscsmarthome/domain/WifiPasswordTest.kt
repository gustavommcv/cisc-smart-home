package io.github.gustavommcv.ciscsmarthome.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** What a Wi-Fi password can be, so a mistyped one is caught before the phone is asked to join with it. */
class WifiPasswordTest {
  @Test
  fun `a password of 8 to 63 characters is valid`() {
    assertTrue(WifiPassword.isValid("a".repeat(8)))
    assertTrue(WifiPassword.isValid("a".repeat(63)))
    assertTrue(WifiPassword.isValid("a".repeat(30)))
  }

  @Test
  fun `a password of fewer than 8 characters is not valid`() {
    assertFalse(WifiPassword.isValid("a".repeat(7)))
    assertFalse(WifiPassword.isValid(""))
  }

  @Test
  fun `a password of more than 63 characters is not valid, for the phone cannot join with it`() {
    assertFalse(WifiPassword.isValid("a".repeat(64)))
  }

  @Test
  fun `spaces and signs are part of a password`() {
    assertTrue(WifiPassword.isValid("my wifi pass!"))
    assertTrue(WifiPassword.isValid("        "))
  }

  @Test
  fun `a character outside printable ASCII is not valid, for the phone would send it as other bytes`() {
    assertFalse(WifiPassword.isValid("senha-com-acento-é"))
    assertFalse(WifiPassword.isValid("12345678\n"))
    assertFalse(WifiPassword.isValid("12345678\u007f"))
  }

  @Test
  fun `the limits are the ones of WPA2`() {
    assertEquals(8, WifiPassword.MIN_LENGTH)
    assertEquals(63, WifiPassword.MAX_LENGTH)
  }
}
