package io.github.gustavommcv.ciscsmarthome.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/** Something the app will do and does not yet: shown, so it is clear where it goes, and locked, so it is clear it does not work. */
@Composable
internal fun LockedCard(@StringRes title: Int, modifier: Modifier = Modifier) {
  OutlinedCard(modifier = modifier.fillMaxWidth().alpha(0.6f)) {
    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
      Icon(Icons.Filled.Lock, contentDescription = null)
      Column {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(io.github.gustavommcv.ciscsmarthome.R.string.add_soon), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}
