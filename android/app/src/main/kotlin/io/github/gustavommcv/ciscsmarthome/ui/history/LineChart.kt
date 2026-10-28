package io.github.gustavommcv.ciscsmarthome.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The height of the chart of one sensor, and of each band of the chart that compares two. */
internal val ChartHeight = 160.dp
internal val BandHeight = 96.dp
private val LineWidth = 2.dp
private val DotRadius = 3.dp

// The line has half its width above the top and below the bottom of the box, so it is kept this far from the edges.
private val PlotPadding = 8.dp

/**
 * A line chart, drawn by hand: one line, in the [line] color (the color of the app by default), over three lines of grid. Each stretch the app heard
 * is a line of its own, so the time it heard nothing stays empty, and a stretch of a single reading is a dot. [description] is what a screen reader says.
 */
@Composable
internal fun LineChart(chart: Chart, description: String, modifier: Modifier = Modifier, line: Color = MaterialTheme.colorScheme.primary, height: Dp = ChartHeight) {
  val grid = MaterialTheme.colorScheme.outlineVariant
  Canvas(modifier = modifier.fillMaxWidth().height(height).semantics { contentDescription = description }) {
    val padding = PlotPadding.toPx()
    val plotHeight = size.height - 2 * padding
    fun ChartPoint.place() = Offset(x * size.width, padding + (1 - y) * plotHeight)
    listOf(0f, 0.5f, 1f).forEach { fraction ->
      val y = padding + plotHeight * fraction
      drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = Dp.Hairline.toPx().coerceAtLeast(1f))
    }
    chart.lines.forEach { points ->
      if (points.size == 1) {
        drawCircle(line, DotRadius.toPx(), center = points.single().place())
      } else {
        val path = Path()
        points.forEachIndexed { index, point -> point.place().let { if (index == 0) path.moveTo(it.x, it.y) else path.lineTo(it.x, it.y) } }
        drawPath(path, line, style = Stroke(width = LineWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
      }
    }
  }
}
