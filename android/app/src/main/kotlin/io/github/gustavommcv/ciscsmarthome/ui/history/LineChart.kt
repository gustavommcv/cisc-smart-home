package io.github.gustavommcv.ciscsmarthome.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gustavommcv.ciscsmarthome.domain.ChartRange
import io.github.gustavommcv.ciscsmarthome.domain.LimitKind
import io.github.gustavommcv.ciscsmarthome.ui.home.formatNumber

/** The height of the chart of one sensor, and of each band of the chart that compares two. */
internal val ChartHeight = 160.dp
internal val BandHeight = 96.dp
private val LineWidth = 2.dp
private val DotRadius = 3.dp
private val LimitWidth = 1.dp
private val LabelSize = 11.sp

// The line has half its width above the top and below the bottom of the box, so it is kept this far from the edges.
private val PlotPadding = 8.dp

private const val FILL_ALPHA = 0.18f
private const val BAND_ALPHA = 0.16f

/** A line drawn over a chart, with its words: where it is in the range ([fraction], 0 at the bottom to 1 at the top), and what it is. */
internal data class LimitLine(val fraction: Float, val label: String, val kind: LimitKind)

/** The colors of the chart: the line, the one for what is over the limit of an alarm, and a quiet one for the other limits, which are not the color of a line. */
private class ChartColors(val line: Color, val alarm: Color, val limit: Color, val grid: Color, val text: Color)

@Composable
private fun chartColors(line: Color): ChartColors =
  MaterialTheme.colorScheme.let { ChartColors(line, it.error, it.onSurfaceVariant, it.outlineVariant, it.onSurfaceVariant) }

private fun LimitLine.isAlarm() = kind == LimitKind.Alarm

// The lines of the grid, at the bottom, in the middle and at the top, and the numbers of the bottom and the top, so the scale is read and not guessed.
private fun DrawScope.drawGrid(range: ChartRange, colors: ChartColors, measurer: TextMeasurer, unit: String?, padding: Float, plotHeight: Float) {
  listOf(0f, 0.5f, 1f).forEach { fraction ->
    val y = padding + plotHeight * fraction
    drawLine(colors.grid, Offset(0f, y), Offset(size.width, y), strokeWidth = Dp.Hairline.toPx().coerceAtLeast(1f))
  }
  val style = TextStyle(color = colors.text, fontSize = LabelSize)
  val suffix = unit?.let { " $it" }.orEmpty()
  drawText(measurer, formatNumber(range.high) + suffix, Offset(0f, padding), style)
  val low = measurer.measure(formatNumber(range.low) + suffix, style)
  drawText(low, topLeft = Offset(0f, padding + plotHeight - low.size.height))
}

private fun DrawScope.drawLimits(limits: List<LimitLine>, colors: ChartColors, measurer: TextMeasurer, padding: Float, plotHeight: Float) {
  limits.forEach { limit ->
    val y = padding + (1 - limit.fraction) * plotHeight
    val color = if (limit.isAlarm()) colors.alarm else colors.limit
    drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = LimitWidth.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
    val text = measurer.measure(limit.label, TextStyle(color = color, fontSize = LabelSize))
    // The words sit over the line, at the right; the limit that is the upper one of two has them over it and the other one under it, so they do not overlap.
    val above = limit.kind != LimitKind.DehumidifierOff
    drawText(text, topLeft = Offset(size.width - text.size.width, if (above) y - text.size.height else y))
  }
}

/**
 * A line chart, drawn by hand: one line, in the [line] color (the color of the app by default), over three lines of grid with the numbers of the bottom and the
 * top of its range. Each stretch the app heard is a line of its own, so the time it heard nothing stays empty, and a stretch of a single reading is a dot.
 * The [limits] are dashed lines over it with their words; what the line does above the limit of an alarm is drawn in the color of an alert; the space between
 * the two limits of the dehumidifier is shaded; and with [fillBelow] the space under the line is washed with its color. [description] is what a screen reader says.
 */
@Composable
internal fun LineChart(
  chart: Chart,
  description: String,
  modifier: Modifier = Modifier,
  line: Color = MaterialTheme.colorScheme.primary,
  height: Dp = ChartHeight,
  limits: List<LimitLine> = emptyList(),
  fillBelow: Boolean = false,
  unit: String? = null,
) {
  val colors = chartColors(line)
  val measurer = rememberTextMeasurer()
  Canvas(modifier = modifier.fillMaxWidth().height(height).semantics { contentDescription = description }) {
    val padding = PlotPadding.toPx()
    val plotHeight = size.height - 2 * padding
    fun ChartPoint.place() = Offset(x * size.width, padding + (1 - y) * plotHeight)
    drawGrid(chart.range, colors, measurer, unit, padding, plotHeight)
    // The shade between what the dehumidifier turns on at and off at.
    val on = limits.firstOrNull { it.kind == LimitKind.DehumidifierOn }
    val off = limits.firstOrNull { it.kind == LimitKind.DehumidifierOff }
    if (on != null && off != null) {
      val top = padding + (1 - maxOf(on.fraction, off.fraction)) * plotHeight
      val bottom = padding + (1 - minOf(on.fraction, off.fraction)) * plotHeight
      drawRect(colors.limit.copy(alpha = BAND_ALPHA), Offset(0f, top), Size(size.width, bottom - top))
    }
    chart.lines.forEach { points ->
      if (fillBelow && points.size > 1) {
        val area = Path()
        area.moveTo(points.first().place().x, padding + plotHeight)
        points.forEach { point -> point.place().let { area.lineTo(it.x, it.y) } }
        area.lineTo(points.last().place().x, padding + plotHeight)
        area.close()
        drawPath(area, colors.line.copy(alpha = FILL_ALPHA))
      }
    }
    drawLimits(limits, colors, measurer, padding, plotHeight)
    val alarmY = limits.firstOrNull { it.isAlarm() }?.let { padding + (1 - it.fraction) * plotHeight }
    fun DrawScope.strokes(color: Color) {
      chart.lines.forEach { points ->
        if (points.size == 1) {
          drawCircle(color, DotRadius.toPx(), center = points.single().place())
        } else {
          val path = Path()
          points.forEachIndexed { index, point -> point.place().let { if (index == 0) path.moveTo(it.x, it.y) else path.lineTo(it.x, it.y) } }
          drawPath(path, color, style = Stroke(width = LineWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
      }
    }
    strokes(colors.line)
    // What is over the limit of an alarm is drawn again, in the color of an alert, clipped to the part of the chart above the limit.
    if (alarmY != null) clipRect(left = 0f, top = 0f, right = size.width, bottom = alarmY) { strokes(colors.alarm) }
  }
}

/**
 * The chart of a month or a year of a sensor drawn by the day: a bar for each day, as tall as the greatest reading of that day, over a grid with the numbers
 * of the bottom and the top of its range and the [limits] as dashed lines. A bar over the limit of an alarm is in the color of an alert.
 */
@Composable
internal fun BarChart(
  bars: Bars,
  description: String,
  modifier: Modifier = Modifier,
  bar: Color = MaterialTheme.colorScheme.primary,
  height: Dp = ChartHeight,
  limits: List<LimitLine> = emptyList(),
  limitValue: Double? = null,
  unit: String? = null,
) {
  val colors = chartColors(bar)
  val measurer = rememberTextMeasurer()
  Canvas(modifier = modifier.fillMaxWidth().height(height).semantics { contentDescription = description }) {
    val padding = PlotPadding.toPx()
    val plotHeight = size.height - 2 * padding
    drawGrid(bars.range, colors, measurer, unit, padding, plotHeight)
    val gap = 1.dp.toPx()
    bars.bars.forEach { day ->
      val top = padding + (1 - bars.range.fractionOf(day.value)) * plotHeight
      val color = if (limitValue != null && day.value > limitValue) colors.alarm else colors.line
      val left = day.from * size.width + gap / 2
      val width = ((day.to - day.from) * size.width - gap).coerceAtLeast(1f)
      drawRect(color, Offset(left, top), Size(width, padding + plotHeight - top))
    }
    drawLimits(limits, colors, measurer, padding, plotHeight)
  }
}
