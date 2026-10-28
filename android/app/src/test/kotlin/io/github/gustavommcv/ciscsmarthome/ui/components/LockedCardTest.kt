package io.github.gustavommcv.ciscsmarthome.ui.components

import androidx.compose.ui.graphics.Color
import io.github.gustavommcv.ciscsmarthome.ui.theme.DarkColors
import io.github.gustavommcv.ciscsmarthome.ui.theme.LightColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The dashed line of what is not there yet: grey for what is only to come, a faded red for what takes something away, so it is not taken for the others. */
class LockedCardTest {
  private val schemes = listOf("light" to LightColors, "dark" to DarkColors)

  @Test
  fun `what is only to come has the grey line of the palette`() {
    schemes.forEach { (_, scheme) -> assertEquals(scheme.outline, Tone.Neutral.lockedLine(scheme)) }
  }

  @Test
  fun `what takes something away has a red line, faded, for it does not work yet`() {
    schemes.forEach { (name, scheme) ->
      val line = Tone.Alert.lockedLine(scheme)
      assertEquals("the red of the $name theme", scheme.error.copy(alpha = line.alpha), line)
      assertTrue("faded, in the $name theme", line.alpha in 0.2f..0.8f)
    }
  }

  @Test
  fun `the red line is not the grey one`() {
    schemes.forEach { (name, scheme) -> assertNotEquals(name, Tone.Neutral.lockedLine(scheme), Tone.Alert.lockedLine(scheme)) }
  }

  @Test
  fun `the primary tone, which no place uses, keeps the grey line`() {
    schemes.forEach { (_, scheme) -> assertEquals(scheme.outline, Tone.Primary.lockedLine(scheme)) }
  }

  @Test
  fun `the title of a place that takes something away is red too, and can be read`() {
    schemes.forEach { (name, scheme) ->
      val title = Tone.Alert.lockedTitle(scheme)
      assertEquals("the red of the $name theme", scheme.error.copy(alpha = title.alpha), title)
      assertTrue("less faded than the line, in the $name theme", title.alpha > Tone.Alert.lockedLine(scheme).alpha && title.alpha < 1f)
    }
  }

  @Test
  fun `the title of the other places keeps the color of the text`() {
    schemes.forEach { (_, scheme) ->
      assertEquals(Color.Unspecified, Tone.Neutral.lockedTitle(scheme))
      assertEquals(Color.Unspecified, Tone.Primary.lockedTitle(scheme))
    }
  }

  @Test
  fun `the faded red is still a color, not transparent`() {
    schemes.forEach { (_, scheme) -> assertNotEquals(Color.Transparent, Tone.Alert.lockedLine(scheme)) }
  }
}
