package io.github.gustavommcv.ciscsmarthome.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The measures and the corners are a scale, so the screens ask for a step of it instead of a number. */
class DimensionsTest {
  private val steps = listOf(Spacing.Xs, Spacing.S, Spacing.M, Spacing.L, Spacing.Xl)

  private fun CornerBasedShape.radius(): Float = topStart.toPx(Size(1_000f, 1_000f), Density(1f))

  @Test
  fun `each step of the spacing is bigger than the one before`() {
    steps.zipWithNext().forEach { (smaller, bigger) -> assertTrue("$smaller is not smaller than $bigger", smaller < bigger) }
  }

  @Test
  fun `every step of the spacing sits on the 4 dp grid`() {
    steps.forEach { step -> assertEquals("$step is off the grid", 0f, step.value % 4f, 0f) }
  }

  @Test
  fun `the corners grow from the smallest to the biggest`() {
    val radii = listOf(CiscShapes.extraSmall, CiscShapes.small, CiscShapes.medium, CiscShapes.large, CiscShapes.extraLarge).map { it.radius() }
    radii.zipWithNext().forEach { (smaller, bigger) -> assertTrue("$smaller is not smaller than $bigger", smaller < bigger) }
  }

  @Test
  fun `a card has the rounded corner of the design`() {
    assertEquals(18.dp.value, CiscShapes.large.radius(), 0f)
  }
}
