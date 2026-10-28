package io.github.gustavommcv.ciscsmarthome.ui.home

import io.github.gustavommcv.ciscsmarthome.domain.Names
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** What is kept of what was typed in the dialog: only a name that is the user's own, so a room that was only given an icon goes on following the default name. */
class NameToKeepTest {
  @Test
  fun `the default name, untouched, is not kept as the user's`() {
    assertNull(nameToKeep("Quarto", default = "Quarto"))
  }

  @Test
  fun `the default name with spaces around it is the default name`() {
    assertNull(nameToKeep("  Quarto ", default = "Quarto"))
  }

  @Test
  fun `a different name is kept, cleaned`() {
    assertEquals("Quarto do Pedro", nameToKeep("  Quarto do Pedro ", default = "Quarto"))
  }

  @Test
  fun `a blank name is no name`() {
    assertNull(nameToKeep("   ", default = "Quarto"))
    assertNull(nameToKeep("", default = "Quarto"))
  }

  @Test
  fun `a name that is the default only after it is cut is the default`() {
    val long = "d".repeat(Names.MAX_LENGTH)
    assertNull(nameToKeep(long + "extra", default = long))
  }

  @Test
  fun `a name that differs only in case is the user's own`() {
    assertEquals("quarto", nameToKeep("quarto", default = "Quarto"))
  }
}
