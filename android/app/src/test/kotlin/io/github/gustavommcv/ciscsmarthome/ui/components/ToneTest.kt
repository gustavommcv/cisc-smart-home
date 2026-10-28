package io.github.gustavommcv.ciscsmarthome.ui.components

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.github.gustavommcv.ciscsmarthome.ui.theme.DarkColors
import io.github.gustavommcv.ciscsmarthome.ui.theme.LightColors
import kotlin.math.max
import kotlin.math.min
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The tones of the base components: which roles of the palette each one takes, and that what is drawn in it can be read. */
class ToneTest {
  private val schemes = listOf("light" to LightColors, "dark" to DarkColors)

  private fun contrast(a: Color, b: Color): Double {
    val hi = max(a.luminance(), b.luminance())
    val lo = min(a.luminance(), b.luminance())
    return (hi + 0.05) / (lo + 0.05)
  }

  private fun ColorScheme.of(tone: Tone) = tone.colors(this)

  @Test
  fun `what is drawn in a tone can be read on its container`() {
    schemes.forEach { (name, scheme) ->
      Tone.entries.forEach { tone ->
        val colors = scheme.of(tone)
        val ratio = contrast(colors.content, colors.container)
        assertTrue("$tone content in the $name theme has contrast $ratio", ratio >= 4.5)
      }
    }
  }

  @Test
  fun `what is drawn on the accent of a tone can be seen`() {
    schemes.forEach { (name, scheme) ->
      Tone.entries.forEach { tone ->
        val colors = scheme.of(tone)
        val ratio = contrast(colors.onAccent, colors.accent)
        assertTrue("$tone accent in the $name theme has contrast $ratio", ratio >= 4.5)
      }
    }
  }

  @Test
  fun `the primary tone takes the primary roles`() {
    schemes.forEach { (_, s) -> assertEquals(ToneColors(s.primaryContainer, s.onPrimaryContainer, s.primary, s.onPrimary), s.of(Tone.Primary)) }
  }

  @Test
  fun `the alert tone takes the error roles`() {
    schemes.forEach { (_, s) -> assertEquals(ToneColors(s.errorContainer, s.onErrorContainer, s.error, s.onError), s.of(Tone.Alert)) }
  }

  @Test
  fun `the neutral tone takes the quiet roles`() {
    schemes.forEach { (_, s) -> assertEquals(ToneColors(s.surfaceVariant, s.onSurfaceVariant, s.onSurfaceVariant, s.surfaceVariant), s.of(Tone.Neutral)) }
  }

  @Test
  fun `the tones are told apart on the container`() {
    schemes.forEach { (_, s) ->
      val containers = Tone.entries.map { s.of(it).container }
      assertEquals(containers.size, containers.toSet().size)
    }
  }

  @Test
  fun `the same tone is not the same color in the light and the dark theme`() {
    Tone.entries.forEach { tone -> assertNotEquals(LightColors.of(tone), DarkColors.of(tone)) }
  }
}
