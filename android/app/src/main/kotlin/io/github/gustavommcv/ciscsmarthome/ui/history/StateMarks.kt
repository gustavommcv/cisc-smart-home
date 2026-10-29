package io.github.gustavommcv.ciscsmarthome.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.StateKind
import io.github.gustavommcv.ciscsmarthome.ui.theme.Spacing

private val StripHeight = 6.dp
private val MarkSwatch = 10.dp

// A span of a single sample has no width of its own: it is given this one, so it can be seen.
internal val LeastSpanWidth = 2.dp

private const val TRACK_ALPHA = 0.5f

/**
 * A strip under a chart that is lit where something was on (the dehumidifier), at the same places of the window as the chart above it. The time it was off is
 * a quiet track, so the strip is also readable when it is never lit.
 */
@Composable
internal fun StateStrip(spans: List<StateSpan>, color: Color, modifier: Modifier = Modifier) {
  val track = MaterialTheme.colorScheme.outlineVariant.copy(alpha = TRACK_ALPHA)
  Canvas(modifier = modifier.fillMaxWidth().height(StripHeight)) {
    drawRect(track, Offset.Zero, size)
    val least = LeastSpanWidth.toPx()
    spans.forEach { span ->
      val width = maxOf((span.to - span.from) * size.width, least)
      drawRect(color, Offset((span.from * size.width).coerceAtMost(size.width - width), 0f), Size(width, size.height))
    }
  }
}

/** The words that say what a mark or a strip is, with the color it is drawn in: a swatch and the text, which tell it apart without the color alone. */
@Composable
internal fun StateCaption(text: String, color: Color, modifier: Modifier = Modifier) {
  Row(modifier = modifier.padding(top = Spacing.Xs), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.S)) {
    Box(Modifier.size(MarkSwatch).background(color, CircleShape))
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

/** The words for a kind of state. */
@Composable
internal fun StateKind.caption(): String =
  stringResource(
    when (this) {
      StateKind.GasEmergency -> R.string.history_state_gas_emergency
      StateKind.DehumidifierRunning -> R.string.history_state_dehumidifier_running
    },
  )
