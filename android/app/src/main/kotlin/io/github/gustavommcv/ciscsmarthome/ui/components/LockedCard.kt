package io.github.gustavommcv.ciscsmarthome.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.ui.theme.Spacing

/** Something the app will do and does not yet: shown, so it is clear where it goes, and drawn as an empty place, so it is clear it does not work. */
@Composable
internal fun LockedCard(@StringRes title: Int, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier.fillMaxWidth().dashedBorder(MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large).padding(horizontal = Spacing.L, vertical = Spacing.M),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(Spacing.M),
  ) {
    IconBadge(R.drawable.ic_lock, Tone.Neutral)
    Column {
      Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
      Text(stringResource(R.string.add_soon), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}
