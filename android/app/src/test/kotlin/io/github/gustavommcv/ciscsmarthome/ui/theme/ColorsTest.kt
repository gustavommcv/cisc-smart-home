package io.github.gustavommcv.ciscsmarthome.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min
import org.junit.Assert.assertTrue
import org.junit.Test

/** The palette is readable: every text and every drawing sits on a ground it has enough contrast with (WCAG 2.1). */
class ColorsTest {
  private val schemes = listOf("light" to LightColors, "dark" to DarkColors)

  private fun contrast(a: Color, b: Color): Double {
    val hi = max(a.luminance(), b.luminance())
    val lo = min(a.luminance(), b.luminance())
    return (hi + 0.05) / (lo + 0.05)
  }

  private fun assertContrast(name: String, scheme: ColorScheme, minimum: Double, vararg pairs: Pair<(ColorScheme) -> Color, (ColorScheme) -> Color>, label: List<String>) {
    pairs.forEachIndexed { i, (front, ground) ->
      val ratio = contrast(front(scheme), ground(scheme))
      assertTrue("${label[i]} in the $name theme has contrast $ratio, and needs $minimum", ratio >= minimum)
    }
  }

  @Test
  fun `text is readable on every ground it is drawn on`() {
    schemes.forEach { (name, scheme) ->
      assertContrast(
        name,
        scheme,
        4.5,
        { s: ColorScheme -> s.onBackground } to { s: ColorScheme -> s.background },
        { s: ColorScheme -> s.onSurface } to { s: ColorScheme -> s.surface },
        { s: ColorScheme -> s.onSurfaceVariant } to { s: ColorScheme -> s.background },
        { s: ColorScheme -> s.onSurfaceVariant } to { s: ColorScheme -> s.surface },
        { s: ColorScheme -> s.onSurfaceVariant } to { s: ColorScheme -> s.surfaceVariant },
        { s: ColorScheme -> s.primary } to { s: ColorScheme -> s.background },
        { s: ColorScheme -> s.primary } to { s: ColorScheme -> s.surface },
        { s: ColorScheme -> s.onPrimary } to { s: ColorScheme -> s.primary },
        { s: ColorScheme -> s.onPrimaryContainer } to { s: ColorScheme -> s.primaryContainer },
        { s: ColorScheme -> s.onSecondaryContainer } to { s: ColorScheme -> s.secondaryContainer },
        { s: ColorScheme -> s.error } to { s: ColorScheme -> s.background },
        { s: ColorScheme -> s.error } to { s: ColorScheme -> s.surface },
        { s: ColorScheme -> s.onError } to { s: ColorScheme -> s.error },
        { s: ColorScheme -> s.onErrorContainer } to { s: ColorScheme -> s.errorContainer },
        { s: ColorScheme -> s.inverseOnSurface } to { s: ColorScheme -> s.inverseSurface },
        label =
          listOf(
            "onBackground/background", "onSurface/surface", "onSurfaceVariant/background", "onSurfaceVariant/surface", "onSurfaceVariant/surfaceVariant", "primary/background",
            "primary/surface", "onPrimary/primary", "onPrimaryContainer/primaryContainer", "onSecondaryContainer/secondaryContainer", "error/background", "error/surface",
            "onError/error", "onErrorContainer/errorContainer", "inverseOnSurface/inverseSurface",
          ),
      )
    }
  }

  @Test
  fun `the outline of a control is seen against the ground`() {
    schemes.forEach { (name, scheme) ->
      assertContrast(
        name,
        scheme,
        3.0,
        { s: ColorScheme -> s.outline } to { s: ColorScheme -> s.background },
        { s: ColorScheme -> s.outline } to { s: ColorScheme -> s.surface },
        label = listOf("outline/background", "outline/surface"),
      )
    }
  }

  @Test
  fun `the light theme is light and the dark theme is dark`() {
    assertTrue(LightColors.background.luminance() > 0.8)
    assertTrue(LightColors.surface.luminance() > 0.8)
    assertTrue(DarkColors.background.luminance() < 0.05)
    assertTrue(DarkColors.surface.luminance() < 0.05)
  }

  @Test
  fun `a card stands out from the screen behind it`() {
    schemes.forEach { (name, scheme) ->
      assertTrue("surface and background are the same in the $name theme", scheme.surface != scheme.background)
    }
  }

  @Test
  fun `the line around a card is quieter than the outline of a control`() {
    schemes.forEach { (name, scheme) ->
      assertTrue("in the $name theme the hairline is stronger than the outline", contrast(scheme.outlineVariant, scheme.surface) < contrast(scheme.outline, scheme.surface))
    }
  }
}
