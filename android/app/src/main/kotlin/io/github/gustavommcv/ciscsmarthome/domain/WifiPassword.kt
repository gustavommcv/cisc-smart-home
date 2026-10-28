package io.github.gustavommcv.ciscsmarthome.domain

/** What a WPA2 password can be: 8 to 63 printable ASCII characters. The phone cannot join with anything else, so a mistyped one is caught before it is asked to. */
object WifiPassword {
  const val MIN_LENGTH = 8
  const val MAX_LENGTH = 63

  fun isValid(text: String): Boolean = text.length in MIN_LENGTH..MAX_LENGTH && text.all { it in ' '..'~' }
}
