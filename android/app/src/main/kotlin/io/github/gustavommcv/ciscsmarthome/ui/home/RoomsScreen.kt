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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.Area
import io.github.gustavommcv.ciscsmarthome.domain.AreaId
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionState
import io.github.gustavommcv.ciscsmarthome.domain.Home

/** The first screen: a card for each room, with what matters about it, and the cards of what is to come. */
@Composable
internal fun RoomsScreen(home: Home, connection: ConnectionState, onOpenRoom: (AreaId) -> Unit, modifier: Modifier = Modifier) {
  val lastReading = connection is ConnectionState.Disconnected
  LazyColumn(
    modifier = modifier.fillMaxSize().safeDrawingPadding(),
    contentPadding = PaddingValues(24.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineLarge)
      Text(stringResource(R.string.home_subtitle), style = MaterialTheme.typography.titleMedium)
      ConnectionBanner(connection, showingLastReading = true, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
    }
    items(home.areas, key = { it.id.value }) { area ->
      RoomCard(area, inEmergency = home.isInEmergency(area), dimmed = lastReading, onClick = { onOpenRoom(area.id) })
    }
    item { LockedCard(R.string.rooms_add) }
  }
}

@Composable
private fun RoomCard(area: Area, inEmergency: Boolean, dimmed: Boolean, onClick: () -> Unit) {
  val colors =
    if (inEmergency) {
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
    } else {
      CardDefaults.cardColors()
    }
  Card(onClick = onClick, colors = colors, modifier = Modifier.fillMaxWidth().alpha(if (dimmed) 0.6f else 1f)) {
    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (inEmergency) Icon(Icons.Filled.Warning, contentDescription = null)
        Text(roomName(area), style = MaterialTheme.typography.titleLarge)
      }
      val highlights = area.highlights()
      if (highlights.isEmpty()) Text(stringResource(R.string.room_empty), style = MaterialTheme.typography.bodyMedium)
      highlights.forEach { entity ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(entityLabel(entity), style = MaterialTheme.typography.bodyLarge)
          Text(entityValue(entity), style = MaterialTheme.typography.bodyLarge)
        }
      }
    }
  }
}
