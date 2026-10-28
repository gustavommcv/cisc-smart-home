package io.github.gustavommcv.ciscsmarthome.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.gustavommcv.ciscsmarthome.ui.theme.Spacing

private val DotSize = 8.dp

/** A short word about a state, with a dot of the same tone: how things are, at a glance. */
@Composable
internal fun StatusPill(text: String, tone: Tone, modifier: Modifier = Modifier) {
  val colors = tone.colors()
  Row(
    modifier = modifier.clip(CircleShape).background(colors.container).padding(horizontal = Spacing.M, vertical = Spacing.Xs),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(Spacing.S),
  ) {
    Box(modifier = Modifier.size(DotSize).background(colors.accent, CircleShape))
    Text(text, color = colors.content, style = MaterialTheme.typography.labelLarge)
  }
}
