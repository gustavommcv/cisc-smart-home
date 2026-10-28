package io.github.gustavommcv.ciscsmarthome.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.ui.theme.Spacing

/** How faded the red line of a locked place that takes something away is: it does not work yet, so it is not as loud as the red of a warning. */
private const val FADED_RED = 0.5f

/** The title of a faded red place is red too, a little less faded, so it can be read. */
private const val FADED_TEXT = 0.85f

/** The color of the dashed line of a locked place: the grey of the palette, or, for [Tone.Alert], the red of the palette, faded. */
internal fun Tone.lockedLine(scheme: ColorScheme): Color = if (this == Tone.Alert) scheme.error.copy(alpha = FADED_RED) else scheme.outline

/** The color of the title of a locked place: red for [Tone.Alert], less faded than its line, so it can be read; the color of the text for the others. */
internal fun Tone.lockedTitle(scheme: ColorScheme): Color = if (this == Tone.Alert) scheme.error.copy(alpha = FADED_TEXT) else Color.Unspecified

/**
 * Something the app will do and does not yet: shown, so it is clear where it goes, and drawn as an empty place, so it is clear it does not work. What only adds (a room, a
 * device) is grey; what takes something away (delete a room) is [Tone.Alert], a faded red, so it is not taken for the ones that add.
 */
@Composable
internal fun LockedCard(@StringRes title: Int, modifier: Modifier = Modifier, tone: Tone = Tone.Neutral) {
  Row(
    modifier = modifier.fillMaxWidth().dashedBorder(tone.lockedLine(MaterialTheme.colorScheme), MaterialTheme.shapes.large).padding(horizontal = Spacing.L, vertical = Spacing.M),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(Spacing.M),
  ) {
    IconBadge(R.drawable.ic_lock, tone)
    Column {
      Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = tone.lockedTitle(MaterialTheme.colorScheme))
      Text(stringResource(R.string.add_soon), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}
