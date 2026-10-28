package io.github.gustavommcv.ciscsmarthome.service

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The sound of the alert of a leak is a file of the app, and it has to be one Android can play: an Ogg file, small, and not an empty one. */
class AlarmSoundTest {
  private val sound = File("src/main/res/raw/gas_alarm.ogg")

  @Test
  fun `the sound is in the app`() {
    assertTrue(sound.isFile)
  }

  @Test
  fun `it is an Ogg file, which is what the first bytes say`() {
    assertEquals("OggS", sound.inputStream().use { String(it.readNBytes(4), Charsets.US_ASCII) })
  }

  @Test
  fun `it is neither empty nor a heavy file for a sound of a few seconds`() {
    assertTrue("${sound.length()} bytes", sound.length() in 5_000..100_000)
  }
}
