package io.github.gustavommcv.ciscsmarthome.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.Area
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionState
import io.github.gustavommcv.ciscsmarthome.domain.Entity

/** One room: its devices and what each reads. The controls and the history chart come to it next. */
@Composable
internal fun RoomScreen(area: Area, connection: ConnectionState, onBack: () -> Unit, modifier: Modifier = Modifier) {
  Column(modifier = modifier.fillMaxSize().safeDrawingPadding()) {
    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) }
      Text(roomName(area), style = MaterialTheme.typography.headlineSmall)
    }
    LazyColumn(contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      item { ConnectionBanner(connection, showingLastReading = true, modifier = Modifier.padding(bottom = 8.dp)) }
      if (area.devices.isEmpty()) item { Text(stringResource(R.string.room_empty), style = MaterialTheme.typography.bodyMedium) }
      area.devices.forEach { device ->
        item { Text(device.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 16.dp)) }
        items(device.entities, key = { it.id.value }) { entity -> EntityRow(entity) }
      }
      item { LockedCard(R.string.device_add, modifier = Modifier.padding(top = 16.dp)) }
    }
  }
}

@Composable
private fun EntityRow(entity: Entity) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(entityLabel(entity), style = MaterialTheme.typography.bodyLarge)
    Text(entityValue(entity), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}
