package io.github.gustavommcv.ciscsmarthome.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.gustavommcv.ciscsmarthome.ui.theme.Spacing

/**
 * The card of the app: a white surface with a hairline around it, or, with a [tone], the container of that tone
 * (a room in emergency is an alert card). With [onClick] the whole card is the target of the touch.
 */
@Composable
internal fun CiscCard(modifier: Modifier = Modifier, tone: Tone? = null, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
  val scheme = MaterialTheme.colorScheme
  val toned = tone?.colors()
  val container = toned?.container ?: scheme.surface
  val colors = CardDefaults.cardColors(containerColor = container, contentColor = toned?.content ?: scheme.onSurface)
  val border = BorderStroke(Dp.Hairline, if (toned != null) container else scheme.outlineVariant)
  val inside: @Composable ColumnScope.() -> Unit = { Column(modifier = Modifier.padding(horizontal = Spacing.L, vertical = Spacing.M), content = content) }
  if (onClick != null) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = colors, border = border, content = inside)
  } else {
    Card(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = colors, border = border, content = inside)
  }
}
